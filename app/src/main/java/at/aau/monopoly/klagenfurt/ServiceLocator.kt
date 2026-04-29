package at.aau.monopoly.klagenfurt

import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.GameStompClient
import at.aau.monopoly.klagenfurt.networking.ServerConfig
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

object ServiceLocator {

    // Normal properties instead of 'by lazy', so we can override them in tests.
    // Setter visibility remains internally locked for normal usage.
    @Volatile
    private var stompClient: StompClient? = null

    @Volatile
    private var gameService: GameService? = null


    fun provideStompClient(): StompClient {
        // Double-Checked Locking for thread safety (in case of parallel calls)
        return stompClient ?: synchronized(this) {
            stompClient ?: StompClient(OkHttpWebSocketClient()).also { stompClient = it }
        }
    }

    fun provideGameService(): GameService {
        return gameService ?: synchronized(this) {
            gameService ?: GameStompClient(provideStompClient(), websocketUri = ServerConfig.websocketUri).also { gameService = it }
        }
    }

    /**
     * Disconnects the current GameService and clears the cached instance.
     * Call this when the server configuration changes so the next
     * [provideGameService] call creates a fresh client with the new URI.
     */
    fun resetGameService() {
        synchronized(this) {
            gameService?.disconnect()
            gameService = null
        }
    }



    // --- TEST HELPERS ---
    // These methods are essential so you can use MockK in Espresso tests.

    /**
     * Injects a mock for the GameService.
     * Call in the @Before method of your test.
     */
    fun injectGameServiceForTest(mock: GameService) {
        gameService = mock
    }

    /**
     * Injects a mock for the StompClient (if needed).
     */
    fun injectStompClientForTest(mock: StompClient) {
        stompClient = mock
    }

    /**
     * MUST be called in the @After method of every test,
     * so mocks don't leak into the next test!
     */
    fun resetForTests() {
        stompClient = null
        gameService = null

    }
}