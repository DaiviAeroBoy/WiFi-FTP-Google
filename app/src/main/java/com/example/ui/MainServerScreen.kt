package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.DestructiveRed
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
                    Text(
                        text = "WiFi FTP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(
                        onClick = { onToggleDarkTheme(!isDarkTheme) },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                            contentDescription = "Toggle theme",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // One large full-width button at the bottom for the main action
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (serverState.isRunning) {
                        TextButton(
                            onClick = {
                                if (serverState.status == ServerStatus.PAUSED) {
                                    viewModel.resumeServer()
                                } else {
                                    viewModel.pauseServer()
                                }
                            },
                            modifier = Modifier.testTag("pause_resume_button")
                        ) {
                            Text(
                                text = if (serverState.status == ServerStatus.PAUSED) "Resume Server" else "Pause Server",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Button(
                        onClick = {
                            if (!hasStoragePermission) {
                                Toast.makeText(context, "Please grant storage access first.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.toggleServer()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_stop_server_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (serverState.isRunning) DestructiveRed else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (serverState.isRunning) "Stop Server" else "Start Server",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {

            // Storage Permission Alert (Quiet & Simple)
            if (!hasStoragePermission) {
                item {
                    SimplePermissionBanner(
                        onRequestStorage = {
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
                        }
                    )
                }
            }

            // Big Bold Hero Section (The Most Important Thing)
            item {
                ServerHeroSection(
                    serverState = serverState,
                    networkStatus = networkStatus,
                    showQrCode = showQrCode,
                    onToggleQrCode = { viewModel.toggleQrCode() },
                    onCopyAddress = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("FTP Address", serverState.ftpUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied address", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // QR Code (Clean & Flat if toggled)
            if (serverState.isRunning && showQrCode) {
                item {
                    SimpleQrCard(ftpUrl = serverState.ftpUrl)
                }
            }

            // Sharing Scope Selection (Clean buttons)
            item {
                SharingScopeSection(
                    currentShareMode = serverState.shareMode,
                    isRunning = serverState.isRunning,
                    onSelectWhole = { viewModel.setShareMode(ShareMode.WholeStorage) },
                    onPickFolder = { folderPickerLauncher.launch(null) },
                    onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) }
                )
            }

            // Options (Read & Write mode, Port)
            item {
                OptionsSection(
                    isReadOnly = serverState.isReadOnly,
                    port = serverState.port,
                    isRunning = serverState.isRunning,
                    onToggleReadOnly = { viewModel.setReadOnly(!serverState.isReadOnly) },
                    onEditPort = { viewModel.openPortDialog() }
                )
            }

            // Connected Devices
            item {
                ConnectedDevicesSection(
                    clients = serverState.connectedClients,
                    isRunning = serverState.isRunning,
                    onDisconnect = { id -> viewModel.disconnectClient(id) }
                )
            }
        }
    }

    // Connection Authorization Pop-up
    serverState.pendingConnectionRequest?.let { request ->
        SimpleConnectionDialog(
            request = request,
            onAllow = { remember -> viewModel.allowConnection(request.requestId, remember) },
            onBlock = { remember -> viewModel.blockConnection(request.requestId, remember) }
        )
    }

    // Port Dialog
    if (showPortDialog) {
        SimplePortDialog(
            currentPort = serverState.port,
            onDismiss = { viewModel.dismissPortDialog() },
            onSave = { port -> viewModel.setPort(port) }
        )
    }
}

// ---------------- CLEAN & CLASSY COMPONENTS ----------------

@Composable
fun SimplePermissionBanner(onRequestStorage: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("permission_card"),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Storage access needed",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Allow access to share files with your computer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onRequestStorage,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("grant_storage_button")
            ) {
                Text("Grant", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun ServerHeroSection(
    serverState: com.example.model.ServerState,
    networkStatus: com.example.util.NetworkUtils.NetworkStatus,
    showQrCode: Boolean,
    onToggleQrCode: () -> Unit,
    onCopyAddress: () -> Unit
) {
    val isRunning = serverState.isRunning
    val telemetry = serverState.transferTelemetry

    Column(modifier = Modifier.fillMaxWidth()) {

        // Status Indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            serverState.status == ServerStatus.PAUSED -> MaterialTheme.colorScheme.secondary
                            isRunning -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    serverState.status == ServerStatus.PAUSED -> "PAUSED"
                    isRunning -> "ONLINE"
                    else -> "OFFLINE"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!networkStatus.ssid.isNullOrEmpty()) {
                Text(
                    text = " • ${networkStatus.ssid}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Big Bold Address (The Most Important Thing)
        Text(
            text = serverState.ftpUrl,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = if (isRunning) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Small Quiet Hint Text
        Text(
            text = if (isRunning) "Type this address in your computer's File Explorer" else "Connect your computer and phone to the same Wi-Fi network",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Rounded Action Buttons (Copy & QR Code)
        if (isRunning) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCopyAddress,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.testTag("copy_address_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = onToggleQrCode,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.testTag("qr_code_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showQrCode) "Hide QR" else "QR Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Live Speeds & Active Transfer (Small quiet text)
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "↓ ${StorageUtils.formatFileSize(telemetry.downloadSpeedBps)}/s   ↑ ${StorageUtils.formatFileSize(telemetry.uploadSpeedBps)}/s",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!telemetry.activeFileName.isNullOrEmpty()) {
                    Text(
                        text = " • ${telemetry.activeFileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (serverState.errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = serverState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = DestructiveRed
            )
        }
    }
}

@Composable
fun SimpleQrCard(ftpUrl: String) {
    val qrBitmap = remember(ftpUrl) {
        QrCodeGenerator.generateQrBitmap(ftpUrl, size = 320)
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("qr_code_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(180.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "FTP QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scan with camera to open address",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SharingScopeSection(
    currentShareMode: ShareMode,
    isRunning: Boolean,
    onSelectWhole: () -> Unit,
    onPickFolder: () -> Unit,
    onPickFile: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "SHARING",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isWhole = currentShareMode is ShareMode.WholeStorage
            val isFolder = currentShareMode is ShareMode.SpecificFolder
            val isFile = currentShareMode is ShareMode.SpecificFile

            SimpleScopeButton(
                title = "Whole Storage",
                selected = isWhole,
                enabled = !isRunning,
                modifier = Modifier.weight(1f),
                onClick = onSelectWhole
            )

            SimpleScopeButton(
                title = "Folder",
                selected = isFolder,
                enabled = !isRunning,
                modifier = Modifier.weight(1f),
                onClick = onPickFolder
            )

            SimpleScopeButton(
                title = "File",
                selected = isFile,
                enabled = !isRunning,
                modifier = Modifier.weight(1f),
                onClick = onPickFile
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Small Quiet Hint showing selected path
        Text(
            text = when (currentShareMode) {
                is ShareMode.WholeStorage -> "Sharing internal storage and SD card"
                is ShareMode.SpecificFolder -> "Folder: ${currentShareMode.displayName}"
                is ShareMode.SpecificFile -> "File: ${currentShareMode.displayName}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SimpleScopeButton(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun OptionsSection(
    isReadOnly: Boolean,
    port: Int,
    isRunning: Boolean,
    onToggleReadOnly: () -> Unit,
    onEditPort: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "OPTIONS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().testTag("server_options_card"),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                // Read & Write Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isRunning) { onToggleReadOnly() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isReadOnly) "Read-only" else "Read and write",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isReadOnly) "Computer cannot upload or delete files" else "Computer can upload, rename, and delete files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = !isReadOnly,
                        onCheckedChange = { onToggleReadOnly() },
                        enabled = !isRunning,
                        modifier = Modifier.testTag("read_write_switch")
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )

                // Port Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isRunning) { onEditPort() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Port",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$port",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
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
fun ConnectedDevicesSection(
    clients: List<ClientInfo>,
    isRunning: Boolean,
    onDisconnect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONNECTED DEVICES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            if (clients.isNotEmpty()) {
                Text(
                    text = "${clients.size} active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (clients.isEmpty()) {
            Text(
                text = if (isRunning) "Waiting for computer to connect..." else "No devices connected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    clients.forEachIndexed { index, client ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = client.hostName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = client.ipAddress,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { onDisconnect(client.id) },
                                modifier = Modifier.testTag("disconnect_client_${client.id}")
                            ) {
                                Text(
                                    text = "Disconnect",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DestructiveRed
                                )
                            }
                        }
                        if (index < clients.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleConnectionDialog(
    request: ConnectionRequest,
    onAllow: (remember: Boolean) -> Unit,
    onBlock: (remember: Boolean) -> Unit
) {
    var rememberDecision by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { onBlock(false) },
        title = {
            Text("Connection Request", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "A device wants to connect to your server:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(request.hostName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(request.clientIp, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { rememberDecision = !rememberDecision }
                ) {
                    Checkbox(checked = rememberDecision, onCheckedChange = { rememberDecision = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remember device", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAllow(rememberDecision) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("dialog_allow_button")
            ) {
                Text("Allow")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onBlock(rememberDecision) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DestructiveRed),
                modifier = Modifier.testTag("dialog_block_button")
            ) {
                Text("Block")
            }
        }
    )
}

@Composable
fun SimplePortDialog(
    currentPort: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var portText by remember { mutableStateOf(currentPort.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("FTP Port") },
        text = {
            Column {
                Text("Default is 2121. Valid range 1024 - 65535.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = portText,
                    onValueChange = {
                        portText = it.filter { char -> char.isDigit() }
                        errorText = null
                    },
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
                        errorText = "Enter a valid port"
                    } else {
                        onSave(p)
                    }
                },
                shape = RoundedCornerShape(8.dp),
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
