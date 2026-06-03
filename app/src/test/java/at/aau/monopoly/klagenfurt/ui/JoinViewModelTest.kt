package at.aau.monopoly.klagenfurt.ui

import android.util.Log
import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.collections.emptyList

@OptIn(ExperimentalCoroutinesApi::class)
class JoinViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeService: FakeGameService
    private lateinit var viewModel: JoinViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0


        fakeService = spyk(FakeGameService())
        viewModel = JoinViewModel(fakeService)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `createGame sets error when disconnected and does not call service`() = runTest(testDispatcher) {
        fakeService.setConnectionState(false)

        viewModel.createGame("Alice", "lindwurm")
        advanceUntilIdle()

        val state = viewModel.joinState.value
        assertTrue(state is JoinViewModel.JoinState.Error)
        assertEquals("Not connected to server. Please wait…", (state as JoinViewModel.JoinState.Error).message)
        assertEquals(0, fakeService.createGameCalls)
    }

    @Test
    fun `joinGame sets error when disconnected`() = runTest(testDispatcher) {
        fakeService.setConnectionState(false)

        viewModel.joinGame("game1", "Bob", "auto")
        advanceUntilIdle()

        val state = viewModel.joinState.value
        assertTrue(state is JoinViewModel.JoinState.Error)
        assertEquals("Not connected to server. Please wait…", (state as JoinViewModel.JoinState.Error).message)
    }

    @Test
    fun `createGame sets error when service returns null`() = runTest(testDispatcher) {
        fakeService.setConnectionState(true)

        // Da dein Fake immer einen String zurückgibt, erzwingen wir hier für diesen
        // spezifischen Test die Rückgabe von null.
        coEvery { fakeService.createGame(any(), any()) } returns null

        viewModel.createGame("Alice", "lindwurm")
        advanceUntilIdle()

        val state = viewModel.joinState.value
        assertTrue(state is JoinViewModel.JoinState.Error)
        assertEquals("Failed to create game – no response from server", (state as JoinViewModel.JoinState.Error).message)
    }

    @Test
    fun `joinGame logs warning and sets error state on failure`() = runTest(testDispatcher) {
        fakeService.setConnectionState(true)

        // Nutzt die Eigenschaft, die bereits in deinem FakeGameService existiert
        fakeService.joinGameSuccess = false

        viewModel.joinGame("game1", "Bob", "auto")
        advanceUntilIdle()

        val state = viewModel.joinState.value
        assertTrue(state is JoinViewModel.JoinState.Error)

        // In deinem FakeGameService ist "Join rejected by server" hardcodiert
        assertEquals("Join rejected by server", (state as JoinViewModel.JoinState.Error).message)

        verify { Log.w("JoinViewModel", "Join rejected: Join rejected by server") }
    }

    @Test
    fun `observeGame parses valid GameEvent JSON and updates takenIcons`() = runTest(testDispatcher) {
        val gameId = "game1"
        viewModel.observeGame(gameId)
        advanceUntilIdle()


        val testGameState = GameState(
            gameId = gameId,
            fields = emptyList(),
            players = mutableListOf(
                Player(id = "p1", name = "Alice", iconId = "hund"),
                Player(id = "p2", name = "Bob", iconId = "auto")
            )
        )

        fakeService.emitGameState(testGameState)
        advanceUntilIdle()

        val taken = viewModel.takenIcons.value
        assertEquals(setOf("hund", "auto"), taken)
    }
}