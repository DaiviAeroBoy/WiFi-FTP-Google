package com.example.model

enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    PAUSED,
    ERROR
}

enum class TransferDirection {
    NONE,
    DOWNLOADING,
    UPLOADING
}

data class ConnectionRequest(
    val requestId: String,
    val clientIp: String,
    val hostName: String,
    val requestedAt: Long = System.currentTimeMillis()
)

data class TransferTelemetry(
    val activeFileName: String? = null,
    val direction: TransferDirection = TransferDirection.NONE,
    val downloadSpeedBps: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val totalBytesTransferred: Long = 0L
)

data class ServerState(
    val status: ServerStatus = ServerStatus.STOPPED,
    val hostAddress: String? = null,
    val port: Int = 2121,
    val shareMode: ShareMode = ShareMode.WholeStorage,
    val isReadOnly: Boolean = false,
    val connectedClients: List<ClientInfo> = emptyList(),
    val allowedIps: Set<String> = emptySet(),
    val blockedIps: Set<String> = emptySet(),
    val pendingConnectionRequest: ConnectionRequest? = null,
    val transferTelemetry: TransferTelemetry = TransferTelemetry(),
    val errorMessage: String? = null,
    val startedAt: Long = 0L
) {
    val ftpUrl: String
        get() = if (hostAddress != null) "ftp://$hostAddress:$port" else "ftp://0.0.0.0:$port"

    val isRunning: Boolean
        get() = status == ServerStatus.RUNNING || status == ServerStatus.PAUSED
}
