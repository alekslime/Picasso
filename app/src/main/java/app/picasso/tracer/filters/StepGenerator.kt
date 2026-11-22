package app.picasso.tracer.filters

import android.graphics.Bitmap
import androidx.annotation.StringRes
import app.picasso.tracer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** One progressive tracing layer; [bitmap] null means the untouched original. */
data class StepLayer(
    val index: Int,
    @StringRes val nameRes: Int,
    val bitmap: Bitmap?,
)

@Singleton
class StepGenerator @Inject constructor(
    private val processor: FilterProcessor,
) {

    /**
     * Builds 5 layers from blocked shapes to full image, sharing the filter cache.
     * Runs entirely on Dispatchers.Default.
     */
    suspend fun buildSteps(imagePath: String, source: Bitmap): List<StepLayer> =
        withContext(Dispatchers.Default) {
            val worker = ImageFilters.fitWithin(source, FilterProcessor.WORKER_DIMENSION)
            try {
                listOf(
                    StepLayer(0, R.string.step_block, cached(imagePath, 0) {
                        ImageFilters.blockValues(worker)
                    }),
                    StepLayer(1, R.string.step_values, cached(imagePath, 1) {
                        val blurred = ImageFilters.boxBlur(worker, 2)
                        ImageFilters.posterize(blurred, 3).also { blurred.recycle() }
                    }),
                    StepLayer(2, R.string.step_soft, cached(imagePath, 2) {
                        val blurred = ImageFilters.boxBlur(worker, 1)
                        ImageFilters.edges(blurred).also { blurred.recycle() }
                    }),
                    StepLayer(3, R.string.step_outline, cached(imagePath, 3) {
                        ImageFilters.edges(worker)
                    }),
                    StepLayer(4, R.string.step_full_image, null),
                )
            } finally {
                if (worker !== source) worker.recycle()
            }
        }

    private fun cached(imagePath: String, index: Int, compute: () -> Bitmap): Bitmap {
        val key = "step|$imagePath|$index"
        return processor.get(key) ?: compute().also { processor.put(key, it) }
    }

    companion object {
        const val COUNT = 5
    }
}
