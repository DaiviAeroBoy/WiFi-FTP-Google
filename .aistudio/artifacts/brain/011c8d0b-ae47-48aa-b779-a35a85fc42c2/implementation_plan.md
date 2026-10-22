# WiFi FTP Server for Android

A sleek, private, and high-performance local Wi-Fi FTP server app for Android 10+ that allows seamless file browsing, downloading, and uploading from any PC, Mac, or mobile device using their native file explorer (`ftp://<ip>:<port>`). Built with a single-screen Material 3 interface, instant permission onboarding, dynamic full/folder/file sharing modes, and a robust foreground service with pause/stop notification actions.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions were confirmed during clarification:
> - **Authentication**: Anonymous access enabled by default for zero-friction one-tap connection from Windows File Explorer, macOS Finder, or FileZilla without prompt walls.
> - **File Permissions**: Configurable Read-Write vs. Read-Only toggle directly on the main screen (Read-Write allows creating, uploading, and deleting; Read-Only restricts clients to view & download).
> - **Port & Networking**: Default port `2121` with inline tap-to-edit sheet, binding to active Wi-Fi / Hotspot IPv4 address and automatically updating if network changes.

---

## 1. Overview & Core Concept

- **What It Does**: Turns the Android phone into a high-speed local FTP server over Wi-Fi or Personal Hotspot. The user taps "Start Server", views the displayed `ftp://192.168.x.x:2121` URL, and enters it directly into Windows Explorer, macOS Finder, Linux file managers, or FTP clients without requiring internet or third-party cloud services.
- **Target Audience**: Users seeking fast, private, direct PC-to-phone and phone-to-PC wireless transfers without USB cables, proprietary cloud sync, or third-party web services.
- **Key Value**: 100% offline, privacy-first local network transfer with full support for entire internal storage/SD card, specific folders, or specific files, fully compliant with Android 10, 11, 12, 13, 14, and 15+ storage and notification paradigms.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Onboarding & Permission Flow**:
   - On first launch, the app smoothly requests `POST_NOTIFICATIONS` (Android 13+) and checks All Files Access (`MANAGE_EXTERNAL_STORAGE` on Android 11+ / `READ_EXTERNAL_STORAGE` & `WRITE_EXTERNAL_STORAGE` on Android 10).
   - Clear, friendly explanation banner explains why storage permission is required for FTP file transfer, with a one-tap system settings launcher button.

2. **Main Server Dashboard (Single Screen)**:
   - **Header**: App title ("WiFi FTP Server"), Wi-Fi SSID / Hotspot status pill badge, and Dark/Light theme toggle.
   - **Hero Connection Card**: 
     - When Idle: Displays large glowing power button, selected share path summary, and prompt to connect.
     - When Active: Pulsing connection badge, large copyable server URL (`ftp://192.168.1.45:2121`), tap-to-copy button, and active transfer indicators.
   - **Storage Scope Selector**:
     - Three segmented tabs: **Whole Storage** (Internal Storage + SD card selector), **Specific Folder** (launches folder picker), **Specific File** (launches file picker).
     - Live preview showing the selected root path and friendly display name.
   - **Quick Preferences Bar**:
     - Read / Write toggle switch with tooltip ("Allow PC to upload and delete files").
     - Port display button (e.g., `Port: 2121`) with sheet dialog for custom port selection.
   - **Connected Clients Monitor**:
     - Real-time card showing active connections count.
     - Expanded list displaying client IP address, hostname, connection duration, and current status (Idle / Transferring).

3. **Notification Control & Background Operation**:
   - Running in an Android Foreground Service with `FOREGROUND_SERVICE_DATA_SYNC` / `FOREGROUND_SERVICE_SPECIAL_USE` type.
   - Notification shows active URL, number of connected clients, and quick action buttons: **Pause / Resume** and **Stop Server**.

### Visual Identity & Theme
- **Theme**: Utilitarian, modern Material 3 design with vibrant network accent colors (Electric Cyan / Teal `#00C49F` for active server, Deep Indigo `#1E1B4B` / Navy `#0F172A` dark surface, Crisp Slate for light mode).
- **Typography**: M3 typography with high-contrast monospace font styling for IP addresses and server URLs (`ftp://...`) for effortless readability across distances.
- **Micro-Interactions**: Smooth animated transitions between server stopped, starting, running, and paused states. Haptic feedback on server start/stop and address copy.

---

## 3. Key Product Decisions & Trade-Offs

