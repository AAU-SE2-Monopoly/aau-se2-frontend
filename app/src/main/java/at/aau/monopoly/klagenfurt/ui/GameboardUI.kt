package at.aau.monopoly.klagenfurt.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import at.aau.monopoly.klagenfurt.DebugSettings
import at.aau.monopoly.klagenfurt.ServiceLocator
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.OwnableField
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
import androidx.compose.foundation.shape.RoundedCornerShape
import at.aau.monopoly.klagenfurt.model.field.ChanceField
import at.aau.monopoly.klagenfurt.model.field.CommunityChestField
import kotlin.math.hypot
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import com.example.myapplication.BuildConfig



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
    val isBuyableField = currentField is OwnableField

    val isUnownedField = (currentField as? OwnableField)?.ownerId == null

    val isOnChanceField = currentField is ChanceField
    val isOnCommunityChestField = currentField is CommunityChestField
    val eventLog by viewModel.eventLog.collectAsState()

    val isRollingPhaseForCurrentPlayer by viewModel.isRollingPhaseForCurrentPlayer.collectAsState()
    val isBuyingPhaseForCurrentPlayer by viewModel.isBuyingPhaseForCurrentPlayer.collectAsState()
    val lastDiceRoll by viewModel.lastDiceRoll.collectAsState()
    val canStartGame by viewModel.canStartGame.collectAsState()
    val canEndTurnForCurrentPlayer by viewModel.canEndTurnForCurrentPlayer.collectAsState()
    val canBuyCurrentField =
        isBuyingPhaseForCurrentPlayer &&
                isBuyableField &&
                isUnownedField

    val gameStarted = gameState?.phase != null &&
            gameState!!.phase != GamePhase.WAITING &&
            gameState!!.phase != GamePhase.FINISHED

    val isPayingRent = gameState?.phase == GamePhase.PAYING_RENT

    val myPlayer = gameState?.players?.find { it.id == currentPlayerId }
    val myPlayerIsActive = myPlayer != null && !myPlayer.eliminated && gameStarted


    // Action Card states
    val currentActionCard by viewModel.currentActionCard.collectAsState()
    val isExecutingAction by viewModel.isExecutingAction.collectAsState()
    val showActionCardOverlay by viewModel.showActionCardOverlay.collectAsState()

    // Payment overlay states
    val showPayRentOverlay by viewModel.showPayRentOverlay.collectAsState()
    val showMortgageOverlay by viewModel.showMortgageOverlay.collectAsState()
    val showBankruptcyOverlay by viewModel.showBankruptcyOverlay.collectAsState()
    val showBankruptcyConfirmation by viewModel.showBankruptcyConfirmation.collectAsState()
    val currentRentAmount by viewModel.currentRentAmount.collectAsState()
    val currentRentOwnerId by viewModel.currentRentOwnerId.collectAsState()
    val currentRentFieldId by viewModel.currentRentFieldId.collectAsState()
    val manageableProperties by viewModel.manageableProperties.collectAsState()
    val canPayRent by viewModel.canPayRent.collectAsState()
    val canRaiseFunds by viewModel.canRaiseFunds.collectAsState()
    val paymentActionInFlight by viewModel.paymentActionInFlight.collectAsState()
    val propertyActionInFlight by viewModel.propertyActionInFlight.collectAsState()
    val bankruptcyPlayerId by viewModel.bankruptcyPlayerId.collectAsState()
    val bankruptcyPlayerName by viewModel.bankruptcyPlayerName.collectAsState()
    val bankruptcyTotalAssets by viewModel.bankruptcyTotalAssets.collectAsState()
    val bankruptcyTotalDebt by viewModel.bankruptcyTotalDebt.collectAsState()
    val bankruptcyPropertiesOwned by viewModel.bankruptcyPropertiesOwned.collectAsState()
    val hasPendingPayment by viewModel.hasPendingPayment.collectAsState()

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
    // Circular reveal animation for game content
    // ═══════════════════════════════════════════════
    val revealProgress = remember { Animatable(0f) }

    // Start the reveal animation immediately on first composition
    LaunchedEffect(Unit) {
        // Small delay to ensure layout is ready
        delay(300)
        revealProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    // ═══════════════════════════════════════════════
    // Back button slide-in from top
    // ═══════════════════════════════════════════════
    val backButtonOffsetY = remember { Animatable(-200f) }
    LaunchedEffect(Unit) {
        backButtonOffsetY.animateTo(0f, animationSpec = tween(durationMillis = 400))
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
    var hasShaken by remember { mutableStateOf(false) }

    // Detect emulator to auto-trigger shake (emulators lack accelerometer)
    val isEmulator = remember {
        android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("emulator")
    }

    // Reset on overlay open and on phase changes so each turn starts fresh.
    LaunchedEffect(showOverlay) {
        if (showOverlay) hasShaken = false
    }

    // Auto-click shake on emulator when overlay is visible and it's rolling phase
    LaunchedEffect(showOverlay, isRollingPhaseForCurrentPlayer, hasShaken) {
        if (isEmulator && showOverlay && isRollingPhaseForCurrentPlayer && !hasShaken) {
            hasShaken = true
            viewModel.rollDice()
        }
    }
    LaunchedEffect(isRollingPhaseForCurrentPlayer) {
        if (isRollingPhaseForCurrentPlayer) hasShaken = false
    }

    // Auto-close dice overlay when player ends turn or buys property (phase leaves BUYING)
    LaunchedEffect(isBuyingPhaseForCurrentPlayer, canEndTurnForCurrentPlayer) {
        if (!isBuyingPhaseForCurrentPlayer && !isRollingPhaseForCurrentPlayer && showOverlay) {
            showOverlay = false
        }
    }

    // Auto-end turn after dice overlay closes when a double was rolled
    val pendingDoubleAutoEnd by viewModel.pendingDoubleAutoEnd.collectAsState()
    LaunchedEffect(showOverlay, pendingDoubleAutoEnd) {
        if (!showOverlay && pendingDoubleAutoEnd) {
            viewModel.consumeDoubleAutoEnd()
        }
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
        // Background is always visible
        FullscreenImage(R.drawable.background, "Klagenfurt-Map Background")

        // Game content with circular reveal
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = revealProgress.value
                    clip = p < 1f
                    if (p < 1f) {
                        shape = CircularRevealShape(p)
                    }
                }
        ) {
            GameboardContent(
                fields = fields,
                players = players,
                currentPlayerId = currentPlayerId,
                currentTurnPlayer = currentTurnPlayer,
                onPlayerCardClick = { player ->
                    if (player.id == currentPlayerId && myPlayerIsActive) {
                        viewModel.showMortgageManagementOverlay()
                    } else {
                        viewModel.showPlayerOverlay(player)
                    }
                },
                selectedPlayerForOverlay = selectedPlayer,
                onDismissOverlay = { viewModel.hidePlayerOverlay() },
                movementAnimationState = movementState,
                modifier = Modifier.fillMaxSize()
            )

            val buttonWidth = 180.dp

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canStartGame) {
                    GlassButton(
                        onClick = { viewModel.startGame() },
                        modifier = Modifier.width(buttonWidth)
                    ) {
                        Text("▶️ Start Game")
                    }
                }

                if (isRollingPhaseForCurrentPlayer && currentTurnPlayer != null) {
                    // Jail Logic
                    if (currentTurnPlayer.inJail) {

                        Text(
                            text = "🔒 Im Gefängnis (Versuch ${currentTurnPlayer.jailTurns + 1}/3)",
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White
                        )

                        GlassButton(
                            onClick = { viewModel.payJailFine() },
                            enabled = currentTurnPlayer.money >= 50,
                            modifier = Modifier.width(buttonWidth).testTag("pay_jail_fine_button")
                        ) {
                            Text("💰 50 M zahlen")
                        }


                        if (currentTurnPlayer.getOutOfJailCards > 0) {
                            GlassButton(
                                onClick = { viewModel.useJailCard() },
                                modifier = Modifier.width(buttonWidth).testTag("use_jail_card_button")
                            ) {
                                Text("🃏 Karte nutzen (${currentTurnPlayer.getOutOfJailCards})")
                            }
                        }


                        GlassButton(
                            onClick = { showOverlay = true },
                            modifier = Modifier.width(buttonWidth).testTag("roll_dice_button")
                        ) {
                            Text("🎲 Pasch versuchen")
                        }
                    } else {
                        GlassButton(
                            onClick = { showOverlay = true },
                            modifier = Modifier.width(buttonWidth).testTag("roll_dice_button")
                        ) {
                            Text("🎲 Roll Dice")
                        }
                    }
                }

                if (canEndTurnForCurrentPlayer && !showActionCardOverlay) {
                    GlassButton(
                        onClick = { viewModel.endTurn() },
                        modifier = Modifier.width(buttonWidth).testTag("end_turn_button")
                    ) {
                        Text("⏭️ End Turn")
                    }
                }

                if (hasPendingPayment && !showPayRentOverlay && currentTurnPlayer?.id == currentPlayerId) {
                    GlassButton(
                        onClick = { viewModel.showPayRentOverlay(currentRentAmount, currentRentOwnerId, currentRentFieldId) },
                        modifier = Modifier.width(buttonWidth).testTag("pay_rent_reopen_button")
                    ) {
                        Text("💸 Pay Rent Due")
                    }
                }

                if (canBuyCurrentField) {
                    GlassButton(
                        onClick = {
                            viewModel.buyProperty(currentTurnPlayer.position)
                        },
                        modifier = Modifier.width(buttonWidth).testTag("buy_property_button")
                    ) {
                        Text("🏠 Buy Property")
                    }
                }

                if (isOnChanceField && isBuyingPhaseForCurrentPlayer) {
                    DrawCardButton(
                        cardType = "CHANCE",
                        alreadyDrawn = false,
                        enabled = !showActionCardOverlay,
                        label = "🎰 Draw Chance",
                        onDraw = { viewModel.drawCard("CHANCE") },
                        modifier = Modifier.width(buttonWidth)
                    )
                }

                if (isOnCommunityChestField && isBuyingPhaseForCurrentPlayer) {
                    DrawCardButton(
                        cardType = "COMMUNITY_CHEST",
                        alreadyDrawn = false,
                        enabled = !showActionCardOverlay,
                        label = "⭐ Draw Community",
                        onDraw = { viewModel.drawCard("COMMUNITY_CHEST") },
                        modifier = Modifier.width(buttonWidth)
                    )
                }

            }

            /** DEBUG remove this block of code to remove */
            if (BuildConfig.DEBUG && DebugSettings.isEnabled && currentTurnPlayer?.id == currentPlayerId) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassButton(
                        onClick = { viewModel.debugForwardGame() },
                        modifier = Modifier.width(buttonWidth).testTag("debug_forward_game_button")
                    ) {
                        Text("⚡ DEBUG: Forward")
                    }

                    GlassButton(
                        onClick = { viewModel.debugSetupBankruptcy() },
                        modifier = Modifier.width(buttonWidth).testTag("debug_bankruptcy_setup_button")
                    ) {
                        Text("💀 DEBUG: Bankrupt")
                    }
                }
            }

            GameboardOverlayLayer(eventLog = bufferedEventLog)


            ActionCardOverlay(
                isVisible = showActionCardOverlay,
                card = currentActionCard,
                isExecuting = isExecutingAction,
                onExecuteAction = { viewModel.executeAction() }
            )

            DiceRollOverlay(
                isVisible = showOverlay,
                diceResult = lastDiceRoll?.let { Pair(it.die1, it.die2) },
                isRolling = isRollingPhaseForCurrentPlayer,
                hasShaken = hasShaken,
                onShakeButton = {
                    if (!hasShaken && isRollingPhaseForCurrentPlayer) {
                        hasShaken = true
                        viewModel.rollDice()
                    }
                },
                onClose = { showOverlay = false }
            )

            // Payment overlays
            PayRentOverlay(
                isVisible = showPayRentOverlay && currentTurnPlayer?.id == currentPlayerId,
                rentAmount = currentRentAmount,
                ownerName = currentRentOwnerId?.let { ownerId ->
                    players.find { it.id == ownerId }?.name
                },
                fieldName = currentRentFieldId?.let { id ->
                    fields.find { it.id == id }?.name
                } ?: "",
                currentMoney = currentTurnPlayer?.money ?: 0,
                canPay = canPayRent,
                canRaiseFunds = canRaiseFunds,
                paymentInFlight = paymentActionInFlight,
                propertyInFlight = propertyActionInFlight,
                onPay = { viewModel.payRent() },
                onManageProperties = { viewModel.showMortgageManagementOverlay() },
                onDeclareBankruptcy = { viewModel.declareBankruptcy() },
                onDismiss = { viewModel.dismissPayRentOverlay() }
            )

            MortgageManagementOverlay(
                isVisible = showMortgageOverlay && myPlayerIsActive == true,
                properties = manageableProperties,
                currentMoney = myPlayer?.money ?: 0,
                actionInFlight = propertyActionInFlight,
                isPayingRent = isPayingRent == true,
                onBuyHouse = { fieldId -> viewModel.buyHouse(fieldId) },
                onBuyHotel = { fieldId -> viewModel.buyHotel(fieldId) },
                onMortgage = { fieldId -> viewModel.mortgageProperty(fieldId) },
                onUnmortgage = { fieldId -> viewModel.unmortgageProperty(fieldId) },
                onSellHouse = { fieldId -> viewModel.sellHouse(fieldId) },
                onSellHotel = { fieldId -> viewModel.sellHotel(fieldId) },
                onDismiss = { viewModel.dismissMortgageOverlay() }
            )

            // Bankruptcy confirmation (shown to current player only, before backend call)
            BankruptcyResolutionOverlay(
                isVisible = showBankruptcyConfirmation && currentTurnPlayer?.id == currentPlayerId,
                playerName = currentTurnPlayer?.name ?: "",
                isConfirmation = true,
                totalAssets = currentTurnPlayer?.money ?: 0,
                totalDebt = currentRentAmount,
                propertiesOwned = manageableProperties.size,
                onConfirm = { viewModel.confirmDeclareBankruptcy() },
                onDismiss = { viewModel.cancelDeclareBankruptcy() }
            )

            // Bankruptcy result (shown to all players after backend processes)
            BankruptcyResolutionOverlay(
                isVisible = showBankruptcyOverlay,
                playerName = bankruptcyPlayerName,
                isOwnBankruptcy = bankruptcyPlayerId == currentPlayerId,
                totalAssets = bankruptcyTotalAssets,
                totalDebt = bankruptcyTotalDebt,
                propertiesOwned = bankruptcyPropertiesOwned.size,
                onConfirm = { viewModel.acceptBankruptcyResolution() },
                onDismiss = { viewModel.dismissBankruptcyOverlay() }
            )
        }

            // Back button animated from top
            val activity = context as? Activity
            val backOffsetYDp = backButtonOffsetY.value.dp
            GlassButton(
                onClick = { activity?.finish() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .offset(y = backOffsetYDp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Back",
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    /**
     * Shape that clips to a circle expanding from center based on [progress] (0..1).
     */
    class CircularRevealShape(private val progress: Float) : androidx.compose.ui.graphics.Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
            density: androidx.compose.ui.unit.Density
        ): androidx.compose.ui.graphics.Outline {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = hypot(size.width, size.height) / 2f
            val radius = maxRadius * progress
            val path = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center = center,
                        radius = radius
                    )
                )
            }
            return androidx.compose.ui.graphics.Outline.Generic(path)
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

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val panelWidth = maxWidth * 0.32f
            val panelMargin = 8.dp
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

                        FullscreenImage(R.drawable.background, "Klagenfurt-Map")
                        // Semi-transparent warm overlay to match field backgrounds
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFFF3E0).copy(alpha = 0.40f))
                        )

                        fields.forEachIndexed { index, field ->
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
                    }


                    // Field card centered on the board
                    if (currentField != null) {
                        BoxWithConstraints {
                            val cw = (maxWidth * 0.12f).coerceAtMost(140.dp)
                            val ch = cw * (224f / 140f)
                            FieldCardUI(
                                field = currentField,
                                cardWidth = cw,
                                cardHeight = ch,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            // Overlay: Left panel – other players
            if (otherPlayers.isNotEmpty()) {
                PlayerPanel(
                    alignment = Alignment.CenterStart,
                    panelWidth = panelWidth,
                    panelMargin = panelMargin,
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


            // Overlay: Right panel – own player
            if (myPlayer != null) {
                PlayerPanel(
                    alignment = Alignment.CenterEnd,
                    panelWidth = panelWidth,
                    panelMargin = panelMargin,
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

    /**
     * Semi-transparent rounded button used throughout the gameboard UI.
     */
    @Composable
    private fun GlassButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable RowScope.() -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.35f),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            content = content
        )
    }

    /**
     * Draw-card button used for Chance and Community Chest fields.
     */
    @Composable
    private fun DrawCardButton(
        cardType: String,
        alreadyDrawn: Boolean,
        enabled: Boolean,
        label: String,
        onDraw: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        GlassButton(
            onClick = onDraw,
            enabled = enabled && !alreadyDrawn,
            modifier = modifier
        ) {
            Text(if (alreadyDrawn) "✓ Card Drawn" else label)
        }
    }

    /**
     * Scrollable side panel used for player info on left/right edges of the gameboard.
     */
    @Composable
    private fun BoxWithConstraintsScope.PlayerPanel(
        alignment: Alignment,
        panelWidth: androidx.compose.ui.unit.Dp,
        panelMargin: androidx.compose.ui.unit.Dp,
        verticalArrangement: Arrangement.Vertical,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Column(
            modifier = Modifier
                .align(alignment)
                .width(panelWidth)
                .padding(panelMargin)
                .wrapContentHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = verticalArrangement,
            content = content
        )
    }

    /**
     * Full-size image layer used for board background layers.
     */
    @Composable
    private fun FullscreenImage(@androidx.annotation.DrawableRes resId: Int, description: String) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = description,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }

