package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ClientInfo
import com.example.model.ServerState
import com.example.model.ServerStatus
import com.example.model.ShareMode
import com.example.server.FtpServerManager
import com.example.util.NetworkUtils
import com.example.util.StorageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class FolderPreset(
    val name: String,
    val path: String,
    val iconName: String
)

class FtpViewModel(application: Application) : AndroidViewModel(application) {

    val serverState: StateFlow<ServerState> = FtpServerManager.serverState

    private val _networkStatus = MutableStateFlow(NetworkUtils.getNetworkStatus(application))
    val networkStatus: StateFlow<NetworkUtils.NetworkStatus> = _networkStatus.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(StorageUtils.hasStoragePermission(application))
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(StorageUtils.hasNotificationPermission(application))
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    private val _isDarkThemeOverride = MutableStateFlow<Boolean?>(null)
    val isDarkThemeOverride: StateFlow<Boolean?> = _isDarkThemeOverride.asStateFlow()

    private val _showPortDialog = MutableStateFlow(false)
    val showPortDialog: StateFlow<Boolean> = _showPortDialog.asStateFlow()

    private val _showQrCode = MutableStateFlow(false)
    val showQrCode: StateFlow<Boolean> = _showQrCode.asStateFlow()

    val folderPresets: List<FolderPreset> = listOf(
        FolderPreset("Downloads", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath, "download"),
        FolderPreset("Photos (DCIM)", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath, "camera"),
        FolderPreset("Documents", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath, "document"),
        FolderPreset("Pictures", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath, "image"),
        FolderPreset("Music", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath, "music")
    )

    init {
        checkPermissions()
        refreshNetwork()

        // Periodically refresh network status and handle auto-shutdown if Wi-Fi disconnects
        viewModelScope.launch {
            while (true) {
                delay(3000)
                val app = getApplication<Application>()
                val net = NetworkUtils.getNetworkStatus(app)
                _networkStatus.value = net
                if (!net.isConnected && serverState.value.isRunning) {
                    FtpServerManager.stopServerDueToNetworkLoss(app)
                }
            }
        }
    }

    fun checkPermissions() {
        val app = getApplication<Application>()
        _hasStoragePermission.value = StorageUtils.hasStoragePermission(app)
        _hasNotificationPermission.value = StorageUtils.hasNotificationPermission(app)
    }

    fun refreshNetwork() {
        val app = getApplication<Application>()
        val net = NetworkUtils.getNetworkStatus(app)
        _networkStatus.value = net
        FtpServerManager.refreshNetwork(app)
    }

    fun toggleServer() {
        val app = getApplication<Application>()
        val state = serverState.value
        if (state.isRunning) {
            FtpServerManager.stopServer(app)
        } else {
            FtpServerManager.startServer(app)
        }
    }

    fun pauseServer() {
        val app = getApplication<Application>()
        FtpServerManager.pauseServer(app)
    }

    fun resumeServer() {
        val app = getApplication<Application>()
        FtpServerManager.resumeServer(app)
    }

    fun setShareMode(shareMode: ShareMode) {
        FtpServerManager.setShareMode(shareMode)
    }

    fun setReadOnly(isReadOnly: Boolean) {
        FtpServerManager.setReadOnly(isReadOnly)
    }

    fun setPort(port: Int) {
        FtpServerManager.setPort(port)
        _showPortDialog.value = false
    }

    fun openPortDialog() {
        if (!serverState.value.isRunning) {
            _showPortDialog.value = true
        }
    }

    fun dismissPortDialog() {
        _showPortDialog.value = false
    }

    fun toggleQrCode() {
        _showQrCode.value = !_showQrCode.value
    }

    fun allowConnection(requestId: String, remember: Boolean) {
        FtpServerManager.handleConnectionDecision(requestId, allow = true, remember = remember)
    }

    fun blockConnection(requestId: String, remember: Boolean) {
        FtpServerManager.handleConnectionDecision(requestId, allow = false, remember = remember)
    }

    fun blockClient(ip: String) {
        FtpServerManager.blockClient(ip)
    }

    fun unblockClient(ip: String) {
        FtpServerManager.unblockClient(ip)
    }

    fun disconnectClient(id: String) {
        FtpServerManager.disconnectClient(id)
    }

    fun toggleTheme(isDark: Boolean) {
        _isDarkThemeOverride.value = isDark
    }
}
