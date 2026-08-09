package com.v2ray.ang.service

import android.content.Context
import com.v2ray.ang.core.CoreConfigManager
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.core.DirectSpeedtestConfigManager
import com.v2ray.ang.dto.RealPingEvent
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.SpeedtestManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import libv2ray.CoreCallbackHandler
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Worker that runs a batch of real-ping tests independently.
 * Each batch owns its own CoroutineScope/dispatcher and can be cancelled separately.
 */
class RealPingWorkerService(
    private val context: Context,
    private val guids: List<String>,
    private val onlyTcp: Boolean = false,
    private val onEvent: (RealPingEvent) -> Unit = {}
) {
    private val job = SupervisorJob()
    private val concurrency = SettingsManager.getRealPingConcurrency()
    private val dispatcher = Executors.newFixedThreadPool(if (onlyTcp) concurrency * 2 else concurrency).asCoroutineDispatcher()
    private val scope = CoroutineScope(job + dispatcher + CoroutineName("RealPingBatchWorker"))

    private val runningCount = AtomicInteger(0)
    private val totalCount = AtomicInteger(0)

    fun start() {
        val jobs = guids.map { guid ->
            totalCount.incrementAndGet()
            scope.launch {
                runningCount.incrementAndGet()
                try {
                    val result = if (onlyTcp) startTcping(guid) else startRealPing(guid)
                    if (scope.isActive) {
                        onEvent(RealPingEvent.Result(guid, result))
                    }
                } catch (_: Throwable) {
                    // Keep one bad profile from cancelling the rest of the batch.
                } finally {
                    val count = totalCount.decrementAndGet()
                    val left = runningCount.decrementAndGet()
                    if (scope.isActive) {
                        onEvent(RealPingEvent.Progress("$left / $count"))
                    }
                }
            }
        }

        scope.launch {
            try {
                joinAll(*jobs.toTypedArray())
                if (isActive) {
                    onEvent(RealPingEvent.Finish("0"))
                }
            } catch (_: CancellationException) {
                // If cancelled, don't send finish event to avoid confusion.
            } finally {
                close()
            }
        }
    }

    fun cancel() {
        job.cancel()
    }

    private fun close() {
        try {
            dispatcher.close()
        } catch (_: Throwable) {
            // ignore
        }
    }

    /**
     * Measure delay through a fully started temporary CoreController instead of relying only on
     * Libv2ray.measureOutboundDelay(). The static helper strips most Xray app modules before
     * starting the temporary instance; that behavior was safe with the older 2.0.15-era core but
     * can reject newer transports/config shapes immediately on newer Xray builds.
     *
     * A live controller parses and starts the test config through the same core lifecycle used by
     * a normal connection, then measures delay on the running instance. The historical static
     * helper remains a final compatibility fallback so nodes that already worked do not regress.
     */
    private fun startRealPing(guid: String): Long {
        val directConfig = DirectSpeedtestConfigManager.build(context, guid)
        val configResult = if (directConfig.status) {
            directConfig
        } else {
            CoreConfigManager.getV2rayConfig4Speedtest(context, guid)
        }
        if (!configResult.status) return -1L

        val primaryUrl = SettingsManager.getDelayTestUrl()
        val secondaryUrl = SettingsManager.getDelayTestUrl(true)

        val controller = CoreNativeManager.newCoreController(TestCoreCallback())
        try {
            controller.startLoop(configResult.content, 0)
            if (controller.isRunning) {
                val first = tryMeasureController(controller, primaryUrl)
                if (first > 0L) return first

                val second = tryMeasureController(controller, secondaryUrl)
                if (second > 0L) return second
            }
        } catch (_: Throwable) {
            // Fall through to the static helper below. The controller path is preferred because
            // it preserves the complete Xray app graph, but the old helper is kept for compatibility.
        } finally {
            try {
                if (controller.isRunning) {
                    controller.stopLoop()
                }
            } catch (_: Throwable) {
                // ignore cleanup errors
            }
        }

        val staticPrimary = CoreNativeManager.measureOutboundDelay(configResult.content, primaryUrl)
        if (staticPrimary > 0L) return staticPrimary

        return CoreNativeManager.measureOutboundDelay(configResult.content, secondaryUrl)
    }

    private fun tryMeasureController(controller: libv2ray.CoreController, url: String): Long {
        return try {
            controller.measureDelay(url)
        } catch (_: Throwable) {
            -1L
        }
    }

    private fun startTcping(guid: String): Long {
        val retFailure = -1L

        val config = MmkvManager.decodeServerConfig(guid) ?: return retFailure
        if (!config.configType.isComplexType()
            && config.configType != EConfigType.HYSTERIA2
            && config.configType != EConfigType.WIREGUARD
            && config.alpn?.startsWith("h3") != true
            && config.server.isNotNullEmpty()
            && config.serverPort?.toIntOrNull() != null
        ) {
            val url = config.server.orEmpty()
            val port = config.serverPort.orEmpty().toInt()
            return SpeedtestManager.socketConnectTime(url, port, 1000)
        }

        return retFailure
    }

    private class TestCoreCallback : CoreCallbackHandler {
        override fun startup(): Long = 0L
        override fun shutdown(): Long = 0L
        override fun onEmitStatus(l: Long, s: String?): Long = 0L
    }
}
