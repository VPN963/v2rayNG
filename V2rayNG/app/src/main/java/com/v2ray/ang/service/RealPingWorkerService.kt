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
                    if (!onlyTcp) SpeedtestDiagnostics.clear(guid)
                    val result = if (onlyTcp) startTcping(guid) else startRealPing(guid)
                    if (result > 0L && !onlyTcp) SpeedtestDiagnostics.clear(guid)
                    if (scope.isActive) {
                        onEvent(RealPingEvent.Result(guid, result))
                    }
                } catch (t: Throwable) {
                    if (!onlyTcp) {
                        SpeedtestDiagnostics.record(guid, "WORKER", t)
                    }
                    if (scope.isActive) {
                        onEvent(RealPingEvent.Result(guid, -1L))
                    }
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
     * Diagnostic Real Delay path.
     *
     * Every stage that can fail before a positive delay is captured by GUID. The UI can then show
     * whether a node failed while building config, starting Xray, doing TLS/network delay, or in
     * the legacy static MeasureOutboundDelay helper.
     */
    private fun startRealPing(guid: String): Long {
        val directConfig = DirectSpeedtestConfigManager.build(context, guid)
        val configResult = if (directConfig.status) {
            directConfig
        } else {
            SpeedtestDiagnostics.recordMessage(guid, "DIRECT CONFIG", directConfig.errorMessage)
            val fallbackConfig = CoreConfigManager.getV2rayConfig4Speedtest(context, guid)
            if (!fallbackConfig.status) {
                SpeedtestDiagnostics.recordMessage(guid, "SPEED CONFIG", fallbackConfig.errorMessage)
                return -1L
            }
            fallbackConfig
        }

        val primaryUrl = SettingsManager.getDelayTestUrl()
        val secondaryUrl = SettingsManager.getDelayTestUrl(true)

        var controller: libv2ray.CoreController? = null
        try {
            controller = CoreNativeManager.newCoreController(TestCoreCallback())
            try {
                controller.startLoop(configResult.content, 0)
            } catch (t: Throwable) {
                SpeedtestDiagnostics.record(guid, "CORE START", t)
            }

            if (controller.isRunning) {
                val first = tryMeasureController(guid, controller, primaryUrl, "LIVE PRIMARY")
                if (first > 0L) return first

                val second = tryMeasureController(guid, controller, secondaryUrl, "LIVE SECONDARY")
                if (second > 0L) return second
            } else {
                SpeedtestDiagnostics.recordMessage(guid, "CORE START", "CoreController did not enter running state")
            }
        } catch (t: Throwable) {
            SpeedtestDiagnostics.record(guid, "CORE CREATE", t)
        } finally {
            try {
                if (controller?.isRunning == true) {
                    controller.stopLoop()
                }
            } catch (t: Throwable) {
                SpeedtestDiagnostics.record(guid, "CORE STOP", t)
            }
        }

        val staticPrimary = CoreNativeManager.measureOutboundDelayDetailed(configResult.content, primaryUrl)
        if (staticPrimary.delayMillis > 0L) return staticPrimary.delayMillis
        SpeedtestDiagnostics.recordMessage(guid, "STATIC PRIMARY", staticPrimary.errorMessage)

        val staticSecondary = CoreNativeManager.measureOutboundDelayDetailed(configResult.content, secondaryUrl)
        if (staticSecondary.delayMillis > 0L) return staticSecondary.delayMillis
        SpeedtestDiagnostics.recordMessage(guid, "STATIC SECONDARY", staticSecondary.errorMessage)

        return -1L
    }

    private fun tryMeasureController(
        guid: String,
        controller: libv2ray.CoreController,
        url: String,
        stage: String
    ): Long {
        return try {
            val delay = controller.measureDelay(url)
            if (delay < 0L) {
                SpeedtestDiagnostics.recordMessage(guid, stage, "CoreController returned $delay without throwing an exception")
            }
            delay
        } catch (t: Throwable) {
            SpeedtestDiagnostics.record(guid, stage, t)
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
