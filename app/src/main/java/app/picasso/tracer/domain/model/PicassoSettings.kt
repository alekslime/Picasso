package app.picasso.tracer.domain.model

/** Appearance choice for the whole app. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Persisted user settings: appearance plus defaults for new tracing projects. */
data class PicassoSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultOpacity: Float = 0.5f,
    val defaultGridRows: Int = 3,
    val defaultGridCols: Int = 3,
    /** First-trace tutorial: shown until the user finishes or skips it once. */
    val tutorialCompleted: Boolean = false,
    /** Google account profile stored on-device (sign-in is local-only, no backend). */
    val accountEmail: String? = null,
    val accountName: String? = null,
    /** One-time Pro purchase unlocked (also restored from Play on every connect). */
    val proUnlocked: Boolean = false,
)
