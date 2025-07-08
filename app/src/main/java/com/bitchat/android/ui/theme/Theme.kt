package com.bitchat.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Material Design 3 Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004B6F),
    onPrimaryContainer = Color(0xFFB8E6FF),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF003A16),
    secondaryContainer = Color(0xFF00531E),
    onSecondaryContainer = Color(0xFF9DF5A2),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF3E2D00),
    tertiaryContainer = Color(0xFF584200),
    onTertiaryContainer = Color(0xFFFFDEA6),
    error = Color(0xFFE57373),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFFC9D1D9),
    outline = Color(0xFF30363D),
    inverseOnSurface = Color(0xFF0D1117),
    inverseSurface = Color(0xFFE6EDF3),
    inversePrimary = Color(0xFF0969DA),
    surfaceTint = Color(0xFF4FC3F7),
    outlineVariant = Color(0xFF21262D),
    scrim = Color(0xFF000000),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0969DA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8E6FF),
    onPrimaryContainer = Color(0xFF003258),
    secondary = Color(0xFF2D5A41),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9DF5A2),
    onSecondaryContainer = Color(0xFF003A16),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA6),
    onTertiaryContainer = Color(0xFF3E2D00),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF0D1117),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0D1117),
    surfaceVariant = Color(0xFFF6F8FA),
    onSurfaceVariant = Color(0xFF4A5568),
    outline = Color(0xFFD0D7DE),
    inverseOnSurface = Color(0xFFE6EDF3),
    inverseSurface = Color(0xFF0D1117),
    inversePrimary = Color(0xFF4FC3F7),
    surfaceTint = Color(0xFF0969DA),
    outlineVariant = Color(0xFFE1E4E8),
    scrim = Color(0xFF000000),
)

@Composable
fun BitChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}