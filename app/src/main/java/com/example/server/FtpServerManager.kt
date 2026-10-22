package com.example.server

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.model.ClientInfo
import com.example.model.ConnectionRequest
import com.example.model.ServerState
import com.example.model.ServerStatus
import com.example.model.ShareMode
import com.example.model.TransferDirection
import com.example.model.TransferTelemetry
import com.example.service.FtpForegroundService
import com.example.util.NetworkUtils
import com.example.util.WakeLockManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

object FtpServerManager {

    private val _serverState = MutableStateFlow(ServerState())
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private var activeServer: FtpServer? = null
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    fun refreshNetwork(context: Context) {
        val ip = NetworkUtils.getLocalIpv4Address()
        _serverState.update { current ->
            current.copy(hostAddress = ip)
        }
    }

    fun setPort(port: Int) {
        if (_serverState.value.isRunning) return
        _serverState.update { it.copy(port = port) }
    }

    fun setShareMode(shareMode: ShareMode) {
        if (_serverState.value.isRunning) return
        _serverState.update { it.copy(shareMode = shareMode) }
    }

    fun setReadOnly(isReadOnly: Boolean) {
        if (_serverState.value.isRunning) return
        _serverState.update { it.copy(isReadOnly = isReadOnly) }
    }

    fun isIpBlocked(ip: String): Boolean {
        return _serverState.value.blockedIps.contains(ip)
    }

    suspend fun verifyClientConnection(clientIp: String, hostName: String): Boolean {
        // 1. Check if explicitly blocked
        if (_serverState.value.blockedIps.contains(clientIp)) {
            return false
        }

        // 2. Check if explicitly allowed already
        if (_serverState.value.allowedIps.contains(clientIp)) {
            return true
        }

        // 3. New connection: trigger connection request dialog in UI
        val requestId = "${clientIp}_${System.currentTimeMillis()}"
        val deferred = CompletableDeferred<Boolean>()
        pendingRequests[requestId] = deferred

        _serverState.update { current ->
            current.copy(
                pendingConnectionRequest = ConnectionRequest(
                    requestId = requestId,
                    clientIp = clientIp,
                    hostName = hostName
                )
            )
        }

        // Wait up to 30 seconds for user action
        val allowed = withTimeoutOrNull(30_000L) {
            deferred.await()
        } ?: false

        pendingRequests.remove(requestId)
        _serverState.update { current ->
            if (current.pendingConnectionRequest?.requestId == requestId) {
                current.copy(pendingConnectionRequest = null)
            } else {
                current
            }
        }

        return allowed
    }

    fun handleConnectionDecision(requestId: String, allow: Boolean, remember: Boolean) {
        val request = _serverState.value.pendingConnectionRequest
        val deferred = pendingRequests.remove(requestId)

        if (request != null && request.requestId == requestId) {
            _serverState.update { current ->
                val newAllowed = current.allowedIps.toMutableSet()
                val newBlocked = current.blockedIps.toMutableSet()

                if (allow) {
                    if (remember) newAllowed.add(request.clientIp)
                    newBlocked.remove(request.clientIp)
                } else {
                    if (remember) newBlocked.add(request.clientIp)
                    newAllowed.remove(request.clientIp)
                }

                current.copy(
                    allowedIps = newAllowed,
                    blockedIps = newBlocked,
                    pendingConnectionRequest = null
                )
            }
        }

        deferred?.complete(allow)
    }

    fun blockClient(clientIp: String) {
        _serverState.update { current ->
            current.copy(
                blockedIps = current.blockedIps + clientIp,
                allowedIps = current.allowedIps - clientIp
            )
        }
        // Disconnect immediately if currently connected
        activeServer?.disconnectClientsWithIp(clientIp)
    }

    fun unblockClient(clientIp: String) {
        _serverState.update { current ->
            current.copy(blockedIps = current.blockedIps - clientIp)
        }
    }

    fun updateTransferProgress(
        fileName: String?,
        direction: TransferDirection,
        downloadSpeedBps: Long,
        uploadSpeedBps: Long,
        totalBytes: Long
    ) {
        _serverState.update { current ->
            current.copy(
                transferTelemetry = TransferTelemetry(
                    activeFileName = fileName,
                    direction = direction,
                    downloadSpeedBps = downloadSpeedBps,
                    uploadSpeedBps = uploadSpeedBps,
                    totalBytesTransferred = totalBytes
                )
            )
        }
    }

