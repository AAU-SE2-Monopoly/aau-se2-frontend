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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

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
fun ZoomableWrapper(
    modifier: Modifier = Modifier,
    zoomState: ZoomState = remember { ZoomState() },
    content: @Composable () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Use graphicsLayer (compositing-level transform) for zoom/pan.
                        // Unlike the previous layout{} approach, this does NOT re-measure
                        // content at inflated pixel sizes. Content always measures at
                        // normal screen resolution. Vectors rasterize at their natural
                        // size, and the compositing pipeline scales/translates the
                        // already-rendered pixel buffer — preventing "Canvas: trying to
                        // draw too large bitmap" crashes.
                        scaleX = zoomState.scale
                        scaleY = zoomState.scale
                        translationX = zoomState.offset.x
                        translationY = zoomState.offset.y
                    },
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}
