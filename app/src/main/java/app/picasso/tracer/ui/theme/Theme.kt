package app.picasso.tracer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Figma spacing/radius: 4pt base, every rounded corner 20dp (circles stay circular).
val PicassoShapes = Shapes(
    extraSmall = RoundedCornerShape(20.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun PicassoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default: wallpaper-based colors would hide the Picasso palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> PicassoDarkColors
        else -> PicassoLightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = PicassoTypography,
        shapes = PicassoShapes,
        content = content,
    )
}
