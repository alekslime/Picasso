package app.picasso.tracer.filters

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
 */
@Singleton
class FilterProcessor @Inject constructor() {

    private val cache = LruCache<String, Bitmap>(CACHE_ENTRIES)

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
        // LruCache has no prefix removal; drop everything for this image by key scan.
        for (filter in TraceFilter.entries) {
            cache.remove("$imagePath|${filter.name}")
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
        private const val CACHE_ENTRIES = 8
    }
}
