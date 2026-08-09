package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.ui.compose.QajarBurgundy
import com.v2ray.ang.ui.compose.QajarEmerald
import com.v2ray.ang.ui.compose.QajarGold
import com.v2ray.ang.ui.compose.QajarGoldBright
import com.v2ray.ang.ui.compose.QajarIvory
import com.v2ray.ang.ui.compose.QajarNavy
import com.v2ray.ang.ui.compose.QajarNavyElevated
import com.v2ray.ang.ui.compose.QajarNavySoft
import com.v2ray.ang.util.Utils

enum class QajarSection(val title: String) {
    Home("خانه"),
    Servers("سرورها"),
    Subscriptions("اشتراک‌ها"),
    Tools("ابزارها"),
    Settings("تنظیمات")
}

@Composable
fun QajarSectionTopBar(section: QajarSection, isLoading: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(1.dp, QajarGold.copy(alpha = 0.28f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = QajarNavyElevated,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.qajar_king),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (section == QajarSection.Home) "قاجار VPN" else section.title,
                    color = QajarIvory,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (isLoading) "در حال تازه‌سازی دربار…" else "دربار دیجیتال قاجار",
                    color = QajarGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun QajarBottomNavigation(
    selected: QajarSection,
    onSelected: (QajarSection) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .border(1.dp, QajarGold.copy(alpha = 0.24f), RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = QajarNavyElevated,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QajarSection.entries.forEach { item ->
                val active = item == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelected(item) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (active) QajarGold else QajarNavyElevated
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (active) 6.dp else 4.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (active) 3.dp else 2.dp,
                                    color = if (active) QajarNavy else QajarGold.copy(alpha = 0.55f),
                                    shape = CircleShape
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            color = if (active) QajarNavy else QajarIvory.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QajarHomePanel(
    modifier: Modifier = Modifier,
    isRunning: Boolean,
    statusText: String,
    selectedServerName: String,
    selectedGroupName: String,
    subscriptionCount: Int,
    onAction: (MainAction) -> Unit,
    onOpenServers: () -> Unit,
    onOpenSubscriptions: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QajarGold.copy(alpha = 0.30f), RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = QajarNavyElevated,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.qajar_communications),
                        contentDescription = null,
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (isRunning) "فرمان اتصال صادر شد" else "آماده فرمان اتصال",
                            color = if (isRunning) QajarEmerald else QajarGoldBright,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = selectedServerName.ifBlank { "هنوز سروری انتخاب نشده" },
                            color = QajarIvory,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedGroupName.ifBlank { "اشتراک پیش‌فرض" },
                            color = QajarIvory.copy(alpha = 0.58f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(MainAction.ToggleService) }
                    .border(2.dp, QajarGoldBright.copy(alpha = 0.80f), RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                color = if (isRunning) QajarBurgundy else QajarGold,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 19.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        modifier = Modifier.size(58.dp),
                        shape = CircleShape,
                        color = if (isRunning) QajarNavySoft else QajarNavy
                    ) {
                        Icon(
                            painter = if (isRunning) painterResource(R.drawable.ic_stop_24dp)
                            else painterResource(R.drawable.ic_play_24dp),
                            contentDescription = null,
                            modifier = Modifier.padding(16.dp),
                            tint = QajarIvory
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isRunning) "قطع اتصال" else "اتصال به دربار",
                            color = if (isRunning) QajarIvory else QajarNavy,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = statusText.ifBlank {
                                if (isRunning) "ارتباط امن برقرار است" else "برای شروع لمس کنید"
                            },
                            color = if (isRunning) QajarIvory.copy(alpha = 0.72f)
                            else QajarNavy.copy(alpha = 0.72f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QajarQuickCard(
                    modifier = Modifier.weight(1f),
                    title = "سرورها",
                    subtitle = "انتخاب و تست",
                    iconRes = R.drawable.ic_subscriptions_24dp,
                    onClick = onOpenServers
                )
                QajarQuickCard(
                    modifier = Modifier.weight(1f),
                    title = "اشتراک‌ها",
                    subtitle = "$subscriptionCount اشتراک",
                    iconRes = R.drawable.ic_cloud_download_24dp,
                    onClick = onOpenSubscriptions
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QajarQuickCard(
                    modifier = Modifier.weight(1f),
                    title = "تازه‌سازی",
                    subtitle = "آپدیت Subscription",
                    iconRes = R.drawable.ic_restore_24dp,
                    onClick = { onAction(MainAction.UpdateSubscriptions) }
                )
                QajarQuickCard(
                    modifier = Modifier.weight(1f),
                    title = "تست پینگ",
                    subtitle = "سرور انتخابی",
                    iconRes = R.drawable.ic_check_update_24dp,
                    onClick = { onAction(MainAction.TestCurrentServer) }
                )
            }
        }
    }
}

@Composable
fun QajarSubscriptionsPanel(
    modifier: Modifier = Modifier,
    subscriptions: List<SubscriptionCache>,
    onUpdateAll: () -> Unit,
    onOpenManager: () -> Unit
) {
    val activeCount = subscriptions.count { it.subscription.enabled }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QajarGold.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = QajarNavyElevated
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Subscription Manager", color = QajarGoldBright, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$activeCount فعال از ${subscriptions.size} اشتراک · بروزرسانی خودکار هنگام ورود روشن است",
                        color = QajarIvory.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QajarCompactButton("مدیریت کامل", onOpenManager)
                        QajarCompactButton("بروزرسانی همه", onUpdateAll, primary = true)
                    }
                }
            }
        }

        if (subscriptions.isEmpty()) {
            item {
                QajarEmptyState("هنوز Subscription اضافه نشده", "از مدیریت کامل، لینک اشتراک را اضافه کنید.")
            }
        } else {
            items(subscriptions, key = { it.guid }) { item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (item.subscription.enabled) QajarGold.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    color = QajarNavyElevated
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .border(
                                    5.dp,
                                    if (item.subscription.enabled) QajarEmerald else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                item.subscription.remarks.ifBlank { "اشتراک بدون نام" },
                                color = QajarIvory,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                item.subscription.url,
                                color = QajarIvory.copy(alpha = 0.52f),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "آخرین بروزرسانی: ${Utils.formatTimestamp(item.subscription.lastUpdated)}",
                                color = QajarGold.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QajarToolsPanel(
    modifier: Modifier = Modifier,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { QajarPanelTitle("ابزارهای دربار", "ابزارهای تست، واردسازی، پشتیبان‌گیری و مسیریابی") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QajarQuickCard(Modifier.weight(1f), "تست همه", "Latency", R.drawable.ic_check_update_24dp) { onAction(MainAction.TestAllServers) }
                QajarQuickCard(Modifier.weight(1f), "Real Ping", "تست واقعی", R.drawable.ic_routing_24dp) { onAction(MainAction.TestRealAllServers) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QajarQuickCard(Modifier.weight(1f), "ورود از QR", "اسکن کانفیگ", R.drawable.ic_qr_code_scanner_24dp) { onAction(MainAction.ImportQRcode) }
                QajarQuickCard(Modifier.weight(1f), "Clipboard", "واردسازی متن", R.drawable.ic_copy) { onAction(MainAction.ImportClipboard) }
            }
        }
        item { QajarRouteCard("پشتیبان‌گیری و بازیابی", "Backup / Restore تنظیمات", MainDestination.BackupRestore, onNavigate) }
        item { QajarRouteCard("مسیریابی", "Routing Rules و قوانین عبور ترافیک", MainDestination.Routing, onNavigate) }
        item { QajarRouteCard("فایل‌های کاربر", "مدیریت assetهای شخصی", MainDestination.UserAssets, onNavigate) }
        item { QajarRouteCard("گزارش فنی", "مشاهده Logcat برنامه", MainDestination.Logcat, onNavigate) }
        item { QajarRouteCard("بررسی بروزرسانی", "نسخه و آپدیت برنامه", MainDestination.CheckUpdate, onNavigate) }
    }
}

@Composable
fun QajarSettingsPanel(
    modifier: Modifier = Modifier,
    onNavigate: (MainDestination) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { QajarPanelTitle("تنظیمات قاجار VPN", "تنظیمات اصلی بدون شلوغی منوی قدیمی") }
        item { QajarRouteCard("تنظیمات اصلی", "شبکه، DNS، رابط کاربری و هسته", MainDestination.Settings, onNavigate) }
        item { QajarRouteCard("Proxy برای برنامه‌ها", "انتخاب اپ‌هایی که از VPN عبور کنند", MainDestination.PerAppProxy, onNavigate) }
        item { QajarRouteCard("Subscription Manager", "افزودن، ویرایش و مدیریت اشتراک‌ها", MainDestination.Subscriptions, onNavigate) }
        item { QajarRouteCard("پشتیبان‌گیری", "خروجی گرفتن یا بازیابی تنظیمات", MainDestination.BackupRestore, onNavigate) }
        item { QajarRouteCard("درباره برنامه", "اطلاعات نسخه و هسته", MainDestination.About, onNavigate) }
    }
}

@Composable
private fun QajarPanelTitle(title: String, subtitle: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QajarGold.copy(alpha = 0.24f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = QajarNavyElevated
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(title, color = QajarGoldBright, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = QajarIvory.copy(alpha = 0.62f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun QajarQuickCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(1.dp, QajarGold.copy(alpha = 0.22f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = QajarNavyElevated,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.End
        ) {
            Surface(shape = CircleShape, color = QajarNavySoft) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = QajarGoldBright,
                    modifier = Modifier.padding(9.dp).size(20.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = QajarIvory, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(subtitle, color = QajarIvory.copy(alpha = 0.52f), fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun QajarRouteCard(
    title: String,
    subtitle: String,
    destination: MainDestination,
    onNavigate: (MainDestination) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(destination) }
            .border(1.dp, QajarGold.copy(alpha = 0.18f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = QajarNavyElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = QajarNavySoft) {
                Icon(
                    painter = painterResource(destination.iconRes),
                    contentDescription = null,
                    tint = QajarGoldBright,
                    modifier = Modifier.padding(9.dp).size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(title, color = QajarIvory, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = QajarIvory.copy(alpha = 0.55f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun QajarCompactButton(text: String, onClick: () -> Unit, primary: Boolean = false) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (primary) QajarGold else QajarNavySoft
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            color = if (primary) QajarNavy else QajarIvory,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QajarEmptyState(title: String, subtitle: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QajarGold.copy(alpha = 0.18f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = QajarNavyElevated
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(title, color = QajarIvory, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = QajarIvory.copy(alpha = 0.55f), fontSize = 10.sp)
        }
    }
}
