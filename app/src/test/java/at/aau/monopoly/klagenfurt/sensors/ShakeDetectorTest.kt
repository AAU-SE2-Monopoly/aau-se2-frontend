package at.aau.monopoly.klagenfurt.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.test.advanceUntilIdle

@OptIn(ExperimentalCoroutinesApi::class)
class ShakeDetectorTest {

    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var detector: ShakeDetector

    private var eventCount = 0

    @Before
    fun setup() {
        context = mockk()
        sensorManager = mockk(relaxed = true)
        accelerometer = mockk()

        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns accelerometer
        every { accelerometer.type } returns Sensor.TYPE_ACCELEROMETER

        eventCount = 0
        detector = ShakeDetector(context)
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

        val detectorWithoutSensor = ShakeDetector(context)

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
    fun `shakeEvents emits when event is manually emitted`() = runTest {
        val job = launch {
            detector.shakeEvents.collect {
                eventCount++
            }
        }

        advanceUntilIdle()

        val field = ShakeDetector::class.java.getDeclaredField("_shakeEvents")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableFlow = field.get(detector) as kotlinx.coroutines.flow.MutableSharedFlow<Unit>

        mutableFlow.tryEmit(Unit)

        advanceUntilIdle()

        assertEquals(1, eventCount)

        job.cancel()
    }

    @Test
    fun `onAccuracyChanged does nothing`() {
        detector.onAccuracyChanged(
            accelerometer,
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        )

        assertEquals(0, eventCount)
    }
}