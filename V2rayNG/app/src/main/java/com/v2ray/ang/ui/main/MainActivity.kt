package com.v2ray.ang.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.core.MobileTinaAutomation
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.MobileTinaResetManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.AboutActivity
import com.v2ray.ang.ui.backup.BackupActivity
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.ui.checkupdate.CheckUpdateActivity
import com.v2ray.ang.ui.logcat.LogcatActivity
import com.v2ray.ang.ui.perappproxy.PerAppProxyActivity
import com.v2ray.ang.ui.routing.RoutingSettingActivity
import com.v2ray.ang.ui.server.ProfileEditorResult
import com.v2ray.ang.ui.server.ServerCustomConfigActivity
import com.v2ray.ang.ui.server.ServerGroupActivity
import com.v2ray.ang.ui.server.ServerHttpActivity
import com.v2ray.ang.ui.server.ServerHysteria2Activity
import com.v2ray.ang.ui.server.ServerProxyChainActivity
import com.v2ray.ang.ui.server.ServerShadowsocksActivity
import com.v2ray.ang.ui.server.ServerSocksActivity
import com.v2ray.ang.ui.server.ServerTrojanActivity
import com.v2ray.ang.ui.server.ServerVlessActivity
import com.v2ray.ang.ui.server.ServerVmessActivity
import com.v2ray.ang.ui.server.ServerWireguardActivity
import com.v2ray.ang.ui.settings.SettingsActivity
import com.v2ray.ang.ui.subscription.SubSettingActivity
import com.v2ray.ang.ui.userasset.UserAssetActivity
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

