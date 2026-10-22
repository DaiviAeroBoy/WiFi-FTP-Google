package com.example.util

import android.content.Context
import android.os.PowerManager
import java.util.concurrent.atomic.AtomicInteger

object WakeLockManager {

    private var transferWakeLock: PowerManager.WakeLock? = null
    private val activeTransferCount = AtomicInteger(0)

    @Synchronized
    fun onTransferStarted(context: Context) {
        val count = activeTransferCount.incrementAndGet()
        if (count == 1) {
            try {
                if (transferWakeLock == null) {
                    val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    transferWakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WiFiFtpServer:TransferCpuWakeLock")?.apply {
                        setReferenceCounted(false)
                    }
                }
                transferWakeLock?.acquire(2 * 60 * 60 * 1000L) // 2 hours max safe timeout
            } catch (_: Exception) {
            }
        }
    }

    @Synchronized
    fun onTransferFinished() {
        val count = activeTransferCount.decrementAndGet()
        if (count <= 0) {
            activeTransferCount.set(0)
            try {
                if (transferWakeLock?.isHeld == true) {
                    transferWakeLock?.release()
                }
            } catch (_: Exception) {
            }
        }
    }

    @Synchronized
    fun releaseAll() {
        activeTransferCount.set(0)
        try {
            if (transferWakeLock?.isHeld == true) {
                transferWakeLock?.release()
            }
        } catch (_: Exception) {
        }
    }
}
