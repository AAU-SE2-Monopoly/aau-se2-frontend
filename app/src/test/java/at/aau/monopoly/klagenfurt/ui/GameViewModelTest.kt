package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.networking.GameService
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue

class GameViewModelTest {

    private lateinit var gameService: GameService
    private lateinit var viewModel: GameViewModel

    @Before
    fun setup() {
        // Wir mocken den GameService, da wir nur die Overlay-Logik testen wollen
        gameService = mockk(relaxed = true)
        viewModel = GameViewModel(gameService)
    }

    @Test
    fun `initial selectedPlayerForOverlay state should be null`() {
        assertNull(viewModel.selectedPlayerForOverlay.value)
    }

    @Test
    fun `showPlayerOverlay should update state with correct player`() {
        // Arrange
        val testPlayer = Player(id = "p1", name = "Spieler 1")

        // Act
        viewModel.showPlayerOverlay(testPlayer)

        // Assert
        assertEquals(testPlayer, viewModel.selectedPlayerForOverlay.value)
        assertEquals("Spieler 1", viewModel.selectedPlayerForOverlay.value?.name)
    }

    @Test
    fun `hidePlayerOverlay should reset state to null`() {
        // Arrange
        val testPlayer = Player(id = "p1", name = "Spieler 1")
        viewModel.showPlayerOverlay(testPlayer) // Zuerst setzen

        // Act
        viewModel.hidePlayerOverlay()

        // Assert
        assertNull(viewModel.selectedPlayerForOverlay.value)
    }
    @Test
    fun `connect should call gameService connect`() {
        viewModel.connect()
        verify(exactly = 1) { gameService.connect() }
    }

    @Test
    fun `createGame should call gameService createGame`() {
        val playerName = "Lukas"
        viewModel.createGame(playerName)
        verify(exactly = 1) { gameService.createGame(playerName) }
    }

    @Test
    fun `joinGame should call gameService joinGame`() {
        val gameId = "game123"
        val playerName = "Lukas"
        viewModel.joinGame(gameId, playerName)
        verify(exactly = 1) { gameService.joinGame(gameId, playerName) }
    }

    @Test
    fun `startGame should call gameService startGame`() {
        viewModel.startGame()
        verify(exactly = 1) { gameService.startGame() }
    }

    @Test
    fun `rollDice should call gameService rollDice`() {
        viewModel.rollDice()
        verify(exactly = 1) { gameService.rollDice() }
    }

    @Test
    fun `endTurn should call gameService endTurn`() {
        viewModel.endTurn()
        verify(exactly = 1) { gameService.endTurn() }
    }

    @Test
    fun `requestState should call gameService requestState`() {
        viewModel.requestState()
        verify(exactly = 1) { gameService.requestState() }
    }

    @Test
    fun `setGameId should call gameService setGameId`() {
        val gameId = "game123"
        viewModel.setGameId(gameId)
        verify(exactly = 1) { gameService.setGameId(gameId) }
    }

    // --- FACTORY TEST ---

    @Test
    fun `Factory creates GameViewModel successfully`() {
        val factory = GameViewModel.Factory(gameService)
        val createdViewModel = factory.create(GameViewModel::class.java)

        assertTrue(createdViewModel is GameViewModel)
    }
}