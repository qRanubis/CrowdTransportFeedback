package com.example.crowdtransportfeedback.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = AppPrimary, onPrimary = Color.White, primaryContainer = AppPrimaryContainer,
    onPrimaryContainer = Color(0xFF102B64), background = AppBackground,
    onBackground = AppOnSurface, surface = AppSurface, onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceVariant, onSurfaceVariant = AppOnSurfaceVariant,
    outline = AppOutline, error = Color(0xFFB3261E), errorContainer = Color(0xFFFFDAD6)
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary, onPrimary = Color(0xFF082E72), primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFDCE5FF), background = DarkBackground,
    onBackground = DarkOnSurface, surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = Color(0xFFFFB4AB), errorContainer = Color(0xFF93000A)
)

@Composable
fun CrowdTransportFeedbackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = MaterialTheme(
    colorScheme = if (darkTheme) DarkColors else LightColors,
    typography = Typography,
    shapes = Shapes(
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(16.dp)
    ),
    content = content
)
