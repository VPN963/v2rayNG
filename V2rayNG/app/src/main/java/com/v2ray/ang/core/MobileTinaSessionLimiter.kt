package com.v2ray.ang.core

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.v2ray.ang.AppConfig
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.LogUtil
import java.util.concurrent.TimeUnit

/** Enforces MobileTina's maximum continuous VPN session duration. */
object MobileTinaSessionLimiter {

    private const val UNIQUE_WORK_NAME = "mobiletina_vpn_24h_limit"
    private const val MAX_SESSION_HOURS = 24L

    fun schedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<SessionLimitWorker>()
            .setInitialDelay(MAX_SESSION_HOURS, TimeUnit.HOURS)
            .addTag(UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        LogUtil.i(AppConfig.TAG, "MobileTina: 24-hour VPN session limit scheduled")
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    class SessionLimitWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(appContext, workerParams) {

        override suspend fun doWork(): Result {
            LogUtil.i(AppConfig.TAG, "MobileTina: 24-hour VPN session limit reached, stopping VPN")
            MessageHelper.sendMsg2Service(
                applicationContext,
                AppConfig.MSG_STATE_STOP,
                "mobiletina_24h_limit"
            )
            return Result.success()
        }
    }
}
