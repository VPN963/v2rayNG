package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.util.LogUtil

/** Destructive reset of user-imported VPN data while keeping app preferences and Android grants. */
object MobileTinaResetManager {

    fun reset(context: Context, cancelPendingExpiry: Boolean = true) {
        if (cancelPendingExpiry) {
            MobileTinaExpiryManager.cancel(context)
        }
        LauncherManager.stopService(context)

        val subscriptions = MmkvManager.decodeSubscriptions().map { it.guid }
        subscriptions.forEach(MmkvManager::removeSubscription)

        // Also clear ungrouped/default nodes and any orphan profile/raw/affiliation entries.
        MmkvManager.removeServerViaSubid(AppConfig.DEFAULT_SUBSCRIPTION_ID)
        MmkvManager.removeAllServer()
        MmkvManager.encodeSubsList(mutableListOf())
        MmkvManager.setSelectServer("")
        MmkvManager.encodeSettings(AppConfig.CACHE_SUBSCRIPTION_ID, "")

        LogUtil.i(AppConfig.TAG, "MobileTina: subscriptions and configurations reset")
    }
}