- **Dedicated Lightweight Pure-Kotlin FTP Socket Engine**:
  - *Chosen Approach*: Implement a modular, robust multi-threaded FTP server engine directly in Kotlin using Java `ServerSocket` and coroutine-backed thread pools.
  - *Why*: Apache FtpServer / MINA dependencies introduce heavy legacy dependencies, JNDI lookups, and reflection that frequently cause build failures or runtime crashes on modern Android ART runtimes. A custom, clean Kotlin implementation implements RFC 959 / RFC 3659 commands (`USER`, `PASS`, `SYST`, `FEAT`, `PWD`, `CWD`, `CDUP`, `TYPE`, `PASV`, `EPSV`, `PORT`, `LIST`, `NLST`, `MLSD`, `RETR`, `STOR`, `DELE`, `MKD`, `RMD`, `RNFR`, `RNTO`, `SIZE`, `QUIT`, `NOOP`, `REST`), gives 100% control over client IP/hostname tracking, pause/resume states, and avoids bloated APK size.
- **Unified Storage Architecture for Android 10+**:
  - *Chosen Approach*: Support `MANAGE_EXTERNAL_STORAGE` (All files access) for full access to `/storage/emulated/0` and physical SD cards, while supporting DocumentFile / Storage Access Framework for picked folders and files.
  - *Why*: Guarantees seamless performance on Android 10 through 15+ without scoped storage blocking raw file streams over FTP.
- **Single-Screen Architecture**:
  - *Chosen Approach*: Everything available on one unified, glanceable dashboard without hidden menus or drawer navigation. Settings (port, read-write mode) are handled via bottom sheets or inline toggles.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity (Compose)                   │
│  ┌───────────────────────┐       ┌────────────────────────┐ │
│  │ MainServerScreen      │ <───> │ FtpServerViewModel     │ │
│  │ - Hero Status Card    │       │ - Server State (State) │ │
│  │ - Storage Scope Picker│       │ - Network IP & Port    │ │
│  │ - Connected Clients   │       │ - Permissions Handler  │ │
│  └───────────────────────┘       └───────────┬────────────┘ │
└──────────────────────────────────────────────┼──────────────┘
                                               │
                                 Binds / Starts / Observes
                                               │
                                               ▼
┌─────────────────────────────────────────────────────────────┐
│             FtpForegroundService (Lifecycle Service)        │
│  - Ongoing Foreground Notification with Pause & Stop        │
│  - Wi-Fi Multicast / Network Lock                           │
│  - Manages Kotlin FtpServer Engine                          │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Kotlin FtpServer Engine                     │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Command Listener (Port 2121)                           │ │
│  │ - Handles USER, PASS, PASV, EPSV, CWD, LIST, RETR, etc.│ │
│  └───────────────────────────┬────────────────────────────┘ │
│                              │                              │
│  ┌───────────────────────────▼────────────────────────────┐ │
│  │ Passive Data Transfer Channels (High Ephemeral Ports)  │ │
│  │ - Streaming binary file downloads (RETR)               │ │
│  │ - Streaming uploads (STOR) with Read-Only safeguards   │ │
│  └───────────────────────────┬────────────────────────────┘ │
│                              │                              │
│  ┌───────────────────────────▼────────────────────────────┐ │
│  │ Virtual File System Bridge                             │ │
│  │ - Mode 1: Full Internal Storage & SD Card Roots        │ │
│  │ - Mode 2: Specific Directory Subtree                   │ │
│  │ - Mode 3: Specific Single File Virtual Directory       │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Component Breakdown
1. **`FtpServer` & `FtpClientSession`**: Pure Kotlin socket engine handling RFC 959 FTP commands, passive data channels, directory listings, uploads, downloads, and client tracking (`ClientInfo`).
2. **`FtpForegroundService`**: Keeps server alive when screen is off, manages notification actions (`ACTION_PAUSE`, `ACTION_RESUME`, `ACTION_STOP`), and broadcasts client connection updates.
3. **`NetworkUtils`**: Discovers active Wi-Fi and hotspot IPv4 addresses, handles network interface changes, and resolves client hostnames.
4. **`StorageBridge`**: Normalizes local file operations across `/storage/emulated/0`, secondary SD cards, or user-selected folders/files.
5. **`FtpServerViewModel` & `MainServerScreen`**: Jetpack Compose single-screen UI with dark/light mode toggle, permission onboarding, live connected client cards, and instant tap-to-copy address button.
