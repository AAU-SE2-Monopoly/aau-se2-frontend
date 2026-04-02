package at.aau.serg.websocketbrokerdemo

import at.aau.serg.websocketbrokerdemo.networking.GameService
import at.aau.serg.websocketbrokerdemo.networking.GameStompClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

object ServiceLocator {
    
    private val stompClient by lazy {
        StompClient(OkHttpWebSocketClient())
    }

    fun provideStompClient(): StompClient = stompClient

    private var gameService: GameService? = null

    fun provideGameService(): GameService {
        synchronized(this) {
            return gameService ?: createGameService()
        }
    }

    private fun createGameService(): GameService {
        val newService = GameStompClient(stompClient)
        gameService = newService
        return newService
    }

    /** ONLY USE FOR TESTING **/
    fun injectFakeGameService(fake: GameService) {
        gameService = fake
    }

    /** Resets the locator state. Call in @After methods. **/
    fun reset() {
        gameService = null
    }
}
