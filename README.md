# WiFi FTP Server for Android

A sleek, private, and high-performance local Wi-Fi FTP file server for Android (Android 10+). Turn your phone into an instant file server and transfer files wirelessly with your PC, Mac, Linux machine, or tablet without cables, clouds, or third-party accounts.

![Android Version](https://img.shields.io/badge/Android-10%2B-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue.svg)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-purple.svg)
![Build](https://img.shields.io/badge/GitHub%20Actions-Automated%20APK-success.svg)

---

## ✨ Features

- **Single-Screen Simplicity**: Zero menus or complex drawers. Everything is accessible from one glanceable dashboard.
- **Light & Dark Mode**: One-tap theme toggle with vibrant cyan and deep slate color schemes.
- **Sci-Fi 3D Holographic Server Logo & Splash**: Custom 3D isometric glowing server cube with rotating orbital rings and futuristic boot loading screen.
- **Flexible Storage Sharing Modes**:
  - **Full Storage**: Mounts Internal Storage (`/storage/emulated/0`) and secondary physical SD cards.
  - **Specific Folder**: Choose custom folders with presets for Downloads, DCIM/Photos, Documents, Pictures, and Music.
  - **Specific File**: Share an individual file in complete isolation.
- **Connection Authorization Pop-up**:
  - Interactive approval dialog for newly connecting devices showing **Hostname** and **IP address**.
  - One-tap **Allow** or **Block**, with a "Remember this device" whitelist/blacklist option.
  - One-tap client disconnection and blocked device management.
- **On-Screen QR Code & One-Tap Copy**:
  - Large copy button copies `ftp://<ip>:<port>` directly to clipboard.
  - On-screen QR code allows phones and tablets to connect by scanning with their camera.
- **Real-Time Transfer Speeds**:
  - Live upload & download speed meters (MB/s).
  - Active transferring file name and total transferred bytes counter.
- **Foreground Notification Controls**:
  - Live notification in notification drawer with speed and file status.
  - Actions to **Pause / Resume** and **Disconnect & Stop**.
- **Auto-Shutdown on Wi-Fi Disconnect**:
  - Automatically terminates the server if the local Wi-Fi network or hotspot disconnects.
- **CPU Wake Lock During Transfers**:
  - Holds a CPU partial wake lock during active file transfers to prevent OS throttling when the screen turns off.

---

## 🛠️ Building & Releasing the APK on GitHub

This repository is fully configured with automated **GitHub Actions** workflows to compile and release APKs out-of-the-box:

### Option 1: Trigger Manually from GitHub Actions (1-Click)
1. Push this project to your GitHub repository.
2. Go to the **Actions** tab in your GitHub repository.
3. Select **"Build & Release Android APK"** from the left sidebar.
4. Click **"Run workflow"** and click the green button.
5. In ~2 minutes, the workflow will finish:
   - Go to the **Releases** tab on the right side of the repo to download `WiFi-FTP-Server-release.apk`!
   - Alternatively, scroll to the bottom of the completed workflow run under **Artifacts** to download `WiFi-FTP-Server-APKs.zip`.

### Option 2: Automatic Release on Version Tag
Pushing any git tag starting with `v` (e.g. `git tag v1.0.0 && git push origin v1.0.0`) automatically builds both debug and release APKs, creates a new GitHub Release, and attaches the installable APK files.

---

## 💻 How to Connect from Your Computer

1. Connect your computer and Android device to the same Wi-Fi network (or connect your PC to your phone's Wi-Fi Hotspot).
2. Open the **WiFi FTP Server** app on your phone and tap **"Start Server"**.
3. On Windows:
   - Open **File Explorer** (`Win + E`).
   - Click the address bar at the top, paste the FTP address (e.g. `ftp://192.168.1.100:2121`), and press **Enter**.
4. On macOS:
   - Open **Finder**, press `Cmd + K` (Connect to Server).
   - Enter `ftp://192.168.1.100:2121` and click **Connect**.
5. On mobile/tablet:
   - Scan the on-screen QR code or enter the FTP address into apps like Solid Explorer, CX File Explorer, or VLC.

---

## 📜 Permissions Handled

- `MANAGE_EXTERNAL_STORAGE` (Android 11+) / `READ_EXTERNAL_STORAGE` & `WRITE_EXTERNAL_STORAGE` (Android 10)
- `POST_NOTIFICATIONS` (Android 13+)
- `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`
