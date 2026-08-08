package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.LogUtil

/**
 * Reads optional subscription-userinfo response metadata without downloading the subscription body.
 * Common providers expose values such as upload, download, total and expire in this header.
 */
object MobileTinaSubscriptionInfo {

    fun refreshAll() {
        MmkvManager.decodeSubscriptions()
            .asSequence()
            .filter { it.subscription.enabled && it.subscription.url.isNotBlank() }
            .forEach { cache ->
                runCatching { refreshOne(cache.guid, cache.subscription) }
                    .onFailure {
                        LogUtil.d(
                            AppConfig.TAG,
                            "MobileTina subscription metadata refresh failed for ${cache.subscription.remarks}: ${it.message}"
                        )
                    }
            }
    }

    private fun refreshOne(guid: String, item: SubscriptionItem) {
        val httpPort = SettingsManager.getHttpPort()
        val proxyUsername = SettingsManager.getSocksUsername()
        val proxyPassword = SettingsManager.getSocksPassword()

        val proxied = HttpUtil.getSubscriptionUserInfoHeader(
            UrlContentRequest(
                url = item.url,
                userAgent = item.userAgent,
                requestHeaders = item.requestHeaders,
                timeout = 10_000,
                httpPort = httpPort,
                proxyUsername = proxyUsername,
                proxyPassword = proxyPassword
            )
        )

        val result = if (!proxied.requestSucceeded && httpPort != 0) {
            HttpUtil.getSubscriptionUserInfoHeader(
                UrlContentRequest(
                    url = item.url,
                    userAgent = item.userAgent,
                    requestHeaders = item.requestHeaders,
                    timeout = 10_000
                )
            )
        } else {
            proxied
        }

        // Preserve cached information when the request itself failed. If the request succeeded but
        // the provider no longer sends subscription-userinfo, clear the old metadata so UI hides it.
        if (!result.requestSucceeded) return

        applyUserInfo(item, result.userInfo)
        MmkvManager.encodeSubscription(guid, item)
    }

    private fun applyUserInfo(item: SubscriptionItem, rawHeader: String?) {
        item.trafficUploadBytes = null
        item.trafficDownloadBytes = null
        item.trafficTotalBytes = null
        item.expireEpochSeconds = null

        if (rawHeader.isNullOrBlank()) return

        val fields = rawHeader
            .split(';')
            .mapNotNull { segment ->
                val separator = segment.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val key = segment.substring(0, separator).trim().lowercase()
                val value = segment.substring(separator + 1).trim().toLongOrNull()
                    ?: return@mapNotNull null
                key to value
            }
            .toMap()

        item.trafficUploadBytes = fields["upload"]?.takeIf { it >= 0L }
        item.trafficDownloadBytes = fields["download"]?.takeIf { it >= 0L }
        item.trafficTotalBytes = fields["total"]?.takeIf { it > 0L }
        item.expireEpochSeconds = fields["expire"]?.takeIf { it > 0L }
    }
}
