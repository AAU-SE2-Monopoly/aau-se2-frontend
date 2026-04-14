package at.aau.monopoly.klagenfurt.ui.chat

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.json.JSONObject


class ChatClient(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO+SupervisorJob())): ChatService {


    private var session: StompSession? = null
    private val _messageFlow= MutableSharedFlow<String>()

    override val messageFlow=_messageFlow.asSharedFlow()



    override fun setSession(session: StompSession) {
        this.session = session
        Log.i("ChatClient", "Session injected!")
    }

    override fun subscribeToChat(gameId: String,playerName:String) {
        val currentSession = session
        scope.launch {
            try {

                    currentSession?.subscribeText("/topic/chat/$gameId")
                    ?.collect {
                        _messageFlow.emit(it)
                    }
                    try {
                        sendRaw("/app/chat/first",playerName)
                    }
                    catch (e: Throwable) {
                        Log.e("GameStomp", "send error to /app/chat/first: playerName not set", e)
                    }


                }
            catch (e: Throwable) {
                Log.e("GameStomp", "subscription to chat error", e)


            }
        }

    }
    override fun sendMessage(message: String) {
        val messageJson=JSONObject()
            .put("message",message).toString()
        sendRaw("/app/chat/send",message)
    }
    fun sendRaw(destination: String, json: String) {
        scope.launch {
            try {
                val currentSession = session
                if (currentSession != null) {
                    currentSession.sendText(destination, json)
                } else {
                    Log.w("GameStomp", "Cannot send message: not connected")
                }
            } catch (e: Throwable) {
                Log.e("GameStomp", "send error to $destination", e)

                }
        }
    }
}
