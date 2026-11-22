package app.picasso.tracer.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.picasso.tracer.domain.model.PicassoSettings
import app.picasso.tracer.domain.model.ThemeMode
import app.picasso.tracer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<PicassoSettings> =
        dataStore.data.map { prefs ->
            PicassoSettings(
                themeMode = prefs[Keys.THEME_MODE]?.let {
                    runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
                } ?: ThemeMode.SYSTEM,
                defaultOpacity = prefs[Keys.DEFAULT_OPACITY]?.coerceIn(0f, 1f)
                    ?: PicassoSettings().defaultOpacity,
                defaultGridRows = prefs[Keys.DEFAULT_GRID_ROWS]?.coerceIn(1, 8)
                    ?: PicassoSettings().defaultGridRows,
                defaultGridCols = prefs[Keys.DEFAULT_GRID_COLS]?.coerceIn(1, 8)
                    ?: PicassoSettings().defaultGridCols,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setDefaultOpacity(opacity: Float) {
        dataStore.edit { it[Keys.DEFAULT_OPACITY] = opacity.coerceIn(0f, 1f) }
    }

    override suspend fun setDefaultGrid(rows: Int, cols: Int) {
        dataStore.edit {
            it[Keys.DEFAULT_GRID_ROWS] = rows.coerceIn(1, 8)
            it[Keys.DEFAULT_GRID_COLS] = cols.coerceIn(1, 8)
        }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_OPACITY = floatPreferencesKey("default_opacity")
        val DEFAULT_GRID_ROWS = intPreferencesKey("default_grid_rows")
        val DEFAULT_GRID_COLS = intPreferencesKey("default_grid_cols")
    }
}
