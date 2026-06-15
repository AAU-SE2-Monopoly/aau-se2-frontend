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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import at.aau.monopoly.klagenfurt.DebugSettings
import at.aau.monopoly.klagenfurt.ServiceLocator
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.TradeOffer
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.OwnableField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.sensors.ShakeDetector
import at.aau.monopoly.klagenfurt.ui.board.FieldItem
import at.aau.monopoly.klagenfurt.ui.board.MovementAnimationState
import at.aau.monopoly.klagenfurt.ui.chat.ChatOverlay
import at.aau.monopoly.klagenfurt.ui.util.ownerIdFromField
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

private val GlassPanelColor = Color.Black.copy(alpha = 0.42f)
private val GlassBorderColor = Color.White.copy(alpha = 0.28f)
private val GlassDisabledColor = Color.Black.copy(alpha = 0.16f)

private fun List<Field>.fieldAtBoardPosition(position: Int): Field? =
    firstOrNull { it.id == position } ?: getOrNull(position)

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

    @SuppressLint("RestrictedApi")
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

        return super.dispatchKeyEvent(event)
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
    val rawPlayers = gameState?.players ?: emptyList()
    val presentedBoardPlayers by viewModel.presentedBoardPlayers.collectAsState()
    val boardPlayers = presentedBoardPlayers.takeIf { it.isNotEmpty() } ?: rawPlayers
    val currentPlayerId = viewModel.currentPlayerId
    val currentTurnPlayer = gameState?.currentPlayer
    val visibleCurrentField by viewModel.visibleCurrentField.collectAsState()
    val currentField = visibleCurrentField ?: currentTurnPlayer?.let { player ->
        fields.fieldAtBoardPosition(player.position)
    }
    val isOnChanceField = currentField is ChanceField
    val isOnCommunityChestField = currentField is CommunityChestField
    val eventLog by viewModel.presentedEventLog.collectAsState()
    val actionGates by viewModel.actionGates.collectAsState()
    val activeDicePresentation by viewModel.activeDicePresentation.collectAsState()
    val visiblePaymentState by viewModel.visiblePaymentState.collectAsState()

    val isRollingPhaseForCurrentPlayer by viewModel.isRollingPhaseForCurrentPlayer.collectAsState()
    val isBuyingPhaseForCurrentPlayer by viewModel.isBuyingPhaseForCurrentPlayer.collectAsState()
    val canStartGame by viewModel.canStartGame.collectAsState()
    val canEndTurnForCurrentPlayer by viewModel.canEndTurnForCurrentPlayer.collectAsState()
    val canBuyCurrentField = actionGates.canBuyProperty

    val gameStarted = gameState?.phase != null &&
            gameState!!.phase != GamePhase.WAITING &&
            gameState!!.phase != GamePhase.FINISHED

    val isPayingRent = gameState?.phase == GamePhase.PAYING_RENT

    val myPlayer = gameState?.players?.find { it.id == currentPlayerId }
    val myPlayerIsActive = myPlayer != null && !myPlayer.eliminated && gameStarted

    val currentActionCard by viewModel.visibleActionCard.collectAsState()
    val isExecutingAction by viewModel.isExecutingAction.collectAsState()
    val showActionCardOverlay by viewModel.showActionCardOverlay.collectAsState()
    var dismissedSpectatorActionCardId by remember { mutableStateOf<Int?>(null) }

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
    var dismissedDiceSequenceId by remember { mutableStateOf<Long?>(null) }
    var showFreeParkingOverlay by remember { mutableStateOf(false) }

    val showGameOverOverlay by viewModel.showGameOverOverlay.collectAsState()
    val hostEndedGame by viewModel.hostEndedGame.collectAsState()

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
    val hasDrawnCardThisTurn = gameState?.hasDrawnCardThisTurn == true
    var isDrawingCard by remember { mutableStateOf(false) }
    val diceOverlayVisible =
        showOverlay ||
                (activeDicePresentation != null &&
                        dismissedDiceSequenceId != activeDicePresentation?.sequenceId)
    val diceOverlayHasShaken = hasShaken || activeDicePresentation != null
    val diceOverlayIsRolling = activeDicePresentation?.isRolling ?: (showOverlay && hasShaken)
    val diceOverlayResult = activeDicePresentation?.diceRoll?.let { Pair(it.die1, it.die2) }

    val isEmulator = remember {
        android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("emulator")
    }

    LaunchedEffect(showOverlay, activeDicePresentation?.sequenceId) {
        if (showOverlay && activeDicePresentation == null) hasShaken = false
    }

    LaunchedEffect(activeDicePresentation?.sequenceId) {
        if (activeDicePresentation == null) {
            showOverlay = false
            hasShaken = false
        }
    }

    LaunchedEffect(showOverlay, actionGates.canRollDice, actionGates.canRollAgainAfterDouble, hasShaken) {
        if (isEmulator && showOverlay && (actionGates.canRollDice || actionGates.canRollAgainAfterDouble) &&
            !hasShaken) {
            hasShaken = true
            if (actionGates.canRollAgainAfterDouble) {
                viewModel.rollAgainAfterDouble()
            } else {
                viewModel.rollDice()
            }
        }
    }

    LaunchedEffect(actionGates.canRollDice) {
        if (actionGates.canRollDice) {
            hasShaken = false
            isDrawingCard = false
        }
    }

    LaunchedEffect(actionGates.canRollDice, activeDicePresentation?.sequenceId, canEndTurnForCurrentPlayer) {
        if (!actionGates.canRollDice && activeDicePresentation == null && !canEndTurnForCurrentPlayer && showOverlay) {
            showOverlay = false
        }
    }



    LaunchedEffect(showActionCardOverlay, actionGates.canDrawCard) {
        if (showActionCardOverlay || actionGates.canDrawCard) {
            isDrawingCard = false
        }
    }

    LaunchedEffect(currentActionCard?.id) {
        if (currentActionCard?.id != dismissedSpectatorActionCardId) {
            dismissedSpectatorActionCardId = null
        }
    }

    LaunchedEffect(showOverlay, actionGates.canRollDice, actionGates.canRollAgainAfterDouble, shakeFlow) {
        if (showOverlay && (actionGates.canRollDice || actionGates.canRollAgainAfterDouble)) {
            var localHasShaken = false
            shakeFlow.collect {
                if (!localHasShaken && !hasShaken) {
                    localHasShaken = true
                    hasShaken = true
                    viewModel.activateCheatForNextRoll()
                    if (actionGates.canRollAgainAfterDouble) {
                        viewModel.rollAgainAfterDouble()
                    } else {
                        viewModel.rollDice()
                    }
                }
            }
        }
    }

    val selectedPlayer by viewModel.selectedPlayerForOverlay.collectAsState()
    val selectedTradePlayer by viewModel.selectedPlayerForTrade.collectAsState()
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
                players = rawPlayers,
                boardPlayers = boardPlayers,
                currentPlayerId = currentPlayerId,
                currentTurnPlayer = currentTurnPlayer,
                currentFieldOverride = currentField,
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
                canReportCheater = actionGates.canReportCheater,
                onStartTrade = { player ->
                    viewModel.showTradeOverlay(player)
                    if (gameState?.pendingTradeOffer == null) {
                        viewModel.proposeTrade(
                            toPlayerId = player.id,
                            offerMoney = 0,
                            requestMoney = 0,
                            offerPropertyIds = emptyList(),
                            requestPropertyIds = emptyList(),
                            offerJailCards = 0,
                            requestJailCards = 0
                        )
                    }
                },
                canStartTrade = actionGates.canTrade,
                gameState = gameState,
                onFreeParkingMoneyClick = { showFreeParkingOverlay = true },
                modifier = Modifier.fillMaxSize()
            )

            val buttonWidth = 180.dp
            val shouldShowRollAgain =
                actionGates.canRollAgainAfterDouble ||
                        gameState?.lastDiceRoll?.isDouble == true &&
                        gameState?.phase == GamePhase.ROLLING
            val mustDrawCard =
                (isOnChanceField || isOnCommunityChestField) &&
                        !hasDrawnCardThisTurn &&
                        actionGates.canDrawCard

            val onDrawCard: (String) -> Unit = onDrawCard@ { type ->
                if (isDrawingCard) return@onDrawCard
                isDrawingCard = true
                viewModel.drawCard(type)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!showOverlay) {
                    if (canStartGame) {
                        GlassButton(
                            onClick = { viewModel.startGame() },
                            modifier = Modifier.width(buttonWidth)
                        ) {
                            Text("▶️ Start Game")
                        }
                    }

                    if (
                        (actionGates.canRollDice || actionGates.canRollAgainAfterDouble) &&
                        currentTurnPlayer != null &&
                        !mustDrawCard &&
                        !isDrawingCard
                    ) {
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
                                enabled = actionGates.canUseJailAction && currentTurnPlayer.money >= 50,
                                modifier = Modifier.width(buttonWidth).testTag("pay_jail_fine_button")
                            ) {
                                Text("💰 Pay 50€")
                            }

                            if (currentTurnPlayer.getOutOfJailCards > 0) {
                                GlassButton(
                                    onClick = { viewModel.useJailCard() },
                                    enabled = actionGates.canUseJailAction,
                                    modifier = Modifier.width(buttonWidth).testTag("use_jail_card_button")
                                ) {
                                    Text("🃏 Use Card (${currentTurnPlayer.getOutOfJailCards})")
                                }
                            }

                            GlassButton(
                                onClick = { showOverlay = true },
                                enabled = actionGates.canRollDice,
                                modifier = Modifier.width(buttonWidth).testTag("roll_dice_button")
                            ) {
                                Text("🎲 Attempt Double")
                            }
                        } else {
                            GlassButton(
                                onClick = {
                                        showOverlay = true

                                },
                                enabled = actionGates.canRollDice || actionGates.canRollAgainAfterDouble,
                                modifier = Modifier.width(buttonWidth).testTag("roll_dice_button")
                            ) {
                                Text(if (shouldShowRollAgain) "Roll Again" else "🎲 Roll Dice")
                            }
                        }
                    }

                    if (actionGates.canEndTurn && !showActionCardOverlay && !mustDrawCard && !isDrawingCard) {
                        GlassButton(
                            onClick = { viewModel.endTurn() },
                            modifier = Modifier.width(buttonWidth).testTag("end_turn_button")
                        ) {
                            Text("⏭️ End Turn")
                        }
                    }

                    val isReopenTaxPayment = visiblePaymentState?.source == PaymentSource.TAX
                    if (visiblePaymentState != null && !showPayRentOverlay && currentTurnPlayer?.id == currentPlayerId) {
                        GlassButton(
                            onClick = { viewModel.showPayRentOverlay(currentRentAmount, currentRentOwnerId, currentRentFieldId) },
                            modifier = Modifier.width(buttonWidth).testTag("pay_rent_reopen_button")
                        ) {
                            Text(if (isReopenTaxPayment) "💸 Pay Tax Due" else "💸 Pay Rent Due")
                        }
                    }

                    if (canBuyCurrentField) {
                        GlassButton(
                            onClick = {
                                currentField?.let { field -> viewModel.buyProperty(field.id) }
                            },
                            modifier = Modifier.width(buttonWidth).testTag("buy_property_button")
                        ) {
                            Text("🏠 Buy Property")
                        }
                    }

                    if (isOnChanceField && isBuyingPhaseForCurrentPlayer) {
                        DrawCardButton(
                            cardType = "CHANCE",
                            alreadyDrawn = hasDrawnCardThisTurn,
                            enabled = actionGates.canDrawChance && !showActionCardOverlay && !isDrawingCard,
                            label = "🎰 Draw Chance",
                            onDraw = { onDrawCard("CHANCE") },
                            modifier = Modifier.width(buttonWidth)
                        )
                    }

                    if (isOnCommunityChestField && isBuyingPhaseForCurrentPlayer) {
                        DrawCardButton(
                            cardType = "COMMUNITY_CHEST",
                            alreadyDrawn = hasDrawnCardThisTurn,
                            enabled = actionGates.canDrawCommunityChest && !showActionCardOverlay && !isDrawingCard,
                            label = "⭐ Draw Community",
                            onDraw = { onDrawCard("COMMUNITY_CHEST") },
                            modifier = Modifier.width(buttonWidth)
                        )
                    }
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

            GameboardOverlayLayer(eventLog = eventLog)

            val canExecuteVisibleAction = actionGates.canExecuteCard
            val actionCardDismissedForSpectator =
                !canExecuteVisibleAction && currentActionCard?.id == dismissedSpectatorActionCardId
            ActionCardOverlay(
                isVisible = showActionCardOverlay && !actionCardDismissedForSpectator,
                card = currentActionCard,
                isExecuting = isExecutingAction && canExecuteVisibleAction,
                canExecuteAction = canExecuteVisibleAction,
                executingPlayerName = currentTurnPlayer?.name,
                onDismiss = if (canExecuteVisibleAction) {
                    null
                } else {
                    { currentActionCard?.let { dismissedSpectatorActionCardId = it.id } }
                },
                onExecuteAction = { viewModel.executeAction() }
            )

            DiceRollOverlay(
                isVisible = diceOverlayVisible,
                diceResult = diceOverlayResult,
                isRolling = diceOverlayIsRolling,
                hasShaken = diceOverlayHasShaken,
                sequenceId = activeDicePresentation?.sequenceId ?: 0L,
                onShakeButton = {
                    if (!hasShaken && (actionGates.canRollDice || actionGates.canRollAgainAfterDouble)) {
                        hasShaken = true
                        if (actionGates.canRollAgainAfterDouble) {
                            viewModel.rollAgainAfterDouble()
                        } else {
                            viewModel.rollDice()
                        }
                    }
                },
                onResultDisplayed = { sequenceId -> viewModel.onDiceResultDisplayed(sequenceId) },
                onDismissed = { sequenceId ->
                    dismissedDiceSequenceId = sequenceId
                    viewModel.onDiceDismissed(sequenceId)
                },
                onClose = {
                    showOverlay = false
                    hasShaken = false
                    activeDicePresentation?.let { pres ->
                        dismissedDiceSequenceId = pres.sequenceId
                        viewModel.onDiceDismissed(pres.sequenceId)
                    }
                }
            )
            FreeParkingJackpotOverlay(
                isVisible = showFreeParkingOverlay,
                amount = gameState?.freeParkingMoney ?: 0,
                onClose = { showFreeParkingOverlay = false }
            )

            val pendingTradeOffer = gameState?.pendingTradeOffer
            var dismissedTradeId by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(pendingTradeOffer?.id) {
                if (pendingTradeOffer?.id != dismissedTradeId) dismissedTradeId = null
            }
            val publicTradePlayer = pendingTradeOffer
                ?.takeUnless { it.id == dismissedTradeId }
                ?.let { offer ->
                    when (currentPlayerId) {
                        offer.fromPlayerId -> rawPlayers.find { it.id == offer.toPlayerId }
                        offer.toPlayerId -> rawPlayers.find { it.id == offer.fromPlayerId }
                        else -> rawPlayers.find { it.id == offer.fromPlayerId }
                    }
                }
                ?.takeUnless { it.isBankrupt() }
            val selectedLiveTradePlayer = selectedTradePlayer
                ?.let { selected -> rawPlayers.find { it.id == selected.id } ?: selected }
                ?.takeUnless { it.isBankrupt() }
            val visibleTradePlayer = selectedLiveTradePlayer ?: publicTradePlayer
            if (visibleTradePlayer != null) {
                TradeOverlay(
                    isVisible = true,
                    currentPlayerId = currentPlayerId,
                    tradePartner = visibleTradePlayer,
                    players = rawPlayers,
                    fields = fields,
                    pendingTradeOffer = pendingTradeOffer,
                    onProposeTrade = { toPlayerId, offerMoney, requestMoney, offerPropertyIds, requestPropertyIds, offerJailCards, requestJailCards ->
                        viewModel.proposeTrade(
                            toPlayerId = toPlayerId,
                            offerMoney = offerMoney,
                            requestMoney = requestMoney,
                            offerPropertyIds = offerPropertyIds,
                            requestPropertyIds = requestPropertyIds,
                            offerJailCards = offerJailCards,
                            requestJailCards = requestJailCards
                        )
                    },
                    onAcceptTrade = { tradeId -> viewModel.acceptTrade(tradeId) },
                    onRejectTrade = { tradeId -> viewModel.rejectTrade(tradeId) },
                    onDismiss = {
                        val offer = pendingTradeOffer
                        if (offer != null &&
                            (offer.fromPlayerId == currentPlayerId || offer.toPlayerId == currentPlayerId)
                        ) {
                            viewModel.rejectTrade(offer.id)
                        } else if (offer != null) {
                            dismissedTradeId = offer.id
                        }
                        viewModel.hideTradeOverlay()
                    }
                )
            }
            val isTaxPayment = visiblePaymentState?.source == PaymentSource.TAX
            val canPayVisiblePayment = if (isTaxPayment) {
                actionGates.canPayTax
            } else {
                canPayRent
            }
            PayRentOverlay(
                isVisible = showPayRentOverlay && currentTurnPlayer?.id == currentPlayerId,
                rentAmount = currentRentAmount,
                ownerName = currentRentOwnerId?.let { ownerId ->
                    rawPlayers.find { it.id == ownerId }?.name
                },
                fieldName = currentRentFieldId?.let { id ->
                    fields.find { it.id == id }?.name
                } ?: "",
                currentMoney = currentTurnPlayer?.money ?: 0,
                canPay = canPayVisiblePayment,
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
                isPayingRent = visiblePaymentState != null && isPayingRent == true,
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

            GameOverOverlay(
                isVisible = showGameOverOverlay,
                activePlayers = players.filter { !it.eliminated && !it.isBankrupt() },
                onBackToLobby = {
                    (context as? Activity)?.finish()
                }
            )

            GameTerminatedOverlay(
                isVisible = hostEndedGame,
                onBackToLobby = { (context as? Activity)?.finish() }
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
    boardPlayers: List<Player> = players,
    currentPlayerId: String = "",
    currentTurnPlayer: Player? = null,
    currentFieldOverride: Field? = null,
    onPlayerCardClick: (Player) -> Unit = {},
    selectedPlayerForOverlay: Player? = null,
    onDismissOverlay: () -> Unit = {},
    movementAnimationState: MovementAnimationState? = null,
    onReportCheater: (String) -> Unit = {},
    canReportCheater: Boolean = false,
    onStartTrade: (Player) -> Unit = {},
    canStartTrade: Boolean = false,
    gameState: at.aau.monopoly.klagenfurt.model.GameState? = null,
    onFreeParkingMoneyClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val myPlayer = players.find { it.id == currentPlayerId }
    val otherPlayers = players.filter { it.id != currentPlayerId }

    val currentField = currentFieldOverride ?: currentTurnPlayer?.let { p ->
        fields.fieldAtBoardPosition(p.position)
    }
    val activePlayerFieldIndex = movementAnimationState
        ?.takeUnless { it.isComplete }
        ?.let { animation ->
            when {
                animation.currentStepIndex < 0 -> animation.startPosition
                animation.currentStepIndex in animation.path.indices -> animation.path[animation.currentStepIndex]
                else -> null
            }
        }
        ?: currentTurnPlayer?.position

    val playersByField: Map<Int, List<Player>> = remember(boardPlayers) {
        boardPlayers.groupBy { it.position }
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
                                    if (it.currentStepIndex < 0) it.startPosition
                                    else if (it.currentStepIndex in it.path.indices) it.path[it.currentStepIndex]
                                    else null
                                },
                                animationComplete = movementAnimationState?.isComplete ?: true,
                                isActivePlayerField = index == activePlayerFieldIndex,
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
                    val pendingTradeOffer = gameState?.pendingTradeOffer
                    val isCurrentLocalTurn = currentTurnPlayer?.id == currentPlayerId
                    val myPlayerCanAct = myPlayer != null && !myPlayer.isBankrupt()
                    val playerCanBeTargeted = !player.isBankrupt()
                    val currentPlayerRolled = gameState?.lastDiceRoll != null
                    val isReportTarget = player.id == currentTurnPlayer?.id
                    val shouldShowReport = !isCurrentLocalTurn &&
                            currentPlayerRolled &&
                            isReportTarget &&
                            myPlayerCanAct &&
                            playerCanBeTargeted
                    val canReport = shouldShowReport &&
                            canReportCheater &&
                            myPlayer.money > 500
                    val isPendingTradeParticipant =
                        pendingTradeOffer?.fromPlayerId == player.id ||
                                pendingTradeOffer?.toPlayerId == player.id
                    val canStartNewTrade = pendingTradeOffer == null &&
                            isCurrentLocalTurn &&
                            canStartTrade &&
                            myPlayerCanAct &&
                            playerCanBeTargeted
                    val canReopenTrade = pendingTradeOffer != null &&
                            isPendingTradeParticipant &&
                            myPlayerCanAct &&
                            playerCanBeTargeted
                    val tradeLabel = if (canReopenTrade) "🔁 Reopen Trade" else "🔁 Trade"

                    Column(horizontalAlignment = Alignment.End) {
                        PlayerInfoPanel(
                            player = player,
                            fields = fields,
                            cards = emptyList(),
                            isCurrentTurn = player.id == currentTurnPlayer?.id,
                            onCardClick = { onPlayerCardClick(player) },
                            actionContent = {
                                if (shouldShowReport) {
                                    GlassActionButton(
                                        text = "🚨 Report",
                                        onClick = { onReportCheater(player.id) },
                                        enabled = canReport,
                                        modifier = Modifier.testTag("report_action_button_${player.id}")
                                    )
                                }

                                if (canStartNewTrade || canReopenTrade) {
                                    GlassActionButton(
                                        text = tradeLabel,
                                        onClick = { onStartTrade(player) },
                                        enabled = true,
                                        modifier = Modifier.testTag("trade_action_button_${player.id}")
                                    )
                                }
                            }
                        )
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
            containerColor = GlassPanelColor,
            contentColor = Color.White,
            disabledContainerColor = GlassDisabledColor,
            disabledContentColor = Color.White.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(12.dp),
        content = content
    )
}

@Composable
private fun GlassActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(26.dp)
            .border(1.dp, GlassBorderColor, RoundedCornerShape(8.dp)),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GlassPanelColor,
            contentColor = Color.White,
            disabledContainerColor = GlassDisabledColor,
            disabledContentColor = Color.White.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
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
                text = "€$amount",
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

@Composable
fun TradeOverlay(
    isVisible: Boolean,
    currentPlayerId: String,
    tradePartner: Player,
    players: List<Player>,
    fields: List<Field>,
    pendingTradeOffer: TradeOffer?,
    onProposeTrade: (
        toPlayerId: String,
        offerMoney: Int,
        requestMoney: Int,
        offerPropertyIds: List<Int>,
        requestPropertyIds: List<Int>,
        offerJailCards: Int,
        requestJailCards: Int
    ) -> Unit,
    onAcceptTrade: (String) -> Unit,
    onRejectTrade: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val currentPlayer = players.find { it.id == currentPlayerId } ?: return
    val activeOffer = pendingTradeOffer
    val fromPlayer = activeOffer?.let { offer -> players.find { it.id == offer.fromPlayerId } } ?: currentPlayer
    val toPlayer = activeOffer?.let { offer -> players.find { it.id == offer.toPlayerId } } ?: tradePartner
    val isInvolved = currentPlayerId == fromPlayer.id || currentPlayerId == toPlayer.id
    val currentPlayerAccepted = activeOffer?.acceptedByPlayerIds?.contains(currentPlayerId) == true
    val canToggleAccept = activeOffer != null &&
            isInvolved &&
            activeOffer.hasTradeContents()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .heightIn(max = 660.dp)
                .offset(y = 56.dp)
                .border(1.dp, GlassBorderColor, RoundedCornerShape(10.dp)),
            color = Color.Black.copy(alpha = 0.62f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Trade with ${tradePartner.name}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${fromPlayer.name} is trading with ${toPlayer.name}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { activeOffer?.let { onAcceptTrade(it.id) } },
                            enabled = canToggleAccept,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentPlayerAccepted) Color(0xFF2E7D32) else GlassPanelColor,
                                contentColor = Color.White,
                                disabledContainerColor = GlassDisabledColor,
                                disabledContentColor = Color.White.copy(alpha = 0.45f)
                            )
                        ) {
                            Text(if (currentPlayerAccepted) "Accepted" else "Accept")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close", color = Color.White)
                        }
                    }
                }

                if (activeOffer != null && isInvolved) {
                    TradeProposalEditor(
                        fromPlayer = fromPlayer,
                        toPlayer = toPlayer,
                        currentPlayerId = currentPlayerId,
                        fields = fields,
                        initialOffer = activeOffer,
                        onProposeTrade = onProposeTrade,
                        onRejectTrade = onRejectTrade
                    )
                } else if (activeOffer != null) {
                    TradeOfferReview(
                        offer = activeOffer,
                        players = players,
                        fields = fields,
                        currentPlayerId = currentPlayerId,
                        onAcceptTrade = onAcceptTrade,
                        onRejectTrade = onRejectTrade
                    )
                } else {
                    TradeProposalEditor(
                        fromPlayer = currentPlayer,
                        toPlayer = tradePartner,
                        currentPlayerId = currentPlayerId,
                        fields = fields,
                        initialOffer = null,
                        onProposeTrade = onProposeTrade
                    )
                }
            }
        }
    }
}

