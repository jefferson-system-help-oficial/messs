package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
      primary = OceanSecondary,
      secondary = OceanTertiary,
      tertiary = OceanPrimary,
      background = androidx.compose.ui.graphics.Color(0xFF0F172A),
      surface = androidx.compose.ui.graphics.Color(0xFF1E293B)
  )

private val LightColorScheme =
  lightColorScheme(
      primary = OceanPrimary,
      secondary = OceanSecondary,
      tertiary = OceanTertiary,
      background = OceanBackground,
      surface = OceanSurface
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disabled dynamic color by default to ensure custom pool branding colors shine
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
