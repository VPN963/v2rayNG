package com.v2ray.ang.handler

import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.dto.SubscriptionItem
import com.v2ray.ang.util.HttpUtil

object MobileTinaSubscriptionInfo {

    fun refreshAll() {
        MmkvManager.decodeSubscriptions()
            .filter { it.subscription.enabled && it.subscription.url.isNotBlank() }
            .forEach { cache -> runCatching { refreshOne(cache.guid, cache.subscription) } }
    }

    fun refresh(subscriptionId: String) {
        val item = MmkvManager.decodeSubscription(subscriptionId) ?: return
        if (!item.enabled || item.url.isBlank()) return
        runCatching { refreshOne(subscriptionId, item) }
    }

    private fun refreshOne(guid: String, item: SubscriptionItem) {
        val proxyPort = SettingsManager.getHttpPort()
        val raw: String = fetchHeader(item, proxyPort)
            ?: (if (proxyPort != 0) fetchHeader(item, 0) else null)
            ?: return

        item.trafficUploadBytes = null
        item.trafficDownloadBytes = null
        item.trafficTotalBytes = null
        item.expireEpochSeconds = null

        val fields = raw.split(';').mapNotNull { segment ->
            val i = segment.indexOf('=')
            if (i <= 0) return@mapNotNull null
            val key = segment.substring(0, i).trim().lowercase()
            val value = segment.substring(i + 1).trim().toLongOrNull() ?: return@mapNotNull null
            key to value
        }.toMap()

        item.trafficUploadBytes = fields["upload"]?.takeIf { it >= 0L }
        item.trafficDownloadBytes = fields["download"]?.takeIf { it >= 0L }
        item.trafficTotalBytes = fields["total"]?.takeIf { it > 0L }
        item.expireEpochSeconds = fields["expire"]?.takeIf { it > 0L }
        MmkvManager.encodeSubscription(guid, item)
    }

    private fun fetchHeader(item: SubscriptionItem, httpPort: Int): String? {
        var current = item.url
        repeat(4) {
            val conn = HttpUtil.createProxyConnection(current, httpPort, 10_000, 10_000) ?: return null
            try {
                conn.requestMethod = "GET"
                conn.setRequestProperty(
                    "User-Agent",
                    item.userAgent?.takeIf { it.isNotBlank() } ?: "v2rayNG/${BuildConfig.VERSION_NAME}"
                )
                conn.connect()
                val code = conn.responseCode
                if (code in 300..399) {
                    current = HttpUtil.resolveLocation(conn) ?: return null
                } else {
                    return conn.getHeaderField("subscription-userinfo")
                        ?: conn.getHeaderField("Subscription-Userinfo")
                }
            } catch (e: Exception) {
                Log.d(AppConfig.TAG, "MobileTina subscription-userinfo failed: ${e.message}")
                return null
            } finally {
                conn.disconnect()
            }
        }
        return null
    }
}
