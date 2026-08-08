package com.v2ray.ang.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.v2ray.ang.handler.MmkvManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun MobileTinaDashboard(
    isRunning: Boolean,
    smartConnecting: Boolean,
    smartCountdownSeconds: Int,
    smartConnectionFailed: Boolean,
    selectedServerName: String,
    selectedServerDetails: String,
    selectedPingMillis: Long,
    onToggle: () -> Unit,
    onTestPing: () -> Unit
) {
    val selectedSubscription = MmkvManager.getSelectServer()
        ?.let(MmkvManager::decodeServerConfig)
        ?.subscriptionId
        ?.takeIf { it.isNotBlank() }
        ?.let(MmkvManager::decodeSubscription)

    val totalBytes = selectedSubscription?.trafficTotalBytes ?: 0L
    val uploadBytes = selectedSubscription?.trafficUploadBytes ?: 0L
    val downloadBytes = selectedSubscription?.trafficDownloadBytes ?: 0L
    val usedBytes = (uploadBytes + downloadBytes).coerceAtLeast(0L)
    val remainingBytes = (totalBytes - usedBytes).coerceAtLeast(0L)
    val expireEpochSeconds = selectedSubscription?.expireEpochSeconds ?: 0L
    val showSubscriptionInfo = totalBytes > 0L || expireEpochSeconds > 0L

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showSubscriptionInfo) {
            SubscriptionStatusCard(
                remarks = selectedSubscription?.remarks.orEmpty(),
                usedBytes = usedBytes,
                totalBytes = totalBytes,
                remainingBytes = remainingBytes,
                expireEpochSeconds = expireEpochSeconds
            )
            Spacer(Modifier.height(24.dp))
        } else {
            Spacer(Modifier.height(20.dp))
        }

        FloatingActionButton(
            onClick = onToggle,
            modifier = Modifier.size(156.dp),
            shape = CircleShape,
            containerColor = buttonColor,
            contentColor = buttonContentColor
        ) {
            // Temporary visual. These icons are intentionally isolated here so the four
            // final user-provided images can replace them without touching connection logic.
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

        Spacer(Modifier.height(16.dp))
        Text(
            text = when {
                isRunning -> stringResource(R.string.mobiletina_status_connected)
                smartConnecting -> stringResource(R.string.mobiletina_status_connecting)
                smartConnectionFailed -> stringResource(R.string.mobiletina_status_failed)
                else -> stringResource(R.string.mobiletina_status_disconnected)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (smartConnecting) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.mobiletina_testing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = selectedServerName.isNotBlank(), onClick = onTestPing),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.mobiletina_connected_server),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                if (selectedServerName.isNotBlank()) {
                    Text(
                        text = selectedServerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectedServerDetails.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = selectedServerDetails,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (selectedPingMillis > 0L) {
                                    stringResource(R.string.mobiletina_ping_format, selectedPingMillis)
                                } else {
                                    stringResource(R.string.mobiletina_ping_unknown)
                                },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = stringResource(R.string.mobiletina_tap_for_ping),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.mobiletina_status_disconnected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionStatusCard(
    remarks: String,
    usedBytes: Long,
    totalBytes: Long,
    remainingBytes: Long,
    expireEpochSeconds: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp)
        ) {
            Text(
                text = stringResource(R.string.mobiletina_subscription_status),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (remarks.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = remarks,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (totalBytes > 0L) {
                Spacer(Modifier.height(12.dp))
                val progress = (usedBytes.toDouble() / totalBytes.toDouble())
                    .coerceIn(0.0, 1.0)
                    .toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.mobiletina_subscription_remaining,
                        formatBytes(remainingBytes),
                        formatBytes(totalBytes)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(
                        R.string.mobiletina_subscription_used,
                        formatBytes(usedBytes)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expireEpochSeconds > 0L) {
                Spacer(Modifier.height(if (totalBytes > 0L) 10.dp else 12.dp))
                Text(
                    text = stringResource(
                        R.string.mobiletina_subscription_expire,
                        formatExpiry(expireEpochSeconds)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun formatExpiry(epochSeconds: Long): String {
    val millis = epochSeconds.coerceAtMost(Long.MAX_VALUE / 1000L) * 1000L
    return SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault()).format(Date(millis))
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
