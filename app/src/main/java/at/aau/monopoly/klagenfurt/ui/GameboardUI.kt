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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
import at.aau.monopoly.klagenfurt.model.card.Card

import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.UtilityField
import at.aau.monopoly.klagenfurt.ui.chat.ChatOverlay
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
            GameboardScreen(viewModel = viewModel)
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
        // Request state sync when entering the screen to ensure fresh data
        LaunchedEffect(Unit) {
            viewModel.syncGameboardEntryState()
        }

        val fields by viewModel.fields.collectAsState(initial = emptyList())
        val gameState by viewModel.gameState.collectAsState()
        val players = gameState?.players ?: emptyList()
        val currentPlayerId = viewModel.currentPlayerId
        val currentTurnPlayer = gameState?.currentPlayer
        val eventLog by viewModel.eventLog.collectAsState()

        // 1. Beobachte den ausgewählten Spieler aus dem ViewModel
        val selectedPlayer by viewModel.selectedPlayerForOverlay.collectAsState()

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    Box(modifier = modifier.fillMaxSize()) {
        GameboardContent(
            fields = fields,
            players = players,
            currentPlayerId = currentPlayerId,
            currentTurnPlayer = currentTurnPlayer,
            modifier = Modifier.fillMaxSize()
        )
        GameboardOverlayLayer(eventLog = eventLog)

        // --- DAS OVERLAY ---
        // Wird nur gerendert, wenn selectedPlayer NICHT null ist
        selectedPlayer?.let { player ->
            PlayerPropertyOverlay(
                player = player,
                allFields = fields ?: emptyList(),
                onDismiss = { viewModel.hidePlayerOverlay() }
            )
        }
    }
}

/**
 * Creates the overlay layer for the gameboard.
 * Place components here that should remain fixed (not zoomable), such as UI controls or HUD.
 *
 * eventLog displays events like join, create game, dice rolled etc.
 * The Log ignores State Snapshots -> Technical Logs will be displayed on DoubleClick on ChatBar in an expanded window.
 */
