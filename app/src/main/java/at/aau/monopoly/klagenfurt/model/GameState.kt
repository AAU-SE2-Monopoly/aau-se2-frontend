package at.aau.monopoly.klagenfurt.model

import at.aau.monopoly.klagenfurt.model.card.Card
import at.aau.monopoly.klagenfurt.model.card.ChanceCard
import at.aau.monopoly.klagenfurt.model.card.CommunityChestCard
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.field.Field
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

enum class PaymentSource { RENT, CARD_PAY, CARD_PAY_EACH, CARD_REPAIR, TAX }

data class PendingPayment(
    val amount: Int,
    val source: PaymentSource,
    val sourceFieldId: Int? = null,
    val creditorPlayerId: String? = null,
    val debtorCanPayAfterAssets: Boolean = false
)

data class TradeOffer(
    val id: String = "",
    val fromPlayerId: String = "",
    val toPlayerId: String = "",
    val offerMoney: Int = 0,
    val requestMoney: Int = 0,
    val offerPropertyIds: List<Int> = emptyList(),
    val requestPropertyIds: List<Int> = emptyList(),
    val offerJailCards: Int = 0,
    val requestJailCards: Int = 0
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GameState(
    val gameId: String,
    val fields: List<Field>,
    val players: MutableList<Player> = mutableListOf(),
    var currentPlayerIndex: Int = 0,
    var phase: GamePhase = GamePhase.WAITING,
    val chanceCards: MutableList<ChanceCard> = mutableListOf(),
    val communityChestCards: MutableList<CommunityChestCard> = mutableListOf(),
    var freeParkingMoney: Int = 0,
    var lastDiceRoll: DiceRoll? = null,
    var currentActionCard: Card? = null,
    var pendingPayment: PendingPayment? = null,
    val bankruptcyTotalAssets: Int = 0,
    val bankruptcyTotalDebt: Int = 0,
    val bankruptcyPropertiesCount: Int = 0,
    val bankruptcyOwnedFieldIds: List<Int> = emptyList(),
    val bankruptcyPlayerId: String = "",
    val pendingTradeOffer: TradeOffer? = null
) {
    /** The player whose turn it currently is. */
    val currentPlayer: Player?
        get() = players.getOrNull(currentPlayerIndex)

    /** Advance the turn to the next player (wraps around) and resets turn-specific stats. */
    fun advanceTurn() {
        if (players.isNotEmpty()) {
            var attempts = 0
            do {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size
                attempts++
            } while (attempts < players.size && (players[currentPlayerIndex].isBankrupt()))

            if (players.all { it.isBankrupt() }) {
                phase = GamePhase.FINISHED
                return
            }
        }
        phase = GamePhase.ROLLING
    }

    /** Returns true when only one player has money / properties remaining. */
    fun isGameOver(): Boolean = players.count { !it.isBankrupt() } <= 1
}
