package com.v2ray.ang.core

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.ConfigResult
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils

/**
 * Builds a latency-test config directly from the selected profile.
 *
 * This deliberately mirrors the v2rayNG 2.0.15 speed-test flow: the selected node is
 * converted directly into the proxy outbound and the normal runtime context/routing/
 * subscription-chain analysis is skipped. That prevents a node from being rejected by
 * unrelated routing/chain analysis before Libv2ray.measureOutboundDelay() is even called.
 */
object DirectSpeedtestConfigManager {

    fun build(context: Context, guid: String): ConfigResult {
        return try {
            val profile = MmkvManager.decodeServerConfig(guid)
                ?: return failure(guid, "Profile not found")

            if (profile.configType == EConfigType.CUSTOM) {
                val raw = MmkvManager.decodeServerRaw(guid)
                    ?: return failure(guid, "Custom config is empty")
                return ConfigResult(true, guid, raw)
            }

            // Complex profiles need their relationship data. Keep the modern builder only for
            // those explicit types; ordinary subscription nodes must stay single-node here.
            if (profile.configType == EConfigType.POLICYGROUP ||
                profile.configType == EConfigType.PROXYCHAIN
            ) {
                return CoreConfigManager.getV2rayConfig4Speedtest(context, guid)
            }

            val address = profile.server
                ?: return failure(guid, "Server address is empty")
            if (!Utils.isPureIpAddress(address) && !Utils.isValidUrl(address)) {
                return failure(guid, "Invalid server address: $address")
            }
            if (profile.serverPort?.toIntOrNull() == null) {
                return failure(guid, "Invalid server port")
            }

            val templateRaw = Utils.readTextFromAssets(context, "v2ray_config.json")
            if (templateRaw.isBlank()) {
                return failure(guid, "Missing v2ray_config.json")
            }
            val v2rayConfig = JsonUtil.fromJson(templateRaw, V2rayConfig::class.java)
                ?: return failure(guid, "Failed to parse speed-test template")

            val outbound = CoreOutboundBuilder.convert(profile)
                ?: return failure(guid, "Failed to convert profile to outbound")
            outbound.tag = AppConfig.TAG_PROXY

            // Replace only the template proxy. Keep direct/block outbounds exactly as the
            // historical speed-test template did.
            if (v2rayConfig.outbounds.isNotEmpty()) {
                v2rayConfig.outbounds.removeAt(0)
            }
            v2rayConfig.outbounds.add(0, outbound)

            // Same lightweight trimming used by v2rayNG 2.0.15.
            v2rayConfig.log.loglevel =
                MmkvManager.decodeSettingsString(AppConfig.PREF_LOGLEVEL) ?: "warning"
            v2rayConfig.inbounds.clear()
            v2rayConfig.routing.rules.clear()
            v2rayConfig.routing.balancers = null
            v2rayConfig.dns = null
            v2rayConfig.fakedns = null
            v2rayConfig.stats = null
            v2rayConfig.policy = null
            v2rayConfig.observatory = null
            v2rayConfig.burstObservatory = null
            v2rayConfig.outbounds.forEach { it.mux = null }

            val json = JsonUtil.toJsonPretty(v2rayConfig)
                ?: return failure(guid, "Failed to serialize speed-test config")
            ConfigResult(true, guid, json)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Direct speed-test config failed for $guid", e)
            failure(guid, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun failure(guid: String, reason: String): ConfigResult {
        LogUtil.w(AppConfig.TAG, "Direct speed-test config rejected $guid: $reason")
        return ConfigResult(false, guid, errorMessage = reason)
    }
}
