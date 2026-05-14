package at.aau.monopoly.klagenfurt.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import at.aau.monopoly.klagenfurt.ServiceLocator
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.sensors.ShakeDetector
import at.aau.monopoly.klagenfurt.ui.board.FieldItem
import at.aau.monopoly.klagenfurt.ui.board.MovementAnimationState
import at.aau.monopoly.klagenfurt.ui.chat.ChatOverlay
import at.aau.monopoly.klagenfurt.ui.zoom.ZoomableWrapper
import com.example.myapplication.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import android.view.KeyEvent
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import androidx.compose.runtime.derivedStateOf
import at.aau.monopoly.klagenfurt.model.field.ChanceField
import at.aau.monopoly.klagenfurt.model.field.CommunityChestField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.UtilityField

class GameboardUI : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels {
        GameViewModel.Factory(ServiceLocator.provideGameService())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gameId = intent.getStringExtra("GAME_ID")
        Log.d("DiceDebug", "GameboardUI received GAME_ID=$gameId")
        if (!gameId.isNullOrBlank()) {
            viewModel.setGameId(gameId)
        }
        setContent {
            GameboardScreen(viewModel = viewModel)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // Cheat im ViewModel aktivieren
            viewModel.activateCheatForNextRoll()
            Log.d("DiceDebug", "Cheat activated via Volume Up!")
            // WICHTIG: true zurückgeben, damit sich die Lautstärke nicht ändert
            return true
        }
        return super.onKeyDown(keyCode, event)
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
fun GameboardScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel,
    shakeEventsOverride: Flow<Unit>? = null
) {
    LaunchedEffect(Unit) {
        viewModel.syncGameboardEntryState()
    }

    val fields by viewModel.fields.collectAsState(initial = emptyList())
    val gameState by viewModel.gameState.collectAsState()
    val players = gameState?.players ?: emptyList()
    val currentPlayerId = viewModel.currentPlayerId
    val currentTurnPlayer = gameState?.currentPlayer
    val currentField = currentTurnPlayer?.let { player ->
        fields.getOrNull(player.position)
    }
    val isBuyableField = currentField is PropertyField ||
            currentField is RailroadField ||
            currentField is UtilityField

    val isUnownedField = when (currentField) {
        is PropertyField -> currentField.ownerId == null
        is RailroadField -> currentField.ownerId == null
        is UtilityField -> currentField.ownerId == null
        else -> false
    }

    val isOnChanceField = currentField is ChanceField
    val isOnCommunityChestField = currentField is CommunityChestField
    val eventLog by viewModel.eventLog.collectAsState()

    val isRollingPhaseForCurrentPlayer by viewModel.isRollingPhaseForCurrentPlayer.collectAsState()
    val isBuyingPhaseForCurrentPlayer by viewModel.isBuyingPhaseForCurrentPlayer.collectAsState()
    val lastDiceRoll by viewModel.lastDiceRoll.collectAsState()
    val isGameStarted by viewModel.isGameStarted.collectAsState()
    val isHost by viewModel.isHost.collectAsState()
    val cardDrawnThisTurn by viewModel.cardDrawnThisTurn.collectAsState()

    val canBuyCurrentField =
        isBuyingPhaseForCurrentPlayer &&
                isBuyableField &&
                isUnownedField &&
                (currentTurnPlayer!!.id == currentPlayerId)

    // Action Card states
    val currentActionCard by viewModel.currentActionCard.collectAsState()
    val isExecutingAction by viewModel.isExecutingAction.collectAsState()
    val showActionCardOverlay by viewModel.showActionCardOverlay.collectAsState()

    val context = LocalContext.current

    var showOverlay by remember { mutableStateOf(false) }

    // Filter DICE_ROLLED entries from the log while the overlay is visible,
    // so the dice result appears in chat only after the animation finishes.
    val bufferedEventLog by remember {
        derivedStateOf {
            if (showOverlay) eventLog.filter { it.eventType != "DICE_ROLLED" }
            else eventLog
        }
    }

    // ═══════════════════════════════════════════════
    // ShakeDetector lifecycle – only used when no override provided (production path)
    // ═══════════════════════════════════════════════
    val shakeDetector = remember(shakeEventsOverride) {
        if (shakeEventsOverride == null) ShakeDetector(context) else null
    }

    if (shakeDetector != null) {
        DisposableEffect(shakeDetector) {
            shakeDetector.startListening()
            onDispose { shakeDetector.stopListening() }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, shakeDetector) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> shakeDetector.stopListening()
                    Lifecycle.Event.ON_RESUME -> shakeDetector.startListening()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                shakeDetector.stopListening()
            }
        }
    }

    val shakeFlow: Flow<Unit> = shakeEventsOverride ?: shakeDetector!!.shakeEvents

    // Tracks whether the user has shaken to trigger the actual roll.
    // Drives the dice animation in the overlay (animation starts on shake, not on overlay open).
    var hasShaken by remember { mutableStateOf(false) }

    // Reset on overlay open and on phase changes so each turn starts fresh.
    LaunchedEffect(showOverlay) {
        if (showOverlay) hasShaken = false
    }
    LaunchedEffect(isRollingPhaseForCurrentPlayer) {
        if (isRollingPhaseForCurrentPlayer) hasShaken = false
    }

    // Only consume shakes while the overlay is open AND it is the current player's rolling phase.
    // Guard against double-rolls via hasShaken.
    LaunchedEffect(shakeFlow, viewModel) {
        shakeFlow
            .filter { showOverlay && isRollingPhaseForCurrentPlayer && !hasShaken }
            .collect {
                hasShaken = true
                viewModel.rollDice()
            }
    }

    val selectedPlayer by viewModel.selectedPlayerForOverlay.collectAsState()
    val movementState by viewModel.movementAnimation.collectAsState()

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    Box(modifier = modifier.fillMaxSize()) {
        GameboardContent(
            fields = fields,
            players = players,
            currentPlayerId = currentPlayerId,
            currentTurnPlayer = currentTurnPlayer,
            onPlayerCardClick = { player -> viewModel.showPlayerOverlay(player) },
            selectedPlayerForOverlay = selectedPlayer,
            onDismissOverlay = { viewModel.hidePlayerOverlay() },
            movementAnimationState = movementState,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (isHost && !isGameStarted) {
                Button(
                    onClick = { viewModel.startGame() },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("Start Game")
                }
            }

            if (isRollingPhaseForCurrentPlayer) {
                Button(
                    onClick = {
                        // Only open the overlay – the actual dice roll is triggered by a shake.
                        showOverlay = true
                    },
                    modifier = Modifier.testTag("roll_dice_button")
                ) {
                    Text("🎲 Roll Dice")
                }
            }

            if (isBuyingPhaseForCurrentPlayer) {
                Button(
                    onClick = { viewModel.endTurn() },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag("end_turn_button")
                ) {
                    Text("End Turn")
                }
            }

            if (canBuyCurrentField) {
                Button(
                    onClick = {
                        viewModel.buyProperty(currentTurnPlayer!!.position)
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag("buy_property_button")
                ) {
                    Text("Buy Property")
                }
            }

            if (isOnChanceField) {
                Button(
                    onClick = { viewModel.drawCard("CHANCE") },
                    enabled = !showActionCardOverlay && !cardDrawnThisTurn,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(if (cardDrawnThisTurn) "✓ Card Drawn" else "🎰 Draw Chance")
                }
            }

            if (isOnCommunityChestField) {
                Button(
                    onClick = { viewModel.drawCard("COMMUNITY_CHEST") },
                    enabled = !showActionCardOverlay && !cardDrawnThisTurn,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(if (cardDrawnThisTurn) "✓ Card Drawn" else "⭐ Draw Community")
                }
            }

        }

        DiceRollOverlay(
            isVisible = showOverlay,
            diceResult = lastDiceRoll?.let { Pair(it.die1, it.die2) },
            isRolling = isRollingPhaseForCurrentPlayer,
            hasShaken = hasShaken,
            onClose = { showOverlay = false }
        )

        ActionCardOverlay(
            isVisible = showActionCardOverlay,
            card = currentActionCard,
            isExecuting = isExecutingAction,
            onExecuteAction = { viewModel.executeAction() }
        )

        GameboardOverlayLayer(eventLog = bufferedEventLog)


    }
}

