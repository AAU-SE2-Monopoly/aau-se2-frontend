package at.aau.monopoly.klagenfurt.model.card

import at.aau.monopoly.klagenfurt.model.enums.CardAction
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = CardDeserializer::class)
abstract class Card(
    open val id: Int,
    open val description: String,
    open val action: CardAction,
    /** Amount of money involved (for COLLECT_MONEY, PAY_MONEY, etc.) */
    open val amount: Int = 0,
    /** Target field index (for MOVE_TO) */
    open val targetFieldId: Int? = null,
    /** Number of spaces to move (for MOVE_FORWARD) */
    open val moveSpaces: Int = 0,
    /** Amount owed per house/building (for PAY_PER_BUILDING) */
    open val perBuildingAmount: Int = 0,
    /** Amount owed per hotel (for PAY_PER_BUILDING) */
    open val perHotelAmount: Int = 0
)
