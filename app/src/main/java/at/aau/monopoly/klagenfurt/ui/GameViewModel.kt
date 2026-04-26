package at.aau.monopoly.klagenfurt.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter

class GameViewModel(private val gameService: GameService) : ViewModel() {

    private val objectMapper = JacksonProvider.objectMapper

    private val gameEventFlow: SharedFlow<GameEvent> = gameService.events
        .mapNotNull { jsonString ->
            try {
                objectMapper.readValue(jsonString, GameEvent::class.java)
            } catch (e: Exception) {
                Log.e("GameViewModel", "Parsing Error: ${e.message}", e)
                null
            }
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))

    val gameState: StateFlow<GameState?> = gameEventFlow
        .map { it.gameState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val fields: StateFlow<List<Field>> = gameState
        .mapNotNull { it?.fields }
        .filter { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isGameReady: StateFlow<Boolean> = gameState
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 🎲 Dice Roll states
    val lastDiceRoll: StateFlow<DiceRoll?> = gameState
        .map { it?.lastDiceRoll }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showDiceOverlay = MutableStateFlow(false)
    val showDiceOverlay: StateFlow<Boolean> = _showDiceOverlay.asStateFlow()

    private val _isRolling = MutableStateFlow(false)
    val isRolling: StateFlow<Boolean> = _isRolling.asStateFlow()

    val events: SharedFlow<String> = gameService.events
    val status: SharedFlow<String> = gameService.status

    fun connect() = gameService.connect()
    
    fun createGame(playerName: String) = gameService.createGame(playerName)
    
    fun joinGame(gameId: String, playerName: String) = gameService.joinGame(gameId, playerName)
    
    fun startGame() = gameService.startGame()
    
    fun rollDice() {
        // Show overlay and set rolling state
        _showDiceOverlay.value = true
        _isRolling.value = true

        Log.d("DiceRoll", "rollDice triggered - overlay shown, rolling started")

        // Send the roll action to the server
        gameService.rollDice()
    }

    fun onDiceRollComplete() {
        _isRolling.value = false
        Log.d("DiceRoll", "onDiceRollComplete - rolling finished")
    }

    fun closeDiceOverlay() {
        _showDiceOverlay.value = false
        Log.d("DiceRoll", "closeDiceOverlay - overlay hidden")
    }

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
