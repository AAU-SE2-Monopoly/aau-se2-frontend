package at.aau.monopoly.klagenfurt.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.ServiceLocator
import at.aau.monopoly.klagenfurt.messaging.ChatMessage
import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.GameStompClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


class ChatViewModel(private val chatService: ChatService,
    private val gameService: GameService) : ViewModel() {

    private val playerName= gameService.getCurrentPlayerName()
   val messageFlow: StateFlow<ChatMessage> =chatService.messageFlow
       .map { ChatMessage(playerName,it) }
       .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatMessage(playerName,""))


    class ChatViewModelFactory(private val chatService: ChatService,private val gameService: GameService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatService, gameService) as T
        }
    }
}