@Composable
private fun BoxScope.GameboardOverlayLayer(eventLog: List<GameViewModel.LogEntry>) {
    ChatOverlay(
        entries = eventLog,
        modifier = Modifier
            .align(Alignment.TopCenter)
    )
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
    currentPlayerId: String = "",
    currentTurnPlayer: Player? = null,
    modifier: Modifier = Modifier
) {
    val myPlayer = players.find { it.id == currentPlayerId }
    val otherPlayers = players.filter { it.id != currentPlayerId }

    // Test data: assign random property ownership and cards to players for demo
    val testFields: List<Field> = remember(fields, players) {
        if (players.isEmpty() || fields.isEmpty()) fields
        else {
            // Create test property fields with owners assigned to players
            val testOwnedProps = listOf(
                PropertyField(1, "Herrengasse", FieldType.PROPERTY, PropertyColor.BROWN, 60, listOf(2, 10, 30, 90, 160, 250), 50, 50),
                PropertyField(3, "Alter Platz", FieldType.PROPERTY, PropertyColor.BROWN, 60, listOf(4, 20, 60, 180, 320, 450), 50, 50),
                PropertyField(6, "Neuer Platz", FieldType.PROPERTY, PropertyColor.LIGHT_BLUE, 100, listOf(6, 30, 90, 270, 400, 550), 50, 50),
                PropertyField(8, "Benediktiner Platz", FieldType.PROPERTY, PropertyColor.LIGHT_BLUE, 100, listOf(6, 30, 90, 270, 400, 550), 50, 50),
                PropertyField(9, "Cine City", FieldType.PROPERTY, PropertyColor.LIGHT_BLUE, 120, listOf(8, 40, 100, 300, 450, 600), 50, 50),
                RailroadField(5, "Hauptbahnhof", FieldType.RAILROAD, 200),
                RailroadField(15, "Ostbahnhof", FieldType.RAILROAD, 200),
                UtilityField(12, "Kelag Klagenfurt", FieldType.UTILITY, 150),
                PropertyField(11, "McDonalds", FieldType.PROPERTY, PropertyColor.PINK, 140, listOf(10, 50, 150, 450, 625, 750), 100, 100),
                PropertyField(13, "Ruthar", FieldType.PROPERTY, PropertyColor.PINK, 140, listOf(10, 50, 150, 450, 625, 750), 100, 100, houses = 2),
                PropertyField(14, "Wohnzimmer", FieldType.PROPERTY, PropertyColor.PINK, 160, listOf(12, 60, 180, 500, 700, 900), 100, 100, hasHotel = true),
            )
            // Distribute ownership among players round-robin
            val playerIds = players.map { it.id }
            testOwnedProps.forEachIndexed { i, f ->
                val ownerId = playerIds[i % playerIds.size]
                when (f) {
                    is PropertyField -> f.ownerId = ownerId
                    is RailroadField -> f.ownerId = ownerId
                    is UtilityField -> f.ownerId = ownerId
                    else -> {}
                }
            }
            // Merge: replace fields by id with the test-owned versions
            val ownedById = testOwnedProps.associateBy { it.id }
            fields.map { ownedById[it.id] ?: it }
        }
    }

    val testCardsPerPlayer: Map<String, List<Card>> = emptyMap()

    val currentField = currentTurnPlayer?.let { p ->
        testFields.getOrNull(p.position)
    }

    val panelWidth = 280.dp

    Box(modifier = modifier.fillMaxSize()) {
        // Board layer (zoomable) – fullscreen background
        ZoomableWrapper(
            modifier = Modifier.fillMaxSize()
        ) {
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
                        painter = painterResource(id = R.drawable.background),
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
                    // Stagger field rendering to avoid decoding all images on first frame
                    var visibleFieldCount by remember { mutableIntStateOf(0) }
                    LaunchedEffect(testFields.size) {
                        if (testFields.isNotEmpty() && visibleFieldCount < testFields.size) {
                            // Render in batches to spread image decoding across frames
                            val batchSize = 5
                            var count = 0
                            while (count < testFields.size) {
                                count = (count + batchSize).coerceAtMost(testFields.size)
                                visibleFieldCount = count
                                delay(32) // ~2 frames between batches
                            }
                        }
                    }

                    testFields.take(visibleFieldCount).forEachIndexed { index, field ->
                        key(field.id) {
                            FieldItem(index, field, sw, sh)
                        }
                    }

                    players.forEachIndexed { index, player ->
                        key(player.id) {
                        val bounds = remember(player.position, sw, sh) {
                            calculateFieldBounds(player.position, sw, sh)
                        }

                        val maxCols = 3
                        val maxRows = 2

                        val tokenSizeX = (bounds.width / maxCols) * 0.8f
                        val tokenSizeY = (bounds.height / maxRows) * 0.8f
                        val tokenSize = minOf(tokenSizeX, tokenSizeY).coerceAtMost(16f)

                        val column = index % maxCols
                        val row = index / maxCols

                        val gridWidth = maxCols * tokenSize
                        val gridHeight = maxRows * tokenSize
                        val startX = (bounds.width - gridWidth) / 2
                        val startY = (bounds.height - gridHeight) / 2

                        val offsetX = startX + (column * tokenSize)
                        val offsetY = startY + (row * tokenSize)

                        Image(
                            painter = painterResource(id = getPlayerTokenResource(player.iconId)),
                            contentDescription = player.name,
                            modifier = Modifier
                                .offset(
                                    x = (bounds.x + offsetX).dp,
                                    y = (bounds.y + offsetY).dp
                                )
                                .size(tokenSize.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                        }
                    }
                }            }
        }

        // Overlay: Left panel – other players
        if (otherPlayers.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(panelWidth)
                    .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
            ) {
                otherPlayers.forEach { player ->
                    PlayerInfoPanel(
                        player = player,
                        fields = testFields,
                        cards = testCardsPerPlayer[player.id] ?: emptyList(),
                        isCurrentTurn = player.id == currentTurnPlayer?.id
                    )
                }
            }
        }

        // Overlay: Center – current field card
        if (currentField != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp)
            ) {
                FieldCardUI(field = currentField)
            }
        }

        // Overlay: Right panel – own player
        if (myPlayer != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(panelWidth)
                    .padding(end = 4.dp, top = 4.dp, bottom = 4.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                PlayerInfoPanel(
                    player = myPlayer,
                    fields = testFields,
                    cards = testCardsPerPlayer[myPlayer.id] ?: emptyList(),
                    isCurrentTurn = myPlayer.id == currentTurnPlayer?.id,
                    isOwnPlayer = true
                )
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
        val textRotation = 0f

        val innerScale = 1.0f
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
    val imageMap = remember(field.name) { getFieldImageMapping(field.name) }

    val containerMod = remember(bounds) { fieldItemContainerMod(bounds) }

    Box(
        modifier = containerMod,
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
    val borderWidth = if (bounds.isCorner) 0.5.dp else 0.25.dp
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

    val imagePadding = 0.dp
    val imageShape = RoundedCornerShape(2.dp)
    val borderWidth = if (bounds.isCorner) 1.dp else 0.5.dp

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
            contentScale = ContentScale.FillBounds
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