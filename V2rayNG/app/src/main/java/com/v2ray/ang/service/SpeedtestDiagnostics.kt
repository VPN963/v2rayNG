package com.v2ray.ang.service

import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import java.util.concurrent.ConcurrentHashMap

/**
 * Temporary MobileTina diagnostic store for Real Delay failures.
 *
 * Real-delay workers run off the UI thread. Results are kept by server GUID so the server row can
 * show the actual Xray failure stage/message when the measured delay is negative. A successful
 * result clears the entry automatically.
 */
object SpeedtestDiagnostics {

    data class Diagnostic(
        val stage: String,
        val messages: List<String>
    ) {
        val combinedMessage: String
            get() = messages.distinct().joinToString(" | ")

        val shortLabel: String
            get() {
                val value = combinedMessage.lowercase()
                return when {
                    "certificate" in value || "tls" in value || "x509" in value -> "TLS"
                    "reality" in value || "publickey" in value || "shortid" in value -> "REALITY"
                    "sni" in value || "servername" in value -> "SNI"
                    "dns" in value || "lookup" in value || "resolve" in value -> "DNS"
                    "config" in value || "parse" in value || "loadjson" in value || "invalid" in value -> "CONFIG"
                    "startup" in value || "start" in value || "instance" in value || "core" in value -> "CORE START"
                    "timeout" in value || "deadline" in value -> "TIMEOUT"
                    "network" in value || "dial" in value || "connect" in value || "refused" in value || "reset" in value -> "NETWORK"
                    else -> stage
                }
            }

        val displayMessage: String
            get() = combinedMessage.ifBlank { "Xray returned -1 without an error message" }
    }

    private data class MutableDiagnostic(
        var stage: String,
        val messages: MutableList<String> = mutableListOf()
    )

    private val entries = ConcurrentHashMap<String, MutableDiagnostic>()

    fun clear(guid: String) {
        entries.remove(guid)
    }

    fun record(guid: String, stage: String, throwable: Throwable?) {
        recordMessage(guid, stage, throwable?.message ?: throwable?.javaClass?.simpleName.orEmpty())
    }

    fun recordMessage(guid: String, stage: String, message: String?) {
        val clean = message
            ?.replace('\n', ' ')
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
            .ifBlank { "Xray returned -1 without an error message" }

        entries.compute(guid) { _, previous ->
            val item = previous ?: MutableDiagnostic(stage)
            item.stage = stage
            if (item.messages.none { it == clean } && item.messages.size < 4) {
                item.messages.add(clean)
            }
            item
        }
        LogUtil.e(AppConfig.TAG, "REAL_DELAY_DIAG guid=$guid stage=$stage error=$clean")
    }

    fun get(guid: String): Diagnostic? {
        val item = entries[guid] ?: return null
        return Diagnostic(item.stage, synchronized(item.messages) { item.messages.toList() })
    }
}
