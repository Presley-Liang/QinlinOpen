package top.rpone.qinlinopen.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Keep the chosen Moonlight Silver colors consistent across Android versions.
// Dynamic (wallpaper-derived) colors would override the user-approved palette.
private val LightColorScheme =
  lightColorScheme(
    primary = MoonlightLightPrimary,
    onPrimary = Color.White,
    primaryContainer = MoonlightIce,
    onPrimaryContainer = MoonlightInk,
    secondary = MoonlightSilver,
    onSecondary = Color(0xFF101923), // 4.66:1 text contrast on silver.
    secondaryContainer = MoonlightIce,
    onSecondaryContainer = MoonlightInk,
    tertiary = MoonlightLightPrimary,
    onTertiary = Color.White,
    background = MoonlightLightBackground,
    onBackground = MoonlightInk,
    surface = MoonlightLightSurface,
    onSurface = MoonlightInk,
    surfaceVariant = MoonlightIce,
    onSurfaceVariant = MoonlightMutedText,
    outline = MoonlightOutline,
    error = Color(0xFFB83342),
    onError = Color.White,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = MoonlightDarkPrimary,
    onPrimary = MoonlightDarkSurface,
    primaryContainer = MoonlightDarkVariant,
    onPrimaryContainer = MoonlightDarkText,
    secondary = MoonlightReflection,
    onSecondary = MoonlightDarkSurface,
    secondaryContainer = MoonlightDarkVariant,
    onSecondaryContainer = MoonlightDarkText,
    tertiary = MoonlightDarkPrimary,
    onTertiary = MoonlightDarkSurface,
    background = MoonlightDarkBackground,
    onBackground = MoonlightDarkText,
    surface = MoonlightDarkSurface,
    onSurface = MoonlightDarkText,
    surfaceVariant = MoonlightDarkVariant,
    onSurfaceVariant = MoonlightIce,
    outline = MoonlightDarkOutline,
    error = Color(0xFFFF898D),
    onError = Color(0xFF360D12),
  )

@Composable
fun QinlinOpenTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
    typography = Typography,
    content = content,
  )
}
