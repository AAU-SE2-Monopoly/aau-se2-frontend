package at.aau.serg.websocketbrokerdemo.model.card

import at.aau.serg.websocketbrokerdemo.model.enums.CardAction

data class CommunityChestCard(
    override val id: Int,
    override val description: String,
    override val action: CardAction,
    override val amount: Int = 0,
    override val targetFieldId: Int? = null,
    override val moveSpaces: Int = 0
) : Card(id, description, action, amount, targetFieldId, moveSpaces)

