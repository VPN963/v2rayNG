package com.v2ray.ang.core

import android.content.Context
import android.net.NetworkCapabilities
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Compatibility shim for older MobileTina source references.
 *
 * The standalone MobileTina Automation feature is disabled. Smart Connect is implemented
 * independently in MainActivity and is intentionally unaffected by this object.
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

    fun isAutoConnectOnAppStartEnabled(): Boolean = false
    fun isAutoReconnectEnabled(): Boolean = false
    fun isSmartServerEnabled(): Boolean = false
    fun retryCount(): Int = 1
    fun retryDelayMillis(): Long = 0L

    fun recoveryCandidates(): List<String> = listOfNotNull(MmkvManager.getSelectServer())

    fun selectFastestKnownServer(): String? = MmkvManager.getSelectServer()

    fun isNetworkAllowed(capabilities: NetworkCapabilities?): Boolean = capabilities != null

    fun isActiveNetworkAllowed(context: Context): Boolean = true

    fun scheduleAutoConnect(context: Context, reason: String): Job =
        CoroutineScope(Dispatchers.Default).launch { /* Automation intentionally disabled. */ }
}
