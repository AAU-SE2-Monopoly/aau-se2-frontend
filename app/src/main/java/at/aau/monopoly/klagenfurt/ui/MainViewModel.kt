package at.aau.monopoly.klagenfurt.ui

import MyStompManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.SharedFlow

class MainViewModel(private val stompManager: MyStompManager) : ViewModel() {

    val responses: SharedFlow<String> = stompManager.responses

    fun connect() = stompManager.connect()
    fun sendHello() = stompManager.sendHello()
    fun sendJson() = stompManager.sendJson()

    class Factory(private val stompManager: MyStompManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            // Prüfen, ob die angeforderte Klasse ein MainViewModel (oder eine Superklasse davon) ist
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(stompManager) as T
            }
            // Falls eine falsche Klasse angefordert wird, zwingend abbrechen
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
