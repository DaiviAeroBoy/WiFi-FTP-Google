package com.example.server

import android.content.Context
import android.media.MediaScannerConnection
import com.example.model.ClientInfo
import com.example.model.TransferDirection
import com.example.util.WakeLockManager
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FtpSession(
    private val context: Context,
    val socket: Socket,
    private val vfs: VirtualFileSystem,
    val serverIp: String,
    private val isReadOnly: Boolean,
    private val isPausedProvider: () -> Boolean,
    private val onClientUpdated: (ClientInfo) -> Unit,
    private val onSessionClosed: (FtpSession) -> Unit
) : Runnable {

    val id: String = "${socket.inetAddress.hostAddress}:${socket.port}"
    val clientIp: String = socket.inetAddress.hostAddress ?: "Unknown"
    val hostName: String = socket.inetAddress.hostName ?: clientIp
    val connectedAt: Long = System.currentTimeMillis()

    @Volatile
    var currentActivity: String = "Idle"
        private set

    @Volatile
    var totalBytesTransferred: Long = 0L
        private set

    @Volatile
    private var isRunning = true

    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    private var currentDir = "/"
    private var passiveServerSocket: ServerSocket? = null
    private var activeDataAddress: InetAddress? = null
    private var activeDataPort: Int = 0
    private var restartOffset: Long = 0L
    private var renameFromPath: String? = null

    private val unixDateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
    private val unixOldDateFormat = SimpleDateFormat("MMM dd  yyyy", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
    private val mlsdDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun run() {
        try {
            reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

            // 1. IP Block check
            if (FtpServerManager.isIpBlocked(clientIp)) {
                sendResponse(421, "Access denied: IP address blocked by server administrator.")
                close()
                return
            }

            // 2. Allow / Block popup check for new connections
            val isAllowed = runBlocking {
                FtpServerManager.verifyClientConnection(clientIp, hostName)
            }

            if (!isAllowed) {
                sendResponse(421, "Connection declined by device owner.")
                close()
                return
            }

            updateInfo("Connected")
            sendResponse(220, "Android WiFi FTP Server Ready.")

            while (isRunning && !socket.isClosed) {
                val line = reader?.readLine() ?: break
                handleCommand(line.trim())
            }
        } catch (_: Exception) {
        } finally {
            close()
        }
    }

    private fun handleCommand(fullCommand: String) {
        if (fullCommand.isEmpty()) return

        val spaceIdx = fullCommand.indexOf(' ')
        val cmd = if (spaceIdx != -1) fullCommand.substring(0, spaceIdx).uppercase(Locale.US) else fullCommand.uppercase(Locale.US)
        val arg = if (spaceIdx != -1) fullCommand.substring(spaceIdx + 1).trim() else ""

        // Check if server is paused
        if (isPausedProvider() && cmd !in listOf("QUIT", "NOOP", "USER", "PASS")) {
            sendResponse(421, "Server is currently paused. Please try again later.")
            return
        }

        when (cmd) {
            "USER" -> sendResponse(230, "User logged in, proceed.")
            "PASS" -> sendResponse(230, "User logged in, proceed.")
            "SYST" -> sendResponse(215, "UNIX Type: L8")
            "FEAT" -> {
                sendRaw("211-Features:\r\n UTF8\r\n SIZE\r\n MLSD\r\n REST STREAM\r\n211 End\r\n")
            }
            "OPTS" -> {
                if (arg.uppercase(Locale.US).startsWith("UTF8")) {
                    sendResponse(200, "Always in UTF8 mode.")
                } else {
                    sendResponse(200, "OPTS OK.")
                }
            }
            "PWD", "XPWD" -> {
                sendResponse(257, "\"$currentDir\" is current directory.")
            }
            "TYPE" -> {
                when (arg.uppercase(Locale.US)) {
                    "A" -> sendResponse(200, "Type set to A (ASCII).")
                    "I" -> sendResponse(200, "Type set to I (Binary).")
                    else -> sendResponse(200, "Type set to $arg.")
                }
            }
            "PASV" -> handlePasv()
            "EPSV" -> handleEpsv()
            "PORT" -> handlePort(arg)
            "CWD", "XCWD" -> handleCwd(arg)
            "CDUP", "XCUP" -> handleCdup()
            "LIST" -> handleList(arg, isMlsd = false, isNameOnly = false)
            "NLST" -> handleList(arg, isMlsd = false, isNameOnly = true)
            "MLSD" -> handleList(arg, isMlsd = true, isNameOnly = false)
            "MLST" -> handleMlst(arg)
            "SIZE" -> handleSize(arg)
            "REST" -> {
                val offset = arg.toLongOrNull()
                if (offset != null && offset >= 0) {
                    restartOffset = offset
                    sendResponse(350, "Restarting at $restartOffset. Send STORE or RETRIEVE.")
                } else {
                    sendResponse(501, "Invalid offset.")
                }
            }
            "RETR" -> handleRetr(arg)
            "STOR" -> handleStor(arg, append = false)
            "APPE" -> handleStor(arg, append = true)
            "DELE" -> handleDele(arg)
            "MKD", "XMKD" -> handleMkd(arg)
            "RMD", "XRMD" -> handleRmd(arg)
            "RNFR" -> {
                renameFromPath = arg
                sendResponse(350, "File exists, ready for destination name.")
            }
            "RNTO" -> handleRnto(arg)
            "NOOP" -> sendResponse(200, "NOOP ok.")
            "ABOR" -> sendResponse(226, "ABOR successful.")
            "QUIT" -> {
                sendResponse(221, "Goodbye.")
                close()
            }
            else -> sendResponse(502, "Command not implemented.")
        }
    }

    private fun handlePasv() {
        try {
            passiveServerSocket?.close()
            val pasvServer = ServerSocket(0, 1, socket.localAddress)
            passiveServerSocket = pasvServer

            val port = pasvServer.localPort
            val p1 = port / 256
            val p2 = port % 256

            val ipParts = serverIp.split('.').map { it.toIntOrNull() ?: 0 }
            val formattedIp = if (ipParts.size == 4) ipParts.joinToString(",") else "127,0,0,1"

            sendResponse(227, "Entering Passive Mode ($formattedIp,$p1,$p2).")
        } catch (_: Exception) {
            sendResponse(425, "Cannot open passive connection.")
        }
    }

    private fun handleEpsv() {
        try {
            passiveServerSocket?.close()
            val pasvServer = ServerSocket(0, 1, socket.localAddress)
            passiveServerSocket = pasvServer
            sendResponse(229, "Entering Extended Passive Mode (|||${pasvServer.localPort}|).")
        } catch (_: Exception) {
            sendResponse(425, "Cannot open extended passive connection.")
        }
    }

    private fun handlePort(arg: String) {
        try {
            val parts = arg.split(',')
            if (parts.size == 6) {
                val ip = "${parts[0]}.${parts[1]}.${parts[2]}.${parts[3]}"
                val port = (parts[4].toInt() shl 8) or parts[5].toInt()
                activeDataAddress = InetAddress.getByName(ip)
                activeDataPort = port
                sendResponse(200, "PORT command successful.")
            } else {
                sendResponse(501, "Illegal PORT command syntax.")
            }
        } catch (_: Exception) {
            sendResponse(501, "Cannot parse PORT arguments.")
        }
    }

    private fun openDataSocket(): Socket? {
        val pasv = passiveServerSocket
        if (pasv != null) {
            try {
                pasv.soTimeout = 15000
                val dataSocket = pasv.accept()
                pasv.close()
                passiveServerSocket = null
                return dataSocket
            } catch (_: Exception) {
                pasv.close()
                passiveServerSocket = null
                return null
            }
        }

        val addr = activeDataAddress
        if (addr != null && activeDataPort > 0) {
            return try {
                Socket(addr, activeDataPort)
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    private fun handleCwd(arg: String) {
        val target = vfs.resolveFile(currentDir, arg)
        if (target != null && target.isDirectory) {
            currentDir = vfs.normalizePath(currentDir, arg)
            sendResponse(250, "Directory changed to \"$currentDir\".")
        } else {
            sendResponse(550, "Directory not found.")
        }
    }

    private fun handleCdup() {
        currentDir = vfs.normalizePath(currentDir, "..")
        sendResponse(250, "Directory changed to \"$currentDir\".")
    }

    private fun handleSize(arg: String) {
        val file = vfs.resolveFile(currentDir, arg)
        if (file != null && !file.isDirectory) {
            sendResponse(213, file.size.toString())
        } else {
            sendResponse(550, "Could not get file size.")
        }
    }

    private fun handleList(arg: String, isMlsd: Boolean, isNameOnly: Boolean) {
        val path = if (arg.isEmpty() || arg.startsWith("-")) currentDir else arg
        val dir = vfs.resolveFile(currentDir, path)

        if (dir == null) {
            sendResponse(550, "File or directory not found.")
            return
        }

        val dataSocket = openDataSocket()
        if (dataSocket == null) {
            sendResponse(425, "Unable to build data connection.")
            return
        }

        sendResponse(150, "Here comes the directory listing.")
        updateInfo("Listing $currentDir")

        try {
            val out = BufferedWriter(OutputStreamWriter(dataSocket.getOutputStream(), Charsets.UTF_8))
            val items = if (dir.isDirectory) dir.listFiles() else listOf(dir)

            val sixMonthsAgo = System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000

            for (item in items) {
                val line = when {
                    isMlsd -> {
                        val typeStr = if (item.isDirectory) "dir" else "file"
                        val modifyStr = mlsdDateFormat.format(Date(item.lastModified))
                        val permStr = if (item.isDirectory) "el" else if (isReadOnly) "r" else "adfrw"
                        "type=$typeStr;size=${item.size};modify=$modifyStr;perm=$permStr; ${item.name}\r\n"
                    }
                    isNameOnly -> {
                        "${item.name}\r\n"
                    }
                    else -> {
                        val perm = if (item.isDirectory) "drwxr-xr-x" else "-rw-r--r--"
                        val links = 1
                        val owner = "owner"
                        val group = "group"
                        val size = item.size
                        val date = Date(item.lastModified)
                        val dateStr = if (item.lastModified < sixMonthsAgo) {
                            unixOldDateFormat.format(date)
                        } else {
                            unixDateFormat.format(date)
                        }
                        "$perm $links $owner $group $size $dateStr ${item.name}\r\n"
                    }
                }
                out.write(line)
            }
            out.flush()
            dataSocket.close()
            sendResponse(226, "Directory send OK.")
        } catch (_: Exception) {
            sendResponse(426, "Connection closed; transfer aborted.")
        } finally {
            try { dataSocket.close() } catch (_: Exception) {}
            updateInfo("Idle")
        }
    }

    private fun handleMlst(arg: String) {
        val path = if (arg.isEmpty()) currentDir else arg
        val item = vfs.resolveFile(currentDir, path)
        if (item == null) {
            sendResponse(550, "File not found.")
            return
        }
        val typeStr = if (item.isDirectory) "dir" else "file"
        val modifyStr = mlsdDateFormat.format(Date(item.lastModified))
        val permStr = if (item.isDirectory) "el" else if (isReadOnly) "r" else "adfrw"
        val fact = "type=$typeStr;size=${item.size};modify=$modifyStr;perm=$permStr; ${item.name}"
        sendRaw("250-Listing\r\n $fact\r\n250 End\r\n")
    }

    private fun handleRetr(arg: String) {
        val file = vfs.resolveFile(currentDir, arg)
        if (file == null || file.isDirectory) {
            sendResponse(550, "File not found or is a directory.")
            return
        }

        val dataSocket = openDataSocket()
        if (dataSocket == null) {
            sendResponse(425, "Unable to build data connection.")
            return
        }

        sendResponse(150, "Opening BINARY mode data connection for ${file.name} (${file.size} bytes).")
        updateInfo("Downloading: ${file.name}")

        val offset = restartOffset
        restartOffset = 0L

        // Acquire CPU wake lock during download
        WakeLockManager.onTransferStarted(context)

        var lastTickTime = System.currentTimeMillis()
        var lastTickBytes = totalBytesTransferred

        try {
            val input = file.getInputStream(offset)
            val output = dataSocket.getOutputStream()
            val buffer = ByteArray(65536)
            var read = 0
            while (isRunning && input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                totalBytesTransferred += read

                val now = System.currentTimeMillis()
                val elapsed = now - lastTickTime
                if (elapsed >= 500) {
                    val deltaBytes = totalBytesTransferred - lastTickBytes
                    val speed = (deltaBytes * 1000L) / elapsed.coerceAtLeast(1)
                    FtpServerManager.updateTransferProgress(file.name, TransferDirection.DOWNLOADING, speed, 0L, totalBytesTransferred)
                    lastTickTime = now
                    lastTickBytes = totalBytesTransferred
                }
            }
            output.flush()
            input.close()
            dataSocket.close()
            sendResponse(226, "Transfer complete.")
        } catch (_: Exception) {
            sendResponse(426, "Transfer aborted.")
        } finally {
            WakeLockManager.onTransferFinished()
            FtpServerManager.onTransferFinished()
            try { dataSocket.close() } catch (_: Exception) {}
            updateInfo("Idle")
        }
    }

    private fun handleStor(arg: String, append: Boolean) {
        if (isReadOnly) {
            sendResponse(550, "Access denied: Server is in Read-Only mode.")
            return
        }

        val target = vfs.resolveFile(currentDir, arg)
        if (target == null) {
            sendResponse(550, "Cannot write to this location.")
            return
        }

        val dataSocket = openDataSocket()
        if (dataSocket == null) {
            sendResponse(425, "Unable to build data connection.")
            return
        }

        sendResponse(150, "Ok to send data.")
        updateInfo("Uploading: ${target.name}")

        // Acquire CPU wake lock during upload
        WakeLockManager.onTransferStarted(context)

        var lastTickTime = System.currentTimeMillis()
        var lastTickBytes = totalBytesTransferred

        try {
            val input = dataSocket.getInputStream()
            val output = target.getOutputStream(append)
            val buffer = ByteArray(65536)
            var read = 0
            while (isRunning && input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                totalBytesTransferred += read

                val now = System.currentTimeMillis()
                val elapsed = now - lastTickTime
                if (elapsed >= 500) {
                    val deltaBytes = totalBytesTransferred - lastTickBytes
                    val speed = (deltaBytes * 1000L) / elapsed.coerceAtLeast(1)
                    FtpServerManager.updateTransferProgress(target.name, TransferDirection.UPLOADING, 0L, speed, totalBytesTransferred)
                    lastTickTime = now
                    lastTickBytes = totalBytesTransferred
                }
            }
            output.flush()
            output.close()
            dataSocket.close()

            // Trigger MediaScanner so new file is instantly visible in system gallery & files app
            val underlying = target.getUnderlyingFile()
            if (underlying != null) {
                try {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(underlying.absolutePath),
                        null,
                        null
                    )
                } catch (_: Exception) {
                }
            }

            sendResponse(226, "Transfer complete.")
        } catch (_: Exception) {
            sendResponse(426, "Upload aborted or storage full.")
        } finally {
            WakeLockManager.onTransferFinished()
            FtpServerManager.onTransferFinished()
            try { dataSocket.close() } catch (_: Exception) {}
            updateInfo("Idle")
        }
    }

    private fun handleDele(arg: String) {
        if (isReadOnly) {
            sendResponse(550, "Access denied: Server is in Read-Only mode.")
            return
        }
        val file = vfs.resolveFile(currentDir, arg)
        if (file != null && !file.isDirectory && file.delete()) {
            sendResponse(250, "File deleted successfully.")
        } else {
            sendResponse(550, "Could not delete file.")
        }
    }

    private fun handleMkd(arg: String) {
        if (isReadOnly) {
            sendResponse(550, "Access denied: Server is in Read-Only mode.")
            return
        }
        val dir = vfs.resolveFile(currentDir, arg)
        if (dir != null && dir.mkdir()) {
            sendResponse(257, "\"$arg\" created.")
        } else {
            sendResponse(550, "Could not create directory.")
        }
    }

    private fun handleRmd(arg: String) {
        if (isReadOnly) {
            sendResponse(550, "Access denied: Server is in Read-Only mode.")
            return
        }
        val dir = vfs.resolveFile(currentDir, arg)
        if (dir != null && dir.isDirectory && dir.delete()) {
            sendResponse(250, "Directory removed.")
        } else {
            sendResponse(550, "Could not remove directory.")
        }
    }

    private fun handleRnto(arg: String) {
        if (isReadOnly) {
            sendResponse(550, "Access denied: Server is in Read-Only mode.")
            return
        }
        val fromPath = renameFromPath
        renameFromPath = null
        if (fromPath == null) {
            sendResponse(503, "Bad sequence of commands: RNFR required first.")
            return
        }

        val src = vfs.resolveFile(currentDir, fromPath)
        val dst = vfs.resolveFile(currentDir, arg)

        val srcFile = src?.getUnderlyingFile()
        val dstFile = dst?.getUnderlyingFile()

        if (srcFile != null && dstFile != null && src.renameTo(dstFile)) {
            sendResponse(250, "Rename successful.")
        } else {
            sendResponse(550, "Rename failed.")
        }
    }

    private fun sendResponse(code: Int, message: String) {
        sendRaw("$code $message\r\n")
    }

    private fun sendRaw(raw: String) {
        try {
            writer?.write(raw)
            writer?.flush()
        } catch (_: Exception) {
            close()
        }
    }

    private fun updateInfo(activity: String) {
        currentActivity = activity
        onClientUpdated(
            ClientInfo(
                id = id,
                ipAddress = clientIp,
                hostName = hostName,
                connectedAt = connectedAt,
                currentActivity = currentActivity,
                bytesTransferred = totalBytesTransferred
            )
        )
    }

    fun close() {
        isRunning = false
        try { passiveServerSocket?.close() } catch (_: Exception) {}
        try { socket.close() } catch (_: Exception) {}
        onSessionClosed(this)
    }
}
