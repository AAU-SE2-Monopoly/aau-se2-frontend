package at.aau.serg.websocketbrokerdemo.GameboardUI


import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.serg.websocketbrokerdemo.model.BoardFactory
import at.aau.serg.websocketdemoserver.model.enums.PropertyColor
import at.aau.serg.websocketdemoserver.model.field.Field
import at.aau.serg.websocketdemoserver.model.field.PropertyField
import com.example.myapplication.R

class GameboardUI : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameboardScreen()
        }
    }
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    if (!LocalInspectionMode.current) {
        DisposableEffect(orientation) {
            val activity = context as? Activity
            val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.requestedOrientation = orientation
            onDispose {
                activity?.requestedOrientation = originalOrientation
            }
        }
    }
}

@Composable
fun GameboardScreen(modifier: Modifier = Modifier) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    val fields = remember { BoardFactory.createDefaultBoard() }
    GameboardContent(fields, modifier)
}

@Composable
fun ZoomableWrapper(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    val maxX = (size.width.toFloat() * (newScale - 1)) / 2
                    val maxY = (size.height.toFloat() * (newScale - 1)) / 2

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
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun GameboardContent(
    fields: List<Field>,
    modifier: Modifier = Modifier
) {
    ZoomableWrapper(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(3840f / 2160f),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val sw = this.maxWidth.value
                val sh = this.maxHeight.value

                Image(
                    painter = painterResource(id = R.drawable.inskapedownscalewebp),
                    contentDescription = "Klagenfurt-Map",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                Image(
                    painter = painterResource(id = R.drawable.pathreworked),
                    contentDescription = "Path - Klagenfurt-Ring",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                fields.forEachIndexed { index, field ->
                    FieldItem(index, field, sw, sh)
                }
            }
        }
    }
}

@Composable
fun FieldItem(index: Int, field: Field, sw: Float, sh: Float) {
    val side = (index / 10) % 4
    val posInSide = index % 10
    val isCorner = posInSide == 0

    val corners = listOf(
        Offset(2445f, 1720f),
        Offset(1245f, 1720f),
        Offset(1245f, 520f),
        Offset(2445f, 520f)
    )

    val start = corners[side]
    val end = corners[(side + 1) % 4]
    val designCornerSize = 240f

    fun scaleX(x: Float) = (x / 3840f) * sw
    fun scaleY(y: Float) = (y / 2160f) * sh

    if (isCorner) {
        val textRotation = when (index) {
            0 -> -45f
            10 -> 45f
            20 -> 135f
            30 -> 225f
            else -> 0f
        }
        Box(
            modifier = Modifier
                .offset(
                    x = (scaleX(start.x) - scaleX(designCornerSize) / 2).dp,
                    y = (scaleY(start.y) - scaleY(designCornerSize) / 2).dp
                )
                .size(scaleX(designCornerSize).dp, scaleY(designCornerSize).dp)
                .border(1.dp, Color.White.copy(alpha = 0.3f))
                .background(Color.Red.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = field.name,
                modifier = Modifier.rotate(textRotation),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 6.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(hyphens = Hyphens.Auto),
                overflow = TextOverflow.Clip
            )
        }
    } else {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val innerDist = dist - designCornerSize
        val fieldStep = innerDist / 9f
        val dirX = dx / dist
        val dirY = dy / dist

        val centerX = start.x + dirX * (designCornerSize / 2f + posInSide * fieldStep - fieldStep / 2f)
        val centerY = start.y + dirY * (designCornerSize / 2f + posInSide * fieldStep - fieldStep / 2f)

        val isHorizontal = side == 0 || side == 2
        val dw = if (isHorizontal) fieldStep else 180f
        val dh = if (isHorizontal) 180f else fieldStep

        val boxW = scaleX(dw)
        val boxH = scaleY(dh)
        val textRotation = side * 90f

        val textWidth = if (isHorizontal) boxW else boxH
        val textHeight = if (isHorizontal) boxH else boxW

        Box(
            modifier = Modifier
                .offset(x = (scaleX(centerX) - boxW / 2).dp, y = (scaleY(centerY) - boxH / 2).dp)
                .size(boxW.dp, boxH.dp)
                .border(0.5.dp, Color.White.copy(alpha = 0.2f))
                .background(Color.Black.copy(alpha = 0.1f))
        ) {
            if (field is PropertyField) {
                val barSize = 35f
                val barColor = field.color.toComposeColor()
                val barMod = when (side) {
                    0 -> Modifier.fillMaxWidth().height(scaleY(barSize).dp).align(Alignment.BottomCenter)
                    1 -> Modifier.fillMaxHeight().width(scaleX(barSize).dp).align(Alignment.CenterStart)
                    2 -> Modifier.fillMaxWidth().height(scaleY(barSize).dp).align(Alignment.TopCenter)
                    3 -> Modifier.fillMaxHeight().width(scaleX(barSize).dp).align(Alignment.CenterEnd)
                    else -> Modifier
                }
                Box(modifier = barMod.background(barColor))
            }

            Text(
                text = field.name,
                modifier = Modifier
                    .align(Alignment.Center)
                    .requiredSize(width = textWidth.dp, height = textHeight.dp)
                    .rotate(textRotation),
                color = Color.White,
                fontSize = 4.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(hyphens = Hyphens.Auto),
                overflow = TextOverflow.Clip
            )
        }
    }
}

fun PropertyColor.toComposeColor(): Color = when (this) {
    PropertyColor.BROWN -> Color(0xFF955436)
    PropertyColor.LIGHT_BLUE -> Color(0xFFAAE0FA)
    PropertyColor.PINK -> Color(0xFFD93A96)
    PropertyColor.ORANGE -> Color(0xFFF7941D)
    PropertyColor.RED -> Color(0xFFED1B24)
    PropertyColor.YELLOW -> Color(0xFFFEF200)
    PropertyColor.GREEN -> Color(0xFF1FB25A)
    PropertyColor.DARK_BLUE -> Color(0xFF0072BB)
}
