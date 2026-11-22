package app.picasso.tracer.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Picasso palette from Figma ("Picasso — Camera Tracing UI", Foundations board).
// Ink #171717 · Surface #242424 · Paper #F1EBDD · Ochre #E99A52
// Ochre is reserved for active / record / primary actions.
val PicassoInk = Color(0xFF171717)
val PicassoSurface = Color(0xFF242424)
val PicassoPaper = Color(0xFFF1EBDD)
val PicassoOchre = Color(0xFFE99A52)
val PicassoBlack = Color(0xFF0A0A0A)
val PicassoPaperMuted = Color(0xFFA79F8E)
val PicassoLine = Color(0xFF333333)

val PicassoLightColors = lightColorScheme(
    primary = PicassoOchre,
    onPrimary = PicassoInk,
    primaryContainer = Color(0xFFF3DFC2),
    onPrimaryContainer = PicassoInk,
    secondary = PicassoSurface,
    onSecondary = PicassoPaper,
    secondaryContainer = Color(0xFFE7DFCE),
    onSecondaryContainer = PicassoInk,
    tertiary = Color(0xFF7A6A4F),
    onTertiary = PicassoPaper,
    tertiaryContainer = Color(0xFFEDE3CC),
    onTertiaryContainer = PicassoInk,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = PicassoPaper,
    onBackground = PicassoInk,
    surface = PicassoPaper,
    onSurface = PicassoInk,
    surfaceVariant = Color(0xFFE9E1CF),
    onSurfaceVariant = Color(0xFF5C5648),
    surfaceContainerLowest = Color(0xFFFFFDF6),
    surfaceContainerLow = Color(0xFFFBF6E9),
    surfaceContainer = Color(0xFFF3ECDA),
    surfaceContainerHigh = Color(0xFFEBE1CB),
    surfaceContainerHighest = Color(0xFFE2D6BC),
    inverseSurface = PicassoInk,
    inverseOnSurface = PicassoPaper,
    outline = Color(0xFF8A8069),
    outlineVariant = Color(0xFFCFC4A8),
    scrim = PicassoBlack,
)

val PicassoDarkColors = darkColorScheme(
    primary = PicassoOchre,
    onPrimary = PicassoInk,
    primaryContainer = PicassoSurface,
    onPrimaryContainer = PicassoPaper,
    secondary = PicassoPaper,
    onSecondary = PicassoInk,
    secondaryContainer = PicassoSurface,
    onSecondaryContainer = PicassoPaper,
    tertiary = PicassoPaperMuted,
    onTertiary = PicassoInk,
    tertiaryContainer = PicassoSurface,
    onTertiaryContainer = PicassoPaper,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = PicassoBlack,
    onBackground = PicassoPaper,
    surface = PicassoInk,
    onSurface = PicassoPaper,
    surfaceVariant = PicassoSurface,
    onSurfaceVariant = PicassoPaperMuted,
    surfaceContainerLowest = PicassoBlack,
    surfaceContainerLow = PicassoInk,
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = PicassoSurface,
    surfaceContainerHighest = Color(0xFF2E2E2E),
    inverseSurface = PicassoPaper,
    inverseOnSurface = PicassoInk,
    outline = Color(0xFF6B6B6B),
    outlineVariant = PicassoLine,
    scrim = PicassoBlack,
)
