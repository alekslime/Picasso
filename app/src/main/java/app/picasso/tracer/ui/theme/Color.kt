package app.picasso.tracer.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Picasso green. Light mode is a clean white surface with a deep studio green;
// dark mode is a near-black green with a soft bright green accent.
val PicassoGreen = Color(0xFF18794E)
val PicassoGreenBright = Color(0xFF6FD698)
val PicassoInk = Color(0xFF101714)
val PicassoPaper = Color(0xFFFFFFFF)
val PicassoNight = Color(0xFF0B120E)

val PicassoLightColors = lightColorScheme(
    primary = PicassoGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7EDD6),
    onPrimaryContainer = Color(0xFF0B3B24),
    secondary = Color(0xFF4A6353),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE9D8),
    onSecondaryContainer = Color(0xFF0E2E1E),
    tertiary = Color(0xFF3E7A67),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBFEAD8),
    onTertiaryContainer = Color(0xFF073026),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surface = PicassoPaper,
    onSurface = PicassoInk,
    surfaceVariant = Color(0xFFDFE7E1),
    onSurfaceVariant = Color(0xFF3F4A44),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F7F4),
    surfaceContainer = Color(0xFFEAF0EB),
    surfaceContainerHigh = Color(0xFFE4EAE5),
    surfaceContainerHighest = Color(0xFFDEE5DF),
    inverseSurface = Color(0xFF2C322E),
    inverseOnSurface = Color(0xFFEDF2EE),
    outline = Color(0xFF6F7973),
    outlineVariant = Color(0xFFBFC9C2),
    scrim = Color(0xFF000000),
)

val PicassoDarkColors = darkColorScheme(
    primary = PicassoGreenBright,
    onPrimary = Color(0xFF07331D),
    primaryContainer = Color(0xFF0E5C35),
    onPrimaryContainer = Color(0xFFC9F2DA),
    secondary = Color(0xFFA9CDB8),
    onSecondary = Color(0xFF0E2E1E),
    secondaryContainer = Color(0xFF2A4A38),
    onSecondaryContainer = Color(0xFFD5EADB),
    tertiary = Color(0xFF8ED4BC),
    onTertiary = Color(0xFF073026),
    tertiaryContainer = Color(0xFF1E4A3C),
    onTertiaryContainer = Color(0xFFBFEAD8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surface = PicassoNight,
    onSurface = Color(0xFFE6ECE7),
    surfaceVariant = Color(0xFF223129),
    onSurfaceVariant = Color(0xFFB9C6BD),
    surfaceContainerLowest = Color(0xFF060B08),
    surfaceContainerLow = Color(0xFF0B120E),
    surfaceContainer = Color(0xFF0F1713),
    surfaceContainerHigh = Color(0xFF1A231D),
    surfaceContainerHighest = Color(0xFF242E27),
    inverseSurface = Color(0xFFE6ECE7),
    inverseOnSurface = Color(0xFF101714),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF3A453F),
    scrim = Color(0xFF000000),
)
