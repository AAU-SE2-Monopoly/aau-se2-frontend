package at.aau.monopoly.klagenfurt.ui.zoom

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * The current zoom scale, readable by child composables to adjust
 * rendering density if needed.
 */
val LocalZoomScale = compositionLocalOf { 1f }

class ZoomState(
    initialScale: Float = 1f,
    initialOffset: Offset = Offset.Zero
) {
    var scale by mutableStateOf(initialScale)
    var offset by mutableStateOf(initialOffset)

    fun updateTransformation(pan: Offset, zoom: Float, containerSize: Size) {
        val newScale = (scale * zoom).coerceIn(1f, 5f)
        val maxX = (containerSize.width * (newScale - 1)) / 2
        val maxY = (containerSize.height * (newScale - 1)) / 2

        scale = newScale
        if (scale > 1f) {
            val newOffset = offset + pan
            offset = Offset(
                x = newOffset.x.coerceIn(-maxX, maxX),
                y = newOffset.y.coerceIn(-maxY, maxY)
            )
        } else {
            offset = Offset.Zero
        }
    }
}

@Composable
fun ZoomableWrapper(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val zoomState = remember { ZoomState() }
    val baseDensity = LocalDensity.current
    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomState.updateTransformation(
                        pan, zoom,
                        Size(size.width.toFloat(), size.height.toFloat())
                    )
                }
            }
    ) {
        CompositionLocalProvider(LocalZoomScale provides zoomState.scale) {
            // Scale the density so that sp/dp values grow with the zoom,
            // keeping text and spacing proportional to the enlarged layout.
            val scaledDensity = Density(
                density = baseDensity.density * zoomState.scale,
                fontScale = baseDensity.fontScale
            )
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layout { measurable, constraints ->
                        // Measure content at scaled-up size so vectors render
                        // at the zoomed resolution (sharp, no bitmap upscale).
                        val s = zoomState.scale
                        // Guard against unbounded constraints (e.g. Constraints.Infinity)
                        // multiplying by zoom scale, which would overflow Int.
                        val maxW = if (constraints.hasBoundedWidth) constraints.maxWidth else 4096
                        val maxH = if (constraints.hasBoundedHeight) constraints.maxHeight else 4096
                        val scaledConstraints = constraints.copy(
                            maxWidth = (maxW * s).roundToInt().coerceAtMost(16384),
                            maxHeight = (maxH * s).roundToInt().coerceAtMost(16384)
                        )
                        val placeable = measurable.measure(scaledConstraints)
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            // Centre the scaled content, then apply pan offset.
                            val ox = ((constraints.maxWidth - placeable.width) / 2f +
                                    zoomState.offset.x).roundToInt()
                            val oy = ((constraints.maxHeight - placeable.height) / 2f +
                                    zoomState.offset.y).roundToInt()
                            placeable.place(ox, oy)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                content()
            }
            }
        }
    }
}
