package com.v2ray.ang.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.v2ray.ang.handler.MobileTinaExpiryManager

/** Receives MobileTina's scheduled JSON configuration expiry alarm. */
class MobileTinaExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        MobileTinaExpiryManager.executeIfDue(context)
    }
}
