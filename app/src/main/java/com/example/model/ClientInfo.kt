package com.example.model

data class ClientInfo(
    val id: String,
    val ipAddress: String,
    val hostName: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val currentActivity: String = "Idle",
    val bytesTransferred: Long = 0L
)
