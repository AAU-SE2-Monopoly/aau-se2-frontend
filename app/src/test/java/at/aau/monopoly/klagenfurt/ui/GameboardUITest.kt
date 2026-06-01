package at.aau.monopoly.klagenfurt.ui

import android.content.pm.ActivityInfo
import android.view.KeyEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.ChanceField
import at.aau.monopoly.klagenfurt.model.field.CommunityChestField
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.ui.board.calculateFieldBounds
import at.aau.monopoly.klagenfurt.ui.board.getFieldImageMapping
import at.aau.monopoly.klagenfurt.ui.util.getPlayerTokenResource
import at.aau.monopoly.klagenfurt.ui.util.toComposeColor
import at.aau.monopoly.klagenfurt.ui.zoom.ZoomState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import at.aau.monopoly.klagenfurt.model.field.PropertyField

@RunWith(AndroidJUnit4::class)
class GameboardUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<GameboardUI>()

    @Test
    fun `verify GameboardScreen is called in onCreate`() {
        composeTestRule
            .onNodeWithContentDescription("Klagenfurt-Map")
            .assertExists()
    }


    @Test
    fun testToComposeColor() {
        assertEquals(Color(0xFF955436), PropertyColor.BROWN.toComposeColor())
        assertEquals(Color(0xFFAAE0FA), PropertyColor.LIGHT_BLUE.toComposeColor())
        assertEquals(Color(0xFFD93A96), PropertyColor.PINK.toComposeColor())
        assertEquals(Color(0xFFF7941D), PropertyColor.ORANGE.toComposeColor())
        assertEquals(Color(0xFFED1B24), PropertyColor.RED.toComposeColor())
        assertEquals(Color(0xFFD4A017), PropertyColor.YELLOW.toComposeColor())
        assertEquals(Color(0xFF1FB25A), PropertyColor.GREEN.toComposeColor())
        assertEquals(Color(0xFF0072BB), PropertyColor.DARK_BLUE.toComposeColor())
    }

    @Test
    fun testZoomStateLogic() {
        val state = ZoomState()
        val containerSize = Size(1000f, 1000f)

        // Test zoom in
        state.updateTransformation(Offset.Zero, 2f, containerSize)
        assertEquals(2f, state.scale)
        assertEquals(Offset.Zero, state.offset)

        // Test pan within bounds (maxX = 1000 * (2-1) / 2 = 500)
        state.updateTransformation(Offset(100f, 100f), 1f, containerSize)
        assertEquals(100f, state.offset.x)
        assertEquals(100f, state.offset.y)

        // Test pan out of bounds (should coerce)
        state.updateTransformation(Offset(1000f, 1000f), 1f, containerSize)
        assertEquals(500f, state.offset.x)
        assertEquals(500f, state.offset.y)

        // Test reset on zoom out to 1.0
        state.updateTransformation(Offset.Zero, 0.1f, containerSize)
        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun testCalculateFieldBoundsCorners() {
        // Full HD scale (design size)
        val sw = 3840f
        val sh = 2160f

        // Corner 0 (Go)
        val b0 = calculateFieldBounds(0, sw, sh)
        assertTrue(b0.isCorner)
        assertEquals(0f, b0.rotation)
        assertEquals(2520f - 120f, b0.x, 0.1f)
        assertEquals(1680f - 120f, b0.y, 0.1f)

        // Corner 10 (Jail)
        val b10 = calculateFieldBounds(10, sw, sh)
        assertEquals(0f, b10.rotation)

        // Corner 20 (Free Parking)
        val b20 = calculateFieldBounds(20, sw, sh)
        assertEquals(0f, b20.rotation)

        // Corner 30 (Go To Jail)
        val b30 = calculateFieldBounds(30, sw, sh)
        assertEquals(0f, b30.rotation)
    }

    @Test
    fun testCalculateFieldBoundsSideCalculations() {
        val sw = 3840f
        val sh = 2160f

        // Side 0 (Bottom) - Field 1
        val b1 = calculateFieldBounds(1, sw, sh)
        assertFalse(b1.isCorner)
        assertEquals(0f, b1.rotation)

        // Side 1 (Left) - Field 11
        val b11 = calculateFieldBounds(11, sw, sh)
        assertEquals(90f, b11.rotation)

        // Side 2 (Top) - Field 21
        val b21 = calculateFieldBounds(21, sw, sh)
        assertEquals(180f, b21.rotation)

        // Side 3 (Right) - Field 31
        val b31 = calculateFieldBounds(31, sw, sh)
        assertEquals(270f, b31.rotation)
    }

    @Test
    fun testScalingIndependence() {
        // Test that bounds scale linearly with screen size
        val b1_large = calculateFieldBounds(1, 3840f, 2160f)
        val b1_small = calculateFieldBounds(1, 1920f, 1080f)

        assertEquals(b1_large.x / 2f, b1_small.x, 0.1f)
        assertEquals(b1_large.width / 2f, b1_small.width, 0.1f)
    }

    @Test
    fun testGetFieldImageMapping() {
        assertEquals(com.example.myapplication.R.drawable.corners_go_field, getFieldImageMapping("Go"))
        assertEquals(com.example.myapplication.R.drawable.herrengasse, getFieldImageMapping("Herrengasse"))
        assertEquals(com.example.myapplication.R.drawable.taxes, getFieldImageMapping("Reichensteuer"))
        assertNull(getFieldImageMapping("NonExistentField"))
        // Test trim
        assertEquals(com.example.myapplication.R.drawable.corners_go_field, getFieldImageMapping(" Go "))
    }

    @Test
    fun testGetPlayerTokenResource() {
        assertEquals(com.example.myapplication.R.drawable.lindwurm, getPlayerTokenResource("lindwurm"))
        assertEquals(com.example.myapplication.R.drawable.woertherseemandl, getPlayerTokenResource("woerthersee"))
        assertEquals(com.example.myapplication.R.drawable.gti, getPlayerTokenResource("gti"))
        assertEquals(com.example.myapplication.R.drawable.ironman, getPlayerTokenResource("ironman"))
        assertEquals(com.example.myapplication.R.drawable.josef, getPlayerTokenResource("josef"))
        assertEquals(com.example.myapplication.R.drawable.lindwurm, getPlayerTokenResource("unknown"))
    }

    @Test
    fun testCalculateFieldBoundsAllSides() {
        val sw = 3840f
        val sh = 2160f

        // Bottom side (0-9)
        val b5 = calculateFieldBounds(5, sw, sh)
        assertEquals(0f, b5.rotation)

        // Left side (10-19)
        val b15 = calculateFieldBounds(15, sw, sh)
        assertEquals(90f, b15.rotation)

        // Top side (20-29)
        val b25 = calculateFieldBounds(25, sw, sh)
        assertEquals(180f, b25.rotation)

        // Right side (30-39)
        val b35 = calculateFieldBounds(35, sw, sh)
        assertEquals(270f, b35.rotation)
    }

    @Test
    fun testOnKeyDown_interceptsVolumeUp() {
        // Hole die laufende Activity-Instanz aus der Compose-Rule
        val activity = composeTestRule.activity

        // 1. Teste die Volume Up Taste (sollte abgefangen werden -> return true)
        val volumeUpEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        val resultUp = activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP, volumeUpEvent)

        assertTrue("Volume Up sollte abgefangen werden (true)", resultUp)

        // 2. Teste eine andere Taste, z.B. Volume Down (sollte an super weitergereicht werden -> i.d.R. return false)
        val volumeDownEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)
        val resultDown = activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN, volumeDownEvent)

        assertFalse("Andere Tasten sollten nicht abgefangen werden (false)", resultDown)
    }

    @Test
    fun testZoomStateDoesNotGoBelowMinimumScale() {
        val state = ZoomState(initialScale = 1f)
        val containerSize = Size(1000f, 1000f)

        state.updateTransformation(
            pan = Offset(200f, 200f),
            zoom = 0.2f,
            containerSize = containerSize
        )

        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun testZoomStateDoesNotGoAboveMaximumScale() {
        val state = ZoomState(initialScale = 4f)
        val containerSize = Size(1000f, 1000f)

        state.updateTransformation(
            pan = Offset.Zero,
            zoom = 10f,
            containerSize = containerSize
        )

        assertEquals(5f, state.scale)
    }

    @Test
    fun testZoomStateNegativePanIsClamped() {
        val state = ZoomState(initialScale = 2f)
        val containerSize = Size(1000f, 1000f)

        state.updateTransformation(
            pan = Offset(-1000f, -1000f),
            zoom = 1f,
            containerSize = containerSize
        )

        assertEquals(-500f, state.offset.x)
        assertEquals(-500f, state.offset.y)
    }

    @Test
    fun testZoomStateInitialValues() {
        val state = ZoomState(
            initialScale = 3f,
            initialOffset = Offset(50f, -40f)
        )

        assertEquals(3f, state.scale)
        assertEquals(50f, state.offset.x)
        assertEquals(-40f, state.offset.y)
    }

    @Test
    fun testPlayerTokenResourceIsCaseInsensitive() {
        assertEquals(
            com.example.myapplication.R.drawable.lindwurm,
            getPlayerTokenResource("LINDWURM")
        )

        assertEquals(
            com.example.myapplication.R.drawable.gti,
            getPlayerTokenResource("GTI")
        )
    }

    @Test
    fun testUnknownPlayerTokenFallsBackToLindwurmForBlankString() {
        assertEquals(
            com.example.myapplication.R.drawable.lindwurm,
            getPlayerTokenResource("")
        )

        assertEquals(
            com.example.myapplication.R.drawable.lindwurm,
            getPlayerTokenResource("   ")
        )
    }


    @Test
    fun testCalculateFieldBoundsTextSizeForCorner() {
        val bounds = calculateFieldBounds(0, 3840f, 2160f)

        assertTrue(bounds.isCorner)
        assertEquals(bounds.width, bounds.textWidth, 0.1f)
        assertEquals(bounds.height, bounds.textHeight, 0.1f)
    }

    @Test
    fun testCalculateFieldBoundsTextSizeForHorizontalField() {
        val bounds = calculateFieldBounds(1, 3840f, 2160f)

        assertFalse(bounds.isCorner)
        assertEquals(bounds.width, bounds.textWidth, 0.1f)
        assertEquals(bounds.height, bounds.textHeight, 0.1f)
    }

    @Test
    fun testCalculateFieldBoundsTextSizeForVerticalField() {
        val bounds = calculateFieldBounds(11, 3840f, 2160f)

        assertFalse(bounds.isCorner)
        assertEquals(bounds.height, bounds.textWidth, 0.1f)
        assertEquals(bounds.width, bounds.textHeight, 0.1f)
    }

    @Test
    fun testCalculateFieldBoundsForLastField() {
        val bounds = calculateFieldBounds(39, 3840f, 2160f)

        assertFalse(bounds.isCorner)
        assertEquals(270f, bounds.rotation)
        assertTrue(bounds.width > 0f)
        assertTrue(bounds.height > 0f)
    }

    @Test
    fun testCalculateFieldBoundsForEveryBoardIndexHasPositiveSize() {
        for (index in 0 until 40) {
            val bounds = calculateFieldBounds(index, 3840f, 2160f)

            assertTrue("width should be positive for index $index", bounds.width > 0f)
            assertTrue("height should be positive for index $index", bounds.height > 0f)
            assertTrue("textWidth should be positive for index $index", bounds.textWidth > 0f)
            assertTrue("textHeight should be positive for index $index", bounds.textHeight > 0f)
        }
    }

    @Test
    fun testGameboardUICreationWithGameId() {
        val activity = composeTestRule.activity
        assertNotNull("Activity should be created", activity)
    }

    @Test
    fun testZoomStatePanningPreservesScaleLevel() {
        val state = ZoomState()
        val containerSize = Size(1000f, 1000f)

        // Zoom to 2x
        state.updateTransformation(Offset.Zero, 2f, containerSize)
        val initialScale = state.scale

        // Pan without changing zoom
        state.updateTransformation(Offset(100f, 100f), 1f, containerSize)

        // Scale should remain unchanged
        assertEquals(initialScale, state.scale)
        assertEquals(100f, state.offset.x)
        assertEquals(100f, state.offset.y)
    }

    @Test
    fun testCalculateFieldBoundsBottomSide() {
        val bounds = calculateFieldBounds(3, 3840f, 2160f)
        assertEquals(0f, bounds.rotation)
        assertTrue("Should be on bottom side", bounds.x > 0f && bounds.x < 3840f)
    }

    @Test
    fun testCalculateFieldBoundsTopSide() {
        val bounds = calculateFieldBounds(23, 3840f, 2160f)
        assertEquals(180f, bounds.rotation)
    }

    @Test
    fun testCalculateFieldBoundsRightSide() {
        val bounds = calculateFieldBounds(33, 3840f, 2160f)
        assertEquals(270f, bounds.rotation)
    }

    @Test
    fun testCalculateFieldBoundsLeftSide() {
        val bounds = calculateFieldBounds(13, 3840f, 2160f)
        assertEquals(90f, bounds.rotation)
    }

    @Test
    fun testGetFieldImageMappingReturnsNullForNonExistent() {
        assertNull(getFieldImageMapping("ThisFieldDoesNotExist123"))
        assertNull(getFieldImageMapping(""))
    }

    @Test
    fun testGetPlayerTokenResourceHandlesWhitespace() {
        val resourceWithSpaces = getPlayerTokenResource("  lindwurm  ")
        val resourceWithoutSpaces = getPlayerTokenResource("lindwurm")
        assertEquals(resourceWithoutSpaces, resourceWithSpaces)
    }

    @Test
    fun testCalculateFieldBoundsScalingEffectsAllFields() {
        val sw1 = 1920f
        val sh1 = 1080f
        val sw2 = 3840f
        val sh2 = 2160f

        val bounds1 = calculateFieldBounds(1, sw1, sh1)
        val bounds2 = calculateFieldBounds(1, sw2, sh2)

        // Double resolution should result in double size
        assertEquals(bounds1.x * 2f, bounds2.x, 0.1f)
        assertEquals(bounds1.y * 2f, bounds2.y, 0.1f)
    }

    @Test
    fun testFieldImageMappingIsConsistent() {
        val mapping1 = getFieldImageMapping("Go")
        val mapping2 = getFieldImageMapping("Go")
        assertEquals(mapping1, mapping2)
    }

    @Test
    fun testPlayerTokenResourceIsConsistent() {
        val token1 = getPlayerTokenResource("lindwurm")
        val token2 = getPlayerTokenResource("lindwurm")
        assertEquals(token1, token2)
    }

    @Test
    fun testCalculateFieldBoundsFieldsAreInBoardBounds() {
        for (index in 0 until 40) {
            val bounds = calculateFieldBounds(index, 3840f, 2160f)

            assertTrue("Field $index x position should be within bounds", bounds.x >= 0f && bounds.x <= 3840f)
            assertTrue("Field $index y position should be within bounds", bounds.y >= 0f && bounds.y <= 2160f)
        }
    }

    @Test
    fun testPlayerTokenResourceHandlesAllTokenTypes() {
        val tokenTypes = listOf("lindwurm", "woerthersee", "gti", "ironman", "josef")
        for (type in tokenTypes) {
            val token = getPlayerTokenResource(type)
            assertTrue("Token resource should exist for $type", token > 0)
        }
    }

    @Test
    fun testCalculateFieldBoundsCornerFieldsHaveConsistentSize() {
        val corner0 = calculateFieldBounds(0, 3840f, 2160f)
        val corner10 = calculateFieldBounds(10, 3840f, 2160f)
        val corner20 = calculateFieldBounds(20, 3840f, 2160f)
        val corner30 = calculateFieldBounds(30, 3840f, 2160f)

        // All corners should be marked as corners
        assertTrue(corner0.isCorner)
        assertTrue(corner10.isCorner)
        assertTrue(corner20.isCorner)
        assertTrue(corner30.isCorner)

        // Corner rotations should be 0 (aligned with board)
        assertEquals(0f, corner0.rotation)
        assertEquals(0f, corner10.rotation)
        assertEquals(0f, corner20.rotation)
        assertEquals(0f, corner30.rotation)
    }

    @Test
    fun testGetFieldImageMappingAllFields() {
        // Test that we can map several important fields
        val fieldNames = listOf(
            "Go", "Herrengasse", "Heiligengeistplatz", "Neuer Platz",
            "Hauptbahnhof", "Chance", "Community Chest"
        )

        for (fieldName in fieldNames) {
            val hasMapping = getFieldImageMapping(fieldName) != null
            assertTrue("Field $fieldName should have a mapping", hasMapping)
        }
    }

    @Test
    fun testPlayerTokenResourceFallbackBehavior() {
        // Test that unknown tokens fall back to default
        val unknownToken = getPlayerTokenResource("unknown_token_xyz")
        val defaultToken = getPlayerTokenResource("lindwurm")
        assertEquals("Unknown tokens should fall back to default", unknownToken, defaultToken)
    }

    @Test
    fun testCalculateFieldBoundsResolutionIndependence() {
        // Test that calculations scale properly with different resolutions
        val smallResolution = calculateFieldBounds(5, 1920f, 1080f)
        val largeResolution = calculateFieldBounds(5, 3840f, 2160f)

        // Large resolution should be exactly 2x of small resolution
        assertEquals(smallResolution.x * 2f, largeResolution.x, 0.1f)
        assertEquals(smallResolution.y * 2f, largeResolution.y, 0.1f)
        assertEquals(smallResolution.width * 2f, largeResolution.width, 0.1f)
        assertEquals(smallResolution.height * 2f, largeResolution.height, 0.1f)
    }

    @Test
    fun testPropertyColorAllColorsMap() {
        val colors = listOf(
            PropertyColor.BROWN,
            PropertyColor.LIGHT_BLUE,
            PropertyColor.PINK,
            PropertyColor.ORANGE,
            PropertyColor.RED,
            PropertyColor.YELLOW,
            PropertyColor.GREEN,
            PropertyColor.DARK_BLUE
        )

        for (color in colors) {
            val composeColor = color.toComposeColor()
            assertNotNull("Color $color should map to a Compose color", composeColor)
        }
    }

    @Test
    fun testZoomStateResetOnZoomOut() {
        val state = ZoomState(initialScale = 2f, initialOffset = Offset(100f, 100f))
        val containerSize = Size(1000f, 1000f)

        // Should reset offset when zooming back to 1.0
        state.updateTransformation(Offset(50f, 50f), 0.5f, containerSize)
        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun testGameboardUIActivityIsLandscapeLocked() {
        val activity = composeTestRule.activity

        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            activity.requestedOrientation
        )
    }

    @Test
    fun testGameboardUIShowsBackgroundAndPathImages() {
        composeTestRule
            .onNodeWithContentDescription("Klagenfurt-Map")
            .assertExists()

        composeTestRule
            .onNodeWithContentDescription("Path - Klagenfurt-Ring")
            .assertExists()
    }

    @Test
    fun testBuyPropertyButtonIsNotVisibleInitially() {
        composeTestRule
            .onNodeWithTag("buy_property_button")
            .assertDoesNotExist()
    }

}

