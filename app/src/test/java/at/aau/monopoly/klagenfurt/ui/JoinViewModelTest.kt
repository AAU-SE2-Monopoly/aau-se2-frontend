package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.FakeGameService
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

@OptIn(ExperimentalCoroutinesApi::class)
class JoinViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeService: FakeGameService
    private lateinit var viewModel: JoinViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeService = FakeGameService()
        viewModel = JoinViewModel(fakeService)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createGame sets error when disconnected and does not call service`() = runTest(testDispatcher) {
        fakeService.setConnectionState(false)

        viewModel.createGame("Alice", "lindwurm")
        advanceUntilIdle()

        assertTrue(viewModel.joinState.value is JoinViewModel.JoinState.Error)
        assertEquals(0, fakeService.createGameCalls)
    }
}