    fun onTransferFinished() {
        _serverState.update { current ->
            current.copy(
                transferTelemetry = current.transferTelemetry.copy(
                    activeFileName = null,
                    direction = TransferDirection.NONE,
                    downloadSpeedBps = 0L,
                    uploadSpeedBps = 0L
                )
            )
        }
    }

    fun startServer(context: Context) {
        val currentState = _serverState.value
        if (currentState.isRunning) return

        val ip = NetworkUtils.getLocalIpv4Address()
        if (ip == null) {
            _serverState.update {
                it.copy(
                    status = ServerStatus.ERROR,
                    errorMessage = "Please connect to Wi-Fi or turn on Hotspot."
                )
            }
            return
        }

        _serverState.update {
            it.copy(
                status = ServerStatus.STARTING,
                hostAddress = ip,
                errorMessage = null
            )
        }

        val serviceIntent = Intent(context, FtpForegroundService::class.java).apply {
            action = FtpForegroundService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Exception) {
            internalStartServer(context.applicationContext)
        }
    }

    internal fun internalStartServer(context: Context) {
        activeServer?.stop()

        val currentState = _serverState.value
        val server = FtpServer(
            context = context,
            port = currentState.port,
            shareMode = currentState.shareMode,
            isReadOnly = currentState.isReadOnly,
            onStatusChanged = { status, error ->
                _serverState.update {
                    it.copy(
                        status = status,
                        errorMessage = error,
                        startedAt = if (status == ServerStatus.RUNNING) System.currentTimeMillis() else it.startedAt
                    )
                }
            },
            onClientsChanged = { clients ->
                _serverState.update {
                    it.copy(connectedClients = clients)
                }
            }
        )

        activeServer = server
        val success = server.start()
        if (success) {
            _serverState.update {
                it.copy(
                    status = ServerStatus.RUNNING,
                    hostAddress = server.boundIp ?: NetworkUtils.getLocalIpv4Address(),
                    startedAt = System.currentTimeMillis(),
                    errorMessage = null
                )
            }
        }
    }

    fun pauseServer(context: Context) {
        activeServer?.pause()
        _serverState.update { it.copy(status = ServerStatus.PAUSED) }
        val intent = Intent(context, FtpForegroundService::class.java).apply {
            action = FtpForegroundService.ACTION_UPDATE_NOTIFICATION
        }
        context.startService(intent)
    }

    fun resumeServer(context: Context) {
        activeServer?.resume()
        _serverState.update { it.copy(status = ServerStatus.RUNNING) }
        val intent = Intent(context, FtpForegroundService::class.java).apply {
            action = FtpForegroundService.ACTION_UPDATE_NOTIFICATION
        }
        context.startService(intent)
    }

    fun stopServer(context: Context) {
        activeServer?.stop()
        activeServer = null
        WakeLockManager.releaseAll()

        _serverState.update {
            it.copy(
                status = ServerStatus.STOPPED,
                connectedClients = emptyList(),
                transferTelemetry = TransferTelemetry(),
                errorMessage = null
            )
        }

        val intent = Intent(context, FtpForegroundService::class.java).apply {
            action = FtpForegroundService.ACTION_STOP
        }
        context.stopService(intent)
    }

    fun stopServerDueToNetworkLoss(context: Context) {
        if (!_serverState.value.isRunning) return
        activeServer?.stop()
        activeServer = null
        WakeLockManager.releaseAll()

        _serverState.update {
            it.copy(
                status = ServerStatus.STOPPED,
                connectedClients = emptyList(),
                transferTelemetry = TransferTelemetry(),
                errorMessage = "Wi-Fi disconnected. Server shut down automatically for security."
            )
        }

        val intent = Intent(context, FtpForegroundService::class.java).apply {
            action = FtpForegroundService.ACTION_STOP
        }
        context.stopService(intent)
    }

    fun disconnectClient(id: String) {
        activeServer?.disconnectClient(id)
    }
}
