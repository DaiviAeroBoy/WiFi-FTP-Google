package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ShareMode
import com.example.server.VirtualFileSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WiFi FTP Server", appName)
    }

    @Test
    fun `virtual file system path normalization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = context.cacheDir
        val vfs = VirtualFileSystem(context, ShareMode.SpecificFolder(tempDir.absolutePath, "Cache"), isReadOnly = false)

        val root = vfs.getRootDirectory()
        assertNotNull(root)

        val normalized = vfs.normalizePath("/folder1/subfolder", "../other")
        assertEquals("/folder1/other", normalized)

        val normalizedRoot = vfs.normalizePath("/", "../../../")
        assertEquals("/", normalizedRoot)
    }
}
