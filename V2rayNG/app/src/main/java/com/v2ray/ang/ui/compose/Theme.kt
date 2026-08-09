package com.v2ray.ang.ui.compose

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Royal Qajar design tokens.
 * Dark navy carries the modern VPN identity, while muted gold and burgundy
 * provide the Qajar character without making the interface visually heavy.
 */
val QajarNavy = Color(0xFF071321)
val QajarNavyElevated = Color(0xFF0D1D30)
val QajarNavySoft = Color(0xFF142A43)
val QajarGold = Color(0xFFD8B56A)
val QajarGoldBright = Color(0xFFF0D28A)
val QajarIvory = Color(0xFFF4E8CA)
val QajarBurgundy = Color(0xFF7E2635)
val QajarEmerald = Color(0xFF31C997)
val QajarBlue = Color(0xFF5EA8FF)

private val LightColor = lightColorScheme(
    primary = Color(0xFF7B5A1D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3A5),
    onPrimaryContainer = Color(0xFF281800),
    secondary = QajarBurgundy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = Color(0xFF3B0713),
    tertiary = Color(0xFF006C54),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF77F8CE),
    onTertiaryContainer = Color(0xFF002117),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9F4E8),
    onBackground = Color(0xFF201B12),
    surface = Color(0xFFFFFBF2),
    onSurface = Color(0xFF201B12),
    surfaceVariant = Color(0xFFECE1CB),
    onSurfaceVariant = Color(0xFF514A3D),
    outline = Color(0xFF837662),
    outlineVariant = Color(0xFFD7C8AE),
    inverseSurface = QajarNavy,
    inverseOnSurface = QajarIvory,
    inversePrimary = QajarGoldBright,
    scrim = Color.Black,
    surfaceTint = Color(0xFF7B5A1D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF8EA),
    surfaceContainer = Color(0xFFF9F1E0),
    surfaceContainerHigh = Color(0xFFF2E8D5),
    surfaceContainerHighest = Color(0xFFEBDDC7),
)

private val DarkColor = darkColorScheme(
    primary = QajarGoldBright,
    onPrimary = Color(0xFF3B2A00),
    primaryContainer = Color(0xFF5A4317),
    onPrimaryContainer = Color(0xFFFFE4A3),
    secondary = Color(0xFFFFB1BE),
    onSecondary = Color(0xFF4A101E),
    secondaryContainer = QajarBurgundy,
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = Color(0xFF6EE9BF),
    onTertiary = Color(0xFF003829),
    tertiaryContainer = Color(0xFF00513E),
    onTertiaryContainer = Color(0xFF8CFFD3),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = QajarNavy,
    onBackground = QajarIvory,
    surface = QajarNavyElevated,
    onSurface = QajarIvory,
    surfaceVariant = QajarNavySoft,
    onSurfaceVariant = Color(0xFFD5C7A7),
    outline = Color(0xFF887B62),
    outlineVariant = Color(0xFF3D4651),
    inverseSurface = QajarIvory,
    inverseOnSurface = QajarNavy,
    inversePrimary = Color(0xFF775616),
    scrim = Color.Black,
    surfaceTint = QajarGold,
    surfaceContainerLowest = Color(0xFF040B13),
    surfaceContainerLow = Color(0xFF091726),
    surfaceContainer = QajarNavyElevated,
    surfaceContainerHigh = Color(0xFF12263D),
    surfaceContainerHighest = QajarNavySoft,
)

// Semantic colors used throughout server cards and connection status.
val colorPing = QajarEmerald
val colorPingRed = Color(0xFFFF7D88)
val colorConfigType = QajarGoldBright
val colorFabActive = QajarBurgundy
val colorFabInactiveLight = Color(0xFF9B8352)
val colorFabInactiveDark = Color(0xFF27364A)
val dividerColorLight = Color(0xFFE2D4B8)
val dividerColorDark = Color(0xFF26384D)

// Toast Colors 70%
val toastNormalBgLight = Color(0xD9162332)
val toastNormalBgDark = Color(0xE00A1422)
val toastSuccessBg = Color(0xD91F7158)
val toastErrorBg = Color(0xD98E2533)
val toastInfoBg = Color(0xD9275B91)
val toastIconCircleBg = Color(0x33FFFFFF)
val toastTextColor = Color.White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Ghajar VPN deliberately defaults to the royal dark appearance.
    // Users can still force light mode via the existing preference pipeline.
    val resolvedDark = when (ThemeManager.themeMode.collectAsState().value) {
        "1" -> false
        "2" -> true
        else -> darkTheme
    }
    val colorScheme = if (resolvedDark) DarkColor else LightColor
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = colorScheme.background.hashCode()
            window.navigationBarColor = colorScheme.background.hashCode()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !resolvedDark
                isAppearanceLightNavigationBars = !resolvedDark
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides resolvedDark,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
