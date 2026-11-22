package app.picasso.tracer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.picasso.tracer.R

// Typography from Figma: Headings Manrope SemiBold, Body Inter Regular,
// Numbers / technical controls JetBrains Mono. Fonts are OFL-licensed
// (Manrope, Inter, JetBrains Mono) bundled in res/font.
val ManropeFamily = FontFamily(
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
)

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

val PicassoTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp, lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    // Buttons / chips keep Inter Medium; technical labels switch to mono below.
    labelLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Normal,
        fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.4.sp,
    ),
)
