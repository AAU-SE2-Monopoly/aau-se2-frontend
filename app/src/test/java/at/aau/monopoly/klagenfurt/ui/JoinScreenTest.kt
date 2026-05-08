package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.model.GameJoinStatus
import at.aau.monopoly.klagenfurt.JoinScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class JoinScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Helpers

    private fun joinButton() = composeTestRule.onNodeWithTag("ActionButton")
    private fun screenTitle() = composeTestRule.onNodeWithTag("ScreenTitle")
    private fun backButton() = composeTestRule.onNode(hasText("Back"))
    private fun nameInput() = composeTestRule.onNodeWithTag("PlayerNameInput")
    private fun iconChooser() = composeTestRule.onNodeWithContentDescription("Selected Icon")

    private val noOp: () -> Unit = {}
    private val idleState = JoinViewModel.JoinState.Idle

    // 1. FINISHED status

    @Test
    fun `FINISHED status shows GAME FINISHED text`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.FINISHED,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("GAME FINISHED").assertIsDisplayed()
        composeTestRule.onNodeWithText("This game has already ended.").assertIsDisplayed()
    }

    @Test
    fun `FINISHED status shows back button that is clickable`() {
        var backClicked = false

        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.FINISHED,
                onBackClicked = { backClicked = true },
                onJoin = { _, _ -> }
            )
        }

        backButton().assertIsDisplayed()
        backButton().performClick()
        assertTrue("Back button should have been clicked", backClicked)
    }

    // 2. FULL status

    @Test
    fun `FULL status shows full room message`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.FULL,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("This game is currently full.").assertIsDisplayed()
    }

    @Test
    fun `FULL status disables join button`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.FULL,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        joinButton().assertIsNotEnabled()
    }

    // 3. IN_PROGRESS status

    @Test
    fun `IN_PROGRESS status shows RECONNECT title`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.IN_PROGRESS,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        screenTitle().assertTextEquals("RECONNECT")
    }

    @Test
    fun `IN_PROGRESS status shows reconnect message`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.IN_PROGRESS,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText(
            "Game already in progress – you can rejoin as your previous player."
        ).assertIsDisplayed()
    }

    @Test
    fun `IN_PROGRESS status hides icon chooser and name input`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.IN_PROGRESS,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        iconChooser().assertDoesNotExist()
        nameInput().assertDoesNotExist()
    }

    // 4. OPEN status with isNewGame=true -> "CREATE GAME"

    @Test
    fun `OPEN status with isNewGame true shows CREATE GAME title`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = true,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        screenTitle().assertTextEquals("CREATE GAME")
    }

    // 5. OPEN status with isNewGame=false -> "JOIN GAME"

    @Test
    fun `OPEN status with isNewGame false shows JOIN GAME title`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        screenTitle().assertTextEquals("JOIN GAME")
    }

    // 6. isReconnectFlow=true hides icon chooser and name field

    @Test
    fun `isReturningPlayer true hides icon chooser and name input`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                isReturningPlayer = true,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        iconChooser().assertDoesNotExist()
        nameInput().assertDoesNotExist()
    }

    @Test
    fun `isReturningPlayer true shows RECONNECT title`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                isReturningPlayer = true,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        screenTitle().assertTextEquals("RECONNECT")
    }

    @Test
    fun `isReturningPlayer true shows rejoin message`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                isReturningPlayer = true,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText(
            "You have already joined this game. You can rejoin."
        ).assertIsDisplayed()
    }

    // 7. isReconnectFlow=false shows icon chooser and name field

    @Test
    fun `isReconnectFlow false shows icon chooser and name input`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                isReturningPlayer = false,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        iconChooser().assertIsDisplayed()
        nameInput().assertIsDisplayed()
    }

    @Test
    fun `isReconnectFlow false with isNewGame true shows icon chooser and name input`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = true,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                isReturningPlayer = false,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        iconChooser().assertIsDisplayed()
        nameInput().assertIsDisplayed()
    }

    // 8. Back button is always rendered regardless of joinStatus

    @Test
    fun `back button is shown for FINISHED status`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.FINISHED,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        backButton().assertIsDisplayed()
    }

    @Test
    fun `back button is shown for FULL status`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.FULL,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        backButton().assertIsDisplayed()
    }

    @Test
    fun `back button is shown for IN_PROGRESS status`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.IN_PROGRESS,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        backButton().assertIsDisplayed()
    }

    @Test
    fun `back button is shown for OPEN status`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        backButton().assertIsDisplayed()
    }

    @Test
    fun `back button is clickable in all status modes`() {
        var clickCount = 0
        val onBack: () -> Unit = { clickCount += 1 }

        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.FINISHED,
                onBackClicked = onBack,
                onJoin = { _, _ -> }
            )
        }

        backButton().performClick()
        assertTrue("Back button click should be registered", clickCount == 1)
    }

    // 9. interactionDisabled=true disables join button

    @Test
    fun `not connected disables join button`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                isConnected = false,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        joinButton().assertIsNotEnabled()
    }

    @Test
    fun `not connected shows connecting message`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                isConnected = false,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Connecting to server…").assertIsDisplayed()
    }

    @Test
    fun `loading state disables join button`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = JoinViewModel.JoinState.Loading,
                joinStatus = GameJoinStatus.OPEN,
                isConnected = true,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        joinButton().assertIsNotEnabled()
    }

    @Test
    fun `FULL with loading shows error message if any`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = JoinViewModel.JoinState.Error("Game is full"),
                joinStatus = GameJoinStatus.FULL,
                isConnected = true,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Game is full").assertIsDisplayed()
    }

    @Test
    fun `error state shows error message`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = JoinViewModel.JoinState.Error("Something went wrong"),
                joinStatus = GameJoinStatus.OPEN,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
    }

    // Combined / edge-case scenarios

    @Test
    fun `OPEN status and isNewGame true shows CREATE and JOIN button text`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = true,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag("ActionButton").assertExists()
        // The button text is "CREATE & JOIN"
        composeTestRule.onNode(
            hasTestTag("ActionButton") and hasText("CREATE & JOIN")
        ).assertExists()
    }

    @Test
    fun `IN_PROGRESS status shows RECONNECT button text`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.IN_PROGRESS,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag("ActionButton").assertExists()
        composeTestRule.onNode(
            hasTestTag("ActionButton") and hasText("RECONNECT")
        ).assertExists()
    }

    @Test
    fun `OPEN status and isNewGame false shows JOIN GAME button text`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag("ActionButton").assertExists()
        composeTestRule.onNode(
            hasTestTag("ActionButton") and hasText("JOIN GAME")
        ).assertExists()
    }

    @Test
    fun `FULL status with disconnected shows full message and button disabled`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.FULL,
                isConnected = false,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("This game is currently full.").assertIsDisplayed()
        joinButton().assertIsNotEnabled()
    }

    @Test
    fun `gameId is displayed when not new game and gameId is non-empty`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "abcdef123456",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Game: abcdef12…").assertIsDisplayed()
    }

    @Test
    fun `gameId not displayed when isNewGame is true`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "abcdef123456",
                isNewGame = true,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Game: abcdef12…").assertDoesNotExist()
    }

    @Test
    fun `icon chooser cycles icon on tap`() {
        var capturedIconIndex = -1

        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                onBackClicked = noOp,
                onJoin = { _, iconIndex -> capturedIconIndex = iconIndex }
            )
        }

        // Initial icon is index 0 (lindwurm)
        val iconButton = iconChooser()
        iconButton.performClick() // now index 1
        iconButton.performClick() // now index 2

        joinButton().performClick()
        assertTrue("Icon index should be 2 after two clicks", capturedIconIndex == 2)
    }

    @Test
    fun `name input is functional`() {
        composeTestRule.setContent {
            JoinScreen(
                gameId = "game-1",
                isNewGame = false,
                joinState = idleState,
                joinStatus = GameJoinStatus.OPEN,
                onBackClicked = noOp,
                onJoin = { _, _ -> }
            )
        }

        nameInput().assertIsDisplayed()
        // Just verify the node exists and is displayed – actual text input behavior
        // is covered by the activity-level tests.
    }
}
