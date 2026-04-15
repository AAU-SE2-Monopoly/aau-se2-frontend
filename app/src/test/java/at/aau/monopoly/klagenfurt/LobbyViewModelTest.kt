package at.aau.monopoly.klagenfurt

import at.aau.monopoly.klagenfurt.ui.LobbyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeService: FakeGameService
    private lateinit var viewModel: LobbyViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeService = FakeGameService()
        viewModel = LobbyViewModel(fakeService)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls connect on gameService`() {
        assertTrue(fakeService.connectCalled)
    }

    @Test
    fun `currentPlayerId delegates to gameService`() {
        assertEquals("test-player-id", viewModel.currentPlayerId)
    }

    @Test
    fun `isConnected is false initially`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(false, viewModel.isConnected.value)
    }

    @Test
    fun `isConnected becomes true when status emits Connected`() = runTest(testDispatcher) {
        val values = mutableListOf<Boolean>()
        val job = launch { viewModel.isConnected.collect { values.add(it) } }
        advanceUntilIdle()

        fakeService.emitTestStatus("Connected ✓")
        advanceUntilIdle()

        assertTrue(values.contains(true))
        job.cancel()
    }

    @Test
    fun `isConnected stays false when status emits non-connected message`() = runTest(testDispatcher) {
        val job = launch { viewModel.isConnected.collect {} }
        advanceUntilIdle()

        fakeService.emitTestStatus("Connection error: timeout")
        advanceUntilIdle()
        assertEquals(false, viewModel.isConnected.value)
        job.cancel()
    }

    @Test
    fun `games is empty initially`() {
        assertEquals(emptyList<Any>(), viewModel.games.value)
    }

    @Test
    fun `games updates when lobby event is received`() = runTest(testDispatcher) {
        val lobbyJson = """
        {
            "event": "LOBBY_UPDATE",
            "games": [
                {
                    "gameId": "g1",
                    "hostPlayerName": "Alice",
                    "hostPlayerId": "p1",
                    "playerCount": 1,
                    "maxPlayers": 4,
                    "phase": "WAITING"
                }
            ]
        }
        """.trimIndent()

        fakeService.emitTestLobbyEvent(lobbyJson)
        advanceUntilIdle()

        val games = viewModel.games.value
        assertEquals(1, games.size)
        assertEquals("g1", games[0].gameId)
        assertEquals("Alice", games[0].hostPlayerName)
        assertEquals(1, games[0].playerCount)
    }

    @Test
    fun `games updates to empty when empty lobby event received`() = runTest(testDispatcher) {
        // First populate
        val lobbyJson1 = """{"event":"LOBBY_UPDATE","games":[{"gameId":"g1","hostPlayerName":"A","hostPlayerId":"p","playerCount":1,"maxPlayers":4,"phase":"WAITING"}]}"""
        fakeService.emitTestLobbyEvent(lobbyJson1)
        advanceUntilIdle()
        assertEquals(1, viewModel.games.value.size)

        // Then empty
        val lobbyJson2 = """{"event":"LOBBY_UPDATE","games":[]}"""
        fakeService.emitTestLobbyEvent(lobbyJson2)
        advanceUntilIdle()
        assertEquals(0, viewModel.games.value.size)
    }

    @Test
    fun `onConnected calls subscribeToLobby and requestGameList`() {
        viewModel.onConnected()
        assertTrue(fakeService.subscribeToLobbyCalled)
        assertTrue(fakeService.requestGameListCalled)
    }

    @Test
    fun `createGame delegates to gameService`() {
        viewModel.createGame("TestPlayer")
        assertEquals(1, fakeService.createGameCalls)
        assertEquals("TestPlayer", fakeService.lastCreatedPlayerName)
    }

    @Test
    fun `closeGame delegates to gameService`() {
        viewModel.closeGame("game-123")
        assertEquals(1, fakeService.closeGameCalls)
        assertEquals("game-123", fakeService.lastClosedGameId)
    }

    @Test
    fun `createdGameId is null initially`() {
        assertNull(viewModel.createdGameId.value)
    }

    @Test
    fun `createdGameId updates when GAME_CREATED event received`() = runTest(testDispatcher) {
        val gameEventJson = """{"event":"GAME_CREATED","gameId":"new-game-id"}"""
        fakeService.emitTestEvent(gameEventJson)
        advanceUntilIdle()
        assertEquals("new-game-id", viewModel.createdGameId.value)
    }

    @Test
    fun `clearCreatedGameId resets to null`() = runTest(testDispatcher) {
        val gameEventJson = """{"event":"GAME_CREATED","gameId":"new-game-id"}"""
        fakeService.emitTestEvent(gameEventJson)
        advanceUntilIdle()
        assertEquals("new-game-id", viewModel.createdGameId.value)

        viewModel.clearCreatedGameId()
        assertNull(viewModel.createdGameId.value)
    }

    @Test
    fun `createGame resets createdGameId before creating`() = runTest(testDispatcher) {
        val gameEventJson = """{"event":"GAME_CREATED","gameId":"old-id"}"""
        fakeService.emitTestEvent(gameEventJson)
        advanceUntilIdle()
        assertEquals("old-id", viewModel.createdGameId.value)

        viewModel.createGame("NewPlayer")
        assertNull(viewModel.createdGameId.value)
    }

    @Test
    fun `invalid lobby JSON does not crash`() = runTest(testDispatcher) {
        fakeService.emitTestLobbyEvent("not valid json {{{")
        advanceUntilIdle()
        // Should still have empty games, not crash
        assertEquals(emptyList<Any>(), viewModel.games.value)
    }

    @Test
    fun `invalid game event JSON does not crash`() = runTest(testDispatcher) {
        fakeService.emitTestEvent("not valid json {{{")
        advanceUntilIdle()
        // Should not crash, createdGameId stays null
        assertNull(viewModel.createdGameId.value)
    }
}