@Composable
private fun TradeProposalEditor(
    fromPlayer: Player,
    toPlayer: Player,
    currentPlayerId: String,
    fields: List<Field>,
    initialOffer: TradeOffer?,
    onProposeTrade: (
        toPlayerId: String,
        offerMoney: Int,
        requestMoney: Int,
        offerPropertyIds: List<Int>,
        requestPropertyIds: List<Int>,
        offerJailCards: Int,
        requestJailCards: Int
    ) -> Unit,
    onRejectTrade: (String) -> Unit = {}
) {
    var offerMoneyText by remember { mutableStateOf(initialOffer?.offerMoney?.toString() ?: "0") }
    var requestMoneyText by remember { mutableStateOf(initialOffer?.requestMoney?.toString() ?: "0") }
    var offerJailCards by remember { mutableStateOf(initialOffer?.offerJailCards ?: 0) }
    var requestJailCards by remember { mutableStateOf(initialOffer?.requestJailCards ?: 0) }
    var offeredPropertyIds by remember { mutableStateOf(initialOffer?.offerPropertyIds?.toSet() ?: setOf()) }
    var requestedPropertyIds by remember { mutableStateOf(initialOffer?.requestPropertyIds?.toSet() ?: setOf()) }

    LaunchedEffect(initialOffer) {
        offerMoneyText = initialOffer?.offerMoney?.toString() ?: "0"
        requestMoneyText = initialOffer?.requestMoney?.toString() ?: "0"
        offerJailCards = initialOffer?.offerJailCards ?: 0
        requestJailCards = initialOffer?.requestJailCards ?: 0
        offeredPropertyIds = initialOffer?.offerPropertyIds?.toSet() ?: setOf()
        requestedPropertyIds = initialOffer?.requestPropertyIds?.toSet() ?: setOf()
    }

    val fromProperties = remember(fromPlayer.id, fields) {
        fields.tradeablePropertiesFor(fromPlayer.id)
    }
    val toProperties = remember(toPlayer.id, fields) {
        fields.tradeablePropertiesFor(toPlayer.id)
    }
    val offerMoney = offerMoneyText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val requestMoney = requestMoneyText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val canEditFromSide = currentPlayerId == fromPlayer.id
    val canEditToSide = currentPlayerId == toPlayer.id
    val hasTradeContents = offerMoney > 0 || requestMoney > 0 ||
            offeredPropertyIds.isNotEmpty() || requestedPropertyIds.isNotEmpty() ||
            offerJailCards > 0 || requestJailCards > 0
    val canSubmit =
        offerMoney <= fromPlayer.money &&
                requestMoney <= toPlayer.money &&
                offerJailCards <= fromPlayer.getOutOfJailCards &&
                requestJailCards <= toPlayer.getOutOfJailCards &&
                (initialOffer != null || hasTradeContents)

    fun publishLiveUpdate(
        nextOfferMoneyText: String = offerMoneyText,
        nextRequestMoneyText: String = requestMoneyText,
        nextOfferedPropertyIds: Set<Int> = offeredPropertyIds,
        nextRequestedPropertyIds: Set<Int> = requestedPropertyIds,
        nextOfferJailCards: Int = offerJailCards,
        nextRequestJailCards: Int = requestJailCards
    ) {
        if (initialOffer == null) return
        onProposeTrade(
            toPlayer.id,
            nextOfferMoneyText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            nextRequestMoneyText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            nextOfferedPropertyIds.toList(),
            nextRequestedPropertyIds.toList(),
            nextOfferJailCards,
            nextRequestJailCards
        )
    }

    TradeAcceptStatus(
        fromPlayer = fromPlayer,
        toPlayer = toPlayer,
        acceptedByPlayerIds = initialOffer?.acceptedByPlayerIds ?: emptyList()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TradeSideEditor(
            title = "${fromPlayer.name} gives",
            moneyText = offerMoneyText,
            onMoneyChange = {
                val next = it.filter(Char::isDigit).take(5)
                offerMoneyText = next
                publishLiveUpdate(nextOfferMoneyText = next)
            },
            maxMoney = fromPlayer.money,
            jailCards = offerJailCards,
            maxJailCards = fromPlayer.getOutOfJailCards,
            onJailCardsChange = {
                offerJailCards = it
                publishLiveUpdate(nextOfferJailCards = it)
            },
            properties = fromProperties,
            selectedPropertyIds = offeredPropertyIds,
            onPropertyToggle = { fieldId ->
                val next = offeredPropertyIds.toggle(fieldId)
                offeredPropertyIds = next
                publishLiveUpdate(nextOfferedPropertyIds = next)
            },
            enabled = canEditFromSide,
            modifier = Modifier.weight(1f)
        )
        TradeSideEditor(
            title = "${toPlayer.name} gives",
            moneyText = requestMoneyText,
            onMoneyChange = {
                val next = it.filter(Char::isDigit).take(5)
                requestMoneyText = next
                publishLiveUpdate(nextRequestMoneyText = next)
            },
            maxMoney = toPlayer.money,
            jailCards = requestJailCards,
            maxJailCards = toPlayer.getOutOfJailCards,
            onJailCardsChange = {
                requestJailCards = it
                publishLiveUpdate(nextRequestJailCards = it)
            },
            properties = toProperties,
            selectedPropertyIds = requestedPropertyIds,
            onPropertyToggle = { fieldId ->
                val next = requestedPropertyIds.toggle(fieldId)
                requestedPropertyIds = next
                publishLiveUpdate(nextRequestedPropertyIds = next)
            },
            enabled = canEditToSide,
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (initialOffer != null) {
            TextButton(onClick = { onRejectTrade(initialOffer.id) }) {
                Text("Cancel", color = Color.White)
            }
        }
        if (initialOffer == null) {
            Button(
                onClick = {
                    onProposeTrade(
                        toPlayer.id,
                        offerMoney,
                        requestMoney,
                        offeredPropertyIds.toList(),
                        requestedPropertyIds.toList(),
                        offerJailCards,
                        requestJailCards
                    )
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassPanelColor,
                    contentColor = Color.White,
                    disabledContainerColor = GlassDisabledColor,
                    disabledContentColor = Color.White.copy(alpha = 0.45f)
                )
            ) {
                Text("Start Offer")
            }
        }
    }
}

@Composable
private fun TradeAcceptStatus(
    fromPlayer: Player,
    toPlayer: Player,
    acceptedByPlayerIds: List<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "${fromPlayer.name}: ${if (fromPlayer.id in acceptedByPlayerIds) "Accepted" else "Reviewing"}",
            color = if (fromPlayer.id in acceptedByPlayerIds) Color(0xFF81C784) else Color.White.copy(alpha = 0.72f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${toPlayer.name}: ${if (toPlayer.id in acceptedByPlayerIds) "Accepted" else "Reviewing"}",
            color = if (toPlayer.id in acceptedByPlayerIds) Color(0xFF81C784) else Color.White.copy(alpha = 0.72f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TradeSideEditor(
    title: String,
    moneyText: String,
    onMoneyChange: (String) -> Unit,
    maxMoney: Int,
    jailCards: Int,
    maxJailCards: Int,
    onJailCardsChange: (Int) -> Unit,
    properties: List<Field>,
    selectedPropertyIds: Set<Int>,
    onPropertyToggle: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = Color.White)
        val moneyAmount = moneyText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Money: ${moneyAmount}€ / ${maxMoney}€",
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.55f),
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onMoneyChange((moneyAmount + 100).coerceAtMost(maxMoney).toString())
                    },
                    enabled = enabled && moneyAmount < maxMoney,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassPanelColor)
                ) {
                    Text("+100€")
                }
                Button(
                    onClick = {
                        onMoneyChange((moneyAmount + 10).coerceAtMost(maxMoney).toString())
                    },
                    enabled = enabled && moneyAmount < maxMoney,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassPanelColor)
                ) {
                    Text("+10€")
                }
                TextButton(
                    onClick = { onMoneyChange("0") },
                    enabled = enabled && moneyAmount > 0
                ) {
                    Text("Reset", color = Color.White)
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Jail cards: $jailCards / $maxJailCards", color = Color.White.copy(alpha = 0.86f))
            TextButton(
                onClick = { onJailCardsChange((jailCards - 1).coerceAtLeast(0)) },
                enabled = enabled && jailCards > 0
            ) { Text("-", color = Color.White) }
            TextButton(
                onClick = { onJailCardsChange((jailCards + 1).coerceAtMost(maxJailCards)) },
                enabled = enabled && jailCards < maxJailCards
            ) { Text("+", color = Color.White) }
        }
        if (properties.isEmpty()) {
            Text("No tradeable properties", color = Color.White.copy(alpha = 0.62f), fontSize = 13.sp)
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 170.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(properties) { field ->
                    val isSelected = field.id in selectedPropertyIds
                    val borderColor = when {
                        isSelected -> Color(0xFF2E7D32)
                        enabled -> Color.Transparent
                        else -> Color.LightGray
                    }
                    Box(
                        modifier = Modifier
                            .border(3.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = enabled) { onPropertyToggle(field.id) }
                            .padding(3.dp)
                    ) {
                        FieldCardUI(
                            field = field,
                            cardWidth = 92.dp,
                            cardHeight = 148.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeOfferReview(
    offer: TradeOffer,
    players: List<Player>,
    fields: List<Field>,
    currentPlayerId: String,
    onAcceptTrade: (String) -> Unit,
    onRejectTrade: (String) -> Unit
) {
    val fromPlayer = players.find { it.id == offer.fromPlayerId }
    val toPlayer = players.find { it.id == offer.toPlayerId }
    val isInvolved = offer.toPlayerId == currentPlayerId || offer.fromPlayerId == currentPlayerId
    val currentPlayerAccepted = offer.acceptedByPlayerIds.contains(currentPlayerId)

    Text(
        text = "${fromPlayer?.name ?: "A player"} offers a trade to ${toPlayer?.name ?: "another player"}.",
        color = Color.White
    )
    if (fromPlayer != null && toPlayer != null) {
        TradeAcceptStatus(
            fromPlayer = fromPlayer,
            toPlayer = toPlayer,
            acceptedByPlayerIds = offer.acceptedByPlayerIds
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TradeOfferColumn(
            title = "${fromPlayer?.name ?: "Player"} gives",
            money = offer.offerMoney,
            jailCards = offer.offerJailCards,
            propertyIds = offer.offerPropertyIds,
            fields = fields,
            modifier = Modifier.weight(1f)
        )
        TradeOfferColumn(
            title = "${toPlayer?.name ?: "Player"} gives",
            money = offer.requestMoney,
            jailCards = offer.requestJailCards,
            propertyIds = offer.requestPropertyIds,
            fields = fields,
            modifier = Modifier.weight(1f)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        if (isInvolved) {
            TextButton(
                onClick = { onRejectTrade(offer.id) }
            ) {
                Text("Cancel", color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Button(
            onClick = { onAcceptTrade(offer.id) },
            enabled = isInvolved && !currentPlayerAccepted,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GlassPanelColor)
        ) {
            Text(
                when {
                    !isInvolved -> "Watching"
                    currentPlayerAccepted -> "Accepted"
                    else -> "Accept"
                }
            )
        }
    }
}

@Composable
private fun TradeOfferColumn(
    title: String,
    money: Int,
    jailCards: Int,
    propertyIds: List<Int>,
    fields: List<Field>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Money: $money", color = Color.White.copy(alpha = 0.86f))
        Text("Jail cards: $jailCards", color = Color.White.copy(alpha = 0.86f))
        val propertyNames = propertyIds.mapNotNull { id -> fields.find { it.id == id }?.name }
        if (propertyNames.isEmpty()) {
            Text("No properties", color = Color.White.copy(alpha = 0.62f))
        } else {
            propertyNames.forEach { name ->
                Text("- $name", color = Color.White.copy(alpha = 0.86f), fontSize = 13.sp)
            }
        }
    }
}

private fun Set<Int>.toggle(fieldId: Int): Set<Int> =
    if (fieldId in this) this - fieldId else this + fieldId

private fun TradeOffer.hasTradeContents(): Boolean =
    offerMoney > 0 ||
            requestMoney > 0 ||
            offerPropertyIds.isNotEmpty() ||
            requestPropertyIds.isNotEmpty() ||
            offerJailCards > 0 ||
            requestJailCards > 0

private fun List<Field>.tradeablePropertiesFor(playerId: String): List<Field> =
    filter { field ->
        field is OwnableField &&
                field.ownerIdFromField() == playerId &&
                (field !is PropertyField || (field.houses == 0 && !field.hasHotel))
    }

@Composable
fun GameTerminatedOverlay(
    isVisible: Boolean,
    onBackToLobby: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF1C233D),
        titleContentColor = Color(0xFFA2AAF0),
        textContentColor = Color.White,
        title = {
            Text(
                text = "Game Terminated",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                text = "The host has ended the game.",
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onBackToLobby
            ) {
                Text(
                    text = "Back to Lobby",
                    color = Color(0xFFA2AAF0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    )
}
