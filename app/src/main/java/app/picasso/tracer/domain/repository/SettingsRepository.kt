package app.picasso.tracer.domain.repository

import app.picasso.tracer.domain.model.PicassoSettings
import app.picasso.tracer.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<PicassoSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDefaultOpacity(opacity: Float)

    suspend fun setDefaultGrid(rows: Int, cols: Int)

    suspend fun setTutorialCompleted()
}
