package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.MobileTinaAutomation
import com.v2ray.ang.handler.MmkvManager.rememberMmkvBool
import com.v2ray.ang.handler.MmkvManager.rememberMmkvString

@Composable
fun MobileTinaAutomationDialog(onDismiss: () -> Unit) {
    var autoConnect by rememberMmkvBool(MobileTinaAutomation.PREF_AUTO_CONNECT_ON_APP_START, false)
    var startOnBoot by rememberMmkvBool(AppConfig.PREF_IS_BOOTED, false)
    var autoReconnect by rememberMmkvBool(MobileTinaAutomation.PREF_AUTO_RECONNECT, true)
    var smartServer by rememberMmkvBool(MobileTinaAutomation.PREF_SMART_SERVER, true)
    var wifiAllowed by rememberMmkvBool(MobileTinaAutomation.PREF_WIFI_ALLOWED, true)
    var mobileAllowed by rememberMmkvBool(MobileTinaAutomation.PREF_MOBILE_ALLOWED, true)
    var retryCount by rememberMmkvString(MobileTinaAutomation.PREF_RETRY_COUNT, "3")
    var retryDelay by rememberMmkvString(MobileTinaAutomation.PREF_RETRY_DELAY_SECONDS, "3")
    var autoConnectDelay by rememberMmkvString(MobileTinaAutomation.PREF_AUTO_CONNECT_DELAY_SECONDS, "2")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("MobileTina Automation")
                Text(
                    text = "Smart connection and recovery",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AutomationSwitch(
                    title = "Auto Connect",
                    summary = "Connect automatically when MobileTina starts.",
                    checked = autoConnect,
                    onCheckedChange = { autoConnect = it }
                )
                AutomationSwitch(
                    title = "Connect after phone startup",
                    summary = "Reconnect to the selected server after Android boots.",
                    checked = startOnBoot,
                    onCheckedChange = { startOnBoot = it }
                )
                AutomationSwitch(
                    title = "Auto Reconnect",
                    summary = "Recover the tunnel after Wi-Fi/mobile network changes or temporary disconnects.",
                    checked = autoReconnect,
                    onCheckedChange = { autoReconnect = it }
                )
                AutomationSwitch(
                    title = "Smart Server",
                    summary = "Use the fastest server from previously measured ping results when reconnecting.",
                    checked = smartServer,
                    onCheckedChange = { smartServer = it }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Allowed networks", style = MaterialTheme.typography.titleSmall)

                AutomationSwitch(
                    title = "Wi-Fi",
                    summary = "Allow automatic connection on Wi-Fi networks.",
                    checked = wifiAllowed,
                    onCheckedChange = { wifiAllowed = it }
                )
                AutomationSwitch(
                    title = "Mobile Data",
                    summary = "Allow automatic connection on cellular data.",
                    checked = mobileAllowed,
                    onCheckedChange = { mobileAllowed = it }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Recovery tuning", style = MaterialTheme.typography.titleSmall)

                NumberSetting(
                    label = "Retry count (1-10)",
                    value = retryCount,
                    onValueChange = { value ->
                        if (value.isBlank() || value.toIntOrNull()?.let { it in 1..10 } == true) {
                            retryCount = value
                        }
                    }
                )
                Spacer(Modifier.height(4.dp))
                NumberSetting(
                    label = "Retry delay in seconds (1-60)",
                    value = retryDelay,
                    onValueChange = { value ->
                        if (value.isBlank() || value.toIntOrNull()?.let { it in 1..60 } == true) {
                            retryDelay = value
                        }
                    }
                )
                Spacer(Modifier.height(4.dp))
                NumberSetting(
                    label = "Auto-connect delay in seconds (0-60)",
                    value = autoConnectDelay,
                    onValueChange = { value ->
                        if (value.isBlank() || value.toIntOrNull()?.let { it in 0..60 } == true) {
                            autoConnectDelay = value
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun AutomationSwitch(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NumberSetting(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
