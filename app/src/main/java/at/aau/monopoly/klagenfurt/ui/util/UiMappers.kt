package at.aau.monopoly.klagenfurt.ui.util

import androidx.compose.ui.graphics.Color
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.UtilityField
import at.aau.monopoly.klagenfurt.ui.ManageableProperty
import com.example.myapplication.R
import kotlin.math.ceil

/**
 * Extracts the owner ID from any ownable field type.
 */
fun Field.ownerIdFromField(): String? = when (this) {
    is PropertyField -> ownerId
    is RailroadField -> ownerId
    is UtilityField -> ownerId
    else -> null
}

/**
 * Maps an ownable field to a [ManageableProperty] for UI display.
 * WARNING: does NOT compute canSellHouse/canSellHotel (no sibling context).
 */
fun Field.toManageableProperty(): ManageableProperty = toManageableProperty(emptyList())

/**
 * Maps an ownable field to a [ManageableProperty] for UI display.
 * [allFields] is used to check the even-building rule for Sell House / Sell Hotel.
 */
fun Field.toManageableProperty(allFields: List<Field>): ManageableProperty = when (this) {
    is PropertyField -> {
        val siblings = allFields.filterIsInstance<PropertyField>()
            .filter { it.color == color && it.id != id && it.ownerId == ownerId }
        val sameColorAll = allFields.filterIsInstance<PropertyField>()
            .filter { it.color == color }
        val ownsMonopoly = sameColorAll.all { it.ownerId == ownerId }
        val allUnmortgaged = siblings.all { !it.isMortgaged } && !isMortgaged
        val newHouseCount = houses - 1
        val canSell = houses > 0 && !isMortgaged &&
            siblings.all { it.houses in newHouseCount..houses }
        val canSellH = hasHotel && !isMortgaged && siblings.all { it.houses >= 4 }
        val canBuy = allUnmortgaged && ownsMonopoly && houses < 4 && !hasHotel &&
            siblings.all { it.houses >= houses }
        val canBuyH = allUnmortgaged && ownsMonopoly && houses == 4 && !hasHotel &&
            siblings.all { it.houses == 4 }
        ManageableProperty(
            fieldId = id, name = name, color = color.name,
            price = price, mortgageValue = price / 2,
            unmortgageCost = ceil(price / 2.0 * 1.1).toInt(),
            houses = houses, hasHotel = hasHotel,
            isMortgaged = isMortgaged,
            houseCost = houseCost, hotelCost = hotelCost,
            sellHouseValue = houseCost / 2,
            sellHotelValue = hotelCost / 2,
            canSellHouse = canSell,
            canSellHotel = canSellH,
            canBuyHouse = canBuy,
            canBuyHotel = canBuyH
        )
    }
    is RailroadField -> ManageableProperty(
        fieldId = id, name = name, color = null,
        price = price, mortgageValue = price / 2,
        unmortgageCost = ceil(price / 2.0 * 1.1).toInt(),
        houses = 0, hasHotel = false, isMortgaged = isMortgaged,
        houseCost = 0, hotelCost = 0,
        sellHouseValue = 0, sellHotelValue = 0
    )
    is UtilityField -> ManageableProperty(
        fieldId = id, name = name, color = null,
        price = price, mortgageValue = price / 2,
        unmortgageCost = ceil(price / 2.0 * 1.1).toInt(),
        houses = 0, hasHotel = false, isMortgaged = isMortgaged,
        houseCost = 0, hotelCost = 0,
        sellHouseValue = 0, sellHotelValue = 0
    )
    //dead code: only called after filtering for PropertyField/RailroadField/UtilityField.

    else -> ManageableProperty(
        fieldId = id, name = name, color = null,
        price = 0, mortgageValue = 0, unmortgageCost = 0,
        houses = 0, hasHotel = false, isMortgaged = false,
        houseCost = 0, hotelCost = 0,
        sellHouseValue = 0, sellHotelValue = 0
    )
}

/**
 * Maps a [PropertyColor] enum to a Compose [Color].
 */
fun PropertyColor.toComposeColor(): Color = when (this) {
    PropertyColor.BROWN -> Color(0xFF955436)
    PropertyColor.LIGHT_BLUE -> Color(0xFFAAE0FA)
    PropertyColor.PINK -> Color(0xFFD93A96)
    PropertyColor.ORANGE -> Color(0xFFF7941D)
    PropertyColor.RED -> Color(0xFFED1B24)
    PropertyColor.YELLOW -> Color(0xFFD4A017)
    PropertyColor.GREEN -> Color(0xFF1FB25A)
    PropertyColor.DARK_BLUE -> Color(0xFF0072BB)
}

/**
 * Maps a serialized property color string to the same Compose [Color] used by board cards.
 * Non-color ownables such as railroads and utilities use a neutral dark accent.
 */
fun String?.toPropertyComposeColor(): Color = when (this?.lowercase()) {
    PropertyColor.BROWN.name.lowercase() -> PropertyColor.BROWN.toComposeColor()
    PropertyColor.LIGHT_BLUE.name.lowercase() -> PropertyColor.LIGHT_BLUE.toComposeColor()
    PropertyColor.PINK.name.lowercase() -> PropertyColor.PINK.toComposeColor()
    PropertyColor.ORANGE.name.lowercase() -> PropertyColor.ORANGE.toComposeColor()
    PropertyColor.RED.name.lowercase() -> PropertyColor.RED.toComposeColor()
    PropertyColor.YELLOW.name.lowercase() -> PropertyColor.YELLOW.toComposeColor()
    PropertyColor.GREEN.name.lowercase() -> PropertyColor.GREEN.toComposeColor()
    PropertyColor.DARK_BLUE.name.lowercase() -> PropertyColor.DARK_BLUE.toComposeColor()
    else -> Color(0xFF424242)
}

/**
 * Maps a player icon ID string to the corresponding drawable resource.
 */
fun getPlayerTokenResource(iconId: String): Int {
    return when (iconId.lowercase()) {
        "lindwurm" -> R.drawable.lindwurm
        "woerthersee" -> R.drawable.woertherseemandl
        "gti" -> R.drawable.gti
        "ironman" -> R.drawable.ironman
        "josef" -> R.drawable.josef
        else -> R.drawable.lindwurm
    }
}
