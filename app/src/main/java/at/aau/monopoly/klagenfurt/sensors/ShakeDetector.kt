package at.aau.monopoly.klagenfurt.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() }
) : SensorEventListener {

    companion object {
        private const val SHAKE_THRESHOLD = 25f
        private const val SHAKE_TIME_THRESHOLD = 800L
    }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime = 0L

    fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        detectShake(
            x = event.values[0],
            y = event.values[1],
            z = event.values[2]
        )
    }

    internal fun detectShake(x: Float, y: Float, z: Float) {
        val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val currentTime = currentTimeMillis()

        if (
            acceleration > SHAKE_THRESHOLD &&
            currentTime - lastShakeTime > SHAKE_TIME_THRESHOLD
        ) {
            lastShakeTime = currentTime
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}