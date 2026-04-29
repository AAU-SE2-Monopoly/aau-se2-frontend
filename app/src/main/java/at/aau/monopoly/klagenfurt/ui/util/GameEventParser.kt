package at.aau.monopoly.klagenfurt.ui.util

import android.util.Log
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.networking.JacksonProvider

/**
 * Parses a raw JSON string into a [GameEvent], returning null on failure.
 * Consolidates the duplicated parsing logic from ViewModels.
 */
fun parseGameEvent(jsonString: String): GameEvent? {
    return try {
        JacksonProvider.objectMapper.readValue(jsonString, GameEvent::class.java)
    } catch (e: Exception) {
        Log.e("parseGameEvent", "Parsing error: ${e.message}", e)
        null
    }
}
