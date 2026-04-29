package at.aau.monopoly.klagenfurt.model.field

import at.aau.monopoly.klagenfurt.model.enums.FieldType

abstract class Field(
    open val id: Int,
    open val name: String,
    open val type: FieldType
)

/**
 * Marker interface for fields that can be owned by a player.
 * Implemented by PropertyField, RailroadField, and UtilityField.
 */
interface OwnableField {
    var ownerId: String?
    val price: Int
    var isMortgaged: Boolean
}