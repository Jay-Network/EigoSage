package com.jworks.eigosage.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EigoLightScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E0FF),
    onPrimaryContainer = Color(0xFF1D175B),

    secondary = Color(0xFF0EA5A4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF0A3D3B),

    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF2D1C00),
    tertiaryContainer = Color(0xFFFFE9C2),
    onTertiaryContainer = Color(0xFF4A2F00),

    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF161A23),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF161A23),
    surfaceVariant = Color(0xFFE9ECF5),
    onSurfaceVariant = Color(0xFF444A5A),
    outline = Color(0xFF747A8A)
)

private val EigoDarkScheme = darkColorScheme(
    primary = Color(0xFFB9B3FF),
    onPrimary = Color(0xFF221B71),
    primaryContainer = Color(0xFF39308E),
    onPrimaryContainer = Color(0xFFE2E0FF),

    secondary = Color(0xFF5EE8D7),
    onSecondary = Color(0xFF003734),
    secondaryContainer = Color(0xFF00514D),
    onSecondaryContainer = Color(0xFFB8F5ED),

    tertiary = Color(0xFFFFC766),
    onTertiary = Color(0xFF3B2800),
    tertiaryContainer = Color(0xFF5A3E00),
    onTertiaryContainer = Color(0xFFFFE8BD),

    background = Color(0xFF050508),
    onBackground = Color(0xFFE6EAF3),
    surface = Color(0xFF0A0A10),
    onSurface = Color(0xFFE6EAF3),
    surfaceVariant = Color(0xFF12121A),
    onSurfaceVariant = Color(0xFFC4CAD8),
    outline = Color(0xFF8E96A8)
)

private val EigoTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun EigoSageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EigoDarkScheme
        else -> EigoLightScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = EigoTypography,
        content = content
    )
}

/** Glass gradient for card backgrounds: white 12% → white 5% */
val GlassGradient: Brush
    get() = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.05f)
        )
    )

/** Indigo-tinted border for glass cards at 28% opacity */
val GlassBorder: BorderStroke
    get() = BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.28f))

/** Card colors for glass cards — transparent container with glass gradient applied via Box background */
@Composable
fun glassCardColors(): CardColors = CardDefaults.cardColors(
    containerColor = Color.Transparent
)
