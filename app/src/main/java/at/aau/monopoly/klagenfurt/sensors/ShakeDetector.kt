package at.aau.monopoly.klagenfurt.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.sqrt

/**
 * Detects shake gestures using the accelerometer sensor.
 * Exposes [shakeEvents] as a [SharedFlow] so consumers can react using
 * Flow operators (combine, filter, etc.) instead of callbacks.
 */
class ShakeDetector(context: Context) : SensorEventListener {

    companion object {
        // Shake detection threshold (m/s²)
        // Adjust this value based on desired sensitivity
        private const val SHAKE_THRESHOLD = 25f
        // Minimum time between shake detections (ms)
        private const val SHAKE_TIME_THRESHOLD = 800L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime = 0L

    private val _shakeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val shakeEvents: SharedFlow<Unit> = _shakeEvents.asSharedFlow()

    fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate acceleration magnitude (ignore gravity)
        val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        // Check if acceleration exceeds threshold and debounce
        val currentTime = System.currentTimeMillis()
        if (acceleration > SHAKE_THRESHOLD &&
            currentTime - lastShakeTime > SHAKE_TIME_THRESHOLD) {
            lastShakeTime = currentTime
            _shakeEvents.tryEmit(Unit)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
}

