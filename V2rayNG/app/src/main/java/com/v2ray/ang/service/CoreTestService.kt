package com.v2ray.ang.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.dto.RealPingEvent
import com.v2ray.ang.dto.TestServiceMessage
import com.v2ray.ang.extension.serializable
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.LogUtil
import java.util.Collections

/**
 * Real-delay test service.
 *
 * Intentionally follows the v2rayNG 2.0.15 service model: this is a normal started Service,
 * not a foreground service, so running a manual/Smart-Connect latency test does not create
 * a persistent test notification.
 */
class CoreTestService : Service() {

    private val activeWorkers = Collections.synchronizedList(mutableListOf<RealPingWorkerService>())

    override fun onCreate() {
        super.onCreate()
        CoreNativeManager.initCoreEnv(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        LogUtil.i(
            AppConfig.TAG,
            "CoreTestService is being destroyed, cancelling ${activeWorkers.size} active workers"
        )
        val snapshot = ArrayList(activeWorkers)
        snapshot.forEach { it.cancel() }
        activeWorkers.clear()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.serializable<TestServiceMessage>("content")
        if (message == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        when (message.key) {
            AppConfig.MSG_MEASURE_CONFIG_START -> handleMeasureStart(message, startId)
            AppConfig.MSG_MEASURE_CONFIG_CANCEL -> handleMeasureCancel()
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun handleMeasureStart(message: TestServiceMessage, startId: Int) {
        LogUtil.i(
            AppConfig.TAG,
            "CoreTestService starting worker subscription ${message.subscriptionId}"
        )

        val guidsList = when {
            message.serverGuids.isNotEmpty() -> message.serverGuids
            message.subscriptionId.isNotEmpty() -> MmkvManager.decodeServerList(message.subscriptionId)
            else -> MmkvManager.decodeAllServerList()
        }

        if (guidsList.isNotEmpty()) {
            lateinit var worker: RealPingWorkerService
            worker = RealPingWorkerService(
                context = this,
                guids = guidsList,
                onlyTcp = message.onlyTcp,
                onEvent = { event ->
                    handleWorkerEvent(event, message) { activeWorkers.remove(worker) }
                }
            )
            activeWorkers.add(worker)
            worker.start()
        } else {
            stopSelf(startId)
        }
    }

    private fun handleWorkerEvent(
        event: RealPingEvent,
        message: TestServiceMessage,
        onWorkerDone: () -> Unit
    ) {
        when (event) {
            is RealPingEvent.Progress -> {
                MessageHelper.sendMsg2UI(
                    this,
                    AppConfig.MSG_MEASURE_CONFIG_NOTIFY,
                    event.text
                )
            }

            is RealPingEvent.Result -> {
                MmkvManager.encodeServerTestDelayMillis(event.guid, event.delayMillis)
                MessageHelper.sendMsg2UI(
                    this,
                    AppConfig.MSG_MEASURE_CONFIG_SUCCESS,
                    event.guid
                )
            }

            is RealPingEvent.Finish -> {
                if (message.subscriptionId.isNotEmpty()) {
                    if (MmkvManager.decodeSettingsBool(
                            AppConfig.PREF_AUTO_REMOVE_INVALID_AFTER_TEST,
                            false
                        )
                    ) {
                        AngConfigManager.removeInvalidServer(message.subscriptionId)
                    }

                    if (MmkvManager.decodeSettingsBool(
                            AppConfig.PREF_AUTO_SORT_AFTER_TEST,
                            false
                        )
                    ) {
                        AngConfigManager.sortByTestResultsForSub(message.subscriptionId)
                    }
                }

                MessageHelper.sendMsg2UI(
                    this,
                    AppConfig.MSG_MEASURE_CONFIG_FINISH,
                    event.status
                )
                onWorkerDone()
                if (activeWorkers.isEmpty()) {
                    stopSelf()
                }
            }
        }
    }

    private fun handleMeasureCancel() {
        MessageHelper.sendMsg2UI(this, AppConfig.MSG_MEASURE_CONFIG_FINISH, "0")
        val snapshot = ArrayList(activeWorkers)
        LogUtil.i(
            AppConfig.TAG,
            "CoreTestService received cancel message, cancelling ${snapshot.size} active workers"
        )
        snapshot.forEach { it.cancel() }
        activeWorkers.clear()
        stopSelf()
    }
}
