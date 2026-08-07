package com.v2ray.ang.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MobileTina-specific automation helpers.
 *
 * Keeps custom behavior isolated from upstream v2rayNG code so future upstream merges
 * remain easier to review and maintain.
 */
object MobileTinaAutomation {
    const val PREF_AUTO_CONNECT_ON_APP_START = "mobiletina_auto_connect_on_app_start"
    const val PREF_AUTO_RECONNECT = "mobiletina_auto_reconnect"
    const val PREF_SMART_SERVER = "mobiletina_smart_server"
    const val PREF_WIFI_ALLOWED = "mobiletina_wifi_allowed"
    const val PREF_MOBILE_ALLOWED = "mobiletina_mobile_allowed"
    const val PREF_RETRY_COUNT = "mobiletina_retry_count"
    const val PREF_RETRY_DELAY_SECONDS = "mobiletina_retry_delay_seconds"
    const val PREF_AUTO_CONNECT_DELAY_SECONDS = "mobiletina_auto_connect_delay_seconds"

    private const val DEFAULT_RETRY_COUNT = 3
    private const val DEFAULT_RETRY_DELAY_SECONDS = 3
    private const val DEFAULT_AUTO_CONNECT_DELAY_SECONDS = 2
    private const val MAX_AUTO_CONNECT_NETWORK_WAIT_ATTEMPTS = 6

    fun isAutoConnectOnAppStartEnabled(): Boolean =
        MmkvManager.decodeSettingsBool(PREF_AUTO_CONNECT_ON_APP_START, false)

    fun isAutoReconnectEnabled(): Boolean =
        MmkvManager.decodeSettingsBool(PREF_AUTO_RECONNECT, true)

    fun isSmartServerEnabled(): Boolean =
        MmkvManager.decodeSettingsBool(PREF_SMART_SERVER, true)

    fun retryCount(): Int =
        MmkvManager.decodeSettingsString(PREF_RETRY_COUNT, DEFAULT_RETRY_COUNT.toString())
            ?.toIntOrNull()
            ?.coerceIn(1, 10)
            ?: DEFAULT_RETRY_COUNT

    fun retryDelayMillis(): Long =
        (MmkvManager.decodeSettingsString(
            PREF_RETRY_DELAY_SECONDS,
            DEFAULT_RETRY_DELAY_SECONDS.toString()
        )?.toLongOrNull()?.coerceIn(1L, 60L) ?: DEFAULT_RETRY_DELAY_SECONDS.toLong()) * 1000L

    private fun autoConnectDelayMillis(): Long =
        (MmkvManager.decodeSettingsString(
            PREF_AUTO_CONNECT_DELAY_SECONDS,
            DEFAULT_AUTO_CONNECT_DELAY_SECONDS.toString()
        )?.toLongOrNull()?.coerceIn(0L, 60L) ?: DEFAULT_AUTO_CONNECT_DELAY_SECONDS.toLong()) * 1000L

    /**
     * Select the fastest server for which a positive delay test result is already known.
     * If no tested server exists, the current selection is kept.
     */
    fun selectFastestKnownServer(): String? {
        val current = MmkvManager.getSelectServer()
        if (!isSmartServerEnabled()) return current

        val best = MmkvManager.decodeAllServerList()
            .asSequence()
            .mapNotNull { guid ->
                val delay = MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis ?: 0L
                if (delay > 0L) guid to delay else null
            }
            .minByOrNull { it.second }
            ?.first

        if (!best.isNullOrBlank() && best != current) {
            MmkvManager.setSelectServer(best)
            LogUtil.i(AppConfig.TAG, "MobileTina: selected fastest known server: $best")
            return best
        }
        return current
    }

    fun isNetworkAllowed(capabilities: NetworkCapabilities?): Boolean {
        if (capabilities == null) return false
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return false

        val wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val cellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        if (wifi && !MmkvManager.decodeSettingsBool(PREF_WIFI_ALLOWED, true)) return false
        if (cellular && !MmkvManager.decodeSettingsBool(PREF_MOBILE_ALLOWED, true)) return false

        // Ethernet, USB and other transports are allowed by default.
        return true
    }

    fun isActiveNetworkAllowed(context: Context): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivity.activeNetwork ?: return false
        return isNetworkAllowed(connectivity.getNetworkCapabilities(network))
    }

    /**
     * Schedule an automatic connection. Network availability is rechecked a few times so boot-time
     * connection still works when Android reports BOOT_COMPLETED before Wi-Fi/mobile data is ready.
     */
    fun scheduleAutoConnect(context: Context, reason: String): Job {
        val appContext = context.applicationContext
        return CoroutineScope(Dispatchers.Default).launch {
            val initialDelay = autoConnectDelayMillis()
            if (initialDelay > 0) delay(initialDelay)

            repeat(MAX_AUTO_CONNECT_NETWORK_WAIT_ATTEMPTS) { attempt ->
                if (isActiveNetworkAllowed(appContext)) {
                    val selected = selectFastestKnownServer()
                    if (!selected.isNullOrBlank()) {
                        LogUtil.i(AppConfig.TAG, "MobileTina: auto connect ($reason), attempt=${attempt + 1}")
                        LauncherManager.startService(appContext)
                    } else {
                        LogUtil.w(AppConfig.TAG, "MobileTina: auto connect skipped, no server selected")
                    }
                    return@launch
                }

                LogUtil.i(AppConfig.TAG, "MobileTina: waiting for allowed network ($reason), attempt=${attempt + 1}")
                delay(5_000L)
            }

            LogUtil.w(AppConfig.TAG, "MobileTina: auto connect gave up waiting for network ($reason)")
        }
    }
}
