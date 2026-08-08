package com.v2ray.ang.ui.subscription

import android.os.Bundle
import android.text.TextUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.toLongEx
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.SubscriptionUpdater
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.FormDropdownField
import com.v2ray.ang.ui.compose.FormTextField
import com.v2ray.ang.ui.compose.SettingsSwitchItem
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.util.Utils

/** MobileTina keeps this screen only for adding a new subscription; editing existing links is disabled. */
class SubEditActivity : BaseComponentActivity() {
    private lateinit var suggestions: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Existing subscriptions are intentionally not editable in MobileTina.
        if (!intent.getStringExtra("subId").isNullOrBlank()) {
            finish()
            return
        }

        suggestions = SettingsManager.getProfileRemarks(
            excludeConfigTypes = setOf(
                EConfigType.CUSTOM,
                EConfigType.POLICYGROUP,
                EConfigType.PROXYCHAIN,
            )
        )
    }

    @Composable
    override fun ScreenContent() {
        SubAddScreen(
            initial = SubscriptionItem(),
            profileSuggestions = suggestions,
            onBackClick = { finish() },
            onSave = { saveSubscription(it) }
        )
    }

    private fun saveSubscription(subItem: SubscriptionItem): Boolean {
        if (TextUtils.isEmpty(subItem.remarks)) {
            toast(R.string.sub_setting_remarks)
            return false
        }
        if (subItem.url.isNotEmpty()) {
            if (!Utils.isValidUrl(subItem.url)) {
                toast(R.string.toast_invalid_url)
                return false
            }
            if (!Utils.isValidSubUrl(subItem.url)) {
                toast(R.string.toast_insecure_url_protocol)
                if (!subItem.allowInsecureUrl) return false
            }
        }

        if (subItem.autoUpdate && subItem.updateInterval < AppConfig.SUBSCRIPTION_MIN_INTERVAL_MINUTES) {
            toast(R.string.toast_invalid_update_interval)
            return false
        }

        MmkvManager.encodeSubscription("", subItem)
        SubscriptionUpdater.sync(this)
        SettingsChangeManager.makeSetupGroupTab()
        toastSuccess(R.string.toast_success)
        finish()
        return true
    }
}

@Composable
private fun SubAddScreen(
    initial: SubscriptionItem,
    profileSuggestions: List<String>,
    onBackClick: () -> Unit,
    onSave: (SubscriptionItem) -> Boolean
) {
    var remarks by rememberSaveable { mutableStateOf(initial.remarks) }
    var url by rememberSaveable { mutableStateOf(initial.url) }
    var userAgent by rememberSaveable { mutableStateOf(initial.userAgent.orEmpty()) }
    var requestHeaders by rememberSaveable { mutableStateOf(initial.requestHeaders.orEmpty()) }
    var filter by rememberSaveable { mutableStateOf(initial.filter ?: "") }
    var enabled by rememberSaveable { mutableStateOf(initial.enabled) }
    var autoUpdate by rememberSaveable { mutableStateOf(initial.autoUpdate) }
    var updateInterval by rememberSaveable { mutableStateOf(initial.updateInterval.toString()) }
    var allowInsecureUrl by rememberSaveable { mutableStateOf(initial.allowInsecureUrl) }
    var prevProfile by rememberSaveable { mutableStateOf(initial.prevProfile ?: "") }
    var nextProfile by rememberSaveable { mutableStateOf(initial.nextProfile ?: "") }

    val scrollState = rememberScrollState()

    fun buildSubItem(): SubscriptionItem = SubscriptionItem().also { item ->
        item.remarks = remarks
        item.url = url
        item.userAgent = userAgent
        item.requestHeaders = requestHeaders
        item.filter = filter
        item.enabled = enabled
        item.autoUpdate = autoUpdate
        item.updateInterval = updateInterval.toLongEx()
        item.prevProfile = prevProfile
        item.nextProfile = nextProfile
        item.allowInsecureUrl = allowInsecureUrl
    }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_sub_setting),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { onSave(buildSubItem()) }) {
                        Icon(
                            painterResource(R.drawable.ic_fab_check),
                            contentDescription = stringResource(R.string.acc_save)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .verticalScrollbar(scrollState)
                .padding(vertical = 8.dp)
                .padding(bottom = 36.dp)
        ) {
            FormTextField(stringResource(R.string.sub_setting_remarks), remarks, { remarks = it })
            FormTextField(stringResource(R.string.sub_setting_url), url, { url = it })
            FormTextField(stringResource(R.string.sub_setting_user_agent), userAgent, { userAgent = it })
            FormTextField(stringResource(R.string.sub_setting_request_headers), requestHeaders, { requestHeaders = it })
            FormTextField(stringResource(R.string.sub_setting_filter), filter, { filter = it })
            SettingsSwitchItem(
                title = stringResource(R.string.sub_setting_enable),
                checked = enabled,
                onCheckedChange = { enabled = it }
            )
            SettingsSwitchItem(
                title = stringResource(R.string.sub_auto_update),
                checked = autoUpdate,
                onCheckedChange = { autoUpdate = it }
            )
            FormTextField(
                stringResource(R.string.title_pref_auto_update_interval),
                updateInterval,
                { updateInterval = it },
                keyboardType = KeyboardType.Number
            )
            SettingsSwitchItem(
                title = stringResource(R.string.sub_allow_insecure_url),
                checked = allowInsecureUrl,
                onCheckedChange = { allowInsecureUrl = it }
            )
            FormDropdownField(
                label = stringResource(R.string.sub_setting_pre_profile),
                placeholder = stringResource(R.string.sub_setting_pre_profile_tip),
                value = prevProfile,
                options = profileSuggestions,
                onValueChange = { prevProfile = it },
                editable = true
            )
            FormDropdownField(
                label = stringResource(R.string.sub_setting_next_profile),
                placeholder = stringResource(R.string.sub_setting_pre_profile_tip),
                value = nextProfile,
                options = profileSuggestions,
                onValueChange = { nextProfile = it },
                editable = true
            )
        }
    }
}
