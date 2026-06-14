package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.TradeOffer
import at.aau.monopoly.klagenfurt.model.field.CommunityChestField
import at.aau.monopoly.klagenfurt.model.field.GoField
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameboardIsolatedUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testGameTerminatedOverlayIsVisibleAndClickable() {
        var backToLobbyClicked = false

        composeTestRule.setContent {
            GameTerminatedOverlay(
                isVisible = true,
                onBackToLobby = { backToLobbyClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Game Terminated").assertExists()
        composeTestRule.onNodeWithText("The host has ended the game.").assertExists()

        composeTestRule.onNodeWithText("Back to Lobby")
            .assertExists()
            .performClick()

        assertTrue("Callback onBackToLobby should be executed on click", backToLobbyClicked)
    }

    @Test
    fun testGameTerminatedOverlayIsHidden() {
        composeTestRule.setContent {
            GameTerminatedOverlay(
                isVisible = false,
                onBackToLobby = {}
            )
        }

        composeTestRule.onNodeWithText("Game Terminated").assertDoesNotExist()
        composeTestRule.onNodeWithText("The host has ended the game.").assertDoesNotExist()
    }

    @Test
    fun testReportButtonHiddenBeforeCurrentPlayerRolled() {
        val myPlayer = Player(id = "player1", name = "Me", money = 500, position = 0)
        val otherPlayer = Player(id = "player2", name = "Opponent", money = 1500, position = 0)

        composeTestRule.setContent {
            GameboardContent(
                fields = emptyList(),
                players = listOf(myPlayer, otherPlayer),
                currentPlayerId = "player1",
                currentTurnPlayer = otherPlayer,
                canReportCheater = true,
                gameState = GameState(
                    gameId = "game-1",
                    fields = emptyList(),
                    players = mutableListOf(otherPlayer, myPlayer),
                    currentPlayerIndex = 0
                )
            )
        }

        composeTestRule.onNodeWithText("🚨 Report")
            .assertDoesNotExist()
    }

    @Test
    fun testReportButtonEnabledForNonCurrentPlayerAfterCurrentPlayerRolled() {
        val myPlayer = Player(id = "player1", name = "Me", money = 501, position = 0)
        val otherPlayer = Player(id = "player2", name = "Opponent", money = 1500, position = 0)

        composeTestRule.setContent {
            GameboardContent(
                fields = emptyList(),
                players = listOf(myPlayer, otherPlayer),
                currentPlayerId = "player1",
                currentTurnPlayer = otherPlayer,
                canReportCheater = true,
                gameState = GameState(
                    gameId = "game-1",
                    fields = emptyList(),
                    players = mutableListOf(otherPlayer, myPlayer),
                    currentPlayerIndex = 0,
                    lastDiceRoll = DiceRoll(3, 4)
                )
            )
        }

        composeTestRule.onNodeWithText("🚨 Report")
            .assertExists()
            .assertIsEnabled()
        composeTestRule.onNodeWithTag("report_action_button_player2")
            .assertExists()
            .assertIsEnabled()
    }

    @Test
    fun testReportButtonHiddenForDeadCurrentPlayer() {
        val myPlayer = Player(id = "player1", name = "Me", money = 501, position = 0)
        val otherPlayer = Player(
            id = "player2",
            name = "Opponent",
            money = 0,
            position = 0,
            eliminated = true
        )

        composeTestRule.setContent {
            GameboardContent(
                fields = emptyList(),
                players = listOf(myPlayer, otherPlayer),
                currentPlayerId = "player1",
                currentTurnPlayer = otherPlayer,
                canReportCheater = true,
                gameState = GameState(
                    gameId = "game-1",
                    fields = emptyList(),
                    players = mutableListOf(otherPlayer, myPlayer),
                    currentPlayerIndex = 0,
                    lastDiceRoll = DiceRoll(3, 4)
                )
            )
        }

        composeTestRule.onNodeWithText("🚨 Report")
            .assertDoesNotExist()
    }

    @Test
    fun testTradeButtonOnlyVisibleForCurrentTurnAndLiveOpponent() {
        val myPlayer = Player(id = "player1", name = "Me", money = 501, position = 0)
        val otherPlayer = Player(id = "player2", name = "Opponent", money = 1500, position = 0)

        composeTestRule.setContent {
            GameboardContent(
                fields = emptyList(),
                players = listOf(myPlayer, otherPlayer),
                currentPlayerId = "player1",
                currentTurnPlayer = myPlayer,
                canStartTrade = true
            )
        }

        composeTestRule.onNodeWithText("🔁 Trade")
            .assertExists()
            .assertIsEnabled()
        composeTestRule.onNodeWithTag("trade_action_button_player2")
            .assertExists()
            .assertIsEnabled()
    }

    @Test
    fun testTradeButtonHiddenForDeadOpponent() {
        val myPlayer = Player(id = "player1", name = "Me", money = 501, position = 0)
        val otherPlayer = Player(
            id = "player2",
            name = "Opponent",
            money = 0,
            position = 0,
            eliminated = true
        )

        composeTestRule.setContent {
            GameboardContent(
                fields = emptyList(),
                players = listOf(myPlayer, otherPlayer),
                currentPlayerId = "player1",
                currentTurnPlayer = myPlayer
            )
        }

        composeTestRule.onNodeWithText("🔁 Trade")
            .assertDoesNotExist()
    }

    @Test
    fun testTradeButtonHiddenWhenNotCurrentTurn() {
        val myPlayer = Player(id = "player1", name = "Me", money = 501, position = 0)
        val otherPlayer = Player(id = "player2", name = "Opponent", money = 1500, position = 0)

        composeTestRule.setContent {
            GameboardContent(
                fields = emptyList(),
                players = listOf(myPlayer, otherPlayer),
                currentPlayerId = "player1",
                currentTurnPlayer = otherPlayer
            )
        }

        composeTestRule.onNodeWithText("🔁 Trade")
            .assertDoesNotExist()
    }

    @Test
    fun testReopenTradeButtonVisibleForActiveTrade() {
        val myPlayer = Player(id = "player1", name = "Me", money = 501, position = 0)
        val otherPlayer = Player(id = "player2", name = "Opponent", money = 1500, position = 0)
        val gameState = GameState(
            gameId = "game-1",
            fields = emptyList(),
            players = mutableListOf(myPlayer, otherPlayer),
            pendingTradeOffer = TradeOffer(
                id = "trade-1",
                fromPlayerId = "player1",
                toPlayerId = "player2",
                offerMoney = 10
            )
        )

        composeTestRule.setContent {
            GameboardContent(
                fields = emptyList(),
                players = listOf(myPlayer, otherPlayer),
                currentPlayerId = "player1",
                currentTurnPlayer = myPlayer,
                gameState = gameState
            )
        }

        composeTestRule.onNodeWithText("🔁 Reopen Trade")
            .assertExists()
            .assertIsEnabled()
        composeTestRule.onNodeWithTag("trade_action_button_player2")
            .assertExists()
            .assertIsEnabled()
    }

    @Test
    fun testCurrentFieldUsesFieldIdWhenBoardListIsSparse() {
        val myPlayer = Player(id = "player1", name = "Me", money = 1500, position = 2)

        composeTestRule.setContent {
            GameboardContent(
                fields = listOf(
                    GoField(id = 0),
                    CommunityChestField(id = 2)
                ),
                players = listOf(myPlayer),
                currentPlayerId = "player1",
                currentTurnPlayer = myPlayer
            )
        }

        composeTestRule.onNodeWithText("Community Chest").assertExists()
        composeTestRule.onNodeWithText("COMMUNITY CHEST").assertExists()
    }

    @Test
    fun testCircularRevealShapeCalculatesOutlineCorrectly() {
        val shape = CircularRevealShape(progress = 0.5f)
        val size = Size(100f, 100f)

        val outline = shape.createOutline(
            size = size,
            layoutDirection = LayoutDirection.Ltr,
            density = Density(1f)
        )

        assertNotNull(outline)
    }
}
