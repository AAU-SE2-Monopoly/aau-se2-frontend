package at.aau.monopoly.klagenfurt.model.field

import at.aau.monopoly.klagenfurt.model.enums.FieldType

abstract class Field(
    open val id: Int,
    open val name: String,
    open val type: FieldType
)
