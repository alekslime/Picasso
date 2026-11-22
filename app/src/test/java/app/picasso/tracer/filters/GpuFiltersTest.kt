package app.picasso.tracer.filters

import app.picasso.tracer.domain.model.TraceFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GpuFiltersTest {

    @Test
    fun `defaults produce no filter`() {
        assertNull(GpuFilters.lightMatrix(0f, 1f))
        assertNull(GpuFilters.colorFilterFor(TraceFilter.ORIGINAL))
        assertNull(GpuFilters.colorFilterFor(TraceFilter.ORIGINAL, 0f, 1f))
    }

    @Test
    fun `brightness offsets every channel`() {
        val matrix = GpuFilters.lightMatrix(0.5f, 1f)!!

        assertEquals(1f, matrix[0, 0], 0.001f)
        assertEquals(127.5f, matrix[0, 4], 0.001f)
        assertEquals(127.5f, matrix[1, 4], 0.001f)
        assertEquals(127.5f, matrix[2, 4], 0.001f)
        assertEquals(1f, matrix[3, 3], 0.001f)
    }

    @Test
    fun `contrast scales around mid-gray`() {
        val matrix = GpuFilters.lightMatrix(0f, 2f)!!

        assertEquals(2f, matrix[0, 0], 0.001f)
        assertEquals(-128f, matrix[0, 4], 0.001f)
    }

    @Test
    fun `out-of-range light inputs clamp`() {
        val bright = GpuFilters.lightMatrix(5f, 1f)!!
        val dark = GpuFilters.lightMatrix(-5f, 1f)!!
        val punchy = GpuFilters.lightMatrix(0f, 99f)!!

        assertEquals(255f, bright[0, 4], 0.001f)
        assertEquals(-255f, dark[0, 4], 0.001f)
        assertEquals(GpuFilters.MAX_CONTRAST, punchy[0, 0], 0.001f)
    }

    @Test
    fun `light composes with trace filters`() {
        assertNotNull(GpuFilters.colorFilterFor(TraceFilter.GRAYSCALE))
        assertNotNull(GpuFilters.colorFilterFor(TraceFilter.GRAYSCALE, 0.5f, 1f))
        assertNotNull(GpuFilters.colorFilterFor(TraceFilter.HIGH_CONTRAST, 0f, 2f))
        // CPU filters have no GPU matrix, but light alone still applies.
        assertNotNull(GpuFilters.colorFilterFor(TraceFilter.EDGES, 0.5f, 1f))
    }
}
