package app.picasso.tracer.camera

/**
 * How much faster than real time a finished recording plays back.
 * [NORMAL] saves the camera's own 1x video untouched; the others re-encode it as a timelapse.
 */
enum class RecordingSpeed(val factor: Int) {
    NORMAL(1),
    X8(8),
    X16(16),
    X32(32),
    ;

    /** True when saving needs the speed-up pass. */
    val isSpedUp: Boolean get() = factor > 1

    /** Chip text, e.g. "32x". */
    val label: String get() = "${factor}x"

    /** Length of the saved video for a recording of [inputMs]; never shorter than one frame. */
    fun outputDurationMs(inputMs: Long): Long =
        (inputMs.coerceAtLeast(0L) / factor).coerceAtLeast(MIN_OUTPUT_MS)

    companion object {
        /** A 32x timelapse is what most people want from a drawing session. */
        val DEFAULT = X32

        /** Frame rate of the sped-up video; the extra source frames are dropped. */
        const val OUTPUT_FPS = 30f

        private const val MIN_OUTPUT_MS = 34L

        fun fromName(name: String?): RecordingSpeed? =
            name?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}
