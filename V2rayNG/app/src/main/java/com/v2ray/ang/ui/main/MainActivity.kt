package com.v2ray.ang.ui.main

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.extension.toast
import kotlinx.coroutines.launch

class MainActivity : HelperBaseComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application, MainRepository(application as AngApplication))
    }

    private val firstRunPrefs by lazy {
        getSharedPreferences("mobiletina_first_run", MODE_PRIVATE)
    }

    private val requestFirstRunVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            markFirstRunPermissionCompleted()
        }

    private var subscriptionRefreshRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel.onAction(MainAction.Initialize)
        handleFirstRunPermissions()
    }

    override fun onResume() {
        super.onResume()

        if (!hasInternetConnection()) {
            toast("لطفا اینترنت را روشن کنید")
        }

        updateSubscriptionOnResume()
    }

    private fun updateSubscriptionOnResume() {
        if (subscriptionRefreshRunning) return
        subscriptionRefreshRunning = true

        lifecycleScope.launch {
            try {
                mainViewModel.onAction(MainAction.UpdateSubscriptions)
            } finally {
                subscriptionRefreshRunning = false
            }
        }
    }

    private fun handleFirstRunPermissions() {
        if (firstRunPrefs.getBoolean("completed", false)) return

        checkAndRequestPermission(PermissionType.CAMERA) {
            requestVpnPermissionOnly()
        }
    }

    private fun requestVpnPermissionOnly() {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            markFirstRunPermissionCompleted()
        } else {
            requestFirstRunVpnPermission.launch(intent)
        }
    }

    private fun markFirstRunPermissionCompleted() {
        firstRunPrefs.edit()
            .putBoolean("completed", true)
            .apply()
    }

    private fun hasInternetConnection(): Boolean {
        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun startV2Ray() {
        LauncherManager.startService(this)
    }
}
