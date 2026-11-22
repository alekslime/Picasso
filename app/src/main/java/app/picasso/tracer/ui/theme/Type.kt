package app.picasso.tracer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

// System type with a touch more hierarchy: semibold titles, medium labels.
val PicassoTypography = Typography(
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Typography().titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.Medium),
)
