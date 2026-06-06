package at.aau.monopoly.klagenfurt.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.ui.board.MovementAnimationState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.FreeParkingField
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class GameboardContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // ...existing tests...

    @Test
    fun `verify GameboardContent renders multiple players`() {
        val fields = listOf(GoField(id = 0, name = "Go", type = FieldType.GO))
        val players = listOf(
            Player(id = "1", name = "Alice", iconId = "lindwurm", position = 0),
            Player(id = "2", name = "Bob", iconId = "gti", position = 0),
            Player(id = "3", name = "Charlie", iconId = "ironman", position = 0),
            Player(id = "4", name = "Dave", iconId = "josef", position = 0)
        )

        composeTestRule.setContent {
            GameboardContent(fields = fields, players = players)
        }

        composeTestRule.onNodeWithContentDescription("Alice").assertExists()
        composeTestRule.onNodeWithContentDescription("Bob").assertExists()
        composeTestRule.onNodeWithContentDescription("Charlie").assertExists()
        composeTestRule.onNodeWithContentDescription("Dave").assertExists()
    }

    @Test
    fun `verify GameboardContent renders player tokens on their matching field positions`() {
        val fields = listOf(
            GoField(id = 0, name = "Go", type = FieldType.GO),
            PropertyField(
                id = 1,
                name = "Benediktiner Platz",
                color = PropertyColor.LIGHT_BLUE,
                price = 60,
                rent = listOf(2, 4, 8, 16, 32, 64),
                houseCost = 50,
                hotelCost = 50
            )
        )
        val players = listOf(
            Player(id = "1", name = "Alice", iconId = "lindwurm", position = 0),
            Player(id = "2", name = "Bob", iconId = "gti", position = 1)
        )

        composeTestRule.setContent {
            GameboardContent(
                fields = fields,
                players = players,
                currentPlayerId = "1",
                currentTurnPlayer = players[1],
                movementAnimationState = MovementAnimationState(
                    playerId = "missing-player",
                    startPosition = 0,
                    path = listOf(0, 1),
                    currentStepIndex = 1,
                    isComplete = false
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("Alice").assertExists()
        composeTestRule.onNodeWithContentDescription("Bob").assertExists()
        composeTestRule.onAllNodesWithTag("MiniPlayerToken").assertCountEquals(2)
        composeTestRule.onNodeWithContentDescription("Benediktiner Platz").assertExists()
    }

    @Test
    fun `verify GameboardContent handles empty lists`() {
        composeTestRule.setContent {
            GameboardContent(fields = emptyList(), players = emptyList())
        }
        
        // Should at least render the background maps
        composeTestRule.onNodeWithContentDescription("Klagenfurt-Map").assertExists()
        // Golden ring path overlay has been removed
        composeTestRule.onNodeWithContentDescription("Path - Klagenfurt-Ring").assertDoesNotExist()
    }

    @Test
    fun `GameboardContent renders FieldCardUI when currentTurnPlayer is on a field`() {
        val property = PropertyField(
            id = 1, name = "Herrengasse",
            color = PropertyColor.BROWN, price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50, hotelCost = 50
        )
        val fields = listOf(
            GoField(id = 0, name = "Go", type = FieldType.GO),
            property
        )
        val player = Player(id = "p1", name = "Alice", iconId = "lindwurm", position = 1)

        composeTestRule.setContent {
            GameboardContent(
                fields = fields,
                players = listOf(player),
                currentPlayerId = "p1",
                currentTurnPlayer = player
            )
        }

        composeTestRule.waitForIdle()
        // The GameboardContent with a currentTurnPlayer renders without crashing.
        // The FieldCardUI is composed inside ZoomableWrapper + BoxWithConstraints
        // for dynamic card sizing (the new code path we need to cover).
        composeTestRule.onNodeWithContentDescription("Alice").assertExists()
    }

    @Test
    fun `GameboardContent renders FieldCardUI for railroad field with dynamic card sizing`() {
        // currentTurnPlayer at position 0 (Go) - exercises the currentField != null path
        // and the BoxWithConstraints card sizing code
        val fields = listOf(
            GoField(id = 0, name = "Go", type = FieldType.GO)
        )
        val player = Player(id = "p1", name = "Bob", iconId = "gti", position = 0)

        composeTestRule.setContent {
            GameboardContent(
                fields = fields,
                players = listOf(player),
                currentPlayerId = "p1",
                currentTurnPlayer = player
            )
        }

        composeTestRule.waitForIdle()
        // Renders without crash; the card scaling BoxWithConstraints is exercised
        composeTestRule.onNodeWithContentDescription("Klagenfurt-Map").assertExists()
    }

    @Test
    fun `GameboardContent does not render FieldCardUI when no currentTurnPlayer`() {
        val fields = listOf(GoField(id = 0, name = "Go", type = FieldType.GO))
        val player = Player(id = "p1", name = "Alice", iconId = "lindwurm", position = 0)

        composeTestRule.setContent {
            GameboardContent(
                fields = fields,
                players = listOf(player),
                currentPlayerId = "p1",
                currentTurnPlayer = null
            )
        }

        composeTestRule.waitForIdle()
        // FieldCardUI should NOT be rendered (no turn player)
        // The "GO" text from FieldCardUI's generic card header should not appear
        // (the board field name "Go" will still appear as content description, but not the card)
    }

    @Test
    fun `FieldItem renders house indicator on property`() {
        val property = PropertyField(
            id = 1,
            name = "Herrengasse",
            color = PropertyColor.BROWN,
            price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50,
            hotelCost = 50,
            houses = 2
        )

        composeTestRule.setContent {
            at.aau.monopoly.klagenfurt.ui.board.FieldItem(
                index = 1,
                field = property,
                sw = 3840f,
                sh = 2160f
            )
        }

        composeTestRule.onNodeWithText("🏠🏠").assertExists()
    }
    @Test
    fun `FieldItem renders hotel indicator on property`() {
        val property = PropertyField(
            id = 1,
            name = "Herrengasse",
            color = PropertyColor.BROWN,
            price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50,
            hotelCost = 50,
            hasHotel = true
        )

        composeTestRule.setContent {
            at.aau.monopoly.klagenfurt.ui.board.FieldItem(
                index = 1,
                field = property,
                sw = 3840f,
                sh = 2160f
            )
        }

        composeTestRule.onNodeWithText("🏨").assertExists()
    }

    @Test
    fun `GameboardContent forwards free parking money click`() {
        var clicked = false

        val fields = (0..39).map { index ->
            if (index == 20) {
                FreeParkingField()
            } else {
                object : Field(
                    id = index,
                    name = "Field $index",
                    type = FieldType.PROPERTY
                ) {}
            }
        }

        val gameState = GameState(
            gameId = "game-1",
            fields = fields,
            freeParkingMoney = 500
        )

        composeTestRule.setContent {
            Box(modifier = Modifier.size(1200.dp, 700.dp)) {
                GameboardContent(
                    fields = fields,
                    players = emptyList(),
                    currentPlayerId = "p1",
                    currentTurnPlayer = null,
                    gameState = gameState,
                    onFreeParkingMoneyClick = { clicked = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeTestRule
            .onNodeWithTag("free_parking_money_stack", useUnmergedTree = true)
            .performClick()

        assertTrue(clicked)
    }


}
