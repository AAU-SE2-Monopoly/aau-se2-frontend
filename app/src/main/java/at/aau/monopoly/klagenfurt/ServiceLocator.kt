package at.aau.monopoly.klagenfurt

import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.GameStompClient
import at.aau.monopoly.klagenfurt.ui.chat.ChatClient
import at.aau.monopoly.klagenfurt.ui.chat.ChatService
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

object ServiceLocator {

    // Normale Properties statt 'by lazy', damit wir sie in Tests überschreiben können.
    // Sichtbarkeit des Setters bleibt intern für normale Nutzung gesperrt.
    @Volatile
    private var stompClient: StompClient? = null

    @Volatile
    private var gameService: GameService? = null

    @Volatile
    private var chatService: ChatService? = null

    fun provideStompClient(): StompClient {
        // Double-Checked Locking für Thread-Sicherheit (falls es parallel aufgerufen wird)
        return stompClient ?: synchronized(this) {
            stompClient ?: StompClient(OkHttpWebSocketClient()).also { stompClient = it }
        }
    }

    fun provideGameService(): GameService {
        return gameService ?: synchronized(this) {
            gameService ?: GameStompClient(provideStompClient()).also { gameService = it }
        }
    }
    fun provideChatService(): ChatService{
        return chatService?: synchronized(this){
            chatService?: ChatClient(provideStompClient()).also{chatService=it}
        }
    }

    // --- TEST HELPERS ---
    // Diese Methoden sind essentiell, damit du MockK in Espresso verwenden kannst.

    /**
     * Injiziert einen Mock für den GameService.
     * Aufruf in der @Before Methode deines Tests.
     */
    fun injectGameServiceForTest(mock: GameService) {
        gameService = mock
    }

    /**
     * Injiziert einen Mock für den StompClient (falls benötigt).
     */
    fun injectStompClientForTest(mock: StompClient) {
        stompClient = mock
    }

    /**
     * ZWINGEND in der @After Methode jedes Tests aufrufen,
     * damit Mocks nicht in den nächsten Test leaken!
     */
    fun resetForTests() {
        stompClient = null
        gameService = null
    }
}