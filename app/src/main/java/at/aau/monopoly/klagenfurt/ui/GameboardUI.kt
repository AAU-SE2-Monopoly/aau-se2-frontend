package at.aau.monopoly.klagenfurt.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.ServiceLocator
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import com.example.myapplication.R
import kotlin.collections.emptyList
import kotlin.collections.listOf
import kotlin.math.sqrt

class GameboardUI : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels {
        GameViewModel.Factory(ServiceLocator.provideGameService())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameboardScreen(viewModel=viewModel)
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
    fun GameboardScreen(modifier: Modifier = Modifier, viewModel: GameViewModel) {
        val fields by viewModel.fields.collectAsState(initial = emptyList())
        val gameState by viewModel.gameState.collectAsState()
        val players = gameState?.players ?: emptyList()

        LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

        GameboardContent(fields ?: emptyList(), players, modifier)
    }

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

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomState.updateTransformation(pan, zoom, Size(size.width.toFloat(), size.height.toFloat()))
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoomState.scale,
                    scaleY = zoomState.scale,
                    translationX = zoomState.offset.x,
                    translationY = zoomState.offset.y
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
    players: List<Player> = emptyList(),
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


                players.forEachIndexed { index, player ->
                    val bounds = calculateFieldBounds(player.position, sw, sh)
                    
                    val offsetX = (index % 2) * 12f
                    val offsetY = (index / 2) * 12f

                    Image(
                        painter = painterResource(id = getPlayerTokenResource(player.iconId)),
                        contentDescription = player.name,
                        modifier = Modifier
                            .offset(
                                x = (bounds.x + offsetX + 5f).dp, 
                                y = (bounds.y + offsetY + 5f).dp
                            )
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

fun getPlayerTokenResource(iconId: String): Int {
    return when (iconId.lowercase()) {
        "lindwurm" -> R.drawable.lindwurm
        "woerthersee" -> R.drawable.woertherseemandl
        "gti" -> R.drawable.gti
        "ironman" -> R.drawable.ironman
        "josef" -> R.drawable.josef
        else -> R.drawable.lindwurm
    }
}

data class FieldBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val isCorner: Boolean,
    val textWidth: Float,
    val textHeight: Float
)

fun calculateFieldBounds(index: Int, sw: Float, sh: Float): FieldBounds {
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

        val innerScale = 0.7f
        val tW = scaleX(designCornerSize) * innerScale
        val tH = scaleY(designCornerSize) * innerScale
        
        return FieldBounds(
            x = scaleX(start.x) - scaleX(designCornerSize) / 2,
            y = scaleY(start.y) - scaleY(designCornerSize) / 2,
            width = scaleX(designCornerSize),
            height = scaleY(designCornerSize),
            rotation = textRotation,
            isCorner = true,
            textWidth = tW,
            textHeight = tH
        )
    } else {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
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

        return FieldBounds(
            x = scaleX(centerX) - boxW / 2,
            y = scaleY(centerY) - boxH / 2,
            width = boxW,
            height = boxH,
            rotation = textRotation,
            isCorner = false,
            textWidth = if (isHorizontal) boxW else boxH,
            textHeight = if (isHorizontal) boxH else boxW
        )
    }
}

@Composable
fun FieldItem(index: Int, field: Field, sw: Float, sh: Float) {
    val bounds = remember(index, sw, sh) { calculateFieldBounds(index, sw, sh) }
    val side = (index / 10) % 4
    val imageMap = getFieldImageMapping(field.name)

    Box(
        modifier = fieldItemContainerMod(bounds),
        contentAlignment = Alignment.Center
    ) {
        FieldImage(
            imageRes = imageMap,
            fieldName = field.name,
            bounds = bounds
        )
        if (shouldShowPropertyBar(bounds, field) && field is PropertyField) {
            PropertyColorBar(
                side = side,
                sw = sw,
                sh = sh,
                color = field.color.toComposeColor()
            )
        }

        FieldTitle(
            text = field.name,
            bounds = bounds
        )
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

private val fieldImageMappings = mapOf(
    "Go" to R.drawable.mono_go,
    "Herrengasse" to R.drawable.herrengasse,
    "Community Chest" to R.drawable.community_chest,
    "Reichensteuer" to R.drawable.tax,
    "Hauptbahnhof" to R.drawable.hauptbahnhof,
    "Neuer Platz" to R.drawable.neuer_platz,
    "Chance" to R.drawable.chance,
    "Alter Platz" to R.drawable.alter_platz,
    "Benediktiner Platz" to R.drawable.bene_platz,
    "Jail / Just Visiting" to R.drawable.mono_jail,
    "Cine City" to R.drawable.cine_city,
    "Kelag Klagenfurt" to R.drawable.kelag,
    "McDonalds" to R.drawable.mcdonalds,
    "Ruthar" to R.drawable.ruthar,
    "Ostbahnhof" to R.drawable.ostbahnhof,
    "Wohnzimmer" to R.drawable.wohnzimmer,
    "Hafenstadt" to R.drawable.hafenstadt,
    "Lendcafe" to R.drawable.lendcafe,
    "Free Parking" to R.drawable.mono_free_parking,
    "City Arkaden" to R.drawable.city_arkaden,
    "Le Burger" to R.drawable.leburger_v2,
    "McMullens" to R.drawable.mcmullens,
    "Westbahnhof" to R.drawable.westbahnhof,
    "Mensa" to R.drawable.mensa,
    "Universität Klagenfurt" to R.drawable.universitaet,
    "Stadtwerke Klagenfurt" to R.drawable.stadtwerke,
    "Lakeside" to R.drawable.lakeside,
    "Go To Jail" to R.drawable.mono_go_to_jail,
    "Strandbad" to R.drawable.strandbad,
    "Loretto" to R.drawable.loretto,
    "Villa Lido" to R.drawable.villa_lido,
    "Lendbahnhof" to R.drawable.lendbahnhof,
    "Botanischer Garten" to R.drawable.botanischer_garten,
    "Kreuzbergl" to R.drawable.kreuzbergl,
    "Heiligengeistplatz" to R.drawable.heiligengeistplatz
)

fun getFieldImageMapping(fieldName: String): Int? {
    return fieldImageMappings[fieldName.trim()]
}

private fun fieldItemContainerMod(bounds: FieldBounds): Modifier {
    val borderWidth = if (bounds.isCorner) 1.dp else 0.5.dp
    val borderAlpha = if (bounds.isCorner) 0.3f else 0.2f
    val backgroundColor = if (bounds.isCorner) {
        Color.Red.copy(alpha = 0.1f)
    } else {
        Color.Black.copy(alpha = 0.1f)
    }

    return Modifier
        .offset(x = bounds.x.dp, y = bounds.y.dp)
        .size(bounds.width.dp, bounds.height.dp)
        .clip(RectangleShape)
        .border(borderWidth, Color.White.copy(alpha = borderAlpha))
        .background(backgroundColor)
}

@Composable
private fun FieldImage(
    imageRes: Int?,
    fieldName: String,
    bounds: FieldBounds
) {
    if (imageRes == null) return

    val imagePadding = if (bounds.isCorner) 2.dp else 1.dp
    val imageShape = RoundedCornerShape(2.dp)
    val borderWidth = if (bounds.isCorner) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .requiredSize(width = bounds.textWidth.dp, height = bounds.textHeight.dp)
            .padding(imagePadding)
            .rotate(bounds.rotation)
            .clip(imageShape)
            .border(borderWidth, Color.White, imageShape)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = fieldName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

private fun shouldShowPropertyBar(bounds: FieldBounds, field: Field): Boolean {
    return !bounds.isCorner && field is PropertyField
}


@Composable
private fun BoxScope.PropertyColorBar(
    side: Int,
    sw: Float,
    sh: Float,
    color: Color
) {
    val barSize = 35f
    val barModifier = when (side) {
        0 -> Modifier
            .fillMaxWidth()
            .height(((barSize / 2160f) * sh).dp)
            .align(Alignment.BottomCenter)
            .testTag("Bottom-Bar")

        1 -> Modifier
            .fillMaxHeight()
            .width(((barSize / 3840f) * sw).dp)
            .align(Alignment.CenterStart)
            .testTag("Left-Bar")

        2 -> Modifier
            .fillMaxWidth()
            .height(((barSize / 2160f) * sh).dp)
            .align(Alignment.TopCenter)
            .testTag("Top-Bar")

        3 -> Modifier
            .fillMaxHeight()
            .width(((barSize / 3840f) * sw).dp)
            .align(Alignment.CenterEnd)
            .testTag("Right-Bar")

        else -> Modifier
    }

    Box(
        modifier = barModifier.background(color)
    )
}

@Composable
private fun FieldTitle(
    text: String,
    bounds: FieldBounds
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .rotate(bounds.rotation)
            .padding(if (bounds.isCorner) 2.dp else 4.dp),
        contentAlignment = if (bounds.isCorner) Alignment.Center else Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                .padding(horizontal = 2.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = if (bounds.isCorner) 3.75.sp else 2.75.sp,
                lineHeight = if (bounds.isCorner) 4.75.sp else 3.75.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 3,
                style = TextStyle(hyphens = Hyphens.Auto)
            )
        }
    }
}