package com.example.server

import android.content.Context
import com.example.model.ShareMode
import com.example.util.StorageUtils
import java.io.File

class VirtualFileSystem(
    private val context: Context,
    private val shareMode: ShareMode,
    private val isReadOnly: Boolean
) {

    private val volumes = if (shareMode is ShareMode.WholeStorage) {
        StorageUtils.getStorageVolumes(context)
    } else {
        emptyList()
    }

    fun getRootDirectory(): VirtualFile {
        return when (shareMode) {
            is ShareMode.WholeStorage -> {
                VirtualDirectory(name = "/") {
                    volumes.map { volume ->
                        RealVirtualFile(
                            file = File(volume.path),
                            displayName = volume.name,
                            readOnly = isReadOnly
                        )
                    }
                }
            }
            is ShareMode.SpecificFolder -> {
                val folder = File(shareMode.path)
                RealVirtualFile(folder, displayName = shareMode.displayName, readOnly = isReadOnly)
            }
            is ShareMode.SpecificFile -> {
                val file = File(shareMode.path)
                VirtualDirectory(name = "/") {
                    if (file.exists()) {
                        listOf(RealVirtualFile(file, displayName = shareMode.displayName, readOnly = isReadOnly))
                    } else {
                        emptyList()
                    }
                }
            }
        }
    }

    fun resolveFile(currentWorkingDirectory: String, requestedPath: String): VirtualFile? {
        val normalized = normalizePath(currentWorkingDirectory, requestedPath)

        when (shareMode) {
            is ShareMode.WholeStorage -> {
                if (normalized == "/" || normalized.isEmpty()) {
                    return getRootDirectory()
                }

                // normalized is like "/Internal Storage/DCIM/photo.jpg"
                val parts = normalized.trim('/').split('/').filter { it.isNotEmpty() }
                if (parts.isEmpty()) return getRootDirectory()

                val volumeName = parts[0]
                val volume = volumes.firstOrNull { it.name.equals(volumeName, ignoreCase = true) }
                    ?: return null

                var target = File(volume.path)
                for (i in 1 until parts.size) {
                    target = File(target, parts[i])
                }
                return RealVirtualFile(target, readOnly = isReadOnly)
            }

            is ShareMode.SpecificFolder -> {
                val rootFolder = File(shareMode.path)
                if (normalized == "/" || normalized.isEmpty()) {
                    return RealVirtualFile(rootFolder, displayName = shareMode.displayName, readOnly = isReadOnly)
                }

                val parts = normalized.trim('/').split('/').filter { it.isNotEmpty() }
                var target = rootFolder
                for (part in parts) {
                    target = File(target, part)
                }
                return RealVirtualFile(target, readOnly = isReadOnly)
            }

            is ShareMode.SpecificFile -> {
                val targetFile = File(shareMode.path)
                if (normalized == "/" || normalized.isEmpty()) {
                    return getRootDirectory()
                }
                val reqName = normalized.trim('/').substringAfterLast('/')
                return if (reqName.equals(targetFile.name, ignoreCase = true) || reqName.equals(shareMode.displayName, ignoreCase = true)) {
                    RealVirtualFile(targetFile, displayName = shareMode.displayName, readOnly = isReadOnly)
                } else {
                    null
                }
            }
        }
    }

    fun normalizePath(currentDir: String, requested: String): String {
        val raw = if (requested.startsWith("/")) {
            requested
        } else {
            val base = if (currentDir.endsWith("/")) currentDir else "$currentDir/"
            base + requested
        }

        val stack = mutableListOf<String>()
        val segments = raw.split('/').filter { it.isNotEmpty() }

        for (segment in segments) {
            when (segment) {
                "." -> {} // Current directory, ignore
                ".." -> {
                    if (stack.isNotEmpty()) {
                        stack.removeAt(stack.size - 1)
                    }
                }
                else -> stack.add(segment)
            }
        }

        return "/" + stack.joinToString("/")
    }
}
