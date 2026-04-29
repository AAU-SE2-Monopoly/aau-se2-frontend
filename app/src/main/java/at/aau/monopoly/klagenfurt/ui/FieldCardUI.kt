package at.aau.monopoly.klagenfurt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.*
import at.aau.monopoly.klagenfurt.ui.util.toComposeColor

private val CardBg = Color(0xFFFFF8E1)
private const val REF_W = 140f // reference width in dp

@Composable
fun FieldCardUI(field: Field, modifier: Modifier = Modifier) {
    val finalMod = modifier.width(140.dp).height(224.dp)
    when (field) {
        is PropertyField -> PropertyTitleDeed(field, finalMod)
        is RailroadField -> RailroadCard(field, finalMod)
        is UtilityField -> UtilityCard(field, finalMod)
        is TaxField -> TaxCard(field, finalMod)
        else -> GenericFieldCard(field, finalMod)
    }
}

// ── Property Title Deed ──────────────────────────────────────────────

@Composable
private fun PropertyTitleDeed(field: PropertyField, modifier: Modifier = Modifier) {
    val colorBand = field.color.toComposeColor()

    CardShell(borderColor = colorBand, modifier = modifier) { s ->
        // Color band (half height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(s.dp(19f))
                .background(colorBand)
        )

        // Property name
        Text(
            text = field.name,
            fontWeight = FontWeight.ExtraBold,
            fontSize = s.sp(13f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = s.dp(4f), vertical = s.dp(2f))
        )

        ThinDivider(s)

        // Rent table
        val rentLabels = listOf("Rent", "1 House", "2 Houses", "3 Houses", "4 Houses", "Hotel")
        field.rent.forEachIndexed { i, rent ->
            if (i < rentLabels.size) {
                RentRow(rentLabels[i], "$$rent", s)
            }
        }

        ThinDivider(s)

        RentRow("House", "$${field.houseCost}", s)
        RentRow("Hotel", "$${field.hotelCost}", s)

        if (field.houses > 0 || field.hasHotel) {
            val status = if (field.hasHotel) "🏨" else "🏠×${field.houses}"
            Text(text = status, fontSize = s.sp(8f), color = Color.DarkGray)
        }
        if (field.isMortgaged) {
            Text(text = "MORTGAGED", fontSize = s.sp(8f), color = Color.Red, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ── Railroad Card ────────────────────────────────────────────────────

@Composable
private fun RailroadCard(field: RailroadField, modifier: Modifier = Modifier) {
    val accent = Color(0xFF424242)
    CardShell(borderColor = accent, modifier = modifier) { s ->
        Box(
            modifier = Modifier.fillMaxWidth().height(s.dp(17f)).background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text("RAILROAD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = s.sp(9f), letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(s.dp(2f)))
        Text("", fontSize = s.sp(18f))
        Text(field.name, fontWeight = FontWeight.ExtraBold, fontSize = s.sp(11f), textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = s.dp(4f)))
        ThinDivider(s)
        RentRow("Price", "$${field.price}", s)
        RentRow("1 RR", "$25", s)
        RentRow("2 RR", "$50", s)
        RentRow("3 RR", "$100", s)
        RentRow("4 RR", "$200", s)
        if (field.isMortgaged) {
            Text("MORTGAGED", fontSize = s.sp(8f), color = Color.Red, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

// ── Utility Card ─────────────────────────────────────────────────────

@Composable
private fun UtilityCard(field: UtilityField, modifier: Modifier = Modifier) {
    val accent = Color(0xFF1B5E20)
    CardShell(borderColor = accent, modifier = modifier) { s ->
        Box(
            modifier = Modifier.fillMaxWidth().height(s.dp(17f)).background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text("UTILITY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = s.sp(9f), letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(s.dp(2f)))
        Text("⚡", fontSize = s.sp(18f))
        Text(field.name, fontWeight = FontWeight.ExtraBold, fontSize = s.sp(11f), textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = s.dp(4f)))
        ThinDivider(s)
        RentRow("Price", "$${field.price}", s)
        Text("1 owned: 4× dice", fontSize = s.sp(7f), modifier = Modifier.padding(horizontal = s.dp(4f)))
        Text("2 owned: 10× dice", fontSize = s.sp(7f), modifier = Modifier.padding(horizontal = s.dp(4f)))
        if (field.isMortgaged) {
            Text("MORTGAGED", fontSize = s.sp(8f), color = Color.Red, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

// ── Tax Card ─────────────────────────────────────────────────────────

@Composable
private fun TaxCard(field: TaxField, modifier: Modifier = Modifier) {
    val accent = Color(0xFF880E4F)
    CardShell(borderColor = accent, modifier = modifier) { s ->
        Box(
            modifier = Modifier.fillMaxWidth().height(s.dp(17f)).background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text("TAX", color = Color.White, fontWeight = FontWeight.Bold, fontSize = s.sp(9f), letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("", fontSize = s.sp(22f))
        Text(field.name, fontWeight = FontWeight.ExtraBold, fontSize = s.sp(11f), textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = s.dp(4f)))
        ThinDivider(s)
        Text("Pay $${field.amount}", fontWeight = FontWeight.Bold, fontSize = s.sp(12f), color = Color(0xFFC62828))
        Spacer(modifier = Modifier.weight(1f))
    }
}

// ── Generic (Go, Jail, Chance, Community Chest, Free Parking, etc.) ──

@Composable
private fun GenericFieldCard(field: Field, modifier: Modifier = Modifier) {
    val (accent, icon) = when (field.type) {
        FieldType.GO -> Color(0xFFD32F2F) to "▶"
        FieldType.JAIL -> Color(0xFF5D4037) to ""
        FieldType.GO_TO_JAIL -> Color(0xFF5D4037) to ""
        FieldType.CHANCE -> Color(0xFFF57C00) to "?"
        FieldType.COMMUNITY_CHEST -> Color(0xFF1565C0) to "☆"
        FieldType.FREE_PARKING -> Color(0xFF2E7D32) to ""
        else -> Color(0xFF616161) to "•"
    }
    CardShell(borderColor = accent, modifier = modifier) { s ->
        Box(
            modifier = Modifier.fillMaxWidth().height(s.dp(17f)).background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                field.type.name.replace('_', ' '),
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = s.sp(9f), letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(icon, fontSize = s.sp(24f))
        Spacer(modifier = Modifier.height(s.dp(2f)))
        Text(field.name, fontWeight = FontWeight.ExtraBold, fontSize = s.sp(12f), textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = s.dp(4f)))
        Spacer(modifier = Modifier.weight(1f))
    }
}

// ── Scale helper ─────────────────────────────────────────────────────

/**
 * Ratio of actual card width to reference width.
 * dp() scales linearly for layout.
 * sp() uses sqrt scaling so text stays readable at small sizes.
 */
class CardScale(val ratio: Float) {
    fun dp(value: Float): Dp = (value * ratio).dp
    fun sp(value: Float): TextUnit {
        val textRatio = kotlin.math.sqrt(ratio).coerceAtLeast(0.65f)
        return (value * textRatio).sp
    }
}

// ── Shared helpers ───────────────────────────────────────────────────

@Composable
private fun CardShell(
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(CardScale) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .background(CardBg)
    ) {
        val scale = CardScale(maxWidth.value / REF_W)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
        ) {
            content(scale)
        }
    }
}

@Composable
private fun ColumnScope.RentRow(label: String, value: String, s: CardScale) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = s.dp(6f), vertical = s.dp(0.5f)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = s.sp(7.5f), color = Color.DarkGray, maxLines = 1)
        Text(value, fontSize = s.sp(7.5f), fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ColumnScope.ThinDivider(s: CardScale) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = s.dp(8f), vertical = s.dp(1.5f)),
        thickness = 0.5.dp,
        color = Color.LightGray
    )
}

// ── Previews ─────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun PreviewPropertyCard() {
    FieldCardUI(
        field = PropertyField(
            id = 1, name = "Herrengasse", color = PropertyColor.BROWN,
            price = 60, rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50, hotelCost = 50
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewPropertyCardSmall() {
    FieldCardUI(
        field = PropertyField(
            id = 1, name = "Herrengasse", color = PropertyColor.BROWN,
            price = 60, rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50, hotelCost = 50
        ),
        modifier = Modifier.size(56.dp, 89.6.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewRailroadCard() {
    FieldCardUI(
        field = RailroadField(id = 5, name = "Hauptbahnhof")
    )
}

