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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.model.card.Card
import at.aau.monopoly.klagenfurt.model.card.ChanceCard
import at.aau.monopoly.klagenfurt.model.card.CommunityChestCard
import at.aau.monopoly.klagenfurt.model.enums.CardAction

private val ChanceOrange = Color(0xFFF57C00)
private val CommunityChestBlue = Color(0xFF1565C0)
private val CardBackground = Color(0xFFFFF8E1)
private const val CARD_REF_W = 140f

@Composable
fun CardUI(card: Card, modifier: Modifier = Modifier) {
    val isChance = card is ChanceCard
    val headerColor = if (isChance) ChanceOrange else CommunityChestBlue
    val title = if (isChance) "CHANCE" else "COMMUNITY CHEST"

    BoxWithConstraints(
        modifier = modifier
            .width(140.dp)
            .height(224.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, headerColor, RoundedCornerShape(4.dp))
            .background(CardBackground)
    ) {
        val s = CardScale(maxWidth.value / CARD_REF_W)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
        ) {
            // Header banner (half height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(s.dp(17f))
                    .background(headerColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = s.sp(9f),
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Card icon/symbol
            Text(
                text = if (isChance) "?" else "\u2606",
                fontSize = s.sp(20f),
                color = headerColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(s.dp(3f)))

            // Description
            Text(
                text = card.description,
                fontSize = s.sp(8f),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                lineHeight = s.sp(10f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = s.dp(4f))
            )

            Spacer(modifier = Modifier.height(s.dp(3f)))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = s.dp(10f)),
                color = headerColor.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(s.dp(2f)))

            // Action details
            CardActionDetails(card, headerColor, s)

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CardActionDetails(card: Card, accentColor: Color, s: CardScale) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = s.dp(4f))
    ) {
        when (card.action) {
            CardAction.COLLECT_MONEY -> {
                Text("Collect", fontSize = s.sp(7f), color = Color.Gray)
                Text("+$$${card.amount}", fontWeight = FontWeight.ExtraBold, fontSize = s.sp(13f), color = Color(0xFF2E7D32))
            }
            CardAction.PAY_MONEY -> {
                Text("Pay", fontSize = s.sp(7f), color = Color.Gray)
                Text("-$$${card.amount}", fontWeight = FontWeight.ExtraBold, fontSize = s.sp(13f), color = Color(0xFFC62828))
            }
            CardAction.COLLECT_FROM_EACH -> {
                Text("From each", fontSize = s.sp(7f), color = Color.Gray)
                Text("+$$${card.amount}", fontWeight = FontWeight.ExtraBold, fontSize = s.sp(13f), color = Color(0xFF2E7D32))
            }
            CardAction.PAY_EACH_PLAYER -> {
                Text("Pay each", fontSize = s.sp(7f), color = Color.Gray)
                Text("-$$${card.amount}", fontWeight = FontWeight.ExtraBold, fontSize = s.sp(13f), color = Color(0xFFC62828))
            }
            CardAction.MOVE_TO -> {
                Text("Advance to", fontSize = s.sp(7f), color = Color.Gray)
                Text("Field #${card.targetFieldId ?: "?"}", fontWeight = FontWeight.Bold, fontSize = s.sp(10f), color = accentColor)
            }
            CardAction.MOVE_FORWARD -> {
                Text("Move", fontSize = s.sp(7f), color = Color.Gray)
                Text("${card.moveSpaces} spaces", fontWeight = FontWeight.Bold, fontSize = s.sp(10f), color = accentColor)
            }
            CardAction.GO_TO_JAIL -> {
                Text("\uD83D\uDE94", fontSize = s.sp(16f))
                Text("Go to Jail", fontWeight = FontWeight.Bold, fontSize = s.sp(8f), color = Color(0xFFC62828))
            }
            CardAction.GET_OUT_OF_JAIL -> {
                Text("\uD83D\uDD13", fontSize = s.sp(16f))
                Text("Jail Free", fontWeight = FontWeight.Bold, fontSize = s.sp(8f), color = Color(0xFF2E7D32))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewChanceCard() {
    CardUI(
        card = ChanceCard(
            id = 1,
            description = "Advance to Go. Collect \$200.",
            action = CardAction.COLLECT_MONEY,
            amount = 200
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewChanceCardSmall() {
    CardUI(
        card = ChanceCard(
            id = 1,
            description = "Advance to Go. Collect \$200.",
            action = CardAction.COLLECT_MONEY,
            amount = 200
        ),
        modifier = Modifier.size(56.dp, 89.6.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewCommunityChestCard() {
    CardUI(
        card = CommunityChestCard(
            id = 2,
            description = "You have been elected Chairman of the Board. Pay each player \$50.",
            action = CardAction.PAY_EACH_PLAYER,
            amount = 50
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewGoToJailCard() {
    CardUI(
        card = ChanceCard(
            id = 3,
            description = "Go directly to Jail. Do not pass Go. Do not collect \$200.",
            action = CardAction.GO_TO_JAIL
        )
    )
}

