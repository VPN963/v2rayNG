package com.v2ray.ang.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R

private val InstagramAccent = Color(0xFFE83E78)
private val TelegramAccent = Color(0xFF159BD7)

@Suppress("UNUSED_PARAMETER")
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
                    .padding(horizontal = 20.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(22.dp))

                Text(
                    text = stringResource(R.string.mobiletina_store_page_title),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.mobiletina_store_about_subtitle),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                StoreContactCard(
                    text = stringResource(R.string.mobiletina_store_instagram_branch_1),
                    accent = InstagramAccent,
                    onClick = { openExternal(context, "https://www.instagram.com/mobile.tina/") }
                )
                Spacer(Modifier.height(12.dp))
                StoreContactCard(
                    text = stringResource(R.string.mobiletina_store_instagram_branch_2),
                    accent = InstagramAccent,
                    onClick = { openExternal(context, "https://www.instagram.com/mobile.tina2/") }
                )
                Spacer(Modifier.height(12.dp))
                StoreContactCard(
                    text = stringResource(R.string.mobiletina_store_instagram_branch_3),
                    accent = InstagramAccent,
                    onClick = { openExternal(context, "https://www.instagram.com/mobile.tinaa/") }
                )
                Spacer(Modifier.height(12.dp))
                StoreContactCard(
                    text = stringResource(R.string.mobiletina_store_developer),
                    accent = TelegramAccent,
                    onClick = { openExternal(context, "https://t.me/vpn963") }
                )

                Spacer(Modifier.height(30.dp))

                Text(
                    text = stringResource(R.string.mobiletina_store_address_section),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

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
private fun StoreContactCard(
    text: String,
    accent: Color,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(48.dp)
                    .background(accent)
            )
        }
    }
}

@Composable
private fun StoreAddressCard(text: String) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
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
