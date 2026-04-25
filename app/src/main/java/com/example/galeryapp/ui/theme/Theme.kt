package com.example.galeryapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Premium Color Palette
private val PrimaryBlue = Color(0xFF1565C0)
private val PrimaryLightBlue = Color(0xFF5E92F3)
private val DarkBlue = Color(0xFF0D47A1)
private val AccentPurple = Color(0xFF7B5BB4)
private val SuccessGreen = Color(0xFF2E7D32)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D3D),
    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7F6),
    onSecondaryContainer = Color(0xFF311B92),
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8E6C9),
    onTertiaryContainer = Color(0xFF1B5E20),
    background = Color(0xFFFAFBFD),
    onBackground = Color(0xFF1A1C1F),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1F),
    surfaceVariant = Color(0xFFE6E0EC),
    onSurfaceVariant = Color(0xFF4F4758),
    outline = Color(0xFF7A7586),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLightBlue,
    onPrimary = DarkBlue,
    primaryContainer = DarkBlue,
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFFBB86FC),
    onSecondary = Color(0xFF311B92),
    secondaryContainer = Color(0xFF311B92),
    onSecondaryContainer = Color(0xFFEDE7F6),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF1B5E20),
    tertiaryContainer = Color(0xFF1B5E20),
    onTertiaryContainer = Color(0xFFC8E6C9),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE1E1E1),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE1E1E1),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF8C8C8C),
    error = Color(0xFFFB6D6D),
    onError = Color(0xFF600E0E)
)

@Composable
fun GaleryAppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val typography = Typography(
        displayLarge = androidx.compose.material3.Typography().displayLarge.copy(
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        ),
        displayMedium = androidx.compose.material3.Typography().displayMedium.copy(
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        ),
        headlineSmall = androidx.compose.material3.Typography().headlineSmall.copy(
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        ),
        titleLarge = androidx.compose.material3.Typography().titleLarge.copy(
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        ),
        bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(
            fontSize = 14.sp
        )
    )

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp)
        ),
        content = content
    )
}

