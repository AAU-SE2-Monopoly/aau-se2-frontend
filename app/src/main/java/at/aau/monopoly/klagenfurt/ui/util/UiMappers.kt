package at.aau.monopoly.klagenfurt.ui.util

import androidx.compose.ui.graphics.Color
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import com.example.myapplication.R

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
