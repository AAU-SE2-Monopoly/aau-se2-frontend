package at.aau.monopoly.websocketdemoserver.model.card

import at.aau.monopoly.websocketdemoserver.model.enums.CardAction

data class ChanceCard(
    override val id: Int,
    override val description: String,
    override val action: CardAction,
    override val amount: Int = 0,
    override val targetFieldId: Int? = null,
    override val moveSpaces: Int = 0
) : Card(id, description, action, amount, targetFieldId, moveSpaces)

