package at.aau.monopoly.websocketdemoserver.model.field

import at.aau.monopoly.websocketdemoserver.model.enums.FieldType

data class CommunityChestField(
    override val id: Int,
    override val name: String = "Community Chest",
    override val type: FieldType = FieldType.COMMUNITY_CHEST
) : Field(id, name, type)

