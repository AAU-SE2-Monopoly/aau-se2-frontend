package at.aau.monopoly.klagenfurt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.aau.monopoly.klagenfurt.networking.GameService

/**
 * Generic ViewModelFactory for ViewModels that take a single [GameService] dependency.
 */
open class GameServiceViewModelFactory<T : ViewModel>(
    private val gameService: GameService,
    private val creator: (GameService) -> T
) : ViewModelProvider.Factory {
    override fun <V : ViewModel> create(modelClass: Class<V>): V {
        @Suppress("UNCHECKED_CAST")
        return creator(gameService) as V
    }
}

