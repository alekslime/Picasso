package app.picasso.tracer.camera

/** Observable camera hardware state for the trace screen. */
data class CameraState(
    val torchOn: Boolean = false,
    val recording: Boolean = false,
    val error: String? = null,
)
