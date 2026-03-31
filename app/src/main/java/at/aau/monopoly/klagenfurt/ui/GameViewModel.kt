package at.aau.serg.websocketbrokerdemo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.serg.websocketbrokerdemo.networking.GameService
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class GameViewModel(private val gameService: GameService) : ViewModel() {

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
