package app.picasso.tracer.domain.repository

import app.picasso.tracer.domain.model.HelpLevel
import app.picasso.tracer.domain.model.PicassoSettings
import app.picasso.tracer.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<PicassoSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDefaultOpacity(opacity: Float)

    suspend fun setDefaultGrid(rows: Int, cols: Int)

    suspend fun setTutorialCompleted()

    suspend fun setAccount(email: String, name: String)

    suspend fun clearAccount()

    suspend fun setProUnlocked(unlocked: Boolean)

    /** Remembers the training wheels level; null turns training wheels off. */
    suspend fun setTrainingLevel(level: HelpLevel?)
}
