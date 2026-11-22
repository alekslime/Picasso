package app.picasso.tracer.camera

import java.util.Locale

/** Observable camera hardware state for the trace screen. */
data class CameraState(
    val torchOn: Boolean = false,
    val recording: Boolean = false,
    val error: String? = null,
)

/** Elapsed recording time as mm:ss. Fixed locale so digits never localize. */
fun formatElapsed(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
}
