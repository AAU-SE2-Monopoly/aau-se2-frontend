package at.aau.serg.websocketbrokerdemo.ui

import MyStompManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow

// --- HIER SIND DIE NEUEN JUNIT 5 IMPORTS ---
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
// -------------------------------------------

class MainViewModelTest {

    private lateinit var mockStompManager: MyStompManager
    private lateinit var viewModel: MainViewModel
    private lateinit var dummyFlow: MutableSharedFlow<String>

    // WICHTIG: In JUnit 5 heißt die Annotation jetzt @BeforeEach (statt @Before)
    @BeforeEach
    fun setup() {
        mockStompManager = mockk(relaxed = true)
        dummyFlow = MutableSharedFlow()

        every { mockStompManager.responses } returns dummyFlow

        viewModel = MainViewModel(mockStompManager)
    }

    @Test
    fun connect_callsStompManagerConnect() {
        viewModel.connect()
        // Verifiziert, dass connect() genau einmal am Manager aufgerufen wurde
        verify(exactly = 1) { mockStompManager.connect() }
    }

    @Test
    fun sendHello_callsStompManagerSendHello() {
        viewModel.sendHello()
        verify(exactly = 1) { mockStompManager.sendHello() }
    }

    @Test
    fun sendJson_callsStompManagerSendJson() {
        viewModel.sendJson()
        verify(exactly = 1) { mockStompManager.sendJson() }
    }

    @Test
    fun responsesFlow_isCorrectlyExposed() {
        // Prüft, ob das ViewModel genau den Flow weitergibt, der vom Manager kommt
        assertEquals(dummyFlow, viewModel.responses)
    }
}