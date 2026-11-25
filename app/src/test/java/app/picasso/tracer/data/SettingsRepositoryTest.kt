package app.picasso.tracer.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.picasso.tracer.BuildConfig
import app.picasso.tracer.domain.model.PicassoSettings
import app.picasso.tracer.domain.model.ThemeMode
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        testScope.cancel()
    }

    private fun repository(): SettingsRepositoryImpl {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("settings.preferences_pb") },
        )
        return SettingsRepositoryImpl(dataStore)
    }

    @Test
    fun `fresh store emits defaults`() = runTest {
        val repo = repository()

        assertEquals(PicassoSettings(), repo.settings.first())
    }

    @Test
    fun `tutorial flag persists`() = runTest {
        val repo = repository()
        assertEquals(false, repo.settings.first().tutorialCompleted)
        repo.setTutorialCompleted()
        assertEquals(true, repo.settings.first().tutorialCompleted)
    }

    @Test
    fun `account and pro flags persist`() = runTest {
        val repo = repository()
        assertEquals(null, repo.settings.first().accountEmail)
        // The pro edition ships unlocked; the base edition starts locked.
        assertEquals(BuildConfig.PRO_BUILD, repo.settings.first().proUnlocked)
        repo.setAccount("a@b.c", "Ada")
        repo.setProUnlocked(true)
        val s = repo.settings.first()
        assertEquals("a@b.c", s.accountEmail)
        assertEquals("Ada", s.accountName)
        assertEquals(true, s.proUnlocked)
        repo.clearAccount()
        assertEquals(null, repo.settings.first().accountEmail)
        assertEquals(true, repo.settings.first().proUnlocked)
    }

    @Test
    fun `setters persist and coerce`() = runTest {
        val repo = repository()

        repo.setThemeMode(ThemeMode.DARK)
        repo.setDefaultOpacity(9f)
        repo.setDefaultGrid(99, -4)

        val settings = repo.settings.first()
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(1f, settings.defaultOpacity, 0.001f)
        assertEquals(8, settings.defaultGridRows)
        assertEquals(1, settings.defaultGridCols)
    }

    @Test
    fun `training level persists and clears`() = runTest {
        val repo = repository()
        assertEquals(null, repo.settings.first().trainingLevel)
        repo.setTrainingLevel(app.picasso.tracer.domain.model.HelpLevel.OUTLINE)
        assertEquals(app.picasso.tracer.domain.model.HelpLevel.OUTLINE, repo.settings.first().trainingLevel)
        repo.setTrainingLevel(null)
        assertEquals(null, repo.settings.first().trainingLevel)
    }
}
