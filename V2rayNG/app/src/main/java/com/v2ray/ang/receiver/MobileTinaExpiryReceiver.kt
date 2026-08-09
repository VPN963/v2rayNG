package com.v2ray.ang.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.v2ray.ang.handler.MobileTinaExpiryManager

class MobileTinaExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == MobileTinaExpiryManager.ACTION_EXPIRE) {
            MobileTinaExpiryManager.executeIfDue(context.applicationContext)
        }
    }
}
