package com.example.ui.theme

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
  primary = AmberAccent,
  onPrimary = EspressoDark,
  primaryContainer = EspressoSurfaceVariant,
  onPrimaryContainer = WarmSand,
  secondary = WarmSand,
  onSecondary = EspressoDark,
  secondaryContainer = EspressoSurfaceVariant,
  onSecondaryContainer = WarmSand,
  tertiary = SoftRed,
  onTertiary = Color.White,
  background = EspressoDark,
  onBackground = DarkTextPrimary,
  surface = EspressoSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = EspressoSurfaceVariant,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkOutline,
  error = SoftRed
)

private val LightColorScheme = lightColorScheme(
  primary = AmberAccentDark,
  onPrimary = Color.White,
  primaryContainer = LatteSurfaceVariant,
  onPrimaryContainer = LatteTextPrimary,
  secondary = MutedSlate,
  onSecondary = Color.White,
  secondaryContainer = LatteSurfaceVariant,
  onSecondaryContainer = LatteTextPrimary,
  tertiary = SoftRed,
  onTertiary = Color.White,
  background = LatteLight,
  onBackground = LatteTextPrimary,
  surface = LatteSurface,
  onSurface = LatteTextPrimary,
  surfaceVariant = LatteSurfaceVariant,
  onSurfaceVariant = LatteTextSecondary,
  outline = LatteOutline,
  error = SoftRed
)

@Composable
fun TedTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent calm branding
  content: @Composable () -> Unit,
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
