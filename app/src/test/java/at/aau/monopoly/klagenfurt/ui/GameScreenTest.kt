package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class GameboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun buyPropertyButton_shouldBeVisibleAndClickable() {

        val fakeService = FakeGameService()

        fakeService.currentGameId = "game-1"
        fakeService.currentPlayerId = "player-1"

        val propertyField = PropertyField(
            id = 1,
            name = "Herrengasse",
            color = PropertyColor.BROWN,
            price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50,
            hotelCost = 50
        )

        val player = Player(
            id = "player-1",
            name = "Alice",
            position = 1
        )

        val gameState = GameState(
            gameId = "game-1",
            players = mutableListOf(player),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            fields = listOf(
                GoField(0),
                propertyField
            )
        )

        fakeService.emitGameState(gameState)

        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel)
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("buy_property_button")
            .assertExists()

        composeTestRule
            .onNodeWithTag("buy_property_button")
            .performClick()

        assertEquals(1, fakeService.lastBoughtFieldId)
    }
}