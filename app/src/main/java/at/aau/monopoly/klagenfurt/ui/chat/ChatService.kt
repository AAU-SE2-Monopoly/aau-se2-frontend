package at.aau.monopoly.klagenfurt.ui.chat

import at.aau.monopoly.klagenfurt.messaging.ChatMessage
import kotlinx.coroutines.flow.SharedFlow
import org.hildan.krossbow.stomp.StompSession

interface ChatService {
    val messageFlow: SharedFlow<String>

    fun subscribeToChat(gameId: String,playerName:String)
    fun sendMessage(message: String)
    fun setSession(session: StompSession)

}
