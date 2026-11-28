package app.picasso.tracer

import android.app.Application
import app.picasso.tracer.data.CrashLogger
import app.picasso.tracer.filters.FilterProcessor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PicassoApp : Application() {

    @Inject lateinit var filterProcessor: FilterProcessor

    override fun onCreate() {
        super.onCreate()
        CrashLogger.forContext(this).install()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::filterProcessor.isInitialized) {
            filterProcessor.onTrimMemory(level)
        }
    }
}
