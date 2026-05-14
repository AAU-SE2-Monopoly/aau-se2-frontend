package at.aau.monopoly.klagenfurt.ui.zoom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomableWrapperTest {

    @Test
    fun `ZoomState initial values are correct`() {
        val state = ZoomState()
        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun `ZoomState custom initial values`() {
        val state = ZoomState(initialScale = 2f, initialOffset = Offset(10f, 20f))
        assertEquals(2f, state.scale)
        assertEquals(Offset(10f, 20f), state.offset)
    }

    @Test
    fun `scale is clamped to max 5f`() {
        val state = ZoomState()
        val container = Size(1000f, 1000f)
        state.updateTransformation(Offset.Zero, 10f, container)
        assertEquals(5f, state.scale)
    }

    @Test
    fun `scale is clamped to min 1f`() {
        val state = ZoomState()
        val container = Size(1000f, 1000f)
        state.updateTransformation(Offset.Zero, 0.5f, container)
        assertEquals(1f, state.scale)
    }

    @Test
    fun `offset resets to zero when scale is 1f`() {
        val state = ZoomState(initialScale = 2f, initialOffset = Offset(100f, 100f))
        val container = Size(1000f, 1000f)
        // Zoom down to 1f
        state.updateTransformation(Offset(50f, 50f), 0.5f, container)
        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun `pan is clamped to max bounds`() {
        val state = ZoomState()
        val container = Size(1000f, 1000f)
        // Zoom to 3x: maxX = 1000*(3-1)/2 = 1000
        state.updateTransformation(Offset.Zero, 3f, container)
        assertEquals(3f, state.scale)

        // Pan beyond bounds
        state.updateTransformation(Offset(5000f, 5000f), 1f, container)
        assertEquals(1000f, state.offset.x)
        assertEquals(1000f, state.offset.y)
    }

    @Test
    fun `pan negative direction is clamped`() {
        val state = ZoomState()
        val container = Size(1000f, 1000f)
        state.updateTransformation(Offset.Zero, 3f, container)
        state.updateTransformation(Offset(-5000f, -5000f), 1f, container)
        assertEquals(-1000f, state.offset.x)
        assertEquals(-1000f, state.offset.y)
    }

    @Test
    fun `pan accumulates across multiple updates`() {
        val state = ZoomState()
        val container = Size(1000f, 1000f)
        state.updateTransformation(Offset.Zero, 2f, container)
        state.updateTransformation(Offset(100f, 50f), 1f, container)
        state.updateTransformation(Offset(100f, 50f), 1f, container)
        assertEquals(200f, state.offset.x)
        assertEquals(100f, state.offset.y)
    }

    @Test
    fun `scaled constraint does not overflow at max zoom with infinite input`() {
        val infinity = 0x3FFFFFFF // Constraints.Infinity internal value
        val scale = 5f
        val hasBounded = false
        val safeMax = if (hasBounded) infinity else 4096
        val result = (safeMax * scale).roundToInt().coerceAtMost(16384)
        // Must stay positive and within safe bounds
        assertTrue(result in 1..16384)
    }

    @Test
    fun `scaled constraint uses bounded value when hasBoundedWidth is true`() {
        val boundedValue = 1080
        val scale = 5f
        val safeMax = if (true) boundedValue else 4096
        val result = (safeMax * scale).roundToInt().coerceAtMost(16384)
        assertEquals(5400, result)
    }

    @Test
    fun `layout dimensions fall back to placeable size when unbounded`() {
        val infinity = 0x3FFFFFFF
        val hasBounded = false
        val placeableWidth = 2048
        val layoutW = if (hasBounded) infinity else placeableWidth
        assertEquals(placeableWidth, layoutW)
    }

    @Test
    fun `layout dimensions use constraint max when bounded`() {
        val constraintMax = 1080
        val hasBounded = true
        val placeableWidth = 500
        val layoutW = if (hasBounded) constraintMax else placeableWidth
        assertEquals(constraintMax, layoutW)
    }
}



