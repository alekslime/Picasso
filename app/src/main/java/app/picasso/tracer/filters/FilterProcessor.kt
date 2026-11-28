package app.picasso.tracer.filters

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.util.LruCache
import app.picasso.tracer.domain.model.TraceFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs CPU filters off the main thread and caches results per image+filter.
 * Bitmaps are produced at [WORKER_DIMENSION] px; the overlay upscales them on the GPU.
 * Cache capacity is bounded in kilobytes and trimmed on system memory pressure.
 */
@Singleton
class FilterProcessor @Inject constructor() {

    private val maxCacheKb: Int =
        (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt().coerceIn(MIN_CACHE_KB, MAX_CACHE_KB)

    private val cache = object : LruCache<String, Bitmap>(maxCacheKb) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    suspend fun filtered(imagePath: String, source: Bitmap, filter: TraceFilter): Bitmap =
        withContext(Dispatchers.Default) {
            val key = "$imagePath|${filter.name}"
            cache.get(key) ?: compute(source, filter).also { cache.put(key, it) }
        }

    /** Used by the step generator to share this cache. */
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun evict(imagePath: String) {
        for (filter in TraceFilter.entries) {
            cache.remove("$imagePath|${filter.name}")
        }
        for (index in 0 until StepGenerator.COUNT) {
            cache.remove("step|$imagePath|$index")
        }
    }

    /** Responds to OS memory pressure by trimming or clearing cached bitmaps. */
    @Suppress("DEPRECATION")
    fun onTrimMemory(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> cache.evictAll()
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> cache.trimToSize(maxCacheKb / 2)
        }
    }

    private fun compute(source: Bitmap, filter: TraceFilter): Bitmap {
        val worker = ImageFilters.fitWithin(source, WORKER_DIMENSION)
        val result = when (filter) {
            TraceFilter.EDGES -> ImageFilters.edges(worker)
            TraceFilter.POSTERIZE -> ImageFilters.posterize(worker)
            // GPU-handled filters never reach here, but stay total: return the worker copy.
            else -> worker.copy(worker.config ?: Bitmap.Config.ARGB_8888, false)
        }
        if (worker !== source && worker !== result) worker.recycle()
        return result
    }

    companion object {
        const val WORKER_DIMENSION = 1024
        private const val MIN_CACHE_KB = 8 * 1024   // 8 MB
        private const val MAX_CACHE_KB = 32 * 1024  // 32 MB
    }
}