class MainActivity : HelperBaseComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application, MainRepository(application as AngApplication))
    }

    private val firstRunPrefs by lazy {
        getSharedPreferences("mobiletina_first_run", MODE_PRIVATE)
    }

    private var lastSubscriptionRefreshAt = 0L
    private var pendingSmartVpnPermission = false

    private var smartConnecting by mutableStateOf(false)
    private var smartCountdownSeconds by mutableStateOf(0)
    private var smartConnectionFailed by mutableStateOf(false)

    private val requestVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val smart = pendingSmartVpnPermission
            pendingSmartVpnPermission = false
            if (result.resultCode == RESULT_OK) {
                startV2Ray(smart)
            } else if (smart) {
                markSmartConnectFailed()
            }
        }

    private val requestFirstRunCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            requestFirstRunVpnPermissionOnly()
        }

    private val requestFirstRunVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            markFirstRunPermissionCompleted()
        }

    private val profileEditorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult
            val action = data.getStringExtra(ProfileEditorResult.EXTRA_ACTION)
                ?: return@registerForActivityResult
            if (action != ProfileEditorResult.ACTION_SAVED &&
                action != ProfileEditorResult.ACTION_DELETED
            ) return@registerForActivityResult
            val restartService = data.getBooleanExtra(
                ProfileEditorResult.EXTRA_RESTART_SERVICE, false
            )
            mainViewModel.onAction(MainAction.RefreshGroups)
            if (restartService && mainViewModel.uiState.value.isRunning) {
                restartV2Ray()
            }
        }

    private val settingsActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val restartService = SettingsChangeManager.consumeRestartService()
            val refreshGroups = SettingsChangeManager.consumeSetupGroupTab()
            mainViewModel.refreshUiSettings()
            if (refreshGroups) mainViewModel.onAction(MainAction.RefreshGroups)
            if (restartService && mainViewModel.uiState.value.isRunning) restartV2Ray()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel.onAction(MainAction.Initialize)

        if (firstRunPrefs.getBoolean(FIRST_RUN_COMPLETED, false)) {
            maybeAutoConnectOnStart()
        } else {
            handleFirstRunPermissions()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!hasInternetConnection()) {
            toast(R.string.mobiletina_enable_internet)
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastSubscriptionRefreshAt >= SUBSCRIPTION_REFRESH_GUARD_MS) {
            lastSubscriptionRefreshAt = now
            mainViewModel.onAction(MainAction.UpdateSubscriptions)
        }
    }

    private fun handleFirstRunPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            requestFirstRunVpnPermissionOnly()
        } else {
            requestFirstRunCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestFirstRunVpnPermissionOnly() {
        if (!SettingsManager.isVpnMode()) {
            markFirstRunPermissionCompleted()
            return
        }

        val intent = VpnService.prepare(this)
        if (intent == null) {
            markFirstRunPermissionCompleted()
        } else {
            requestFirstRunVpnPermission.launch(intent)
        }
    }

    private fun markFirstRunPermissionCompleted() {
        if (firstRunPrefs.getBoolean(FIRST_RUN_COMPLETED, false)) return
        firstRunPrefs.edit().putBoolean(FIRST_RUN_COMPLETED, true).apply()
        maybeAutoConnectOnStart()
    }

    private fun hasInternetConnection(): Boolean {
        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun maybeAutoConnectOnStart() {
        if (!MobileTinaAutomation.isAutoConnectOnAppStartEnabled()) return

        lifecycleScope.launch {
            delay(1200L)
            if (mainViewModel.uiState.value.isRunning) return@launch
            if (MmkvManager.getSelectServer().isNullOrEmpty()) return@launch
            smartConnectAndStart()
        }
    }

    @Composable
    override fun ScreenContent() {
        var showAutomation by remember { mutableStateOf(false) }
        var showResetConfirm by remember { mutableStateOf(false) }

        BackHandler { moveTaskToBack(false) }
        MainScreen(
            mainViewModel = mainViewModel,
            smartConnecting = smartConnecting,
            smartCountdownSeconds = smartCountdownSeconds,
            smartConnectionFailed = smartConnectionFailed,
            onSmartConnect = { smartConnectAndStart() },
            onAction = { action ->
                when (action) {
                    MainAction.ToggleService -> handleManualConnectAction()
                    MainAction.TestCurrentServer -> handleLayoutTestClick()
                    MainAction.ImportQRcode -> importQRcode()
                    MainAction.ImportClipboard -> importClipboard()
                    MainAction.ImportConfigLocal -> importConfigLocal()
                    is MainAction.ImportManually -> importManually(action.type)
                    MainAction.RestartService -> restartV2Ray()
                    MainAction.LocateSelectedServer -> mainViewModel.triggerLocateSelectedServer()
                    is MainAction.SelectServer -> setSelectServer(action.guid)
                    is MainAction.EditServer -> editServer(action.guid, action.profile)
                    is MainAction.ShareClipboard -> shareToClipboard(action.guid)
                    is MainAction.ShareFullContent -> shareFullContentAsync(action.guid)
                    else -> mainViewModel.onAction(action)
                }
            },
            onNavigate = { route ->
                when (route) {
                    MainDestination.MobileTinaAutomation -> showAutomation = true
                    MainDestination.ResetVpn -> showResetConfirm = true
                    else -> navigateTo(route)
                }
            },
        )

        if (showAutomation) {
            MobileTinaAutomationDialog(onDismiss = { showAutomation = false })
        }

        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text(stringResource(R.string.mobiletina_reset_title)) },
                text = { Text(stringResource(R.string.mobiletina_reset_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetConfirm = false
                            MobileTinaResetManager.reset(this@MainActivity)
                            clearSmartConnectState()
                            mainViewModel.onAction(MainAction.RefreshGroups)
                            toast(R.string.mobiletina_reset_done)
                        }
                    ) {
                        Text(stringResource(R.string.mobiletina_reset_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) {
                        Text(stringResource(R.string.mobiletina_close))
                    }
                }
            )
        }
    }

    private fun shareToClipboard(guid: String): Boolean =
        AngConfigManager.share2Clipboard(this, guid) == 0

    private fun shareFullContentAsync(guid: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = AngConfigManager.shareFullContent2Clipboard(this@MainActivity, guid)
            withContext(Dispatchers.Main) {
                if (result == 0) toastSuccess(R.string.toast_success)
                else toastError(R.string.toast_failure)
            }
        }
    }

    private fun navigateTo(destination: MainDestination) {
        val intent = when (destination) {
            MainDestination.Subscriptions -> Intent(this, SubSettingActivity::class.java)
            MainDestination.PerAppProxy -> Intent(this, PerAppProxyActivity::class.java)
            MainDestination.Routing -> Intent(this, RoutingSettingActivity::class.java)
            MainDestination.UserAssets -> Intent(this, UserAssetActivity::class.java)
            MainDestination.Settings -> Intent(this, SettingsActivity::class.java)
            MainDestination.MobileTinaAutomation,
            MainDestination.ResetVpn -> return
            MainDestination.Logcat -> Intent(this, LogcatActivity::class.java)
            MainDestination.CheckUpdate -> Intent(this, CheckUpdateActivity::class.java)
            MainDestination.BackupRestore -> Intent(this, BackupActivity::class.java)
            MainDestination.About -> Intent(this, AboutActivity::class.java)
            MainDestination.Promotion -> {
                Utils.openUri(
                    this,
                    "${Utils.decode(AppConfig.APP_PROMOTION_URL)}?t=${System.currentTimeMillis()}"
                )
                return
            }
        }
        settingsActivityLauncher.launch(intent)
    }

    private fun handleManualConnectAction() {
        val state = mainViewModel.uiState.value
        if (state.isRunning) {
            clearSmartConnectState()
            LauncherManager.stopService(this)
            return
        }
        if (state.isTesting || smartConnecting) return
        requestVpnPermissionAndStart(isSmartConnect = false)
    }

    private fun smartConnectAndStart() {
        val state = mainViewModel.uiState.value
        if (state.isRunning) {
            clearSmartConnectState()
            LauncherManager.stopService(this)
            return
        }
        if (state.isTesting || smartConnecting) return

        val groupId = state.selectedGroupId
        val initialServers = mainViewModel.serversForGroup(groupId).value
        if (initialServers.isEmpty()) {
            markSmartConnectFailed()
            toast(R.string.title_file_chooser)
            return
        }

        smartConnectionFailed = false
        smartConnecting = true

        if (initialServers.size == 1) {
            smartCountdownSeconds = 0
            mainViewModel.updateSelectedGuid(initialServers.first().guid)
            lifecycleScope.launch {
                delay(120L)
                requestVpnPermissionAndStart(isSmartConnect = true)
            }
            return
        }

        smartCountdownSeconds = SMART_CONNECT_TIMEOUT_SECONDS
        mainViewModel.testAllRealPing()

        lifecycleScope.launch {
            val deadline = SystemClock.elapsedRealtime() + SMART_CONNECT_TIMEOUT_MS

            while (mainViewModel.uiState.value.isTesting) {
                val remaining = deadline - SystemClock.elapsedRealtime()
                if (remaining <= 0L) break
                smartCountdownSeconds = ceil(remaining / 1000.0).toInt().coerceAtLeast(1)
                delay(100L)
            }

            if (mainViewModel.uiState.value.isTesting) {
                mainViewModel.cancelAllPing()
            }
            smartCountdownSeconds = 0

            // Execute the same sort command used by the manual menu. Selection below reads
            // directly from the persisted real-delay results, so it does not depend on UI reload timing.
            mainViewModel.onAction(MainAction.SortByTestResults)

            val best = initialServers.mapNotNull { server ->
                val delayMillis = MmkvManager.decodeServerAffiliationInfo(server.guid)?.testDelayMillis ?: 0L
                if (delayMillis > 0L) server.guid to delayMillis else null
            }.minByOrNull { it.second }

            if (best == null) {
                markSmartConnectFailed()
                toast(R.string.mobiletina_no_working_server)
                return@launch
            }

            mainViewModel.updateSelectedGuid(best.first)
            delay(150L)
            requestVpnPermissionAndStart(isSmartConnect = true)
        }
    }

    private fun requestVpnPermissionAndStart(isSmartConnect: Boolean) {
        if (SettingsManager.isVpnMode()) {
            val intent = VpnService.prepare(this)
            if (intent == null) {
                startV2Ray(isSmartConnect)
            } else {
                pendingSmartVpnPermission = isSmartConnect
                requestVpnPermission.launch(intent)
            }
        } else {
            startV2Ray(isSmartConnect)
        }
    }

    private fun handleLayoutTestClick() {
        if (mainViewModel.uiState.value.isRunning) {
            mainViewModel.testCurrentServerRealPing()
        }
    }

    private fun startV2Ray(isSmartConnect: Boolean = false) {
        if (mainViewModel.uiState.value.selectedGuid.isNullOrEmpty()) {
            if (isSmartConnect) markSmartConnectFailed()
            toast(R.string.title_file_chooser)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
            MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING)
        ) {
            checkAndRequestPermission(PermissionType.ACCESS_LOCAL_NETWORK) {}
        }
        LauncherManager.startService(this)
        if (isSmartConnect) monitorSmartConnectionResult()
    }

    private fun monitorSmartConnectionResult() {
        lifecycleScope.launch {
            val deadline = SystemClock.elapsedRealtime() + SMART_START_RESULT_TIMEOUT_MS
            while (SystemClock.elapsedRealtime() < deadline) {
                if (mainViewModel.uiState.value.isRunning) {
                    smartConnecting = false
                    smartConnectionFailed = false
                    smartCountdownSeconds = 0
                    return@launch
                }
                delay(200L)
            }
            if (!mainViewModel.uiState.value.isRunning) {
                markSmartConnectFailed()
            }
        }
    }

    private fun markSmartConnectFailed() {
        smartConnecting = false
        smartCountdownSeconds = 0
        smartConnectionFailed = true
    }

    private fun clearSmartConnectState() {
        smartConnecting = false
        smartCountdownSeconds = 0
        smartConnectionFailed = false
        pendingSmartVpnPermission = false
    }

    private fun restartV2Ray() {
        if (mainViewModel.uiState.value.isRunning) LauncherManager.stopService(this)
        lifecycleScope.launch {
            delay(500)
            startV2Ray()
        }
    }

    private fun importManually(createConfigType: Int) {
        val intent = when (createConfigType) {
            EConfigType.POLICYGROUP.value -> Intent(this, ServerGroupActivity::class.java)
            EConfigType.PROXYCHAIN.value -> Intent(this, ServerProxyChainActivity::class.java)
            EConfigType.VMESS.value -> Intent(this, ServerVmessActivity::class.java)
            EConfigType.VLESS.value -> Intent(this, ServerVlessActivity::class.java)
            EConfigType.SHADOWSOCKS.value -> Intent(this, ServerShadowsocksActivity::class.java)
            EConfigType.SOCKS.value -> Intent(this, ServerSocksActivity::class.java)
            EConfigType.HTTP.value -> Intent(this, ServerHttpActivity::class.java)
            EConfigType.TROJAN.value -> Intent(this, ServerTrojanActivity::class.java)
            EConfigType.WIREGUARD.value -> Intent(this, ServerWireguardActivity::class.java)
            EConfigType.HYSTERIA2.value -> Intent(this, ServerHysteria2Activity::class.java)
            else -> Intent(this, ServerHttpActivity::class.java).apply {
                putExtra("createConfigType", createConfigType)
            }
        }.apply {
            putExtra("subscriptionId", mainViewModel.uiState.value.selectedGroupId)
        }
        profileEditorLauncher.launch(intent)
    }

    private fun importQRcode() {
        launchQRCodeScanner { scanResult ->
            if (scanResult != null) {
                mainViewModel.onAction(MainAction.ImportBatchConfig(scanResult))
            }
        }
    }

    private fun importClipboard() {
        try {
            val text = Utils.getClipboard(this)
            mainViewModel.onAction(MainAction.ImportBatchConfig(text))
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to import config from clipboard", e)
        }
    }

    private fun importConfigLocal() {
        launchFileChooser { uri ->
            if (uri == null) return@launchFileChooser
            try {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    mainViewModel.onAction(MainAction.ImportBatchConfig(reader.readText()))
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to read content from URI", e)
            }
        }
    }

    private fun editServer(guid: String, profile: ProfileItem) {
        val activityClass = when (profile.configType) {
            EConfigType.CUSTOM -> ServerCustomConfigActivity::class.java
            EConfigType.POLICYGROUP -> ServerGroupActivity::class.java
            EConfigType.PROXYCHAIN -> ServerProxyChainActivity::class.java
            EConfigType.VMESS -> ServerVmessActivity::class.java
            EConfigType.VLESS -> ServerVlessActivity::class.java
            EConfigType.SHADOWSOCKS -> ServerShadowsocksActivity::class.java
            EConfigType.SOCKS -> ServerSocksActivity::class.java
            EConfigType.HTTP -> ServerHttpActivity::class.java
            EConfigType.TROJAN -> ServerTrojanActivity::class.java
            EConfigType.WIREGUARD -> ServerWireguardActivity::class.java
            EConfigType.HYSTERIA2 -> ServerHysteria2Activity::class.java
            else -> ServerHttpActivity::class.java
        }
        val intent = Intent(this, activityClass).apply {
            putExtra("guid", guid)
            putExtra("isRunning", mainViewModel.uiState.value.isRunning)
            putExtra("createConfigType", profile.configType.value)
            putExtra("subscriptionId", mainViewModel.uiState.value.selectedGroupId)
        }
        profileEditorLauncher.launch(intent)
    }

    private fun setSelectServer(guid: String) {
        val selected = mainViewModel.uiState.value.selectedGuid
        if (guid != selected) {
            mainViewModel.updateSelectedGuid(guid)
            if (mainViewModel.uiState.value.isRunning) restartV2Ray()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private companion object {
        const val FIRST_RUN_COMPLETED = "permissions_completed"
        const val SUBSCRIPTION_REFRESH_GUARD_MS = 5_000L
        const val SMART_CONNECT_TIMEOUT_SECONDS = 6
        const val SMART_CONNECT_TIMEOUT_MS = 6_000L
        const val SMART_START_RESULT_TIMEOUT_MS = 10_000L
    }
}
