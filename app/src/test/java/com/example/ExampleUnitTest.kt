package com.example

import com.example.model.ServerState
import com.example.model.ServerStatus
import com.example.model.ShareMode
import com.example.model.TransferDirection
import com.example.model.TransferTelemetry
import com.example.util.StorageUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun serverState_ftpUrlFormatting_isCorrect() {
        val state = ServerState(
            status = ServerStatus.RUNNING,
            hostAddress = "192.168.1.50",
            port = 2121
        )
        assertEquals("ftp://192.168.1.50:2121", state.ftpUrl)
        assertTrue(state.isRunning)
    }

    @Test
    fun serverState_stoppedStatus_isNotRunning() {
        val state = ServerState(status = ServerStatus.STOPPED)
        assertFalse(state.isRunning)
    }

    @Test
    fun storageUtils_formatFileSize_formatsCorrectly() {
        assertEquals("0 B", StorageUtils.formatFileSize(0))
        assertEquals("500.0 B", StorageUtils.formatFileSize(500))
        assertEquals("1.0 KB", StorageUtils.formatFileSize(1024))
        assertEquals("1.0 MB", StorageUtils.formatFileSize(1024 * 1024))
        assertEquals("1.5 GB", StorageUtils.formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun shareMode_stringRepresentations() {
        val whole = ShareMode.WholeStorage
        val folder = ShareMode.SpecificFolder("/storage/emulated/0/Download", "Download")
        val file = ShareMode.SpecificFile("/storage/emulated/0/doc.pdf", "doc.pdf")

        assertEquals("Whole Storage", whole.toString())
        assertEquals("Download", folder.displayName)
        assertEquals("doc.pdf", file.displayName)
    }

    @Test
    fun transferTelemetry_defaultState() {
        val telemetry = TransferTelemetry(
            activeFileName = "test.zip",
            direction = TransferDirection.DOWNLOADING,
            downloadSpeedBps = 1048576L
        )
        assertEquals("test.zip", telemetry.activeFileName)
        assertEquals(TransferDirection.DOWNLOADING, telemetry.direction)
        assertEquals(1048576L, telemetry.downloadSpeedBps)
        assertEquals(0L, telemetry.uploadSpeedBps)
    }
}
