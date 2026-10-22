package com.example.server

import android.content.Context
import com.example.model.ClientInfo
import com.example.model.ServerStatus
import com.example.model.ShareMode
import com.example.util.NetworkUtils
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class FtpServer(
    private val context: Context,
    val port: Int,
    val shareMode: ShareMode,
    val isReadOnly: Boolean,
    private val onStatusChanged: (ServerStatus, String?) -> Unit,
    private val onClientsChanged: (List<ClientInfo>) -> Unit
) {

    @Volatile
    private var isRunning = false

    @Volatile
    var isPaused: Boolean = false
        private set

    private var serverSocket: ServerSocket? = null
    private val activeSessions = ConcurrentHashMap<String, FtpSession>()
    private val clientInfoMap = ConcurrentHashMap<String, ClientInfo>()
    private val threadPool = Executors.newCachedThreadPool()
    private var listenerThread: Thread? = null

    var boundIp: String? = null
        private set

    fun start(): Boolean {
        if (isRunning) return true

        val ip = NetworkUtils.getLocalIpv4Address()
        if (ip == null) {
            onStatusChanged(ServerStatus.ERROR, "No active Wi-Fi or local network detected.")
            return false
        }
        boundIp = ip

        try {
            val bindAddress = InetAddress.getByName(ip)
            serverSocket = ServerSocket(port, 50, bindAddress)
        } catch (e: Exception) {
            try {
                // Fallback: bind wildcard
                serverSocket = ServerSocket(port, 50)
            } catch (e2: Exception) {
                onStatusChanged(ServerStatus.ERROR, "Could not bind to port $port: ${e2.localizedMessage}")
                return false
            }
        }

        isRunning = true
        isPaused = false
        onStatusChanged(ServerStatus.RUNNING, null)

        val vfs = VirtualFileSystem(context, shareMode, isReadOnly)

        listenerThread = Thread({
            while (isRunning && serverSocket?.isClosed == false) {
                try {
                    val clientSocket = serverSocket?.accept() ?: break
                    clientSocket.soTimeout = 0 // Keep control connection open

                    val session = FtpSession(
                        context = context,
                        socket = clientSocket,
                        vfs = vfs,
                        serverIp = boundIp ?: ip,
                        isReadOnly = isReadOnly,
                        isPausedProvider = { isPaused },
                        onClientUpdated = { info ->
                            clientInfoMap[info.id] = info
                            notifyClients()
                        },
                        onSessionClosed = { s ->
                            activeSessions.remove(s.id)
                            clientInfoMap.remove(s.id)
                            notifyClients()
                        }
                    )

                    activeSessions[session.id] = session
                    threadPool.execute(session)
                } catch (_: Exception) {
                    if (!isRunning) break
                }
            }
        }, "FtpServer-Listener").apply {
            isDaemon = true
            start()
        }

        return true
    }

    fun pause() {
        if (isRunning && !isPaused) {
            isPaused = true
            onStatusChanged(ServerStatus.PAUSED, null)
        }
    }

    fun resume() {
        if (isRunning && isPaused) {
            isPaused = false
            onStatusChanged(ServerStatus.RUNNING, null)
        }
    }

    fun disconnectClient(id: String) {
        val session = activeSessions[id]
        session?.close()
    }

    fun disconnectClientsWithIp(ip: String) {
        activeSessions.values.filter { it.clientIp == ip }.forEach { it.close() }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        isPaused = false

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        activeSessions.values.forEach { it.close() }
        activeSessions.clear()
        clientInfoMap.clear()
        notifyClients()

        threadPool.shutdownNow()
        listenerThread?.interrupt()
        listenerThread = null

        onStatusChanged(ServerStatus.STOPPED, null)
    }

    private fun notifyClients() {
        onClientsChanged(clientInfoMap.values.toList().sortedByDescending { it.connectedAt })
    }
}
