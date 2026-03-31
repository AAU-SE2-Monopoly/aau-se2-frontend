package at.aau.monopoly.websocketdemoserver.model.field

import at.aau.monopoly.websocketdemoserver.model.enums.FieldType

abstract class Field(
    open val id: Int,
    open val name: String,
    open val type: FieldType
)
