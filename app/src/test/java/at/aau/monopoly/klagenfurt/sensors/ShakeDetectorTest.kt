package at.aau.monopoly.klagenfurt.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ShakeDetectorTest {

    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var detector: ShakeDetector

    private var shakeCount = 0
    private var now = 10_000L

    @Before
    fun setup() {
        context = mockk()
        sensorManager = mockk(relaxed = true)
        accelerometer = mockk()

        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns accelerometer
        every { accelerometer.type } returns Sensor.TYPE_ACCELEROMETER

        shakeCount = 0
        now = 10_000L

        detector = ShakeDetector(
            context = context,
            onShake = { shakeCount++ },
            currentTimeMillis = { now }
        )
    }

    @Test
    fun `startListening registers accelerometer listener when sensor exists`() {
        detector.startListening()

        verify {
            sensorManager.registerListener(
                detector,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    @Test
    fun `startListening does nothing when accelerometer is null`() {
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns null

        val detectorWithoutSensor = ShakeDetector(
            context = context,
            onShake = { shakeCount++ },
            currentTimeMillis = { now }
        )

        detectorWithoutSensor.startListening()

        verify(exactly = 0) {
            sensorManager.registerListener(any(), any<Sensor>(), any())
        }
    }

    @Test
    fun `stopListening unregisters listener`() {
        detector.stopListening()

        verify {
            sensorManager.unregisterListener(detector)
        }
    }

    @Test
    fun `onSensorChanged ignores null event`() {
        detector.onSensorChanged(null)

        assertEquals(0, shakeCount)
    }

    @Test
    fun `detectShake does not trigger shake below threshold`() {
        detector.detectShake(10f, 10f, 10f)

        assertEquals(0, shakeCount)
    }

    @Test
    fun `detectShake does not trigger shake exactly at threshold`() {
        detector.detectShake(25f, 0f, 0f)

        assertEquals(0, shakeCount)
    }

    @Test
    fun `detectShake triggers shake above threshold`() {
        detector.detectShake(26f, 0f, 0f)

        assertEquals(1, shakeCount)
    }

    @Test
    fun `detectShake triggers shake for combined axis acceleration above threshold`() {
        detector.detectShake(15f, 15f, 15f)

        assertEquals(1, shakeCount)
    }

    @Test
    fun `detectShake debounces shake within time threshold`() {
        now = 10_000L
        detector.detectShake(30f, 0f, 0f)

        now = 10_500L
        detector.detectShake(30f, 0f, 0f)

        assertEquals(1, shakeCount)
    }

    @Test
    fun `detectShake allows shake after debounce time threshold`() {
        now = 10_000L
        detector.detectShake(30f, 0f, 0f)

        now = 10_801L
        detector.detectShake(30f, 0f, 0f)

        assertEquals(2, shakeCount)
    }

    @Test
    fun `detectShake does not allow shake exactly at debounce threshold`() {
        now = 10_000L
        detector.detectShake(30f, 0f, 0f)

        now = 10_800L
        detector.detectShake(30f, 0f, 0f)

        assertEquals(1, shakeCount)
    }

    @Test
    fun `onAccuracyChanged does nothing`() {
        detector.onAccuracyChanged(
            accelerometer,
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        )

        assertEquals(0, shakeCount)
    }

    private fun mockSensorEvent(
        sensor: Sensor,
        values: FloatArray
    ): SensorEvent {
        val event = mockk<SensorEvent>()

        every { event.sensor } returns sensor
        every { event.values } returns values

        return event
    }
}