package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CircularRevealShapeTest {

    @Test
    fun `CircularRevealShape at zero progress creates valid outline`() {
        val shape = CircularRevealShape(0f)
        val density = Density(1f)
        val size = Size(1000f, 1000f)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
        assertTrue(outline is androidx.compose.ui.graphics.Outline.Generic)
    }

    @Test
    fun `CircularRevealShape at full progress creates valid outline`() {
        val shape = CircularRevealShape(1f)
        val density = Density(1f)
        val size = Size(1000f, 1000f)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
        assertTrue(outline is androidx.compose.ui.graphics.Outline.Generic)
    }

    @Test
    fun `CircularRevealShape at half progress creates valid outline`() {
        val shape = CircularRevealShape(0.5f)
        val density = Density(1f)
        val size = Size(2000f, 1000f)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
        assertTrue(outline is androidx.compose.ui.graphics.Outline.Generic)
    }

    @Test
    fun `CircularRevealShape with different density`() {
        val shape = CircularRevealShape(0.75f)
        val density = Density(2f)
        val size = Size(500f, 300f)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
        assertNotNull(outline)
    }

    @Test
    fun `CircularRevealShape with RTL layout`() {
        val shape = CircularRevealShape(0.5f)
        val density = Density(1f)
        val size = Size(1000f, 1000f)
        val outline = shape.createOutline(size, LayoutDirection.Rtl, density)
        assertTrue(outline is androidx.compose.ui.graphics.Outline.Generic)
    }

    @Test
    fun `CircularRevealShape with very small size`() {
        val shape = CircularRevealShape(1f)
        val density = Density(1f)
        val size = Size(1f, 1f)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
        assertNotNull(outline)
    }

    @Test
    fun `CircularRevealShape with wide aspect ratio`() {
        val shape = CircularRevealShape(0.3f)
        val density = Density(1f)
        val size = Size(3840f, 2160f)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
        assertTrue(outline is androidx.compose.ui.graphics.Outline.Generic)
    }

    @Test
    fun `CircularRevealShape at near-zero progress`() {
        val shape = CircularRevealShape(0.01f)
        val density = Density(1f)
        val size = Size(1000f, 1000f)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
        assertTrue(outline is androidx.compose.ui.graphics.Outline.Generic)
    }
}
