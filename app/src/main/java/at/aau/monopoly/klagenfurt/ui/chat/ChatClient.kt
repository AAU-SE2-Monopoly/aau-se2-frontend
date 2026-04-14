package at.aau.monopoly.klagenfurt.ui.chat

import at.aau.monopoly.klagenfurt.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.hildan.krossbow.stomp.StompClient

class ChatClient(private val stompClient: StompClient = ServiceLocator.provideStompClient(),
                 private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO+SupervisorJob())): ChatService {
    private val _messageFlow= MutableSharedFlow<String>()
    val messageFlow=_messageFlow.asSharedFlow()
    override fun sendMessage(message: String) {
        TODO("Not yet implemented")
    }

}
