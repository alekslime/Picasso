package app.picasso.tracer.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ArProjectionTest {

    private fun identity() = FloatArray(16) { if (it % 5 == 0) 1f else 0f }

    @Test
    fun `origin projects to screen centre under identity`() {
        val p = ArProjection.projectToScreen(identity(), identity(), identity(), 1080f, 1920f)
        assertNotNull(p)
        assertEquals(540f, p!!.x, 0.01f)
        assertEquals(960f, p.y, 0.01f)
    }

    @Test
    fun `translated model shifts pixels`() {
        // Simple orthographic-ish projection: scale world by 100px, no perspective.
        val proj = identity().also { it[0] = 100f; it[5] = 100f }
        val model = identity().also { it[12] = 2f; it[13] = 1f }
        val p = ArProjection.projectToScreen(model, identity(), proj, 1080f, 1920f)
        assertNotNull(p)
        // NDC = (200, 100) -> off-screen right, but math holds: x = (200*.5+.5)*1080.
        assertEquals((200f * 0.5f + 0.5f) * 1080f, p!!.x, 0.5f)
    }

    @Test
    fun `point behind camera returns null`() {
        // View matrix translating the world +5 in z with a perspective projection
        // puts the origin behind the camera (negative w after multiply).
        val proj = perspective(fovYDeg = 60f, aspect = 0.5f, near = 0.1f, far = 100f)
        val view = identity().also { it[14] = 5f }
        assertNull(ArProjection.projectToScreen(identity(), view, proj, 1080f, 1920f))
    }

    private fun perspective(fovYDeg: Float, aspect: Float, near: Float, far: Float): FloatArray {
        val f = (1.0 / kotlin.math.tan(Math.toRadians(fovYDeg / 2.0))).toFloat()
        return FloatArray(16).also {
            it[0] = f / aspect
            it[5] = f
            it[10] = (far + near) / (near - far)
            it[11] = -1f
            it[14] = (2f * far * near) / (near - far)
        }
    }

    @Test
    fun `identical orientations give zero roll`() {
        val q = floatArrayOf(0f, 0f, 0f, 1f)
        assertEquals(
            0f,
            ArProjection.rollDeltaDeg(q, q, floatArrayOf(0f, 0f, -1f)),
            0.001f,
        )
    }

    @Test
    fun `ninety degree roll about forward axis`() {
        val ref = floatArrayOf(0f, 0f, 0f, 1f)
        // 90 deg about -Z (camera forward): sin(45), axis (0,0,-1).
        val s = 0.7071068f
        val cur = floatArrayOf(0f, 0f, -s, s)
        assertEquals(
            90f,
            ArProjection.rollDeltaDeg(ref, cur, floatArrayOf(0f, 0f, -1f)),
            0.5f,
        )
    }
}
