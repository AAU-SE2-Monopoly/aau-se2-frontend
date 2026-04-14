package at.aau.monopoly.klagenfurt.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.aau.monopoly.klagenfurt.networking.GameService

class ChatViewModel(private val chatService: ChatService) : ViewModel() {




    class ChatViewModelFactory(private val chatService: ChatService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatService) as T
        }
    }
}