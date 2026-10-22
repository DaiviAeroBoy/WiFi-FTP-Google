package com.example.model

sealed class ShareMode {
    object WholeStorage : ShareMode() {
        override fun toString(): String = "Whole Storage"
    }

    data class SpecificFolder(
        val path: String,
        val displayName: String = path.substringAfterLast('/', path)
    ) : ShareMode()

    data class SpecificFile(
        val path: String,
        val displayName: String = path.substringAfterLast('/', path),
        val sizeBytes: Long = 0L
    ) : ShareMode()
}
