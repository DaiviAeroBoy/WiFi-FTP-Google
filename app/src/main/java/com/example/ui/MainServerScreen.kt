package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClientInfo
import com.example.model.ConnectionRequest
import com.example.model.ServerStatus
import com.example.model.ShareMode
import com.example.model.TransferDirection
import com.example.model.TransferTelemetry
import com.example.ui.theme.AmberPaused
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldRunning
import com.example.ui.theme.RoseError
import com.example.ui.components.SciFi3DLogo
import com.example.util.QrCodeGenerator
import com.example.util.StorageUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainServerScreen(
    viewModel: FtpViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val serverState by viewModel.serverState.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()
    val showPortDialog by viewModel.showPortDialog.collectAsState()
    val showQrCode by viewModel.showQrCode.collectAsState()

    val legacyStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkPermissions()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkPermissions()
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolvedPath = StorageUtils.getPathFromTreeUri(uri)
            if (resolvedPath != null && File(resolvedPath).exists()) {
                val folder = File(resolvedPath)
                viewModel.setShareMode(ShareMode.SpecificFolder(folder.absolutePath, folder.name))
            } else {
                val folderName = uri.lastPathSegment?.substringAfterLast(':') ?: "Selected Folder"
                viewModel.setShareMode(ShareMode.SpecificFolder(uri.toString(), folderName))
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = StorageUtils.getFileName(context, uri)
            val resolvedPath = StorageUtils.getPathFromTreeUri(uri)
            if (resolvedPath != null && File(resolvedPath).exists()) {
                val file = File(resolvedPath)
                viewModel.setShareMode(ShareMode.SpecificFile(file.absolutePath, file.name, file.length()))
            } else {
                viewModel.setShareMode(ShareMode.SpecificFile(uri.toString(), fileName, 0L))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissions()
        viewModel.refreshNetwork()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SciFi3DLogo(
                            size = 28.dp,
                            animated = serverState.isRunning
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "WiFi FTP Server",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshNetwork() },
                        modifier = Modifier.testTag("refresh_network_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Network"
                        )
                    }
                    IconButton(
                        onClick = { onToggleDarkTheme(!isDarkTheme) },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                            contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {

            // 1. Storage & Notification Permissions Check
            if (!hasStoragePermission || !hasNotificationPermission) {
                item {
                    PermissionCard(
                        hasStoragePermission = hasStoragePermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestStoragePermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intent = StorageUtils.getManageStorageIntent(context)
                                context.startActivity(intent)
                            } else {
                                legacyStorageLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                )
                            }
                        },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }

            // 2. Wi-Fi / Hotspot Network Status
            item {
                NetworkStatusCard(networkStatus = networkStatus)
            }

            // 3. Hero Server Card (Large Start/Stop, Address, Copy Button & QR Toggle)
            item {
                HeroServerCard(
                    serverState = serverState,
                    networkStatus = networkStatus,
                    showQrCode = showQrCode,
                    onToggleServer = {
                        if (!hasStoragePermission) {
                            Toast.makeText(context, "Please grant Storage Access first to transfer files.", Toast.LENGTH_LONG).show()
                        } else {
                            viewModel.toggleServer()
                        }
                    },
                    onPause = { viewModel.pauseServer() },
                    onResume = { viewModel.resumeServer() },
                    onToggleQrCode = { viewModel.toggleQrCode() },
                    onCopyUrl = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("FTP Server Address", serverState.ftpUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied ${serverState.ftpUrl} to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 4. On-Screen QR Code Card (Collapsible / Expandable)
            if (serverState.isRunning && showQrCode) {
                item {
                    QrCodeDisplayCard(
                        ftpUrl = serverState.ftpUrl,
                        onClose = { viewModel.toggleQrCode() }
                    )
                }
            }

            // 5. Real-Time Transfer Telemetry (Upload/Download Speeds & Active File Name)
            if (serverState.isRunning) {
                item {
                    TransferTelemetryCard(telemetry = serverState.transferTelemetry)
                }
            }

            // 6. Sharing Scope Selector (Full Storage, Folder, File)
            item {
                ShareScopeSelectorCard(
                    currentShareMode = serverState.shareMode,
                    isRunning = serverState.isRunning,
                    folderPresets = viewModel.folderPresets,
                    onSelectWholeStorage = { viewModel.setShareMode(ShareMode.WholeStorage) },
                    onPickFolder = { folderPickerLauncher.launch(null) },
                    onSelectFolderPreset = { preset -> viewModel.setShareMode(ShareMode.SpecificFolder(preset.path, preset.name)) },
                    onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) }
                )
            }

            // 7. Server Preferences (Read/Write toggle & Port)
            item {
                ServerOptionsCard(
                    isReadOnly = serverState.isReadOnly,
                    port = serverState.port,
                    isRunning = serverState.isRunning,
                    onToggleReadOnly = { viewModel.setReadOnly(!serverState.isReadOnly) },
                    onEditPort = { viewModel.openPortDialog() }
                )
            }

            // 8. Connected Devices Section
            item {
                ConnectedDevicesHeader(clientCount = serverState.connectedClients.size)
            }

            if (serverState.connectedClients.isEmpty()) {
                item {
                    EmptyClientsCard(isRunning = serverState.isRunning)
                }
            } else {
                items(serverState.connectedClients, key = { it.id }) { client ->
                    ClientDeviceCard(
                        client = client,
                        onDisconnect = { viewModel.disconnectClient(client.id) },
                        onBlock = { viewModel.blockClient(client.ipAddress) }
                    )
                }
            }

            // 9. Blocked Devices Section (if any devices blocked)
            if (serverState.blockedIps.isNotEmpty()) {
                item {
                    BlockedDevicesCard(
                        blockedIps = serverState.blockedIps.toList(),
                        onUnblock = { ip -> viewModel.unblockClient(ip) }
                    )
                }
            }
        }
    }

    // New Connection Allow / Block Pop-up Dialog
    serverState.pendingConnectionRequest?.let { request ->
        ConnectionRequestDialog(
            request = request,
            onAllow = { remember -> viewModel.allowConnection(request.requestId, remember) },
            onBlock = { remember -> viewModel.blockConnection(request.requestId, remember) }
        )
    }

    // Custom Port Dialog
    if (showPortDialog) {
        PortEditDialog(
            currentPort = serverState.port,
            onDismiss = { viewModel.dismissPortDialog() },
            onSave = { newPort -> viewModel.setPort(newPort) }
        )
    }
}

// ---------------- SUB-COMPONENTS ----------------

@Composable
fun ConnectionRequestDialog(
    request: ConnectionRequest,
    onAllow: (remember: Boolean) -> Unit,
    onBlock: (remember: Boolean) -> Unit
) {
    var rememberDecision by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { onBlock(false) },
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "New Connection Request",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "A device is attempting to connect to your FTP server:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = request.hostName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "IP: ${request.clientIp}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { rememberDecision = !rememberDecision }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = rememberDecision,
                        onCheckedChange = { rememberDecision = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Remember this device",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAllow(rememberDecision) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldRunning),
                modifier = Modifier.testTag("dialog_allow_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Allow")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onBlock(rememberDecision) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                modifier = Modifier.testTag("dialog_block_button")
            ) {
                Icon(imageVector = Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Block")
            }
        }
    )
}

