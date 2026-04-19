package at.aau.monopoly.klagenfurt.ui
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor

import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameboardUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<GameboardUI>()

    @Test
    fun `verify GameboardScreen is called in onCreate`() {
        composeTestRule
            .onNodeWithContentDescription("Klagenfurt-Map")
            .assertExists()
    }


    @Test
    fun testToComposeColor() {
        assertEquals(Color(0xFF955436), PropertyColor.BROWN.toComposeColor())
        assertEquals(Color(0xFFAAE0FA), PropertyColor.LIGHT_BLUE.toComposeColor())
        assertEquals(Color(0xFFD93A96), PropertyColor.PINK.toComposeColor())
        assertEquals(Color(0xFFF7941D), PropertyColor.ORANGE.toComposeColor())
        assertEquals(Color(0xFFED1B24), PropertyColor.RED.toComposeColor())
        assertEquals(Color(0xFFFEF200), PropertyColor.YELLOW.toComposeColor())
        assertEquals(Color(0xFF1FB25A), PropertyColor.GREEN.toComposeColor())
        assertEquals(Color(0xFF0072BB), PropertyColor.DARK_BLUE.toComposeColor())
    }

    @Test
    fun testZoomStateLogic() {
        val state = ZoomState()
        val containerSize = Size(1000f, 1000f)

        // Test zoom in
        state.updateTransformation(Offset.Zero, 2f, containerSize)
        assertEquals(2f, state.scale)
        assertEquals(Offset.Zero, state.offset)

        // Test pan within bounds (maxX = 1000 * (2-1) / 2 = 500)
        state.updateTransformation(Offset(100f, 100f), 1f, containerSize)
        assertEquals(100f, state.offset.x)
        assertEquals(100f, state.offset.y)

        // Test pan out of bounds (should coerce)
        state.updateTransformation(Offset(1000f, 1000f), 1f, containerSize)
        assertEquals(500f, state.offset.x)
        assertEquals(500f, state.offset.y)

        // Test reset on zoom out to 1.0
        state.updateTransformation(Offset.Zero, 0.1f, containerSize)
        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun testCalculateFieldBoundsCorners() {
        // Full HD scale (design size)
        val sw = 3840f
        val sh = 2160f

        // Corner 0 (Go)
        val b0 = calculateFieldBounds(0, sw, sh)
        assertTrue(b0.isCorner)
        assertEquals(0f, b0.rotation)
        assertEquals(2445f - 120f, b0.x, 0.1f)
        assertEquals(1720f - 120f, b0.y, 0.1f)

        // Corner 10 (Jail)
        val b10 = calculateFieldBounds(10, sw, sh)
        assertEquals(0f, b10.rotation)

        // Corner 20 (Free Parking)
        val b20 = calculateFieldBounds(20, sw, sh)
        assertEquals(0f, b20.rotation)

        // Corner 30 (Go To Jail)
        val b30 = calculateFieldBounds(30, sw, sh)
        assertEquals(0f, b30.rotation)
    }

    @Test
    fun testCalculateFieldBoundsSideCalculations() {
        val sw = 3840f
        val sh = 2160f

        // Side 0 (Bottom) - Field 1
        val b1 = calculateFieldBounds(1, sw, sh)
        assertFalse(b1.isCorner)
        assertEquals(0f, b1.rotation)

        // Side 1 (Left) - Field 11
        val b11 = calculateFieldBounds(11, sw, sh)
        assertEquals(90f, b11.rotation)

        // Side 2 (Top) - Field 21
        val b21 = calculateFieldBounds(21, sw, sh)
        assertEquals(180f, b21.rotation)

        // Side 3 (Right) - Field 31
        val b31 = calculateFieldBounds(31, sw, sh)
        assertEquals(270f, b31.rotation)
    }

    @Test
    fun testScalingIndependence() {
        // Test that bounds scale linearly with screen size
        val b1_large = calculateFieldBounds(1, 3840f, 2160f)
        val b1_small = calculateFieldBounds(1, 1920f, 1080f)

        assertEquals(b1_large.x / 2f, b1_small.x, 0.1f)
        assertEquals(b1_large.width / 2f, b1_small.width, 0.1f)
    }
}


