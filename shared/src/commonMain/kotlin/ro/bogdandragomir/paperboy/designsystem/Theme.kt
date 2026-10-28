package ro.bogdandragomir.paperboy.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Newsprint palette: warm paper, soft ink, one stamp-red accent.
private val Paper = Color(0xFFFAF7F2)
private val PaperDim = Color(0xFFF1ECE3)
private val Ink = Color(0xFF1C1B1A)
private val InkSoft = Color(0xFF5C5852)
private val Stamp = Color(0xFFC2410C)
private val NightPaper = Color(0xFF141312)
private val NightCard = Color(0xFF1E1D1B)
private val NightInk = Color(0xFFEDE8DF)
private val NightInkSoft = Color(0xFFA8A29A)
private val NightStamp = Color(0xFFFB923C)

private val LightColors =
    lightColorScheme(
        primary = Stamp,
        onPrimary = Paper,
        background = Paper,
        onBackground = Ink,
        surface = Paper,
        onSurface = Ink,
        surfaceVariant = PaperDim,
        onSurfaceVariant = InkSoft,
    )

private val DarkColors =
    darkColorScheme(
        primary = NightStamp,
        onPrimary = NightPaper,
        background = NightPaper,
        onBackground = NightInk,
        surface = NightPaper,
        onSurface = NightInk,
        surfaceVariant = NightCard,
        onSurfaceVariant = NightInkSoft,
    )

private val Headline = FontFamily.Serif

private val PaperboyTypography =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = Headline,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 40.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = Headline,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium =
            TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )

@Composable
fun PaperboyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PaperboyTypography,
        content = content,
    )
}