@Composable
fun BoxScope.GameboardOverlayLayer(eventLog: List<GameViewModel.LogEntry>) {
    ChatOverlay(
        entries = eventLog,
        modifier = Modifier.align(Alignment.TopCenter)
    )
}

@Composable
fun GameboardContent(
    fields: List<Field>,
    players: List<Player> = emptyList(),
    currentPlayerId: String = "",
    currentTurnPlayer: Player? = null,
    onPlayerCardClick: (Player) -> Unit = {},
    selectedPlayerForOverlay: Player? = null,
    onDismissOverlay: () -> Unit = {},
    movementAnimationState: MovementAnimationState? = null,
    modifier: Modifier = Modifier
) {
    val myPlayer = players.find { it.id == currentPlayerId }
    val otherPlayers = players.filter { it.id != currentPlayerId }

    val currentField = currentTurnPlayer?.let { p ->
        fields.getOrNull(p.position)
    }

    val playersByField: Map<Int, List<Player>> = remember(players) {
        players.groupBy { it.position }
    }

    val panelWidth = 280.dp

    Box(modifier = modifier.fillMaxSize()) {
        // Board layer (zoomable)
        ZoomableWrapper(modifier = Modifier.fillMaxSize()) {
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
                    // Semi-transparent warm overlay to match field backgrounds
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFF3E0).copy(alpha = 0.40f))
                    )

                    // Stagger field rendering
                    var visibleFieldCount by remember { mutableIntStateOf(0) }
                    LaunchedEffect(fields.size) {
                        if (fields.isNotEmpty() && visibleFieldCount < fields.size) {
                            val batchSize = 5
                            var count = 0
                            while (count < fields.size) {
                                count = (count + batchSize).coerceAtMost(fields.size)
                                visibleFieldCount = count
                                delay(32)
                            }
                        }
                    }

                    fields.take(visibleFieldCount).forEachIndexed { index, field ->
                        key(field.id) {
                            FieldItem(
                                index = index,
                                field = field,
                                sw = sw,
                                sh = sh,
                                playersOnField = playersByField[field.id] ?: emptyList(),
                                animatingPlayerId = movementAnimationState?.playerId,
                                animatingStep = movementAnimationState?.let {
                                    if (it.currentStepIndex in it.path.indices) it.path[it.currentStepIndex] else null
                                },
                                animationComplete = movementAnimationState?.isComplete ?: true
                            )
                        }
                    }
                    // Old flat PlayerToken loop removed – tokens are now rendered inside FieldItem.
                }
            }
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
                        fields = fields,
                        cards = emptyList(),
                        isCurrentTurn = player.id == currentTurnPlayer?.id,
                        onCardClick = { onPlayerCardClick(player) }
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
                    fields = fields,
                    cards = emptyList(),
                    isCurrentTurn = myPlayer.id == currentTurnPlayer?.id,
                    isOwnPlayer = true,
                    onCardClick = { onPlayerCardClick(myPlayer) }
                )
            }
        }

        // Player Property Overlay
        selectedPlayerForOverlay?.let { player ->
            PlayerPropertyOverlay(
                player = player,
                allFields = fields,
                onDismiss = onDismissOverlay
            )
        }
    }
}
