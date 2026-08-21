package com.v2ray.ang.handler

import android.content.Context
import android.os.SystemClock
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.util.LogUtil
import java.util.concurrent.atomic.AtomicLong

/**
 * Compatibility facade used by the MobileTina UI while the VPN/Core service runs in a separate
 * Android process.
 *
 * Running state is only the confirmed multi-process MMKV flag. Starts are also coordinated with
 * native shutdown: Samsung firmware can take noticeably longer than the UI to release the old
 * core's sockets/TUN state, so a rapid second FAB tap is deferred briefly instead of racing a new
 * foreground service against stopLoop().
 */
object V2RayServiceManager {
    private const val CACHE_SERVICE_STOP_DEADLINE = "cache_service_stop_deadline"
    private const val DEFERRED_START_WAIT_MS = 4_000L
    private const val STOP_POLL_MS = 50L
    private val commandGeneration = AtomicLong(0L)

    fun startVService(context: Context) {
        val generation = commandGeneration.incrementAndGet()
        val appContext = context.applicationContext

        if (!isStopping()) {
            CoreServiceManager.startVService(appContext)
            return
        }

        // Do not block the UI thread. The existing Smart/Manual connection watchdogs remain the
        // user-facing timeout and will reset the FAB if native teardown cannot finish in time.
        Thread({
            val deadline = SystemClock.elapsedRealtime() + DEFERRED_START_WAIT_MS
            while (
                generation == commandGeneration.get() &&
                isStopping() &&
                SystemClock.elapsedRealtime() < deadline
            ) {
                try {
                    Thread.sleep(STOP_POLL_MS)
                } catch (_: InterruptedException) {
                    return@Thread
                }
            }

            if (generation != commandGeneration.get()) return@Thread
            if (isStopping()) {
                LogUtil.w(AppConfig.TAG, "Deferred VPN start abandoned because native stop is still in progress")
                return@Thread
            }
            CoreServiceManager.startVService(appContext)
        }, "MobileTinaDeferredStart").start()
    }

    fun stopVService(context: Context) {
        // Invalidate any delayed start before dispatching another stop request.
        commandGeneration.incrementAndGet()
        CoreServiceManager.stopVService(context)
    }

    /** Returns only a confirmed service/core state shared by the daemon process. */
    fun isRunning(): Boolean =
        MmkvManager.decodeSettingsBool(AppConfig.CACHE_SERVICE_RUNNING, false)

    private fun isStopping(): Boolean {
        val deadline = MmkvManager.decodeSettingsLong(CACHE_SERVICE_STOP_DEADLINE, 0L)
        return deadline > System.currentTimeMillis()
    }
}
