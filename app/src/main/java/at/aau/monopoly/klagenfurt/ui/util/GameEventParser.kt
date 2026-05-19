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

fun humanReadableEvent(event: GameEvent): String {
    return when (event.event) {
        "RENT_DUE" -> "You owe rent! Pay or declare bankruptcy."
        "TAX_DUE" -> "Tax is due! Pay or declare bankruptcy."
        "RENT_PAID" -> "Rent paid successfully."
        "PROPERTY_MORTGAGED" -> "Property mortgaged."
        "PROPERTY_UNMORTGAGED" -> "Property unmortgaged."
        "PAYMENT_FAILED" -> "Payment failed!"
        "HOUSE_SOLD" -> "House sold."
        "BANKRUPTCY_DECLARED" -> "Player declared bankruptcy."
        "FREE_PARKING_COLLECTED" -> "Collected Free Parking money!"
        else -> event.message ?: event.event
    }
}
