package com.usj.adhoc.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmberRedDark,
    onPrimary = Color(0xFF4A1111),
    primaryContainer = Color(0xFF6A1B1B),
    onPrimaryContainer = Color(0xFFFFDAD6),

    secondary = FlameOrangeDark,
    onSecondary = Color(0xFF3B1C00),
    secondaryContainer = Color(0xFF5A2C00),
    onSecondaryContainer = Color(0xFFFFDDBA),

    tertiary = GoldenSparkDark,
    onTertiary = Color(0xFF3D2800),
    tertiaryContainer = Color(0xFF624200),
    onTertiaryContainer = Color(0xFFFFE7BA),

    background = Color(0xFF1A130F),
    onBackground = Color(0xFFF3DFD1),
    surface = Color(0xFF1A130F),
    onSurface = Color(0xFFF3DFD1),
    surfaceVariant = Color(0xFF3A2F27),
    onSurfaceVariant = Color(0xFFD8C2B3),
    outline = Color(0xFFA78E7F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = EmberRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),

    secondary = FlameOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = Color(0xFF2C1600),

    tertiary = GoldenSpark,
    onTertiary = Color(0xFF2D1E00),
    tertiaryContainer = Color(0xFFFFE6B3),
    onTertiaryContainer = Color(0xFF2D1E00),

    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF241A14),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF241A14),
    surfaceVariant = Color(0xFFF5E5D9),
    onSurfaceVariant = Color(0xFF5E4A3E),
    outline = Color(0xFF8E7668),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun AdHocTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}