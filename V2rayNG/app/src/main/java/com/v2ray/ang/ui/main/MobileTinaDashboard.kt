package com.v2ray.ang.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.core.MobileTinaAutomation
import com.v2ray.ang.handler.MmkvManager.rememberMmkvBool

@Composable
fun MobileTinaDashboard(
    isRunning: Boolean,
    isTesting: Boolean,
    selectedServerName: String,
    selectedPingMillis: Long,
    onToggle: () -> Unit
) {
    val autoConnect by rememberMmkvBool(MobileTinaAutomation.PREF_AUTO_CONNECT_ON_APP_START, false)
    val busy = isTesting

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.mobiletina_app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    busy -> stringResource(R.string.mobiletina_testing)
                    isRunning -> stringResource(R.string.mobiletina_status_connected)
                    else -> stringResource(R.string.mobiletina_status_disconnected)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))

            FloatingActionButton(
                onClick = onToggle,
                modifier = Modifier.size(132.dp),
                shape = CircleShape,
                containerColor = if (isRunning) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                contentColor = if (isRunning) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(targetState = busy, label = "mobiletina-connect-state") { testing ->
                        if (testing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(52.dp),
                                strokeWidth = 5.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(
                                        if (isRunning) R.drawable.ic_stop_24dp else R.drawable.ic_play_24dp
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = stringResource(
                                        if (isRunning) R.string.mobiletina_disconnect else R.string.mobiletina_connect
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedServerName.isNotBlank()) {
                Text(
                    text = selectedServerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (selectedPingMillis > 0L) {
                        stringResource(R.string.mobiletina_ping_format, selectedPingMillis)
                    } else {
                        stringResource(R.string.mobiletina_ping_unknown)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (autoConnect) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (autoConnect) {
                                stringResource(R.string.mobiletina_smart_mode)
                            } else {
                                "Manual Connect"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (autoConnect) {
                            Text(
                                text = stringResource(R.string.mobiletina_smart_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
