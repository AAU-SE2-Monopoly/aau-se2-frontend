package at.aau.monopoly.klagenfurt.ui

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
import com.example.myapplication.BuildConfig
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // Activate cheat in ViewModel
            viewModel.activateCheatForNextRoll()
            Log.d("DiceDebug", "Cheat activated via Volume Up!")
            // IMPORTANT: Return true so volume doesn't change
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
    val context = LocalContext.current

    // NEW: Listen to drama events (cheater reports) and show Toast
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

    var showOverlay by remember { mutableStateOf(false) }
    var showFreeParkingOverlay by remember { mutableStateOf(false) }

    val showGameOverOverlay by viewModel.showGameOverOverlay.collectAsState()
    val winner by viewModel.winner.collectAsState()

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
    val selectedTradePlayer by viewModel.selectedPlayerForTrade.collectAsState()
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
                // NEW: Pass reportCheater to the content layer
                onReportCheater = { reportedPlayerId -> viewModel.reportCheater(reportedPlayerId) },
                onStartTrade = { player ->
                    viewModel.showTradeOverlay(player)
                    viewModel.proposeTrade(
                        toPlayerId = player.id,
                        offerMoney = 0,
                        requestMoney = 0,
                        offerPropertyIds = emptyList(),
                        requestPropertyIds = emptyList(),
                        offerJailCards = 0,
                        requestJailCards = 0
                    )
                },
                gameState = gameState,
                onFreeParkingMoneyClick = { showFreeParkingOverlay = true },
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
                    offer.fromPlayerId -> players.find { it.id == offer.toPlayerId }
                    offer.toPlayerId -> players.find { it.id == offer.fromPlayerId }
                    else -> players.find { it.id == offer.fromPlayerId }
                }
            }
            val visibleTradePlayer = selectedTradePlayer ?: publicTradePlayer
            if (visibleTradePlayer != null) {
                TradeOverlay(
                    isVisible = true,
                    currentPlayerId = currentPlayerId,
                    tradePartner = visibleTradePlayer,
                    players = players,
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
            // Payment overlays
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

            GameOverOverlay(
                isVisible = showGameOverOverlay,
                winnerName = winner?.name,
                onBackToLobby = {
                    (context as? Activity)?.finish()
                }
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
    onStartTrade: (Player) -> Unit = {},
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
                        Button(
                            onClick = { onStartTrade(player) },
                            enabled = gameState?.pendingTradeOffer == null &&
                                currentTurnPlayer?.id == currentPlayerId,
                            modifier = Modifier
                                .padding(top = 4.dp, end = 4.dp)
                                .height(26.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black.copy(alpha = 0.35f),
                                disabledContainerColor = Color.Black.copy(alpha = 0.16f)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("🔁", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
                .heightIn(max = 660.dp),
            color = Color(0xFFF8F4EA),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trade with ${tradePartner.name}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1D1D)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { activeOffer?.let { onAcceptTrade(it.id) } },
                            enabled = canToggleAccept,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentPlayerAccepted) Color(0xFF2E7D32) else Color(0xFF6750A4),
                                contentColor = Color.White,
                                disabledContainerColor = Color.LightGray,
                                disabledContentColor = Color.DarkGray
                            )
                        ) {
                            Text(if (currentPlayerAccepted) "Accepted" else "Accept")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close")
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
                Text("Cancel")
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
                shape = RoundedCornerShape(6.dp)
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
            color = if (fromPlayer.id in acceptedByPlayerIds) Color(0xFF1B5E20) else Color.DarkGray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${toPlayer.name}: ${if (toPlayer.id in acceptedByPlayerIds) "Accepted" else "Reviewing"}",
            color = if (toPlayer.id in acceptedByPlayerIds) Color(0xFF1B5E20) else Color.DarkGray,
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
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
        val moneyAmount = moneyText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Money: ${moneyAmount}€ / ${maxMoney}€",
                color = if (enabled) Color(0xFF222222) else Color.DarkGray,
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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("+100€")
                }
                Button(
                    onClick = {
                        onMoneyChange((moneyAmount + 10).coerceAtMost(maxMoney).toString())
                    },
                    enabled = enabled && moneyAmount < maxMoney,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("+10€")
                }
                TextButton(
                    onClick = { onMoneyChange("0") },
                    enabled = enabled && moneyAmount > 0
                ) {
                    Text("Reset")
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Jail cards: $jailCards / $maxJailCards", color = Color(0xFF333333))
            TextButton(
                onClick = { onJailCardsChange((jailCards - 1).coerceAtLeast(0)) },
                enabled = enabled && jailCards > 0
            ) { Text("-") }
            TextButton(
                onClick = { onJailCardsChange((jailCards + 1).coerceAtMost(maxJailCards)) },
                enabled = enabled && jailCards < maxJailCards
            ) { Text("+") }
        }
        if (properties.isEmpty()) {
            Text("No tradeable properties", color = Color.DarkGray, fontSize = 13.sp)
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
        color = Color.Black
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
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Button(
            onClick = { onAcceptTrade(offer.id) },
            enabled = isInvolved && !currentPlayerAccepted,
            shape = RoundedCornerShape(6.dp)
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
        Text(title, fontWeight = FontWeight.Bold, color = Color.Black)
        Text("Money: $money", color = Color(0xFF333333))
        Text("Jail cards: $jailCards", color = Color(0xFF333333))
        val propertyNames = propertyIds.mapNotNull { id -> fields.find { it.id == id }?.name }
        if (propertyNames.isEmpty()) {
            Text("No properties", color = Color.DarkGray)
        } else {
            propertyNames.forEach { name ->
                Text("- $name", color = Color(0xFF333333), fontSize = 13.sp)
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