@Composable
fun HeroServerCard(
    serverState: com.example.model.ServerState,
    networkStatus: com.example.util.NetworkUtils.NetworkStatus,
    showQrCode: Boolean,
    onToggleServer: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onToggleQrCode: () -> Unit,
    onCopyUrl: () -> Unit
) {
    val isRunning = serverState.isRunning
    val isPaused = serverState.status == ServerStatus.PAUSED

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isPaused -> AmberPaused
            isRunning -> EmeraldRunning
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.5.dp, color = cardBorderColor, shape = RoundedCornerShape(20.dp))
            .testTag("hero_server_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sci-Fi 3D Holographic Server Logo
            SciFi3DLogo(
                size = 56.dp,
                animated = isRunning
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Status Badge
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = when {
                    isPaused -> AmberPaused.copy(alpha = 0.15f)
                    isRunning -> EmeraldRunning.copy(alpha = 0.15f)
                    serverState.status == ServerStatus.STARTING -> CyanAccent.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isPaused -> AmberPaused
                                    isRunning -> EmeraldRunning
                                    serverState.status == ServerStatus.STARTING -> CyanAccent
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isPaused -> "SERVER PAUSED"
                            isRunning -> "SERVER ACTIVE"
                            serverState.status == ServerStatus.STARTING -> "STARTING..."
                            else -> "SERVER STOPPED"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isPaused -> AmberPaused
                            isRunning -> EmeraldRunning
                            serverState.status == ServerStatus.STARTING -> CyanPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // FTP Address Display Box with direct One-Tap Copy
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = isRunning) { onCopyUrl() }
                    .testTag("ftp_url_box"),
                color = if (isRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRunning) "Tap to copy address for PC File Explorer:" else "Server address when started:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = serverState.ftpUrl,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (isRunning) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy URL",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Quick Actions Row (Copy Address Button & Show QR Code Button)
            if (isRunning) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onCopyUrl,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_address_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Address", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onToggleQrCode,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qr_code_toggle_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (showQrCode) Icons.Default.Close else Icons.Default.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showQrCode) "Hide QR" else "QR Code", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // How to connect tip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Open Windows File Explorer, paste address, and press Enter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Start / Stop Button & Pause Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isRunning) {
                    FilledTonalButton(
                        onClick = { if (isPaused) onResume() else onPause() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("pause_resume_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPaused) "Resume" else "Pause", fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = onToggleServer,
                    modifier = Modifier
                        .weight(if (isRunning) 1.4f else 1f)
                        .height(52.dp)
                        .testTag("start_stop_server_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) RoseError else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "Stop Server" else "Start Server",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (serverState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = serverState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun QrCodeDisplayCard(
    ftpUrl: String,
    onClose: () -> Unit
) {
    val qrBitmap = remember(ftpUrl) {
        QrCodeGenerator.generateQrBitmap(ftpUrl, size = 480)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("qr_code_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan to Connect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close QR")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "FTP Server QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("Generating QR Code...", color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = ftpUrl,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scan with your phone or tablet camera to open in FTP file manager",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TransferTelemetryCard(telemetry: TransferTelemetry) {
    val isTransferring = telemetry.direction != TransferDirection.NONE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transfer_telemetry_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTransferring) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (isTransferring) EmeraldRunning else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Real-Time Transfer Speeds",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isTransferring) EmeraldRunning.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = when (telemetry.direction) {
                            TransferDirection.DOWNLOADING -> "DOWNLOADING"
                            TransferDirection.UPLOADING -> "UPLOADING"
                            TransferDirection.NONE -> "IDLE"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isTransferring) EmeraldRunning else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Speeds Row (Download & Upload)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Download Speed
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Download Speed",
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Download",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${StorageUtils.formatFileSize(telemetry.downloadSpeedBps)}/s",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (telemetry.downloadSpeedBps > 0) CyanPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Upload Speed
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Upload Speed",
                            tint = EmeraldRunning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Upload",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${StorageUtils.formatFileSize(telemetry.uploadSpeedBps)}/s",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (telemetry.uploadSpeedBps > 0) EmeraldRunning else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active File & Total Transferred
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = telemetry.activeFileName ?: "No file currently transferring",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (telemetry.activeFileName != null) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (telemetry.totalBytesTransferred > 0) {
                        Text(
                            text = "Total: ${StorageUtils.formatFileSize(telemetry.totalBytesTransferred)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedDevicesCard(
    blockedIps: List<String>,
    onUnblock: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("blocked_devices_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = RoseError,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Blocked Devices (${blockedIps.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            blockedIps.forEach { ip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ip,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                    TextButton(onClick = { onUnblock(ip) }) {
                        Text("Unblock")
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    hasStoragePermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestStoragePermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "To allow computers to browse files and view live status in your notification bar, please grant the following permissions:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasStoragePermission) {
                    Button(
                        onClick = onRequestStoragePermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("grant_storage_button")
                    ) {
                        Text("Grant Storage Access")
                    }
                }

                if (!hasNotificationPermission) {
                    OutlinedButton(
                        onClick = onRequestNotificationPermission,
                        modifier = Modifier.testTag("grant_notification_button")
                    ) {
                        Text("Allow Notifications")
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkStatusCard(networkStatus: com.example.util.NetworkUtils.NetworkStatus) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        networkStatus.isHotspot -> Icons.Default.WifiTethering
                        networkStatus.isWifi -> Icons.Default.Wifi
                        networkStatus.isConnected -> Icons.Default.Wifi
                        else -> Icons.Default.WifiOff
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (networkStatus.isConnected) EmeraldRunning else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    val statusTitle = when {
                        networkStatus.isHotspot -> "Mobile Hotspot Active"
                        networkStatus.isWifi && !networkStatus.ssid.isNullOrEmpty() -> "Wi-Fi: ${networkStatus.ssid}"
                        networkStatus.isWifi -> "Wi-Fi Connected"
                        networkStatus.isConnected -> "Local Network Active"
                        else -> "No Wi-Fi Connection (Auto-Shutdown Active)"
                    }
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = networkStatus.ipAddress?.let { "Device IP: $it" } ?: "Connect to Wi-Fi to start FTP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (networkStatus.isConnected) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldRunning.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "READY",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldRunning
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareScopeSelectorCard(
    currentShareMode: ShareMode,
    isRunning: Boolean,
    folderPresets: List<FolderPreset>,
    onSelectWholeStorage: () -> Unit,
    onPickFolder: () -> Unit,
    onSelectFolderPreset: (FolderPreset) -> Unit,
    onPickFile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("share_scope_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sharing Scope",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose what part of your phone is accessible over FTP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isWhole = currentShareMode is ShareMode.WholeStorage
                val isFolder = currentShareMode is ShareMode.SpecificFolder
                val isFile = currentShareMode is ShareMode.SpecificFile

                ScopeTabButton(
                    title = "Full Storage",
                    icon = Icons.Default.Smartphone,
                    selected = isWhole,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f),
                    onClick = onSelectWholeStorage
                )

                ScopeTabButton(
                    title = "Folder",
                    icon = Icons.Default.Folder,
                    selected = isFolder,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f),
                    onClick = onPickFolder
                )

                ScopeTabButton(
                    title = "File",
                    icon = Icons.Default.InsertDriveFile,
                    selected = isFile,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f),
                    onClick = onPickFile
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (currentShareMode) {
                            is ShareMode.WholeStorage -> Icons.Default.SdCard
                            is ShareMode.SpecificFolder -> Icons.Default.FolderOpen
                            is ShareMode.SpecificFile -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (currentShareMode) {
                                is ShareMode.WholeStorage -> "Whole Storage (Internal + SD Card)"
                                is ShareMode.SpecificFolder -> "Folder: ${currentShareMode.displayName}"
                                is ShareMode.SpecificFile -> "File: ${currentShareMode.displayName}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (currentShareMode) {
                                is ShareMode.WholeStorage -> "All user files in internal storage and mounted SD cards"
                                is ShareMode.SpecificFolder -> currentShareMode.path
                                is ShareMode.SpecificFile -> currentShareMode.path
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!isRunning && (currentShareMode is ShareMode.SpecificFolder || currentShareMode is ShareMode.SpecificFile)) {
                        TextButton(
                            onClick = {
                                if (currentShareMode is ShareMode.SpecificFolder) onPickFolder() else onPickFile()
                            }
                        ) {
                            Text("Change")
                        }
                    }
                }
            }

            if (currentShareMode is ShareMode.SpecificFolder && !isRunning) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Quick Folder Shortcuts:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (preset in folderPresets) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectFolderPreset(preset) }
                        ) {
                            Text(
                                text = preset.name,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScopeTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ServerOptionsCard(
    isReadOnly: Boolean,
    port: Int,
    isRunning: Boolean,
    onToggleReadOnly: () -> Unit,
    onEditPort: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("server_options_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isRunning) { onToggleReadOnly() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isReadOnly) "Read-Only Mode" else "Read & Write Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isReadOnly) "Computer can view and download files only" else "Computer can download, upload, rename, and delete files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = !isReadOnly,
                    onCheckedChange = { onToggleReadOnly() },
                    enabled = !isRunning,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("read_write_switch")
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isRunning) { onEditPort() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "FTP Port",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Standard port $port",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("port_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$port",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (!isRunning) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Port",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectedDevicesHeader(clientCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Connected Devices",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (clientCount > 0) EmeraldRunning.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "$clientCount active",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (clientCount > 0) EmeraldRunning else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyClientsCard(isRunning: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_clients_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isRunning) "Waiting for PC connection..." else "No devices connected",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isRunning) {
                    "Open File Explorer on your computer or scan QR code to connect"
                } else {
                    "Tap 'Start Server' above to begin sharing"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ClientDeviceCard(
    client: ClientInfo,
    onDisconnect: () -> Unit,
    onBlock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("client_device_card_${client.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldRunning.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            tint = EmeraldRunning,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = client.hostName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "IP: ${client.ipAddress}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Status: ${client.currentActivity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (client.currentActivity.startsWith("Downloading") || client.currentActivity.startsWith("Uploading")) {
                            CyanPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Row {
                IconButton(
                    onClick = onBlock,
                    modifier = Modifier.testTag("block_client_${client.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Block Device",
                        tint = RoseError
                    )
                }

                IconButton(
                    onClick = onDisconnect,
                    modifier = Modifier.testTag("disconnect_client_${client.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Disconnect Client",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PortEditDialog(
    currentPort: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var portText by remember { mutableStateOf(currentPort.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change FTP Port") },
        text = {
            Column {
                Text(
                    text = "Enter a port number between 1024 and 65535. Default is 2121.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = portText,
                    onValueChange = {
                        portText = it.filter { char -> char.isDigit() }
                        errorText = null
                    },
                    label = { Text("Port") },
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it) } },
                    modifier = Modifier.testTag("port_input_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = portText.toIntOrNull()
                    if (p == null || p < 1024 || p > 65535) {
                        errorText = "Please enter a valid port (1024 - 65535)"
                    } else {
                        onSave(p)
                    }
                },
                modifier = Modifier.testTag("save_port_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
