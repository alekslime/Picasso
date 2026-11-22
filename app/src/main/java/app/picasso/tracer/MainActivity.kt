package app.picasso.tracer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
        super.onCreate(savedInstanceState)
        openProjectId = intent?.getLongExtra(EXTRA_OPEN_PROJECT_ID, -1L) ?: -1L
        PicassoWidgetProvider.refresh(this)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsState(PicassoSettings())
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
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
