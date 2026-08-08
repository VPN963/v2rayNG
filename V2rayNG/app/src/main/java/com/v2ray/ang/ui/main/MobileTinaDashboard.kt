package com.v2ray.ang.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.MmkvManager
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun MobileTinaDashboard(
    isRunning: Boolean,
    smartConnecting: Boolean,
    smartCountdownSeconds: Int,
    smartConnectionFailed: Boolean,
    selectedSubscriptionId: String,
    selectedServerName: String,
    selectedPingMillis: Long,
    subscriptionRefreshKey: Boolean,
    onToggle: () -> Unit,
    onTestPing: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MobileTinaAutoConnectButton(
            isRunning = isRunning,
            smartConnecting = smartConnecting,
            smartCountdownSeconds = smartCountdownSeconds,
            smartConnectionFailed = smartConnectionFailed,
            selectedServerName = selectedServerName,
            selectedPingMillis = selectedPingMillis,
            onToggle = onToggle,
            onTestPing = onTestPing
        )

        if (selectedServerName.isNotBlank()) {
            Spacer(Modifier.height(22.dp))
            Text(
                text = selectedServerName,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(26.dp))

        // Subscription usage belongs at the bottom of Automatic mode so the primary action stays
        // visually dominant and matches MobileTina's connection-first layout.
        MobileTinaSubscriptionStatusCard(
            selectedSubscriptionId = selectedSubscriptionId,
            refreshKey = subscriptionRefreshKey,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MobileTinaAutoConnectButton(
    isRunning: Boolean,
    smartConnecting: Boolean,
    smartCountdownSeconds: Int,
    smartConnectionFailed: Boolean,
    selectedServerName: String,
    selectedPingMillis: Long,
    onToggle: () -> Unit,
    onTestPing: () -> Unit
) {
    val targetButtonColor = when {
        isRunning -> Color(0xFF1976D2)
        smartConnecting -> Color(0xFFFFC107)
        smartConnectionFailed -> Color(0xFFD32F2F)
        else -> Color.White
    }
    val buttonColor by animateColorAsState(targetButtonColor, label = "mobiletina-fab-color")
    val buttonContentColor = when {
        isRunning -> Color.White
        smartConnecting -> Color(0xFF3D3000)
        smartConnectionFailed -> Color.White
        else -> Color(0xFF202124)
    }

    FloatingActionButton(
        onClick = onToggle,
        modifier = Modifier.size(156.dp),
        shape = CircleShape,
        containerColor = buttonColor,
        contentColor = buttonContentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = Triple(isRunning, smartConnecting, smartConnectionFailed),
                label = "mobiletina-connect-visual"
            ) { state ->
                val (running, connecting, failed) = state
                when {
                    connecting -> {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(82.dp),
                                strokeWidth = 5.dp,
                                color = buttonContentColor
                            )
                            if (smartCountdownSeconds > 0) {
                                Text(
                                    text = stringResource(
                                        R.string.mobiletina_countdown_format,
                                        smartCountdownSeconds
                                    ),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_flash_on_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }

                    running -> Icon(
                        painter = painterResource(R.drawable.ic_stop_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(58.dp)
                    )

                    failed -> Icon(
                        painter = painterResource(R.drawable.ic_flash_off_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(58.dp)
                    )

                    else -> Icon(
                        painter = painterResource(R.drawable.ic_play_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(58.dp)
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    Column(
        modifier = Modifier
            .clickable(
                enabled = selectedServerName.isNotBlank() && !smartConnecting,
                onClick = onTestPing
            )
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                isRunning -> stringResource(R.string.mobiletina_status_connected)
                smartConnecting -> stringResource(R.string.mobiletina_status_connecting)
                smartConnectionFailed -> stringResource(R.string.mobiletina_status_failed)
                else -> stringResource(R.string.mobiletina_status_disconnected)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        if (smartConnecting) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.mobiletina_testing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else if (selectedServerName.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = when {
                    selectedPingMillis > 0L -> selectedPingMillis.toString()
                    selectedPingMillis < 0L -> stringResource(R.string.mobiletina_ping_inactive)
                    else -> stringResource(R.string.mobiletina_tap_for_ping)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selectedPingMillis != 0L) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    selectedPingMillis > 0L -> MaterialTheme.colorScheme.primary
                    selectedPingMillis < 0L -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun MobileTinaSubscriptionStatusCard(
    selectedSubscriptionId: String,
    refreshKey: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedSubscription = remember(selectedSubscriptionId, refreshKey) {
        resolveSelectedSubscription(selectedSubscriptionId)
    } ?: return

    val totalBytes = (selectedSubscription.trafficTotalBytes ?: 0L).coerceAtLeast(0L)
    val uploadBytes = (selectedSubscription.trafficUploadBytes ?: 0L).coerceAtLeast(0L)
    val downloadBytes = (selectedSubscription.trafficDownloadBytes ?: 0L).coerceAtLeast(0L)
    val usedBytes = (uploadBytes + downloadBytes).coerceAtLeast(0L)
    val expireEpochSeconds = (selectedSubscription.expireEpochSeconds ?: 0L).coerceAtLeast(0L)

    if (totalBytes <= 0L && expireEpochSeconds <= 0L) return

    SubscriptionStatusCard(
        remarks = selectedSubscription.remarks,
        usedBytes = usedBytes,
        totalBytes = totalBytes,
        expireEpochSeconds = expireEpochSeconds,
        modifier = modifier
    )
}

private fun resolveSelectedSubscription(selectedSubscriptionId: String): SubscriptionItem? {
    return selectedSubscriptionId
        .takeIf { it.isNotBlank() }
        ?.let(MmkvManager::decodeSubscription)
        ?: MmkvManager.getSelectServer()
            ?.let(MmkvManager::decodeServerConfig)
            ?.subscriptionId
            ?.takeIf { it.isNotBlank() }
            ?.let(MmkvManager::decodeSubscription)
}

@Composable
private fun SubscriptionStatusCard(
    remarks: String,
    usedBytes: Long,
    totalBytes: Long,
    expireEpochSeconds: Long,
    modifier: Modifier = Modifier
) {
    val hasTraffic = totalBytes > 0L
    val daysRemaining = remainingDays(expireEpochSeconds)

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp)
        ) {
            Text(
                text = remarks.ifBlank { stringResource(R.string.mobiletina_subscription_status) },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (hasTraffic) {
                Spacer(Modifier.height(14.dp))
                val progress = (usedBytes.toDouble() / totalBytes.toDouble())
                    .coerceIn(0.0, 1.0)
                    .toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (daysRemaining != null) {
                    Text(
                        text = stringResource(
                            R.string.mobiletina_subscription_days_remaining,
                            daysRemaining
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                if (hasTraffic) {
                    Text(
                        text = stringResource(
                            R.string.mobiletina_subscription_usage_compact,
                            formatBytes(usedBytes),
                            formatBytes(totalBytes)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

private fun remainingDays(epochSeconds: Long): Long? {
    if (epochSeconds <= 0L) return null
    val remainingSeconds = epochSeconds - (System.currentTimeMillis() / 1000L)
    if (remainingSeconds <= 0L) return 0L
    return (remainingSeconds + SECONDS_PER_DAY - 1L) / SECONDS_PER_DAY
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    val unitIndex = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        .coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(unitIndex.toDouble())
    return if (unitIndex == 0) {
        "$bytes ${units[unitIndex]}"
    } else {
        String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
    }
}

private const val SECONDS_PER_DAY = 86_400L
