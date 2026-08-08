package com.v2ray.ang.handler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.JsonParser
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.receiver.BootReceiver
import com.v2ray.ang.util.LogUtil
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Schedules and executes MobileTina configuration expiry declared by a top-level JSON `_comment`.
 *
 * Example: "_comment": "2026-09-10T12:00:00"
 */
object MobileTinaExpiryManager {

    const val DATA_CHANGED_MARKER = "mobiletina_data_changed"
    const val ACTION_EXPIRE = "com.v2ray.mobiletina.action.CONFIG_EXPIRE"

    private const val PREFS_NAME = "mobiletina_config_expiry"
    private const val KEY_TRIGGER_AT_MILLIS = "trigger_at_millis"
    private const val UNIQUE_FALLBACK_WORK = "mobiletina_config_expiry_fallback"
    private const val ALARM_REQUEST_CODE = 96323
    private const val EXPIRED_SUBSCRIPTION_ID = "mobiletina_expired_subscription"
    private const val EXPIRED_REMARKS = "اشتراک منقضی شد"
    private const val EXPIRED_CONFIG = "socks://Og@1:1#%D8%A7%D8%B4%D8%AA%D8%B1%D8%A7%DA%A9%20%D9%85%D9%86%D9%82%D8%B6%DB%8C%20%D8%B4%D8%AF"

    /** Inspect one imported JSON payload and schedule its expiry if supported. */
    fun scheduleFromImportedText(context: Context, configText: String?) {
        scheduleFromImportedTexts(context, listOf(configText))
    }

    /** Inspect multiple imported/raw payloads and schedule the earliest declared expiry. */
    fun scheduleFromImportedTexts(context: Context, configTexts: Iterable<String?>) {
        val triggerAt = configTexts.mapNotNull(::extractTriggerAtMillis).minOrNull() ?: return
        schedule(context.applicationContext, triggerAt)
    }

    /** Rebuild a pending alarm after reboot, package replacement or process recreation. */
    fun recoverPending(context: Context) {
        val appContext = context.applicationContext
        val triggerAt = prefs(appContext).getLong(KEY_TRIGGER_AT_MILLIS, 0L)
        if (triggerAt <= 0L) return

        if (System.currentTimeMillis() >= triggerAt) {
            executeIfDue(appContext)
        } else {
            scheduleInternal(appContext, triggerAt, persist = false)
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        prefs(appContext).edit().remove(KEY_TRIGGER_AT_MILLIS).apply()
        cancelScheduledArtifacts(appContext)
    }

    /** Called by BootReceiver's exact-alarm action and WorkManager fallback. */
    @Synchronized
    fun executeIfDue(context: Context) {
        val appContext = context.applicationContext
        val storedTrigger = prefs(appContext).getLong(KEY_TRIGGER_AT_MILLIS, 0L)
        if (storedTrigger <= 0L) return

        val now = System.currentTimeMillis()
        if (now < storedTrigger) {
            scheduleInternal(appContext, storedTrigger, persist = false)
            return
        }

        // Clear pending state before destructive work so duplicate alarm/worker deliveries are idempotent.
        prefs(appContext).edit().remove(KEY_TRIGGER_AT_MILLIS).commit()
        cancelScheduledArtifacts(appContext)

        try {
            MobileTinaResetManager.reset(appContext, cancelPendingExpiry = false)

            // Use a visible non-default group because the internal default group is intentionally
            // hidden from MobileTina's manual screen.
            MmkvManager.encodeSubscription(
                EXPIRED_SUBSCRIPTION_ID,
                SubscriptionItem(
                    remarks = EXPIRED_REMARKS,
                    url = "",
                    enabled = false
                )
            )
            AngConfigManager.importBatchConfig(
                EXPIRED_CONFIG,
                EXPIRED_SUBSCRIPTION_ID,
                true
            )
            MmkvManager.encodeSettings(AppConfig.CACHE_SUBSCRIPTION_ID, EXPIRED_SUBSCRIPTION_ID)

            // Reuse the existing app-to-UI broadcast channel with a MobileTina marker.
            MessageHelper.sendMsg2UI(
                appContext,
                AppConfig.MSG_MEASURE_CONFIG_FINISH,
                DATA_CHANGED_MARKER
            )
            LogUtil.i(AppConfig.TAG, "MobileTina: expired JSON configuration replaced successfully")
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "MobileTina: failed to apply expired configuration", e)
        }
    }

    private fun schedule(context: Context, triggerAtMillis: Long) {
        prefs(context).edit().putLong(KEY_TRIGGER_AT_MILLIS, triggerAtMillis).apply()
        scheduleInternal(context, triggerAtMillis, persist = false)
    }

    private fun scheduleInternal(context: Context, triggerAtMillis: Long, persist: Boolean) {
        if (persist) {
            prefs(context).edit().putLong(KEY_TRIGGER_AT_MILLIS, triggerAtMillis).apply()
        }

        cancelScheduledArtifacts(context)

        val delayMillis = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        if (delayMillis == 0L) {
            executeIfDue(context)
            return
        }

        scheduleAlarm(context, triggerAtMillis)

        // WorkManager is intentionally also scheduled as a fallback. If the exact alarm fires first,
        // executeIfDue() clears the persisted timestamp and this worker becomes a harmless no-op.
        val fallback = OneTimeWorkRequestBuilder<ExpiryFallbackWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(UNIQUE_FALLBACK_WORK)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_FALLBACK_WORK,
            ExistingWorkPolicy.REPLACE,
            fallback
        )

        LogUtil.i(
            AppConfig.TAG,
            "MobileTina: config expiry scheduled for $triggerAtMillis; exact=${canScheduleExactAlarm(context)}"
        )
    }

    private fun scheduleAlarm(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = expiryPendingIntent(context)

        try {
            if (canScheduleExactAlarm(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // No special exact-alarm grant: retain a system alarm plus WorkManager fallback.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (security: SecurityException) {
            LogUtil.w(AppConfig.TAG, "MobileTina: exact alarm permission unavailable; using fallback")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun canScheduleExactAlarm(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    private fun cancelScheduledArtifacts(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(expiryPendingIntent(context))
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_FALLBACK_WORK)
    }

    private fun expiryPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_EXPIRE
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun extractTriggerAtMillis(configText: String?): Long? {
        val text = configText?.trim().orEmpty()
        if (text.isEmpty() || !text.startsWith('{')) return null

        val rawTimestamp = try {
            val root = JsonParser.parseString(text)
            if (!root.isJsonObject) return null
            val comment = root.asJsonObject.get("_comment") ?: return null
            if (!comment.isJsonPrimitive || !comment.asJsonPrimitive.isString) return null
            comment.asString.trim()
        } catch (_: Exception) {
            return null
        }
        if (rawTimestamp.isBlank()) return null

        return parseTimestamp(rawTimestamp)
    }

    private fun parseTimestamp(value: String): Long? {
        try {
            return Instant.parse(value).toEpochMilli()
        } catch (_: Exception) {
        }
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
        }
        return try {
            LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    class ExpiryFallbackWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            executeIfDue(applicationContext)
            return Result.success()
        }
    }
}
