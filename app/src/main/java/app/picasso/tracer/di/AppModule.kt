package app.picasso.tracer.di

import android.content.Context
import androidx.room.Room
import app.picasso.tracer.camera.ClassicAnchor
import app.picasso.tracer.camera.OverlayAnchor
import app.picasso.tracer.data.ProjectRepositoryImpl
import app.picasso.tracer.data.local.PicassoDatabase
import app.picasso.tracer.data.local.ProjectDao
import app.picasso.tracer.domain.repository.ProjectRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PicassoDatabase =
        Room.databaseBuilder(context, PicassoDatabase::class.java, "picasso.db").build()

    @Provides
    @Singleton
    fun provideProjectDao(db: PicassoDatabase): ProjectDao = db.projectDao()

    @Provides
    @Singleton
    fun provideProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository = impl

    @Provides
    @Singleton
    fun provideOverlayAnchor(): OverlayAnchor = ClassicAnchor()
}