@RunWith(AndroidJUnit4::class)
class GameboardScreenCoverageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createMockViewModel(
        canStart: Boolean = false,
        isGameReadyValue: Boolean = true,
        isRollingPhase: Boolean = false,
        isBuyingPhase: Boolean = false,
        canEndTurn: Boolean = false,
        currentPlayerId: String = "player1",
        players: List<Player> = emptyList(),
        fields: List<Field> = emptyList()

    ): GameViewModel {
        val vm = mockk<GameViewModel>(relaxed = true)
        val gameState = GameState(
            gameId = "test_game",
            fields = fields,
            players = players.toMutableList(),
            currentPlayerIndex = players.indexOfFirst { it.id == currentPlayerId }.takeIf { it >= 0 } ?: 0
        )


        every { vm.fields } returns MutableStateFlow(fields)
        every { vm.gameState } returns MutableStateFlow(gameState)
        every { vm.currentPlayerId } returns currentPlayerId
        every { vm.eventLog } returns MutableStateFlow(emptyList())
        every { vm.isRollingPhaseForCurrentPlayer } returns MutableStateFlow(isRollingPhase)
        every { vm.isBuyingPhaseForCurrentPlayer } returns MutableStateFlow(isBuyingPhase)
        every { vm.canEndTurnForCurrentPlayer } returns MutableStateFlow(isBuyingPhase)
        every { vm.lastDiceRoll } returns MutableStateFlow(null)
        every { vm.currentActionCard } returns MutableStateFlow(null)
        every { vm.isExecutingAction } returns MutableStateFlow(false)
        every { vm.showActionCardOverlay } returns MutableStateFlow(false)
        every { vm.selectedPlayerForOverlay } returns MutableStateFlow(null)
        every { vm.movementAnimation } returns MutableStateFlow(null)
        every { vm.buildingActionPending } returns MutableStateFlow(false)
        every { vm.pendingDoubleAutoEnd } returns MutableStateFlow(false)


        every { vm.canStartGame } returns MutableStateFlow(canStart)
        every { vm.isGameReady } returns MutableStateFlow(isGameReadyValue)
        every { vm.showDiceOverlayForCurrentPlayer } returns MutableStateFlow(false)
        every { vm.diceResultForCurrentPlayer } returns MutableStateFlow(null)
        every { vm.errorMessage } returns MutableStateFlow(null)
        every { vm.canEndTurnForCurrentPlayer } returns MutableStateFlow(canEndTurn)


        every { vm.showPayRentOverlay } returns MutableStateFlow(false)
        every { vm.showMortgageOverlay } returns MutableStateFlow(false)
        every { vm.showBankruptcyOverlay } returns MutableStateFlow(false)
        every { vm.canPayRent } returns MutableStateFlow(false)
        every { vm.canRaiseFunds } returns MutableStateFlow(false)
        every { vm.currentRentAmount } returns MutableStateFlow(0)
        every { vm.currentRentOwnerId } returns MutableStateFlow(null)
        every { vm.currentRentFieldId } returns MutableStateFlow(null)
        every { vm.manageableProperties } returns MutableStateFlow(emptyList())
        every { vm.paymentActionInFlight } returns MutableStateFlow(false)
        every { vm.propertyActionInFlight } returns MutableStateFlow(false)
        every { vm.bankruptcyPlayerName } returns MutableStateFlow("")
        every { vm.bankruptcyTotalAssets } returns MutableStateFlow(0)
        every { vm.bankruptcyTotalDebt } returns MutableStateFlow(0)
        every { vm.bankruptcyPropertiesOwned } returns MutableStateFlow(emptyList())
        every { vm.hasPendingPayment } returns MutableStateFlow(false)

        return vm
    }

    @Test
    fun testStartGameButton() {
        val mockVm = createMockViewModel(canStart = true, isGameReadyValue = false)

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithText("Start Game").performClick()
        verify { mockVm.startGame() }
    }

    @Test
    fun testChanceFieldButton() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            players = listOf(player),
            fields = listOf(ChanceField(id = 0, name = "Chance"))
        )

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithText("🎰 Draw Chance").performClick()
        verify { mockVm.drawCard("CHANCE") }
    }

    @Test
    fun testCommunityChestFieldButton() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            players = listOf(player),
            fields = listOf(CommunityChestField(id = 0, name = "Community Chest"))
        )

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithText("⭐ Draw Community").performClick()
        verify { mockVm.drawCard("COMMUNITY_CHEST") }
    }

    @Test
    fun testDrawChanceCardButtonShowsOnChanceField() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            players = listOf(player),
            fields = listOf(ChanceField(id = 0, name = "Chance"))
        )

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithText("🎰 Draw Chance").assertExists()
    }

    @Test
    fun testEndTurnShowsOnCommunityChestField() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = true,
            players = listOf(player),
            fields = listOf(CommunityChestField(id = 0, name = "Community Chest"))
        )

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithTag("end_turn_button").assertExists()
    }

    @Test
    fun testEndTurnShowsOnChanceField() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = true,
            players = listOf(player),
            fields = listOf(ChanceField(id = 0, name = "Chance"))
        )

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithTag("end_turn_button").assertExists()
    }

    @Test
    fun testEndTurnVisibleOnCommunityChestField() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = true,
            players = listOf(player),
            fields = listOf(CommunityChestField(id = 0, name = "Community Chest"))
        )

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithTag("end_turn_button").assertExists()
    }

    @Test
    fun testDrawCommunityChestCardButtonShowsOnCommunityChestField() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            players = listOf(player),
            fields = listOf(CommunityChestField(id = 0, name = "Community Chest"))
        )

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithText("⭐ Draw Community").assertExists()
    }

    @Test
    fun testJailLogic() {
        val jailedPlayer = Player(
            id = "player1",
            name = "P1",
            position = 10,
            inJail = true,
            jailTurns = 1,
            money = 100,
            getOutOfJailCards = 1
        )

        val mockVm = createMockViewModel(
            isRollingPhase = true,
            players = listOf(jailedPlayer)
        )

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithText("Im Gefängnis (Versuch 2/3)").assertExists()

        composeTestRule.onNodeWithTag("pay_jail_fine_button").performClick()
        verify { mockVm.payJailFine() }

        composeTestRule.onNodeWithTag("use_jail_card_button").performClick()
        verify { mockVm.useJailCard() }

        composeTestRule.onNodeWithTag("roll_dice_button").assertExists()
    }

    @Test
    fun testShakeButtonTriggersRollDice() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isRollingPhase = true,
            players = listOf(player),
            fields = listOf(GoField(id = 0, name = "Go"))
        )

        every { mockVm.isRollingPhaseForCurrentPlayer } returns MutableStateFlow(true)

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.waitForIdle()

        // Click the "Roll Dice" button to open the dice overlay (sets showOverlay = true)
        composeTestRule.onNodeWithTag("roll_dice_button").performClick()
        composeTestRule.waitForIdle()

        // The shake button should now be visible in the dice overlay
        composeTestRule.onNodeWithTag("shake_button").assertExists()
        composeTestRule.onNodeWithTag("shake_button").performClick()

        verify { mockVm.rollDice() }
    }

    @Test
    fun testManageBuildingsButtonVisibleForCompleteColorSet() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val property1 = PropertyField(
            id = 1,
            name = "Property 1",
            color = PropertyColor.BROWN,
            price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50,
            hotelCost = 50,
            ownerId = "player1"
        )

        val property2 = PropertyField(
            id = 2,
            name = "Property 2",
            color = PropertyColor.BROWN,
            price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50,
            hotelCost = 50,
            ownerId = "player1"
        )

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = true,
            currentPlayerId = "player1",
            players = listOf(player),
            fields = listOf(property1, property2)
        )

        composeTestRule.setContent {
            GameboardScreen(viewModel = mockVm)
        }

        composeTestRule
            .onNodeWithTag("manage_buildings_button")
            .assertExists()
    }

    @Test
    fun testBuildingManagerOverlayOpens() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val property1 = PropertyField(
            id = 1,
            name = "Property 1",
            color = PropertyColor.BROWN,
            price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50,
            hotelCost = 50,
            ownerId = "player1"

        )

        val property2 = PropertyField(
            id = 2,
            name = "Property 2",
            color = PropertyColor.BROWN,
            price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50,
            hotelCost = 50,
            ownerId = "player1"
        )

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = true,
            currentPlayerId = "player1",
            players = listOf(player),
            fields = listOf(property1, property2)
        )

        composeTestRule.setContent {
            GameboardScreen(viewModel = mockVm)
        }

        composeTestRule
            .onNodeWithTag("manage_buildings_button")
            .performClick()

        composeTestRule
            .onNodeWithText("🏗️ Manage Buildings")
            .assertExists()
    }

    @Test
    fun testBuyHouseButtonCallsViewModel() {
        val property1 = PropertyField(
            id = 1,
            name = "Property 1",
            color = PropertyColor.BROWN,
            price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50,
            hotelCost = 50,
            ownerId = "player1"
        )

        val property2 = PropertyField(
            id = 2,
            name = "Property 2",
            color = PropertyColor.BROWN,
            price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50,
            hotelCost = 50,
            ownerId = "player1"
        )

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = true,
            currentPlayerId = "player1",
            players = listOf(Player(id = "player1", name = "P1")),
            fields = listOf(property1, property2)
        )

        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property1, property2),
                onBuyHouse = { mockVm.buyHouse(it) },
                onBuyHotel = { mockVm.buyHotel(it) },
                onSellHouse = { mockVm.sellHouse(it) },
                onSellHotel = { mockVm.sellHotel(it) },
                onDismiss = {},
                isBuildingActionPending = false

            )
        }

        composeTestRule
            .onAllNodesWithText("Buy 🏠")[0]
            .performClick()

        verify { mockVm.buyHouse(property1.id) }
    }

    @Test
    fun `PayRentOverlay visible for current player`() {
        composeTestRule.setContent {
            PayRentOverlay(
                isVisible = true,
                rentAmount = 200,
                ownerName = "Bob",
                fieldName = "Herrengasse",
                currentMoney = 500,
                canPay = true,
                canRaiseFunds = true,
                paymentInFlight = false,
                propertyInFlight = false,
                onPay = {},
                onManageProperties = {},
                onDeclareBankruptcy = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("RENT DUE").assertExists()
        composeTestRule.onNodeWithText("Pay Rent").assertExists()
    }

    @Test
    fun `PayRentOverlay not visible when isVisible false`() {
        composeTestRule.setContent {
            PayRentOverlay(
                isVisible = false,
                rentAmount = 200,
                ownerName = "Bob",
                fieldName = "Herrengasse",
                currentMoney = 500,
                canPay = true,
                canRaiseFunds = true,
                paymentInFlight = false,
                propertyInFlight = false,
                onPay = {},
                onManageProperties = {},
                onDeclareBankruptcy = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("RENT DUE").assertDoesNotExist()
    }

    @Test
    fun `MortgageManagementOverlay visible when isVisible true`() {
        val props = listOf(
            ManageableProperty(
                fieldId = 1, name = "Herrengasse", color = "brown",
                price = 60, mortgageValue = 30, unmortgageCost = 33,
                houses = 0, hasHotel = false, isMortgaged = false,
                houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
            )
        )

        composeTestRule.setContent {
            MortgageManagementOverlay(
                isVisible = true,
                properties = props,
                currentMoney = 500,
                actionInFlight = false,
                onMortgage = {},
                onUnmortgage = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Property Management").assertExists()
    }

    @Test
    fun `BankruptcyResolutionOverlay visible for current player`() {
        composeTestRule.setContent {
            BankruptcyResolutionOverlay(
                isVisible = true,
                playerName = "Alice",
                totalAssets = 50,
                totalDebt = 400,
                propertiesOwned = 1,
                onConfirm = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("BANKRUPTCY").assertExists()
        composeTestRule.onNodeWithText("Confirm Bankruptcy").assertExists()
    }

    @Test
    fun `Pay Rent reopen button visible when hasPendingPayment and overlay closed`() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = false,
            currentPlayerId = "player1",
            players = listOf(player)
        )

        every { mockVm.hasPendingPayment } returns MutableStateFlow(true)
        every { mockVm.showPayRentOverlay } returns MutableStateFlow(false)
        every { mockVm.currentRentAmount } returns MutableStateFlow(100)
        every { mockVm.currentRentOwnerId } returns MutableStateFlow("p2")
        every { mockVm.currentRentFieldId } returns MutableStateFlow(1)
        every { mockVm.canEndTurnForCurrentPlayer } returns MutableStateFlow(false)

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithTag("pay_rent_reopen_button").assertExists()
    }

    @Test
    fun `Manage Properties button visible during current player turn`() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = true,
            currentPlayerId = "player1",
            players = listOf(player)
        )

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithTag("manage_properties_button").assertExists()
    }

    @Test
    fun `Manage Properties button not visible when not current player turn`() {
        val player1 = Player(id = "player1", name = "P1", position = 0)
        val player2 = Player(id = "player2", name = "P2", position = 1)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = true,
            currentPlayerId = "player1",
            players = listOf(player1, player2)
        )

        val modifiedState = mockVm.gameState.value!!.apply { currentPlayerIndex = 1 }
        every { mockVm.gameState } returns MutableStateFlow(modifiedState)

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithTag("manage_properties_button").assertDoesNotExist()
    }

    @Test
    fun `Manage Properties button hidden for eliminated player`() {
        val player = Player(id = "player1", name = "P1", position = 0, eliminated = true)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = true,
            currentPlayerId = "player1",
            players = listOf(player)
        )

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithTag("manage_properties_button").assertDoesNotExist()
    }

    @Test
    fun `Manage Properties visible during ROLLING phase`() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isRollingPhase = true,
            isBuyingPhase = false,
            canEndTurn = false,
            currentPlayerId = "player1",
            players = listOf(player)
        )

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithTag("manage_properties_button").assertExists()
    }

    @Test
    fun `during PAYING_RENT with overlay dismissed end turn blocked but manage visible`() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = false,
            currentPlayerId = "player1",
            players = listOf(player)
        )

        every { mockVm.hasPendingPayment } returns MutableStateFlow(true)
        every { mockVm.showPayRentOverlay } returns MutableStateFlow(false)
        every { mockVm.currentRentAmount } returns MutableStateFlow(100)
        every { mockVm.currentRentOwnerId } returns MutableStateFlow("p2")
        every { mockVm.currentRentFieldId } returns MutableStateFlow(1)
        every { mockVm.canEndTurnForCurrentPlayer } returns MutableStateFlow(false)

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithTag("pay_rent_reopen_button").assertExists()
        composeTestRule.onNodeWithTag("manage_properties_button").assertExists()
        composeTestRule.onNodeWithTag("end_turn_button").assertDoesNotExist()
    }

    @Test
    fun `BankruptcyResolutionOverlay shows confirm button and summary`() {
        composeTestRule.setContent {
            BankruptcyResolutionOverlay(
                isVisible = true,
                playerName = "Alice",
                totalAssets = 50,
                totalDebt = 400,
                propertiesOwned = 1,
                onConfirm = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Confirm Bankruptcy").assertExists()
        composeTestRule.onNodeWithText("BANKRUPTCY").assertExists()
    }

    @Test
    fun `End Turn button disabled while hasPendingPayment is true`() {
        val player = Player(id = "player1", name = "P1", position = 0)

        val mockVm = createMockViewModel(
            isBuyingPhase = true,
            canEndTurn = false,
            currentPlayerId = "player1",
            players = listOf(player)
        )

        every { mockVm.hasPendingPayment } returns MutableStateFlow(true)
        every { mockVm.showPayRentOverlay } returns MutableStateFlow(false)
        every { mockVm.canEndTurnForCurrentPlayer } returns MutableStateFlow(false)

        composeTestRule.setContent { GameboardScreen(viewModel = mockVm) }

        composeTestRule.onNodeWithTag("end_turn_button").assertDoesNotExist()
    }

    @Test
    fun `PayRentOverlay Pay Rent button enabled when canPay is true`() {
        composeTestRule.setContent {
            PayRentOverlay(
                isVisible = true, rentAmount = 200, ownerName = "Bob",
                fieldName = "Herrengasse", currentMoney = 500,
                canPay = true, canRaiseFunds = true,
                paymentInFlight = false, propertyInFlight = false,
                onPay = {}, onManageProperties = {}, onDeclareBankruptcy = {}, onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Pay Rent").assertExists()
    }

    @Test
    fun `PayRentOverlay Pay Rent button shows Insufficient Funds when canPay is false`() {
        composeTestRule.setContent {
            PayRentOverlay(
                isVisible = true, rentAmount = 200, ownerName = "Bob",
                fieldName = "Herrengasse", currentMoney = 50,
                canPay = false, canRaiseFunds = true,
                paymentInFlight = false, propertyInFlight = false,
                onPay = {}, onManageProperties = {}, onDeclareBankruptcy = {}, onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Insufficient Funds").assertExists()
    }

    @Test
    fun `PayRentOverlay shows Processing when paymentInFlight ignores canPay`() {
        composeTestRule.setContent {
            PayRentOverlay(
                isVisible = true, rentAmount = 200, ownerName = "Bob",
                fieldName = "Herrengasse", currentMoney = 500,
                canPay = true, canRaiseFunds = true,
                paymentInFlight = true, propertyInFlight = false,
                onPay = {}, onManageProperties = {}, onDeclareBankruptcy = {}, onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Processing...").assertExists()
    }

    @Test
    fun `PayRentOverlay shows Processing when canPay is false and paymentInFlight is true`() {
        composeTestRule.setContent {
            PayRentOverlay(
                isVisible = true, rentAmount = 500, ownerName = "Bob",
                fieldName = "Herrengasse", currentMoney = 50,
                canPay = false, canRaiseFunds = false,
                paymentInFlight = true, propertyInFlight = false,
                onPay = {}, onManageProperties = {}, onDeclareBankruptcy = {}, onDismiss = {}
            )
        }

        composeTestRule.onAllNodesWithText("Processing...").assertCountEquals(2)
    }

    @Test
    fun `PayRentOverlay shows Declare Bankruptcy button when canRaiseFunds is false`() {
        composeTestRule.setContent {
            PayRentOverlay(
                isVisible = true, rentAmount = 500, ownerName = "Bob",
                fieldName = "Herrengasse", currentMoney = 50,
                canPay = false, canRaiseFunds = false,
                paymentInFlight = false, propertyInFlight = false,
                onPay = {}, onManageProperties = {}, onDeclareBankruptcy = {}, onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Declare Bankruptcy").assertExists()
    }

    @Test
    fun `PayRentOverlay hides Declare Bankruptcy button when canRaiseFunds is true`() {
        composeTestRule.setContent {
            PayRentOverlay(
                isVisible = true, rentAmount = 200, ownerName = "Bob",
                fieldName = "Herrengasse", currentMoney = 1500,
                canPay = true, canRaiseFunds = true,
                paymentInFlight = false, propertyInFlight = false,
                onPay = {}, onManageProperties = {}, onDeclareBankruptcy = {}, onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Declare Bankruptcy").assertDoesNotExist()
    }

    @Test
    fun `PayRentOverlay shows Manage Properties button regardless of canRaiseFunds`() {
        composeTestRule.setContent {
            PayRentOverlay(
                isVisible = true, rentAmount = 200, ownerName = "Bob",
                fieldName = "Herrengasse", currentMoney = 500,
                canPay = true, canRaiseFunds = true,
                paymentInFlight = false, propertyInFlight = false,
                onPay = {}, onManageProperties = {}, onDeclareBankruptcy = {}, onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Manage Properties").assertExists()
    }

    // ─── MortgageManagementContent Sell House / Sell Hotel button state ───────

    private fun testSellableProperty(
        houses: Int = 1,
        hasHotel: Boolean = false,
        canSellHouse: Boolean = true,
        canSellHotel: Boolean = true
    ): ManageableProperty = ManageableProperty(
        fieldId = 1, name = "TestProp", color = "brown",
        price = 60, mortgageValue = 30, unmortgageCost = 33,
        houses = houses, hasHotel = hasHotel,
        isMortgaged = false, houseCost = 50, hotelCost = 50,
        sellHouseValue = 25, sellHotelValue = 25,
        canSellHouse = canSellHouse, canSellHotel = canSellHotel
    )

    @Test
    fun `Sell House button disabled when even-building rule violated`() {
        val prop = testSellableProperty(canSellHouse = false)

        composeTestRule.setContent {
            MortgageManagementContent(
                properties = listOf(prop),
                currentMoney = 500,
                actionInFlight = false,
                onMortgage = {},
                onUnmortgage = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("TestProp").performClick()
        composeTestRule.onNodeWithText("Sell House").assertExists()
        composeTestRule.onNodeWithText("Sell House").assertIsNotEnabled()
    }

    @Test
    fun `Sell House button enabled when even-building rule satisfied`() {
        val prop = testSellableProperty(canSellHouse = true)

        composeTestRule.setContent {
            MortgageManagementContent(
                properties = listOf(prop),
                currentMoney = 500,
                actionInFlight = false,
                onMortgage = {},
                onUnmortgage = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("TestProp").performClick()
        composeTestRule.onNodeWithText("Sell House").assertExists()
        composeTestRule.onNodeWithText("Sell House").assertIsEnabled()
    }

    @Test
    fun `Sell Hotel button disabled when even-building rule violated`() {
        val prop = testSellableProperty(hasHotel = true, canSellHotel = false)

        composeTestRule.setContent {
            MortgageManagementContent(
                properties = listOf(prop),
                currentMoney = 500,
                actionInFlight = false,
                onMortgage = {},
                onUnmortgage = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("TestProp").performClick()
        composeTestRule.onNodeWithText("Sell Hotel").assertExists()
        composeTestRule.onNodeWithText("Sell Hotel").assertIsNotEnabled()
    }

    @Test
    fun `Sell Hotel button enabled when even-building rule satisfied`() {
        val prop = testSellableProperty(hasHotel = true, canSellHotel = true)

        composeTestRule.setContent {
            MortgageManagementContent(
                properties = listOf(prop),
                currentMoney = 500,
                actionInFlight = false,
                onMortgage = {},
                onUnmortgage = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("TestProp").performClick()
        composeTestRule.onNodeWithText("Sell Hotel").assertExists()
        composeTestRule.onNodeWithText("Sell Hotel").assertIsEnabled()
    }

    @Test
    fun `Sell House re-enables reactively when even-building rule becomes satisfied`() {
        val propsState = mutableStateOf(
            listOf(testSellableProperty(canSellHouse = false))
        )

        composeTestRule.setContent {
            MortgageManagementContent(
                properties = propsState.value,
                currentMoney = 500,
                actionInFlight = false,
                onMortgage = {},
                onUnmortgage = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("TestProp").performClick()
        composeTestRule.onNodeWithText("Sell House").assertIsNotEnabled()

        propsState.value = listOf(testSellableProperty(canSellHouse = true))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sell House").assertIsEnabled()
    }

    @Test
    fun `Sell Hotel re-enables reactively when even-building rule becomes satisfied`() {
        val propsState = mutableStateOf(
            listOf(testSellableProperty(hasHotel = true, canSellHotel = false))
        )

        composeTestRule.setContent {
            MortgageManagementContent(
                properties = propsState.value,
                currentMoney = 500,
                actionInFlight = false,
                onMortgage = {},
                onUnmortgage = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("TestProp").performClick()
        composeTestRule.onNodeWithText("Sell Hotel").assertIsNotEnabled()

        propsState.value = listOf(testSellableProperty(hasHotel = true, canSellHotel = true))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sell Hotel").assertIsEnabled()
    }
}