package com.example.ui.theme

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyberEmerald,
    onPrimary = Color(0xFF00291B),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = CyberCyan,
    onSecondary = Color(0xFF002F45),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = CyberIndigo,
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = Color(0xFF312E81),
    onTertiaryContainer = Color(0xFFE0E7FF),
    background = CipherBackground,
    onBackground = TextPrimaryDark,
    surface = CipherSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CipherSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = Color(0xFF162033),
    surfaceContainerHighest = Color(0xFF1E293B),
    outline = Color(0xFF334155),
    error = CyberRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SapphirePrimary,
    onPrimary = Color.White,
    primaryContainer = SapphireContainer,
    onPrimaryContainer = Color(0xFF002B6B),
    secondary = LagoonSecondary,
    onSecondary = Color.White,
    secondaryContainer = LagoonContainer,
    onSecondaryContainer = Color(0xFF0F524B),
    tertiary = RoyalViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = Color(0xFF312E81),
    background = PearlBackground,
    onBackground = TextPrimaryLight,
    surface = PearlSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = PearlSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHigh = Color(0xFFF8FAFC),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    error = CyberRed,
    onError = Color.White
)

@Composable
private fun animateColor(targetValue: Color, label: String): Color {
    val animated by animateColorAsState(
        targetValue = targetValue,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = label
    )
    return animated
}

@Composable
fun SecureSmsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val base = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is ComponentActivity) {
                if (darkTheme) {
                    context.enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    )
                } else {
                    context.enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        ),
                        navigationBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    )
                }
            }
            val window = (context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    val animatedScheme: ColorScheme = base.copy(
        primary = animateColor(base.primary, "primary"),
        onPrimary = animateColor(base.onPrimary, "onPrimary"),
        primaryContainer = animateColor(base.primaryContainer, "primaryContainer"),
        onPrimaryContainer = animateColor(base.onPrimaryContainer, "onPrimaryContainer"),
        secondary = animateColor(base.secondary, "secondary"),
        onSecondary = animateColor(base.onSecondary, "onSecondary"),
        secondaryContainer = animateColor(base.secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = animateColor(base.onSecondaryContainer, "onSecondaryContainer"),
        tertiary = animateColor(base.tertiary, "tertiary"),
        background = animateColor(base.background, "background"),
        onBackground = animateColor(base.onBackground, "onBackground"),
        surface = animateColor(base.surface, "surface"),
        onSurface = animateColor(base.onSurface, "onSurface"),
        surfaceVariant = animateColor(base.surfaceVariant, "surfaceVariant"),
        onSurfaceVariant = animateColor(base.onSurfaceVariant, "onSurfaceVariant"),
        surfaceContainerHigh = animateColor(base.surfaceContainerHigh, "surfaceContainerHigh"),
        surfaceContainerHighest = animateColor(base.surfaceContainerHighest, "surfaceContainerHighest"),
        outline = animateColor(base.outline, "outline")
    )

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalTextStyle provides Typography.bodyLarge
        ) {
            content()
        }
    }
}
