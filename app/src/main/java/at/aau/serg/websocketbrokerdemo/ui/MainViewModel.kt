package at.aau.serg.websocketbrokerdemo.ui

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
            return MainViewModel(stompManager) as T
        }
    }
}
