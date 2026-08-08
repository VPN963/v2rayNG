package com.v2ray.ang.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.compose.colorFabActive
import com.v2ray.ang.util.QRCodeDecoder
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull

private const val SUBSCRIPTION_REVEAL_HOLD_MS = 10_000L

@Composable
fun GroupTabBar(
    groups: List<GroupMapItem>,
    selectedTabIndex: Int,
    mainViewModel: MainViewModel,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var revealedSubscriptionId by remember { mutableStateOf<String?>(null) }

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex.coerceIn(0, groups.lastIndex),
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 16.dp,
        minTabWidth = 56.dp,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier
                    .tabIndicatorOffset(
                        selectedTabIndex = selectedTabIndex.coerceIn(0, groups.lastIndex),
                        matchContentSize = true
                    )
                    .clip(RoundedCornerShape(3.dp)),
                width = Dp.Unspecified,
                color = colorFabActive
            )
        },
        divider = {}
    ) {
        groups.forEachIndexed { index, group ->
            GroupTabItem(
                group = group,
                selected = index == selectedTabIndex,
                serverFlowProvider = { mainViewModel.serversForGroup(group.id) },
                onClick = { onTabClick(index) },
                onRevealSubscription = {
                    if (group.id.isNotBlank()) {
                        val subscription = MmkvManager.decodeSubscription(group.id)
                        if (subscription != null && subscription.url.isNotBlank()) {
                            revealedSubscriptionId = group.id
                        }
                    }
                }
            )
        }
    }

    revealedSubscriptionId?.let { subscriptionId ->
        SubscriptionSecretDialog(
            subscriptionId = subscriptionId,
            onDismiss = { revealedSubscriptionId = null }
        )
    }
}

@Composable
private fun GroupTabItem(
    group: GroupMapItem,
    selected: Boolean,
    serverFlowProvider: () -> StateFlow<List<ServersCache>>,
    onClick: () -> Unit,
    onRevealSubscription: () -> Unit
) {
    val serverFlow = remember(group.id) { serverFlowProvider() }
    val servers by serverFlow.collectAsStateWithLifecycle()
    val text = if (group.id.isEmpty()) {
        group.remarks
    } else {
        "${group.remarks} (${servers.size})"
    }

    Box(
        modifier = Modifier
            .tenSecondHold(
                onClick = onClick,
                onHold = onRevealSubscription
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) colorFabActive else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SubscriptionSecretDialog(
    subscriptionId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val subscription = remember(subscriptionId) {
        MmkvManager.decodeSubscription(subscriptionId)
    } ?: return
    val qrBitmap = remember(subscription.url) {
        QRCodeDecoder.createQRCode(subscription.url)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = subscription.remarks.ifBlank {
                    stringResource(R.string.mobiletina_subscription_secret_title)
                }
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(220.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }

                Button(
                    onClick = { Utils.setClipboard(context, subscription.url) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.mobiletina_copy_subscription_link))
                }

                Button(
                    onClick = {
                        AngConfigManager.shareNonCustomConfigsToClipboard(
                            context,
                            MmkvManager.decodeServerList(subscriptionId)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.mobiletina_copy_all_configs))
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.mobiletina_close))
                }
            }
        },
        confirmButton = {}
    )
}

private fun Modifier.tenSecondHold(
    onClick: () -> Unit,
    onHold: () -> Unit
): Modifier = pointerInput(onClick, onHold) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var cancelledBeforeTimeout = false

        val up = withTimeoutOrNull(SUBSCRIPTION_REVEAL_HOLD_MS) {
            val result = waitForUpOrCancellation()
            if (result == null) cancelledBeforeTimeout = true
            result
        }

        when {
            up != null -> onClick()
            !cancelledBeforeTimeout -> {
                onHold()
                // Consume the remainder of this gesture without turning the 10-second hold into a tap.
                waitForUpOrCancellation()
            }
        }
    }
}
