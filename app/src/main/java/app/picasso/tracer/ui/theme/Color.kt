package app.picasso.tracer.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Original palette built around a cobalt-blue seed.
val PicassoBlue = Color(0xFF3B6FE0)
val PicassoBlueDark = Color(0xFF8AA8F5)
val PicassoAccent = Color(0xFF7DD3FC)
val PicassoInk = Color(0xFF101828)
val PicassoPaper = Color(0xFFFAFAF7)

val PicassoLightColors = lightColorScheme(
    primary = PicassoBlue,
    onPrimary = Color.White,
    secondary = Color(0xFF5B7C99),
    tertiary = PicassoAccent,
    surface = PicassoPaper,
    onSurface = PicassoInk,
    surfaceVariant = Color(0xFFE8ECF3),
    onSurfaceVariant = Color(0xFF3E4A5E),
)

val PicassoDarkColors = darkColorScheme(
    primary = PicassoBlueDark,
    onPrimary = PicassoInk,
    secondary = Color(0xFF9DB4C8),
    tertiary = PicassoAccent,
    surface = PicassoInk,
    onSurface = Color(0xFFF2F4F8),
    surfaceVariant = Color(0xFF1D2939),
    onSurfaceVariant = Color(0xFFD0D8E4),
)
