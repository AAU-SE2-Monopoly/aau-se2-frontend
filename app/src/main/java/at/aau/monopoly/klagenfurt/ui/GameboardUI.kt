package at.aau.monopoly.klagenfurt.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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

import at.aau.monopoly.klagenfurt.model.PaymentSource

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
    private var isVolumeUpPressed = false


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (!isVolumeUpPressed) {
                        isVolumeUpPressed = true
                        Log.d("DiceDebug", "Cheat enabled (dispatchKeyEvent).")
                        viewModel.activateCheatForNextRoll()
                    }
                }
                KeyEvent.ACTION_UP -> {
                    isVolumeUpPressed = false
                }
            }
            return true
        }


        return window.superDispatchKeyEvent(event)
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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.dramaEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

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

    val currentActionCard by viewModel.currentActionCard.collectAsState()
    val isExecutingAction by viewModel.isExecutingAction.collectAsState()
    val showActionCardOverlay by viewModel.showActionCardOverlay.collectAsState()

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

    var showOverlay by remember { mutableStateOf(false) }
    var showFreeParkingOverlay by remember { mutableStateOf(false) }

    val bufferedEventLog by remember {
        derivedStateOf {
            if (showOverlay) eventLog.filter { it.eventType != "DICE_ROLLED" }
            else eventLog
        }
    }

    val revealProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(300)
        revealProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val backButtonOffsetY = remember { Animatable(-200f) }
    LaunchedEffect(Unit) {
        backButtonOffsetY.animateTo(0f, animationSpec = tween(durationMillis = 400))
    }

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

    var hasShaken by remember { mutableStateOf(false) }
    var hasDrawnCard by remember { mutableStateOf(false) }
    var isDrawingCard by remember { mutableStateOf(false) }

    val isEmulator = remember {
        android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("emulator")
    }

    LaunchedEffect(showOverlay) {
        if (showOverlay) hasShaken = false
    }

    LaunchedEffect(showOverlay, isRollingPhaseForCurrentPlayer, hasShaken) {
        if (isEmulator && showOverlay && isRollingPhaseForCurrentPlayer && !hasShaken) {
            hasShaken = true
            viewModel.rollDice()
        }
    }

    LaunchedEffect(isRollingPhaseForCurrentPlayer) {
        if (isRollingPhaseForCurrentPlayer) {
            hasShaken = false
            hasDrawnCard = false
            isDrawingCard = false
        }
    }

    LaunchedEffect(isBuyingPhaseForCurrentPlayer, canEndTurnForCurrentPlayer) {
        if (!isBuyingPhaseForCurrentPlayer && !isRollingPhaseForCurrentPlayer && showOverlay) {
            showOverlay = false
        }
    }

    val pendingDoubleAutoEnd by viewModel.pendingDoubleAutoEnd.collectAsState()
    LaunchedEffect(showOverlay, pendingDoubleAutoEnd) {
        if (!showOverlay && pendingDoubleAutoEnd) {
            viewModel.consumeDoubleAutoEnd()
        }
    }

    LaunchedEffect(showActionCardOverlay) {
        if (showActionCardOverlay) {
            isDrawingCard = false
        }
    }

    LaunchedEffect(showOverlay, isRollingPhaseForCurrentPlayer, shakeFlow) {
        if (showOverlay && isRollingPhaseForCurrentPlayer) {
            var localHasShaken = false
            shakeFlow.collect {
                if (!localHasShaken && !hasShaken) {
                    localHasShaken = true
                    hasShaken = true
                    viewModel.rollDice()
                }
            }
        }
    }

    val selectedPlayer by viewModel.selectedPlayerForOverlay.collectAsState()
    val movementState by viewModel.movementAnimation.collectAsState()

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    Box(modifier = modifier.fillMaxSize()) {
        FullscreenImage(R.drawable.background, "Klagenfurt-Map Background")

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
                onReportCheater = { reportedPlayerId -> viewModel.reportCheater(reportedPlayerId) },
                gameState = gameState,
                onFreeParkingMoneyClick = { showFreeParkingOverlay = true },
                modifier = Modifier.fillMaxSize()
            )

            val buttonWidth = 180.dp
            val mustDrawCard = (isOnChanceField || isOnCommunityChestField) && !hasDrawnCard && isBuyingPhaseForCurrentPlayer
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

                if (isRollingPhaseForCurrentPlayer && currentTurnPlayer != null && !mustDrawCard && !isDrawingCard) {
                    if (currentTurnPlayer.inJail) {

                        Text(
                            text = "🔒 In Jail (Attempt ${currentTurnPlayer.jailTurns + 1}/3)",
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
                            Text("💰 Pay 50 M")
                        }

                        if (currentTurnPlayer.getOutOfJailCards > 0) {
                            GlassButton(
                                onClick = { viewModel.useJailCard() },
                                modifier = Modifier.width(buttonWidth).testTag("use_jail_card_button")
                            ) {
                                Text("🃏 Use Card (${currentTurnPlayer.getOutOfJailCards})")
                            }
                        }

                        GlassButton(
                            onClick = { showOverlay = true },
                            modifier = Modifier.width(buttonWidth).testTag("roll_dice_button")
                        ) {
                            Text("🎲 Attempt Double")
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

                if (canEndTurnForCurrentPlayer && !showActionCardOverlay && !mustDrawCard && !isDrawingCard) {
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
                        alreadyDrawn = hasDrawnCard,
                        enabled = !showActionCardOverlay && !hasDrawnCard,
                        label = "🎰 Draw Chance",
                        onDraw = { viewModel.drawCard("CHANCE")
                                    hasDrawnCard = true
                                    isDrawingCard = true
                                 },
                        modifier = Modifier.width(buttonWidth)
                    )
                }

                if (isOnCommunityChestField && isBuyingPhaseForCurrentPlayer) {
                    DrawCardButton(
                        cardType = "COMMUNITY_CHEST",
                        alreadyDrawn = hasDrawnCard,
                        enabled = !showActionCardOverlay && !hasDrawnCard,
                        label = "⭐ Draw Community",
                        onDraw = { viewModel.drawCard("COMMUNITY_CHEST")
                                    hasDrawnCard = true
                                    isDrawingCard = true
                                 },
                        modifier = Modifier.width(buttonWidth)
                    )
                }
            }

            if (DebugSettings.isEnabled && currentTurnPlayer?.id == currentPlayerId) {
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
            FreeParkingJackpotOverlay(
                isVisible = showFreeParkingOverlay,
                amount = gameState?.freeParkingMoney ?: 0,
                onClose = { showFreeParkingOverlay = false }
            )

            val isTaxPayment = gameState?.pendingPayment?.source == PaymentSource.TAX
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
                isTaxPayment = isTaxPayment,
                onPay = {
                    if (isTaxPayment) viewModel.payTax()
                    else viewModel.payRent()
                },
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

internal class CircularRevealShape(private val progress: Float) : androidx.compose.ui.graphics.Shape {
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
    onReportCheater: (String) -> Unit = {},
    gameState: at.aau.monopoly.klagenfurt.model.GameState? = null,
    onFreeParkingMoneyClick: () -> Unit = {},
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
                                animationComplete = movementAnimationState?.isComplete ?: true,
                                freeParkingMoney = gameState?.freeParkingMoney ?: 0,
                                onFreeParkingMoneyClick = onFreeParkingMoneyClick
                            )
                        }
                    }
                }

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

        if (otherPlayers.isNotEmpty()) {
            PlayerPanel(
                alignment = Alignment.CenterStart,
                panelWidth = panelWidth,
                panelMargin = panelMargin,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
            ) {
                otherPlayers.forEach { player ->
                    Column(horizontalAlignment = Alignment.End) {
                        PlayerInfoPanel(
                            player = player,
                            fields = fields,
                            cards = emptyList(),
                            isCurrentTurn = player.id == currentTurnPlayer?.id,
                            onCardClick = { onPlayerCardClick(player) }
                        )

                        Button(
                            onClick = { onReportCheater(player.id) },
                            modifier = Modifier
                                .padding(top = 4.dp, end = 4.dp)
                                .height(26.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAA0000)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("🚨 Report", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

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

        selectedPlayerForOverlay?.let { player ->
            PlayerPropertyOverlay(
                player = player,
                allFields = fields,
                onDismiss = onDismissOverlay
            )
        }
    }
}

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

@Composable
private fun FullscreenImage(@androidx.annotation.DrawableRes resId: Int, description: String) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = description,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
    )
}

@Composable
fun FreeParkingJackpotOverlay(
    isVisible: Boolean,
    amount: Int,
    onClose: () -> Unit
) {
    if (!isVisible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(24.dp)
                .width(280.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "💵 Free Parking Jackpot",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.Black
            )

            Text(
                text = "$$amount",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = Color(0xFF2E7D32)
            )

            Text(
                text = "This amount is collected by the next player who lands on Free Parking.",
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Button(onClick = onClose) {
                Text("Close")
            }
        }
    }
}