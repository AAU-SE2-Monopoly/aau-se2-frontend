package at.aau.monopoly.websocketdemoserver.model.field

import at.aau.monopoly.websocketdemoserver.model.enums.FieldType

data class GoToJailField(
    override val id: Int = 30,
    override val name: String = "Go To Jail",
    override val type: FieldType = FieldType.GO_TO_JAIL
) : Field(id, name, type)

