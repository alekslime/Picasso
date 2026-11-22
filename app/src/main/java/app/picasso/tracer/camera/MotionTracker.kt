package app.picasso.tracer.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Feeds [AnchorDrift] for AR anchor mode from the game rotation-vector
 * sensor. The first frame after [start] becomes the reference pose; later
 * frames are converted to a smoothed, clamped screen-space drift so the
 * overlay counter-moves with the phone and appears fixed to the paper.
 *
 * Deliberately behind this seam: an ARCore session can replace the
 * sensor math later without changing ViewModel or UI code.
 */
@Singleton
class MotionTracker @Inject constructor(
    @ApplicationContext context: Context,
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    val available: Boolean get() = sensor != null

    private var onDrift: ((AnchorDrift) -> Unit)? = null
    private var reference: FloatArray? = null
    private var smoothed = AnchorDrift()

    fun start(onDrift: (AnchorDrift) -> Unit) {
        val s = sensor ?: run {
            onDrift(AnchorDrift())
            return
        }
        this.onDrift = onDrift
        reference = null
        smoothed = AnchorDrift()
        sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        onDrift = null
        reference = null
        smoothed = AnchorDrift()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val emit = onDrift ?: return
        val current = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(current, event.values)
        val ref = reference
        if (ref == null) {
            reference = current
            return
        }
        val delta = FloatArray(3)
        SensorManager.getAngleChange(delta, current, ref)
        // Phone turns right (yaw+) -> overlay must slide left to stay world-fixed.
        val target = AnchorDrift(
            x = (-delta[0] * PX_PER_RAD).coerceIn(-MAX_DRIFT_PX, MAX_DRIFT_PX),
            y = (delta[1] * PX_PER_RAD).coerceIn(-MAX_DRIFT_PX, MAX_DRIFT_PX),
            rotationDeg = Math.toDegrees(delta[2].toDouble()).toFloat()
                .coerceIn(-MAX_DRIFT_DEG, MAX_DRIFT_DEG),
        )
        // Ignore micro-jitter, then low-pass smooth what remains.
        smoothed = AnchorDrift(
            x = smooth(smoothed.x, target.x),
            y = smooth(smoothed.y, target.y),
            rotationDeg = smooth(smoothed.rotationDeg, target.rotationDeg),
        )
        emit(smoothed)
    }

    private fun smooth(previous: Float, target: Float): Float {
        if (abs(target - previous) < DEAD_ZONE) return previous
        return previous + (target - previous) * SMOOTHING
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val PX_PER_RAD = 700f
        const val MAX_DRIFT_PX = 420f
        const val MAX_DRIFT_DEG = 30f
        const val SMOOTHING = 0.25f
        const val DEAD_ZONE = 0.35f
    }
}
