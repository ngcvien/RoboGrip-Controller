package com.example.robogripcontroller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.example.robogripcontroller.ui.AppColors

private val AppColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.Background,
    primaryContainer = AppColors.PrimarySoft,
    onPrimaryContainer = AppColors.TextMain,
    secondary = AppColors.Info,
    onSecondary = AppColors.TextMain,
    secondaryContainer = AppColors.SurfaceSoft,
    onSecondaryContainer = AppColors.TextMain,
    tertiary = AppColors.Success,
    onTertiary = AppColors.Background,
    background = AppColors.Background,
    onBackground = AppColors.TextMain,
    surface = AppColors.SurfaceStrong,
    onSurface = AppColors.TextMain,
    surfaceVariant = AppColors.SurfaceSoft,
    onSurfaceVariant = AppColors.TextMuted,
    outline = AppColors.Border,
    error = AppColors.Danger,
    onError = AppColors.TextMain
)

@Composable
fun RobogripcontrollerTheme(
    @Suppress("UNUSED_PARAMETER")
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
