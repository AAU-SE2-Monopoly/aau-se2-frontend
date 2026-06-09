package at.aau.monopoly.klagenfurt.networking

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object ServerConfig {

    //private const val LOCAL_URI = "ws://10.0.2.2:8080/ws"
    private const val LOCAL_URI = "ws://localhost:8080/ws"
    private const val GLOBAL_URI = "ws://se2-demo.aau.at:53208/ws"

    var isGlobal by mutableStateOf(false)

    val websocketUri: String
        get() = if (isGlobal) GLOBAL_URI else LOCAL_URI

    val displayLabel: String
        get() = if (isGlobal) "Global" else "Local"
}


