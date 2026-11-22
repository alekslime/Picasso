package app.picasso.tracer.camera

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Pure projection math for AR anchoring: where an anchored world point lands
 * on screen, and how much the camera rolled between two orientations.
 *
 * ARCore supplies the matrices/quaternions ([ArCoreAnchorSource]); this file
 * only does arithmetic on floats, so it is unit-testable on the JVM with no
 * ARCore classes on the classpath.
 */
object ArProjection {

    /** Screen position in pixels, or null when the point is behind the camera. */
    fun projectToScreen(
        modelMatrix: FloatArray,
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        widthPx: Float,
        heightPx: Float,
    ): ScreenPoint? {
        require(modelMatrix.size == 16 && viewMatrix.size == 16 && projMatrix.size == 16)
        val mv = multiply(viewMatrix, modelMatrix)
        val clip = multiplyVec(projMatrix, floatArrayOf(mv[12], mv[13], mv[14], mv[15]))
        val w = clip[3]
        if (w <= 0f) return null
        val ndcX = clip[0] / w
        val ndcY = clip[1] / w
        return ScreenPoint(
            x = (ndcX * 0.5f + 0.5f) * widthPx,
            y = (1f - (ndcY * 0.5f + 0.5f)) * heightPx,
        )
    }

    /**
     * In-plane roll in degrees between two world orientations, measured about
     * [forwardAxis] (the camera forward direction in world space at reference).
     * Positive follows the right-hand rule about that axis.
     */
    fun rollDeltaDeg(
        refQuat: FloatArray,
        curQuat: FloatArray,
        forwardAxis: FloatArray,
    ): Float {
        require(refQuat.size == 4 && curQuat.size == 4 && forwardAxis.size == 3)
        val relative = multiplyQuat(curQuat, conjugate(normalizeQuat(refQuat)))
        val twist = dot3(
            floatArrayOf(relative[0], relative[1], relative[2]),
            normalize3(forwardAxis),
        )
        return Math.toDegrees(2.0 * atan2(twist.toDouble(), relative[3].toDouble())).toFloat()
    }

    fun multiply(a: FloatArray, b: FloatArray): FloatArray {
        // Column-major 4x4, matching android.opengl.Matrix / ARCore Pose.toMatrix.
        val out = FloatArray(16)
        for (col in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) sum += a[k * 4 + row] * b[col * 4 + k]
                out[col * 4 + row] = sum
            }
        }
        return out
    }

    private fun multiplyVec(m: FloatArray, v: FloatArray): FloatArray =
        FloatArray(4) { row ->
            m[row] * v[0] + m[4 + row] * v[1] + m[8 + row] * v[2] + m[12 + row] * v[3]
        }

    private fun normalizeQuat(q: FloatArray): FloatArray {
        val n = sqrt((q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]).toDouble()).toFloat()
        if (n <= 0f) return floatArrayOf(0f, 0f, 0f, 1f)
        return floatArrayOf(q[0] / n, q[1] / n, q[2] / n, q[3] / n)
    }

    private fun conjugate(q: FloatArray) = floatArrayOf(-q[0], -q[1], -q[2], q[3])

    private fun multiplyQuat(a: FloatArray, b: FloatArray): FloatArray {
        val (ax, ay, az, aw) = a
        val (bx, by, bz, bw) = b
        return floatArrayOf(
            aw * bx + ax * bw + ay * bz - az * by,
            aw * by - ax * bz + ay * bw + az * bx,
            aw * bz + ax * by - ay * bx + az * bw,
            aw * bw - ax * bx - ay * by - az * bz,
        )
    }

    private operator fun FloatArray.component1() = this[0]
    private operator fun FloatArray.component2() = this[1]
    private operator fun FloatArray.component3() = this[2]
    private operator fun FloatArray.component4() = this[3]

    private fun dot3(a: FloatArray, b: FloatArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

    private fun normalize3(v: FloatArray): FloatArray {
        val n = sqrt((v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble()).toFloat()
        if (n <= 0f) return floatArrayOf(0f, 0f, -1f)
        return floatArrayOf(v[0] / n, v[1] / n, v[2] / n)
    }
}

data class ScreenPoint(val x: Float, val y: Float)
