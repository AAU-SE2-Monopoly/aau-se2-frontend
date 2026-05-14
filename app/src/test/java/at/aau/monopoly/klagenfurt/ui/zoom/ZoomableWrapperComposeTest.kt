package at.aau.monopoly.klagenfurt.ui.zoom

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [ZoomableWrapper] that verify it renders correctly
 * even when parent constraints are unbounded (e.g., inside a scrollable list).
 */
@RunWith(AndroidJUnit4::class)
class ZoomableWrapperComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `does not crash with unbounded height constraints in LazyColumn`() {
        // LazyColumn provides unbounded height constraints to children,
        // which previously caused an integer overflow when multiplied by the zoom scale.
        composeTestRule.setContent {
            LazyColumn {
                item {
                    ZoomableWrapper(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        Box(modifier = Modifier.size(100.dp))
                    }
                }
            }
        }
        // If we reach here without crashing, the unbounded constraint guard works.
        composeTestRule.waitForIdle()
    }

    @Test
    fun `does not crash with unbounded width constraints`() {
        // Using LazyRow to provide unbounded width constraints.
        composeTestRule.setContent {
            LazyRow {
                item {
                    ZoomableWrapper(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        Box(modifier = Modifier.size(100.dp))
                    }
                }
            }
        }
        // If we reach here without crashing, the unbounded constraint guard works.
        composeTestRule.waitForIdle()
    }

    @Test
    fun `renders normally with bounded constraints`() {
        // Normal use case: bounded constraints via fixed size modifier.
        composeTestRule.setContent {
            ZoomableWrapper(
                modifier = Modifier.size(400.dp)
            ) {
                Box(modifier = Modifier.size(100.dp))
            }
        }
        composeTestRule.waitForIdle()
        // If we reach here without crashing, bounded constraints work fine.
    }

    @Test
    fun `does not crash with large vector drawable at max zoom`() {
        // Regression test: the original bug caused a "Canvas: trying to draw too
        // large bitmap" crash when a vector drawable was rendered at 5× zoom.
        // Back then, ZoomableWrapper used layout{} to re-measure content at
        // inflated pixel sizes, causing VectorPainter to rasterize a ~241 MB
        // bitmap. The fix uses graphicsLayer{} instead — content always measures
        // at normal screen resolution and the compositing pipeline handles
        // zoom/pan after rasterization.

        // Pre-configure a ZoomState at max zoom (5×) with a small pan offset
        // to verify the pan path also works.
        val zoomState = ZoomState(initialScale = 5f, initialOffset = Offset(50f, -30f))

        composeTestRule.setContent {
            ZoomableWrapper(
                modifier = Modifier.size(400.dp),
                zoomState = zoomState
            ) {
                // pathreworked.xml has a 3840×2160 viewport — large enough
                // to trigger the crash at inflated measure sizes.
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.pathreworked),
                        contentDescription = "Path - Klagenfurt-Ring",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        // If we reach here without crashing at 5× zoom with a large vector,
        // the fix works correctly.
    }

    @Test
    fun `does not crash with large vector drawable at intermediate zoom levels`() {
        // Test multiple zoom levels to ensure the fix is robust across the range.
        // We test each zoom level in a separate @Test-method-style approach
        // by ramping the zoomState within a single setContent call.
        val zoomState = ZoomState(initialScale = 1f)
        val simulateContainer = androidx.compose.ui.geometry.Size(400f, 400f)

        composeTestRule.setContent {
            ZoomableWrapper(
                modifier = Modifier.size(400.dp),
                zoomState = zoomState
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.pathreworked),
                        contentDescription = "Path",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Step through increasing zoom levels
        zoomState.updateTransformation(Offset.Zero, 3f, simulateContainer)
        composeTestRule.waitForIdle()

        zoomState.updateTransformation(Offset.Zero, 1.5f, simulateContainer)
        composeTestRule.waitForIdle()

        zoomState.updateTransformation(Offset.Zero, 5f, simulateContainer)
        composeTestRule.waitForIdle()

        // No crash at any zoom level.
    }

    @Test
    fun `rapid zoom changes do not crash with large vector drawable`() {
        // Simulate rapid zoom changes like pinch-gestures would produce.
        val zoomState = ZoomState(initialScale = 1f)

        composeTestRule.setContent {
            ZoomableWrapper(
                modifier = Modifier.size(400.dp),
                zoomState = zoomState
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.pathreworked),
                        contentDescription = "Path",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Simulate rapid pinch-zooming through the full range
        val simulatePan = Offset(10f, 10f)
        val simulateContainer = androidx.compose.ui.geometry.Size(400f, 400f)
        for (i in 1..20) {
            zoomState.updateTransformation(simulatePan, 1.2f, simulateContainer) // zoom in
            zoomState.updateTransformation(simulatePan, 0.9f, simulateContainer) // zoom out slightly
        }
        composeTestRule.waitForIdle()
        // If we reach here without crashing after 20 rapid zoom changes,
        // the fix handles dynamic zoom correctly.
    }
}
