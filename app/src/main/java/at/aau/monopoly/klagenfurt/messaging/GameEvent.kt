package at.aau.monopoly.klagenfurt.messaging

import at.aau.monopoly.klagenfurt.model.GameState
import org.json.JSONObject

data class GameEvent(
    val gameId: String = "",
    val event: String = "",
    val gameState: GameState? = null,
    val message: String? = null
){
    companion object{
        fun fromJson(json: JSONObject): GameEvent {
            val gameId = json.getString("gameId")
            val event = json.getString("event")
            val gameState = json.optJSONObject("gameState")?.let { GameState.fromJson(it) }
            val message = json.optString("message")
            return GameEvent(gameId, event, gameState, message)
        }
    }
}
