package app.picasso.tracer

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.picasso.tracer.domain.model.PicassoSettings
import app.picasso.tracer.domain.model.ThemeMode
import app.picasso.tracer.domain.repository.SettingsRepository
import app.picasso.tracer.ui.nav.PicassoNav
import app.picasso.tracer.ui.theme.PicassoTheme
import app.picasso.tracer.widget.PicassoWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_PROJECT_ID = "app.picasso.tracer.OPEN_PROJECT_ID"

        /** How long the animated splash is held on Android 12+ so the stroke finishes drawing. */
        private const val SPLASH_MIN_MS = 950L
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openProjectId = intent.getLongExtra(EXTRA_OPEN_PROJECT_ID, -1L)
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    // Widget deep link: opened project id, consumed once by the nav host.
    private var openProjectId by mutableStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: swaps the splash theme for the app theme.
        val splash = installSplashScreen()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ plays the brush-stroke animation; hold the splash just long enough to finish it.
            val shownAt = SystemClock.uptimeMillis()
            splash.setKeepOnScreenCondition { SystemClock.uptimeMillis() - shownAt < SPLASH_MIN_MS }
        }
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(220)
                .withEndAction { provider.remove() }
                .start()
        }
        super.onCreate(savedInstanceState)
        openProjectId = intent?.getLongExtra(EXTRA_OPEN_PROJECT_ID, -1L) ?: -1L
        PicassoWidgetProvider.refresh(this)
        setContent {
            val settings by settingsRepository.settings.collectAsState(PicassoSettings())
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }
            // Bar icons follow the app's theme setting, not just the phone's: a Light app on a dark phone
            // would otherwise get light icons on Paper.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.argb(0xE6, 0xFF, 0xFF, 0xFF),
                        Color.argb(0x80, 0x1B, 0x1B, 0x1B),
                    ) { darkTheme },
                )
                onDispose { }
            }
            PicassoTheme(darkTheme = darkTheme) {
                PicassoNav(
                    openProjectId = openProjectId.takeIf { it > 0 },
                    onProjectConsumed = { openProjectId = -1L },
                )
            }
        }
    }
}
