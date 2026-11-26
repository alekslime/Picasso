package app.picasso.tracer.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import app.picasso.tracer.camera.ClassicAnchor
import app.picasso.tracer.camera.OverlayAnchor
import app.picasso.tracer.data.ImageFiles
import app.picasso.tracer.data.ImageStore
import app.picasso.tracer.data.ProjectRepositoryImpl
import app.picasso.tracer.data.SettingsRepositoryImpl
import app.picasso.tracer.data.local.PicassoDatabase
import app.picasso.tracer.data.local.ProjectDao
import app.picasso.tracer.domain.repository.ProjectRepository
import app.picasso.tracer.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore("settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PicassoDatabase =
        Room.databaseBuilder(context, PicassoDatabase::class.java, "picasso.db")
            .addMigrations(*PicassoDatabase.ALL_MIGRATIONS)
            .build()

    @Provides
    @Singleton
    fun provideProjectDao(db: PicassoDatabase): ProjectDao = db.projectDao()

    @Provides
    @Singleton
    fun provideImageFiles(store: ImageStore): ImageFiles = store

    @Provides
    @Singleton
    fun provideProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository = impl

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl

    @Provides
    @Singleton
    fun provideOverlayAnchor(): OverlayAnchor = ClassicAnchor()
}
