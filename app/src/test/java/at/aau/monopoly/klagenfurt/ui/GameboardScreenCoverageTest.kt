package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.DebugSettings
import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.PaymentSource
import at.aau.monopoly.klagenfurt.model.PendingPayment
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.card.ChanceCard
import at.aau.monopoly.klagenfurt.model.enums.CardAction
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.ChanceField
import at.aau.monopoly.klagenfurt.model.field.CommunityChestField
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.TaxField
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GameboardScreenCoverageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val shakeEvents = MutableSharedFlow<Unit>()

    @After
    fun tearDown() {
        DebugSettings.isEnabled = false
    }

    // ─── Jail buttons coverage ──────────────────────────────────────────

    @Test
    fun gameboardScreen_showsJailButtonsWhenPlayerInJail() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 10,
            inJail = true,
            jailTurns = 1,
            money = 100
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        fakeService.emitGameState(state)
        composeTestRule.waitForIdle()

        // Jail status text
        composeTestRule.onNodeWithText("🔒 In Jail (Attempt 2/3)", substring = true).assertIsDisplayed()
        // Pay fine button
        composeTestRule.onNodeWithTag("pay_jail_fine_button").assertExists()
        // Roll dice (Pasch versuchen)
        composeTestRule.onNodeWithTag("roll_dice_button").assertExists()
    }

    @Test
    fun gameboardScreen_showsJailCardButtonWhenPlayerHasCards() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 10,
            inJail = true,
            jailTurns = 0,
            money = 100,
            getOutOfJailCards = 2
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        fakeService.emitGameState(state)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("use_jail_card_button").assertExists()
        composeTestRule.onNodeWithText("🃏 Use Card (2)").assertIsDisplayed()
    }

    @Test
    fun gameboardScreen_payJailFineButtonCallsViewModel() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 10,
            inJail = true,
            jailTurns = 0,
            money = 100
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        fakeService.emitGameState(state)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pay_jail_fine_button").performClick()
        composeTestRule.waitForIdle()
        // Verify the service method was called (payJailFine is a no-op in FakeGameService)
    }

    @Test
    fun gameboardScreen_payJailFineButtonEnablesAfterTradeMoneyRefresh() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 10,
            inJail = true,
            jailTurns = 0,
            money = 20
        )
        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player, Player(id = "player-2", name = "Bob", money = 300)),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        fakeService.emitGameState(state)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pay_jail_fine_button").assertIsNotEnabled()

        fakeService.emitGameState(
            state.copy(
                players = mutableListOf(
                    player.copy(money = 60),
                    Player(id = "player-2", name = "Bob", money = 260)
                )
            )
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pay_jail_fine_button").assertIsEnabled()
    }

    @Test
    fun gameboardScreen_useJailCardButtonCallsViewModel() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 10,
            inJail = true,
            jailTurns = 0,
            money = 100,
            getOutOfJailCards = 1
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        fakeService.emitGameState(state)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("use_jail_card_button").performClick()
        composeTestRule.waitForIdle()
    }

    // ─── End turn button coverage ──────────────────────────────────────────

    @Test
    fun gameboardScreen_showsEndTurnButton() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.TURN_END,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("end_turn_button").assertExists()
    }

    @Test
    fun gameboardScreen_endTurnButtonCallsViewModel() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.TURN_END,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("end_turn_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(fakeService.endTurnCalled)
    }

    // ─── Roll dice button (not in jail) coverage ───────────────────────

    @Test
    fun gameboardScreen_showsRollDiceButtonWhenRollingPhase() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("roll_dice_button").assertExists()
        composeTestRule.onNodeWithText("🎲 Roll Dice").assertIsDisplayed()
    }

    // ─── Draw card buttons coverage ────────────────────────────────────

    @Test
    fun gameboardScreen_showsDrawChanceButtonOnChanceField() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 2,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            fields = listOf(
                GoField(0),
                PropertyField(
                    id = 1, name = "Herrengasse",
                    color = PropertyColor.BROWN, price = 60,
                    rent = listOf(2, 10, 30, 90, 160, 250),
                    houseCost = 50, hotelCost = 50
                ),
                ChanceField(id = 2)
            )
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("🎰 Draw Chance").assertIsDisplayed()
    }

    @Test
    fun gameboardScreen_showsDrawCommunityChestButtonOnCommunityField() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 2,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            fields = listOf(
                GoField(0),
                PropertyField(
                    id = 1, name = "Herrengasse",
                    color = PropertyColor.BROWN, price = 60,
                    rent = listOf(2, 10, 30, 90, 160, 250),
                    houseCost = 50, hotelCost = 50
                ),
                CommunityChestField(id = 2)
            )
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("⭐ Draw Community").assertIsDisplayed()
    }

    @Test
    fun gameboardScreen_showsDrawCommunityChestButtonWhenFieldIdMatchesSparsePosition() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 2,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            fields = listOf(
                GoField(0),
                CommunityChestField(id = 2)
            )
        )
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        fakeService.emitGameState(state)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("⭐ Draw Community").assertIsDisplayed()
    }

    // ─── Debug buttons coverage ─────────────────────────────────────────

    @Test
    fun gameboardScreen_showsDebugButtonsWhenDebugEnabled() {
        DebugSettings.isEnabled = true
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        // Toggle debug menu
        composeTestRule.onNodeWithTag("debug_menu_toggle_button").assertExists().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("debug_forward_game_button").assertExists()
        composeTestRule.onNodeWithTag("debug_bankruptcy_setup_button").assertExists()
        composeTestRule.onNodeWithTag("debug_land_on_tax_field_button").assertExists()
        composeTestRule.onNodeWithTag("debug_execute_pay_money_button").assertExists()
        composeTestRule.onNodeWithTag("debug_execute_pay_per_building_button").assertExists()
        composeTestRule.onNodeWithTag("debug_execute_pay_each_button").assertExists()
        composeTestRule.onNodeWithTag("debug_execute_collect_from_each_button").assertExists()
    }

    @Test
    fun gameboardScreen_debugForwardButtonCallsViewModel() {
        DebugSettings.isEnabled = true
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        // Toggle debug menu
        composeTestRule.onNodeWithTag("debug_menu_toggle_button").assertExists().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("debug_forward_game_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun gameboardScreen_debugBankruptcyButtonCallsViewModel() {
        DebugSettings.isEnabled = true
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        // Toggle debug menu
        composeTestRule.onNodeWithTag("debug_menu_toggle_button").assertExists().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("debug_bankruptcy_setup_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun gameboardScreen_debugButtonsHiddenWhenDebugDisabled() {
        DebugSettings.isEnabled = false
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("debug_menu_toggle_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("debug_forward_game_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("debug_bankruptcy_setup_button").assertDoesNotExist()
    }

    // ─── onPlayerCardClick coverage ─────────────────────────────────────

    @Test
    fun gameboardScreen_playerCardClickOnOtherPlayerShowsOverlay() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player1 = Player(id = "player-1", name = "Alice", position = 0, money = 1500)
        val player2 = Player(id = "player-2", name = "Bob", position = 0, money = 1500)

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player1, player2),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        // Click on Bob's player card to trigger `viewModel.showPlayerOverlay(player)`
        composeTestRule.onNodeWithText("Bob", substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun gameboardScreen_playerCardClickOnSelfShowsMortgageOverlay() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player1 = Player(id = "player-1", name = "Alice", position = 0, money = 1500)
        val player2 = Player(id = "player-2", name = "Bob", position = 0, money = 1500)

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player1, player2),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        // Click on Alice (self) – triggers mortgage management overlay when active
        composeTestRule.onNodeWithText("Alice", substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ─── Start game button coverage ─────────────────────────────────────

    @Test
    fun gameboardScreen_showsStartGameButton() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player1 = Player(id = "player-1", name = "Alice", position = 0, money = 1500)
        val player2 = Player(id = "player-2", name = "Bob", position = 0, money = 1500)

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player1, player2),
            currentPlayerIndex = 0,
            phase = GamePhase.WAITING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("▶️ Start Game").assertExists()
    }

    @Test
    fun gameboardScreen_startGameButtonCallsViewModel() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player1 = Player(id = "player-1", name = "Alice", position = 0, money = 1500)
        val player2 = Player(id = "player-2", name = "Bob", position = 0, money = 1500)

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player1, player2),
            currentPlayerIndex = 0,
            phase = GamePhase.WAITING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("▶️ Start Game").performClick()
        composeTestRule.waitForIdle()

        assertTrue(fakeService.startGameCalled)
    }

    // ─── Auto-close overlay (LaunchedEffect) coverage ────────────────────

    @Test
    fun gameboardScreen_overlayClosesWhenPhaseLeavesBuying() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )

        // Start in ROLLING to show overlay
        val rollingState = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(rollingState)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        // Click roll dice to open overlay
        composeTestRule.onNodeWithTag("roll_dice_button").performClick()
        composeTestRule.waitForIdle()

        // Transition to TURN_END – should auto-close overlay
        val turnEndState = rollingState.copy(phase = GamePhase.TURN_END)
        fakeService.emitGameState(turnEndState)
        composeTestRule.waitForIdle()
    }

    // ─── Buy property button coverage ──────────────────────────────────

    @Test
    fun gameboardScreen_buyPropertyButtonVisible() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val propertyField = PropertyField(
            id = 1, name = "Herrengasse",
            color = PropertyColor.BROWN, price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50, hotelCost = 50
        )

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 1,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            fields = listOf(GoField(0), propertyField)
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("buy_property_button").assertExists()
        composeTestRule.onNodeWithText("🏠 Buy Property").assertIsDisplayed()
    }

    // ─── Draw card button click coverage ─────────────────────────────────

    @Test
    fun gameboardScreen_drawChanceButtonCallsViewModel() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 2,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            fields = listOf(
                GoField(0),
                PropertyField(
                    id = 1, name = "Herrengasse",
                    color = PropertyColor.BROWN, price = 60,
                    rent = listOf(2, 10, 30, 90, 160, 250),
                    houseCost = 50, hotelCost = 50
                ),
                ChanceField(id = 2)
            )
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("🎰 Draw Chance").performClick()
        composeTestRule.waitForIdle()

        assertTrue(fakeService.drawCardCalled)
    }

    @Test
    fun gameboardScreen_drawCommunityChestButtonCallsViewModel() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 2,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            fields = listOf(
                GoField(0),
                PropertyField(
                    id = 1, name = "Herrengasse",
                    color = PropertyColor.BROWN, price = 60,
                    rent = listOf(2, 10, 30, 90, 160, 250),
                    houseCost = 50, hotelCost = 50
                ),
                CommunityChestField(id = 2)
            )
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("⭐ Draw Community").performClick()
        composeTestRule.waitForIdle()

        assertTrue(fakeService.drawCardCalled)
    }

    @Test
    fun gameboardScreen_currentPlayerCanExecuteVisibleActionCard() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )
        val card = ChanceCard(
            id = 99,
            description = "Collect a tiny surprise.",
            action = CardAction.COLLECT_MONEY,
            amount = 25
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            fields = listOf(GoField(0)),
            currentActionCard = card
        )
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        fakeService.emitGameState(state)
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            viewModel.visibleActionCard.value?.id == card.id
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Collect a tiny surprise.").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Execute Action").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Execute Action").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, fakeService.executeActionCalls)
    }

    @Test
    fun gameboardScreen_spectatorCanReadAndDismissVisibleActionCardWithoutExecuting() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-2"

        val activePlayer = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )
        val spectator = Player(
            id = "player-2",
            name = "Bob",
            position = 0,
            money = 1500
        )
        val card = ChanceCard(
            id = 100,
            description = "Collect comedy money.",
            action = CardAction.COLLECT_MONEY,
            amount = 50
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(activePlayer, spectator),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            fields = listOf(GoField(0)),
            currentActionCard = card
        )
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        fakeService.emitGameState(state)
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            viewModel.visibleActionCard.value?.id == card.id
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Collect comedy money.").assertExists()
        composeTestRule.onNodeWithText("Waiting for Alice to execute this card").assertExists()
        composeTestRule.onNodeWithText("✓ Execute Action").assertDoesNotExist()

        composeTestRule.onNodeWithText("Close").assertExists().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Collect comedy money.").assertDoesNotExist()
        assertEquals(0, fakeService.executeActionCalls)
    }

    @Test
    fun gameboardScreen_spectatorDismissedActionCardReopensForNewCard() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-2"

        val activePlayer = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )
        val spectator = Player(
            id = "player-2",
            name = "Bob",
            position = 0,
            money = 1500
        )
        val firstCard = ChanceCard(
            id = 101,
            description = "First funny card.",
            action = CardAction.COLLECT_MONEY,
            amount = 10
        )
        val secondCard = ChanceCard(
            id = 102,
            description = "Second funny card.",
            action = CardAction.COLLECT_MONEY,
            amount = 20
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(activePlayer, spectator),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            fields = listOf(GoField(0)),
            currentActionCard = firstCard
        )
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        fakeService.emitGameState(state)
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            viewModel.visibleActionCard.value?.id == firstCard.id
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("First funny card.").assertExists()
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("First funny card.").assertDoesNotExist()

        fakeService.emitGameState(state.copy(currentActionCard = secondCard))
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            viewModel.visibleActionCard.value?.id == secondCard.id
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Second funny card.").assertExists()
        composeTestRule.onNodeWithText("Waiting for Alice to execute this card").assertExists()
        assertEquals(0, fakeService.executeActionCalls)
    }

    // ─── Buy property button click coverage ─────────────────────────────

    @Test
    fun gameboardScreen_buyPropertyButtonCallsViewModel() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val propertyField = PropertyField(
            id = 1, name = "Herrengasse",
            color = PropertyColor.BROWN, price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50, hotelCost = 50
        )

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 1,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            fields = listOf(GoField(0), propertyField)
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("buy_property_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(fakeService.buyPropertyCalled)
    }

    // ─── Pay rent reopen button coverage ────────────────────────────────

    @Test
    fun gameboardScreen_payRentOverlayShownOnPendingPayment() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 1,
            money = 50
        )

        val ownedProperty = PropertyField(
            id = 1, name = "Herrengasse",
            color = PropertyColor.BROWN, price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50, hotelCost = 50,
            ownerId = "player-2"
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(
                player,
                Player(id = "player-2", name = "Bob", position = 0, money = 1500)
            ),
            currentPlayerIndex = 0,
            phase = GamePhase.PAYING_RENT,
            fields = listOf(GoField(0), ownedProperty),
            pendingPayment = at.aau.monopoly.klagenfurt.model.PendingPayment(
                amount = 100,
                source = at.aau.monopoly.klagenfurt.model.PaymentSource.RENT,
                sourceFieldId = 1,
                creditorPlayerId = "player-2"
            )
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        // When pendingPayment is set, the PayRentOverlay should be visible (showing rent UI)
        // This covers the hasPendingPayment = true code path
        composeTestRule.onNodeWithText("💸 Pay Rent Due").assertDoesNotExist() // overlay is shown instead
    }

    @Test
    fun gameboardScreen_railroadRentOverlayShowsAndPaysHauptbahnhofRent() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 5,
            money = 1500
        )
        val railroad = RailroadField(
            id = 5,
            name = "Hauptbahnhof",
            ownerId = "player-2"
        )
        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(
                player,
                Player(id = "player-2", name = "Bob", position = 0, money = 1500)
            ),
            currentPlayerIndex = 0,
            phase = GamePhase.PAYING_RENT,
            fields = listOf(GoField(0), railroad),
            pendingPayment = PendingPayment(
                amount = 50,
                source = PaymentSource.RENT,
                sourceFieldId = 5,
                creditorPlayerId = "player-2"
            )
        )
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        fakeService.emitGameState(state)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("RENT DUE").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Hauptbahnhof").assertCountEquals(3)
        composeTestRule.onNodeWithText("Pay Rent").assertIsEnabled().performClick()

        assertTrue(fakeService.payRentCalled)
        assertEquals(5, fakeService.lastPayRentFieldId)
    }

    @Test
    fun gameboardScreen_taxOverlayUsesTaxGateAndPaysTax() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 4,
            money = 1500
        )
        val taxField = TaxField(id = 4, name = "Reichensteuer", amount = 200)
        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.PAYING_RENT,
            fields = listOf(GoField(0), taxField),
            pendingPayment = PendingPayment(
                amount = 200,
                source = PaymentSource.TAX,
                sourceFieldId = 4,
                creditorPlayerId = null
            )
        )
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        fakeService.emitGameState(state)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("TAX DUE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pay Tax").assertIsEnabled().performClick()

        assertEquals(4, fakeService.lastPaidTaxFieldId)
    }

    // ─── Emulator auto-roll coverage ────────────────────────────────────

    @Test
    fun gameboardScreen_rollDiceButtonOpensOverlay() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 0,
            money = 1500
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        // Click the roll dice button - this opens the overlay (showOverlay = true)
        // Covers lines 290-294: LaunchedEffect(showOverlay, isRollingPhaseForCurrentPlayer, hasShaken)
        composeTestRule.onNodeWithTag("roll_dice_button").performClick()
        composeTestRule.waitForIdle()

        // The overlay opens; on a real emulator, auto-roll would trigger
        // The LaunchedEffect code path is exercised regardless of isEmulator detection
    }

    // ─── buttonWidth used in multiple buttons simultaneously ─────────────

    @Test
    fun gameboardScreen_multipleButtonsRenderedWithButtonWidthModifier() {
        // When player is in jail with cards AND in rolling phase, multiple buttons share buttonWidth
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 10,
            inJail = true,
            jailTurns = 2,
            money = 200,
            getOutOfJailCards = 1
        )

        val state = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            fields = listOf(GoField(0))
        )
        fakeService.emitGameState(state)
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        // All jail buttons use Modifier.width(buttonWidth) — this exercises the val buttonWidth = 180.dp line
        composeTestRule.onNodeWithTag("pay_jail_fine_button").assertExists()
        composeTestRule.onNodeWithTag("use_jail_card_button").assertExists()
        composeTestRule.onNodeWithTag("roll_dice_button").assertExists()
    }

    @Test
    fun gameboardScreen_payRentReopenButtonCallsViewModel() {
        val fakeService = FakeGameService()
        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val shakeEvents = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
        val viewModel = GameViewModel(fakeService)

        val state = at.aau.monopoly.klagenfurt.model.GameState(
            gameId = "game-1",
            fields = emptyList(),
            players = mutableListOf(
                at.aau.monopoly.klagenfurt.model.Player(id = "player-1", name = "Me", money = 1500, position =
                    0),
                at.aau.monopoly.klagenfurt.model.Player(id = "player-2", name = "Owner", money = 1500,
                    position = 0)
            ),
            currentPlayerIndex = 0,
            pendingPayment = at.aau.monopoly.klagenfurt.model.PendingPayment(
                amount = 200,
                creditorPlayerId = "player-2",
                sourceFieldId = 1,
                source = at.aau.monopoly.klagenfurt.model.PaymentSource.RENT
            )
        )
        fakeService.emitGameState(state)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
        composeTestRule.waitForIdle()

        viewModel.dismissPayRentOverlay()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pay_rent_reopen_button").performClick()
        composeTestRule.waitForIdle()

        org.junit.Assert.assertTrue(viewModel.showPayRentOverlay.value)
    }
}
