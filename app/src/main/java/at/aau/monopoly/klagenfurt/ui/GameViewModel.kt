package at.aau.monopoly.klagenfurt.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.networking.GameService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import org.json.JSONObject
import kotlinx.coroutines.flow.SharedFlow

class GameViewModel(private val gameService: GameService) : ViewModel() {

    private val gameEventFlow: SharedFlow<GameEvent> = gameService.events
        .mapNotNull { jsonString ->
            try {
                val json = JSONObject(jsonString)
                GameEvent.fromJson(json)
            } catch (e: Exception) {
                Log.e("GameViewModel", "Parsing Error: ${e.message}", e)
                null
            }
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))

    val isGameReady: StateFlow<Boolean> = gameEventFlow
        .map { event -> event.gameState != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val gameState: StateFlow<GameState?> = gameEventFlow
        .map { it.gameState }
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
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(gameService) as T
        }
    }
}
