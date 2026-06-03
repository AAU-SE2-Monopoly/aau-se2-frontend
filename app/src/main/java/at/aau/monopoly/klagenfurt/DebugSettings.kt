package at.aau.monopoly.klagenfurt

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import at.aau.monopoly.klagenfurt.networking.ServerConfig

/**
 * Global toggle for debug mode.
 * When disabled (default), debug buttons are hidden in the game UI.
 * Debug mode can only be enabled on the local environment.
 */
object DebugSettings {
    var isEnabled by mutableStateOf(false)

    /** Returns true if debug mode is allowed (only on local server). */
    val canEnable: Boolean
        get() = !ServerConfig.isGlobal
}
