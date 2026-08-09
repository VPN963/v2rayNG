package com.v2ray.ang.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R

private val InstagramAccent = Color(0xFFE83E78)
private val TelegramAccent = Color(0xFF159BD7)

@Composable
fun MobileTinaStoreAboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.mobiletina_store_back))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.mobiletina_store_about_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }

                Spacer(Modifier.height(18.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "T",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.mobiletina_store_about_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.mobiletina_store_about_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = stringResource(R.string.mobiletina_store_official_channels),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                SectionTitle(stringResource(R.string.mobiletina_store_social_section))
                Spacer(Modifier.height(12.dp))

                StoreContactCard(
                    title = stringResource(R.string.mobiletina_store_instagram_branch_1),
                    handle = "@mobile.tina",
                    accent = InstagramAccent,
                    onClick = { openExternal(context, "https://www.instagram.com/mobile.tina/") }
                )
                Spacer(Modifier.height(12.dp))
                StoreContactCard(
                    title = stringResource(R.string.mobiletina_store_instagram_branch_2),
                    handle = "@mobile.tina2",
                    accent = InstagramAccent,
                    onClick = { openExternal(context, "https://www.instagram.com/mobile.tina2/") }
                )
                Spacer(Modifier.height(12.dp))
                StoreContactCard(
                    title = stringResource(R.string.mobiletina_store_instagram_branch_3),
                    handle = "@mobile.tinaa",
                    accent = InstagramAccent,
                    onClick = { openExternal(context, "https://www.instagram.com/mobile.tinaa/") }
                )
                Spacer(Modifier.height(12.dp))
                StoreContactCard(
                    title = stringResource(R.string.mobiletina_store_developer),
                    handle = "Telegram  @vpn963",
                    accent = TelegramAccent,
                    onClick = { openExternal(context, "https://t.me/vpn963") }
                )

                Spacer(Modifier.height(30.dp))

                SectionTitle(stringResource(R.string.mobiletina_store_address_section))
                Spacer(Modifier.height(12.dp))

                StoreAddressCard(
                    text = stringResource(R.string.mobiletina_store_address_branch_1)
                )
                Spacer(Modifier.height(12.dp))
                StoreAddressCard(
                    text = stringResource(R.string.mobiletina_store_address_branch_2)
                )

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start
    )
}

@Composable
private fun StoreContactCard(
    title: String,
    handle: String,
    accent: Color,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = handle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(54.dp)
                    .background(accent, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun StoreAddressCard(text: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start
        )
    }
}

private fun openExternal(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
        )
    }
}
