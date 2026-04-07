package at.aau.serg.websocketbrokerdemo.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.serg.websocketbrokerdemo.messaging.GameEvent
import at.aau.serg.websocketbrokerdemo.networking.GameService
import at.aau.serg.websocketdemoserver.model.GameState
import at.aau.serg.websocketdemoserver.model.field.Field
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class GameViewModel(private val gameService: GameService) : ViewModel() {
    private val gson = Gson()

    private val gameEventFlow: SharedFlow<GameEvent> = gameService.events
        .mapNotNull { json ->
            try {
                val jsonObj = JSONObject(json)
                val eventType = jsonObj.optString("event")
                val gameId = jsonObj.optString("gameId")
                val message = jsonObj.optString("message", null)

                val gameStateJson = jsonObj.optJSONObject("gameState")
                val parsedState: GameState? = gameStateJson?.let {
                    // Explicitly tell Gson to parse this part as GameState
                    gson.fromJson(it.toString(), GameState::class.java)
                }

                GameEvent(gameId, eventType, parsedState, message)
            } catch (e: Exception) {
                Log.e("GameViewModel", "Fehler beim Parsen: ${e.message}", e)
                null // Return null so mapNotNull skips this event
            }
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))

    val gameState: StateFlow<GameState?> = gameEventFlow
        .map { it.gameState as? GameState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val fields: StateFlow<List<Field>?> = gameState
        .map { it?.fields }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)



    val events: SharedFlow<String> = gameService.events
    val status: SharedFlow<String> = gameService.status

    fun connect() = gameService.connect()
    
    fun createGame(playerName: String) = gameService.createGame(playerName)
    
    fun joinGame(gameId: String, playerName: String) = gameService.joinGame(gameId, playerName)
    
    fun startGame() = gameService.startGame()
    
    fun rollDice() = gameService.rollDice()
    
    fun endTurn() = gameService.endTurn()
    
    fun requestState() = gameService.requestState()


    
    fun setGameId(gameId: String) = gameService.setGameId(gameId)

    // Factory to create ViewModel with dependencies
    class Factory(private val gameService: GameService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(gameService) as T
        }
    }
}
