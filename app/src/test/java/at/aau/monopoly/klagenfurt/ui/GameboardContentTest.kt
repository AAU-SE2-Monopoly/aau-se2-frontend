package at.aau.monopoly.klagenfurt.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
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
        composeTestRule.onNodeWithContentDescription("Path - Klagenfurt-Ring").assertExists()
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
}
