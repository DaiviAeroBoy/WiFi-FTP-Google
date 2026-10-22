package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File

object StorageUtils {

    data class StorageVolume(
        val name: String,
        val path: String,
        val isRemovable: Boolean,
        val totalSpace: Long,
        val freeSpace: Long
    )

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            read && write
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun getManageStorageIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (_: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun getStorageVolumes(context: Context): List<StorageVolume> {
        val list = mutableListOf<StorageVolume>()

        // 1. Primary Internal Storage
        val internalDir = Environment.getExternalStorageDirectory()
        if (internalDir != null && internalDir.exists()) {
            list.add(
                StorageVolume(
                    name = "Internal Storage",
                    path = internalDir.absolutePath,
                    isRemovable = false,
                    totalSpace = internalDir.totalSpace,
                    freeSpace = internalDir.freeSpace
                )
            )
        }

        // 2. Secondary SD Cards
        val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (dir in externalDirs) {
            if (dir != null) {
                val path = dir.absolutePath
                val storageIdx = path.indexOf("/storage/")
                if (storageIdx >= 0) {
                    val sub = path.substring(storageIdx + "/storage/".length)
                    val id = sub.substringBefore('/')
                    if (id != "emulated" && id != "self") {
                        val sdRoot = File("/storage/$id")
                        if (sdRoot.exists() && sdRoot.canRead()) {
                            if (list.none { it.path == sdRoot.absolutePath }) {
                                list.add(
                                    StorageVolume(
                                        name = "SD Card ($id)",
                                        path = sdRoot.absolutePath,
                                        isRemovable = true,
                                        totalSpace = sdRoot.totalSpace,
                                        freeSpace = sdRoot.freeSpace
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Fallback check inside /storage
        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.isDirectory) {
                storageDir.listFiles()?.forEach { file ->
                    val name = file.name
                    if (name != "emulated" && name != "self" && file.isDirectory && file.canRead()) {
                        if (list.none { it.path == file.absolutePath }) {
                            list.add(
                                StorageVolume(
                                    name = "SD Card ($name)",
                                    path = file.absolutePath,
                                    isRemovable = true,
                                    totalSpace = file.totalSpace,
                                    freeSpace = file.freeSpace
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return list
    }

    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            result = cursor.getString(idx)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Selected File"
    }

    fun getPathFromTreeUri(treeUri: Uri): String? {
        val path = treeUri.path ?: return null
        if (path.contains("primary:")) {
            val relative = path.substringAfter("primary:")
            return "${Environment.getExternalStorageDirectory().absolutePath}/$relative"
        }
        if (path.contains("/tree/")) {
            val docId = path.substringAfter("/tree/")
            if (docId.contains(':')) {
                val storageId = docId.substringBefore(':')
                val relative = docId.substringAfter(':')
                return if (storageId == "primary") {
                    "${Environment.getExternalStorageDirectory().absolutePath}/$relative"
                } else {
                    "/storage/$storageId/$relative"
                }
            }
        }
        return null
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format(java.util.Locale.US, "%.1f %s", value, units[index])
    }
}
