package com.example.server

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

interface VirtualFile {
    val name: String
    val isDirectory: Boolean
    val size: Long
    val lastModified: Long

    fun canRead(): Boolean
    fun canWrite(): Boolean
    fun listFiles(): List<VirtualFile>
    fun getInputStream(offset: Long = 0L): InputStream
    fun getOutputStream(append: Boolean = false): OutputStream
    fun delete(): Boolean
    fun mkdir(): Boolean
    fun renameTo(destFile: File): Boolean
    fun getUnderlyingFile(): File?
}

class RealVirtualFile(
    private val file: File,
    private val displayName: String? = null,
    private val readOnly: Boolean = false
) : VirtualFile {

    override val name: String
        get() = displayName ?: file.name

    override val isDirectory: Boolean
        get() = file.isDirectory

    override val size: Long
        get() = if (file.isDirectory) 0L else file.length()

    override val lastModified: Long
        get() = file.lastModified()

    override fun canRead(): Boolean = file.canRead()

    override fun canWrite(): Boolean = !readOnly && (if (file.exists()) file.canWrite() else file.parentFile?.canWrite() == true)

    override fun listFiles(): List<VirtualFile> {
        if (!file.isDirectory) return emptyList()
        val children = file.listFiles() ?: return emptyList()
        return children
            .filter { !it.name.startsWith(".") } // optional hidden files filter
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { RealVirtualFile(it, readOnly = readOnly) }
    }

    override fun getInputStream(offset: Long): InputStream {
        val fis = FileInputStream(file)
        if (offset > 0) {
            fis.skip(offset)
        }
        return fis
    }

    override fun getOutputStream(append: Boolean): OutputStream {
        if (readOnly) throw IllegalStateException("Server is in Read-Only mode")
        return FileOutputStream(file, append)
    }

    override fun delete(): Boolean {
        if (readOnly) return false
        return if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    override fun mkdir(): Boolean {
        if (readOnly) return false
        return file.mkdirs()
    }

    override fun renameTo(destFile: File): Boolean {
        if (readOnly) return false
        return file.renameTo(destFile)
    }

    override fun getUnderlyingFile(): File = file
}

class VirtualDirectory(
    override val name: String,
    private val childrenProvider: () -> List<VirtualFile>
) : VirtualFile {

    override val isDirectory: Boolean = true
    override val size: Long = 0L
    override val lastModified: Long = System.currentTimeMillis()

    override fun canRead(): Boolean = true
    override fun canWrite(): Boolean = false
    override fun listFiles(): List<VirtualFile> = childrenProvider()

    override fun getInputStream(offset: Long): InputStream =
        throw UnsupportedOperationException("Cannot read directory stream directly")

    override fun getOutputStream(append: Boolean): OutputStream =
        throw UnsupportedOperationException("Cannot write to virtual root directory")

    override fun delete(): Boolean = false
    override fun mkdir(): Boolean = false
    override fun renameTo(destFile: File): Boolean = false
    override fun getUnderlyingFile(): File? = null
}
