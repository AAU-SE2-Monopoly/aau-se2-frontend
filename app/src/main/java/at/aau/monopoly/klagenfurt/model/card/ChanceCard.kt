
package at.aau.monopoly.klagenfurt.model.card

import at.aau.monopoly.klagenfurt.model.enums.CardAction
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChanceCard(
    override val id: Int,
    override val description: String,
    override val action: CardAction,
    override val amount: Int = 0,
    override val targetFieldId: Int? = null,
    override val moveSpaces: Int = 0,
    override val perBuildingAmount: Int = 0,
    override val perHotelAmount: Int = 0
) : Card(
    id = id,
    description = description,
    action = action,
    amount = amount,
    targetFieldId = targetFieldId,
    moveSpaces = moveSpaces,
    perBuildingAmount = perBuildingAmount,
    perHotelAmount = perHotelAmount
)
