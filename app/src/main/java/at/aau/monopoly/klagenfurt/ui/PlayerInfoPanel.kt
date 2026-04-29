package at.aau.monopoly.klagenfurt.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.card.Card
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.UtilityField

private val PanelBg = Color(0xCC1B1B1B)
private val GoldAccent = Color(0xFFFFD54F)

// Original card width (from FieldCardUI / CardUI)
private const val ORIG_CARD_W = 140f // dp
// 8:5 ratio means height = width * 8/5
private const val ASPECT_H_OVER_W = 8f / 5f

private const val OTHER_SCALE = 0.4f
private const val OWN_SCALE = 0.6f

@Composable
fun PlayerInfoPanel(
    player: Player,
    fields: List<Field>,
    cards: List<Card> = emptyList(),
    isCurrentTurn: Boolean = false,
    isOwnPlayer: Boolean = false,
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scale = if (isOwnPlayer) OWN_SCALE else OTHER_SCALE
    val cardW = (ORIG_CARD_W * scale).dp
    val cardH = (ORIG_CARD_W * ASPECT_H_OVER_W * scale).dp
    val sameGap = 2.dp
    val groupGap = 8.dp

    val ownedFields = fields.filter { f ->
        when (f) {
            is PropertyField -> f.ownerId == player.id
            is RailroadField -> f.ownerId == player.id
            is UtilityField -> f.ownerId == player.id
            else -> false
        }
    }

    val groupedFields = ownedFields.sortedBy { f ->
        when (f) {
            is PropertyField -> f.color.ordinal
            is RailroadField -> 100
            is UtilityField -> 101
            else -> 200
        }
    }

    // Panel width: same for all players
    Column(
        modifier = modifier
            .width(280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBg)
            .border(
                width = if (isCurrentTurn) 2.dp else 1.dp,
                color = if (isCurrentTurn) GoldAccent else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(6.dp),
        horizontalAlignment = if (isOwnPlayer) Alignment.End else Alignment.Start
    ) {
        // Player name + money + icon row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isOwnPlayer) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = player.name,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("💵", fontSize = 9.sp)
            Text(
                text = "$${player.money}",
                color = Color(0xFF81C784),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            // Player icon
            Image(
                painter = painterResource(id = getPlayerTokenResource(player.iconId)),
                contentDescription = "${player.name} icon",
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            )
        }

        if (isCurrentTurn) {
            Text("⭐ Current Turn", color = GoldAccent, fontSize = 6.sp, fontWeight = FontWeight.Bold)
        }
        if (player.inJail) {
            Text("🔒 In Jail (${player.jailTurns})", color = Color(0xFFEF9A9A), fontSize = 6.sp)
        }

        // Property cards – horizontal, grouped by color
        if (groupedFields.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                thickness = 0.5.dp,
                color = Color.White.copy(alpha = 0.2f)
            )

            if (isOwnPlayer) {
                Box(modifier = Modifier.clickable(enabled = onCardClick != null) { onCardClick?.invoke() }) {
                    MiniCardFlowRow(groupedFields, scale, cardW, cardH, sameGap, groupGap, alignEnd = true)
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .clickable(enabled = onCardClick != null) { onCardClick?.invoke() }
                ) {
                    var lastKey = -999
                    groupedFields.forEach { field ->
                        val key = when (field) {
                            is PropertyField -> field.color.ordinal
                            is RailroadField -> 100
                            is UtilityField -> 101
                            else -> 200
                        }
                        val gap = if (lastKey != -999 && key != lastKey) groupGap else if (lastKey != -999) sameGap else 0.dp
                        if (gap > 0.dp) Spacer(modifier = Modifier.width(gap))
                        ScaledFieldCard(field, scale, cardW, cardH)
                        lastKey = key
                    }
                }
            }
        }

        // Chance / Community Chest cards
        if (cards.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                thickness = 0.5.dp,
                color = Color.White.copy(alpha = 0.2f)
            )
            val chanceArrangement = if (isOwnPlayer) Arrangement.End else Arrangement.Start
            Row(
                horizontalArrangement = chanceArrangement,
                modifier = Modifier.fillMaxWidth()
            ) {
                var lastIsChance: Boolean? = null
                cards.forEach { card ->
                    val isChance = card is at.aau.monopoly.klagenfurt.model.card.ChanceCard
                    val gap = when {
                        lastIsChance == null -> 0.dp
                        lastIsChance != isChance -> groupGap
                        else -> sameGap
                    }
                    if (gap > 0.dp) Spacer(modifier = Modifier.width(gap))
                    ScaledChanceCard(card, scale, cardW, cardH)
                    lastIsChance = isChance
                }
            }
        }

        if (player.isBankrupt()) {
            Text("💀 BANKRUPT", color = Color(0xFFEF5350), fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniCardFlowRow(fields: List<Field>, scale: Float, cardW: Dp, cardH: Dp, sameGap: Dp, groupGap: Dp, alignEnd: Boolean = false) {
    data class ColorGroup(val key: Int, val fields: List<Field>)

    val groups = mutableListOf<ColorGroup>()
    var currentKey = -999
    var currentGroup = mutableListOf<Field>()
    for (f in fields) {
        val key = when (f) {
            is PropertyField -> f.color.ordinal
            is RailroadField -> 100
            is UtilityField -> 101
            else -> 200
        }
        if (key != currentKey && currentGroup.isNotEmpty()) {
            groups.add(ColorGroup(currentKey, currentGroup.toList()))
            currentGroup = mutableListOf()
        }
        currentKey = key
        currentGroup.add(f)
    }
    if (currentGroup.isNotEmpty()) groups.add(ColorGroup(currentKey, currentGroup.toList()))


    Row(
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        groups.forEachIndexed { gi, group ->
            if (gi > 0) Spacer(modifier = Modifier.width(groupGap))
            Row(horizontalArrangement = Arrangement.spacedBy(sameGap)) {
                group.fields.forEach { field ->
                    ScaledFieldCard(field, scale, cardW, cardH)
                }
            }
        }
    }
}

/**
 * Renders a FieldCardUI at the specified size directly (no graphicsLayer scaling).
 */
@Composable
private fun ScaledFieldCard(field: Field, scale: Float, w: Dp, h: Dp) {
    FieldCardUI(
        field = field,
        modifier = Modifier.size(w, h)
    )
}

@Composable
private fun ScaledChanceCard(card: Card, scale: Float, w: Dp, h: Dp) {
    CardUI(
        card = card,
        modifier = Modifier.size(w, h)
    )
}

