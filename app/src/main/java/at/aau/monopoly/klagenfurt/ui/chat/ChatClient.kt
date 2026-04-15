package at.aau.monopoly.klagenfurt.ui.chat

import android.util.Log
import at.aau.monopoly.klagenfurt.messaging.ChatMessage
import at.aau.monopoly.klagenfurt.networking.GameService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.json.JSONObject

class ChatClient(
    private val gameService: GameService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : ChatService {

    private val _messageFlow = MutableSharedFlow<ChatMessage>(replay = 10)
    override val messageFlow = _messageFlow.asSharedFlow()
    
    private var subscriptionJob: Job? = null
    private val _hasJoinedFlow = MutableStateFlow<Boolean>(false)
    val hasJoinedFlow = _hasJoinedFlow.asStateFlow()

    override fun subscribeToChat(gameId: String) {
        subscriptionJob?.cancel()
        subscriptionJob = scope.launch {
            // Use collectLatest to ensure that when a new session is established (reconnect),
            // the previous session's collection and logic are automatically cancelled.
            gameService.sessionFlow.collectLatest { session ->
                if (session != null) {
                    try {
                        Log.d("ChatClient", "Session available, subscribing to chat: $gameId")
                        
                        // Subscribe to incoming chat messages
                        launch {
                            session.subscribeText("/topic/chat/$gameId").collect { msg ->
                                try {
                                    val chatMsg = ChatMessage.fromJson(JSONObject(msg))
                                    _messageFlow.emit(chatMsg)
                                } catch (e: Exception) {
                                    Log.e("ChatClient", "Error parsing chat message: $msg", e)
                                }
                            }
                        }

                        val playerName = gameService.getCurrentPlayerName()
                        session.sendText("/app/chat/first", playerName)
                        _hasJoinedFlow.value = true
                    } catch (e: Exception) {
                        Log.e("ChatClient", "Error in chat subscription", e)
                        _hasJoinedFlow.value = false
                    }
                } else {
                    _hasJoinedFlow.value = false
                }
            }
        }
    }

    override fun sendMessage(message: String) {
        val session = gameService.sessionFlow.value
        val gameId = gameService.getGameId()
        
        if (session == null) {
            Log.w("ChatClient", "Cannot send message: not connected")
            return
        }

        scope.launch {
            try {
                val messageJson = JSONObject()
                    .put("gameId", gameId)
                    .put("playerId", gameService.getCurrentUserId())
                    .put("message", message)
                    .toString()
                session.sendText("/app/chat/send", messageJson)
            } catch (e: Exception) {
                Log.e("ChatClient", "Error sending message", e)
            }
        }
    }

    override fun disconnectChatroom() {
        subscriptionJob?.cancel()
        _hasJoinedFlow.value = false
    }
}
