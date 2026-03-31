package at.aau.serg.websocketbrokerdemo

import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
object StompClientProvider {
    val client by lazy{ StompClient(OkHttpWebSocketClient())
    }

}