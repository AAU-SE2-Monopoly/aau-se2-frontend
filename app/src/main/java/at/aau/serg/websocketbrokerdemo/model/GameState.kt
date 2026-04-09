package at.aau.serg.websocketbrokerdemo.model

import at.aau.serg.websocketbrokerdemo.model.card.ChanceCard
import at.aau.serg.websocketbrokerdemo.model.card.CommunityChestCard
import at.aau.serg.websocketbrokerdemo.model.enums.GamePhase
import at.aau.serg.websocketbrokerdemo.model.field.Field
import org.json.JSONArray
import org.json.JSONObject

data class GameState(
    val gameId: String,
    val fields: List<Field>,
    val players: MutableList<Player> = mutableListOf(),
    var currentPlayerIndex: Int = 0,
    var phase: GamePhase = GamePhase.WAITING,
    val chanceCards: MutableList<ChanceCard> = mutableListOf(),
    val communityChestCards: MutableList<CommunityChestCard> = mutableListOf(),
    var freeParkingMoney: Int = 0,
    var lastDiceRoll: DiceRoll? = null // replaced Pair with serializable DiceRoll
) {
    /** The player whose turn it currently is. */
    val currentPlayer: Player?
        get() = players.getOrNull(currentPlayerIndex)

    /** Advance the turn to the next player (wraps around). */
    fun advanceTurn() {
        if (players.isNotEmpty()) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        }
        phase = GamePhase.ROLLING
    }

    /** Returns true when only one player has money / properties remaining. */
    fun isGameOver(): Boolean = players.count { !it.isBankrupt() } <= 1

    companion object {
        fun fromJson(json: JSONObject): GameState {
            val gameId = json.getString("gameId")
            val fieldsArray = json.getJSONArray("fields")
            val fields = mutableListOf<Field>()
            for (i in 0 until fieldsArray.length()) {
                fields.add(Field.fromJson(fieldsArray.getJSONObject(i)))
            }
            val playersArray = json.optJSONArray("players") ?: JSONArray()
            val players = mutableListOf<Player>()
            for (i in 0 until playersArray.length()) {
                players.add(Player.fromJson(playersArray.getJSONObject(i)))
            }
            val currentPlayerIndex = json.optInt("currentPlayerIndex", 0)
            val phase = GamePhase.valueOf(json.optString("phase", "WAITING"))
            val freeParkingMoney = json.optInt("freeParkingMoney", 0)
            val lastDiceRoll = json.optJSONObject("lastDiceRoll")?.let { DiceRoll.fromJson(it) }
            // Assume cards are empty for now
            val chanceCards = mutableListOf<ChanceCard>()
            val communityChestCards = mutableListOf<CommunityChestCard>()
            return GameState(gameId, fields, players, currentPlayerIndex, phase, chanceCards, communityChestCards, freeParkingMoney, lastDiceRoll)
        }
    }
}
