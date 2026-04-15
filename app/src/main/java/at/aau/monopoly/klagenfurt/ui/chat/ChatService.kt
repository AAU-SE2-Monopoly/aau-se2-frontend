package at.aau.monopoly.klagenfurt.ui.chat

import at.aau.monopoly.klagenfurt.messaging.ChatMessage
import kotlinx.coroutines.flow.SharedFlow

interface ChatService {
    val messageFlow: SharedFlow<ChatMessage>

    fun subscribeToChat(gameId: String)
    fun sendMessage(message: String)
    fun disconnectChatroom()
}
