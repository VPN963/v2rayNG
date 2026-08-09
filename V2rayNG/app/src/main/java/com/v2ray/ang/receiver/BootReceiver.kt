package com.v2ray.ang.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.MobileTinaExpiryManager
import com.v2ray.ang.handler.V2RayServiceManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        Log.i(AppConfig.TAG, "BootReceiver received: ${intent?.action}")

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            MobileTinaExpiryManager.recoverPending(context)
        }

        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!MmkvManager.decodeStartOnBoot()) return
        if (MmkvManager.getSelectServer().isNullOrEmpty()) return
        V2RayServiceManager.startVService(context)
    }
}
