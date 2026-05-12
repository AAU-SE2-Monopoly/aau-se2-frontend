package at.aau.monopoly.klagenfurt.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.model.card.Card
import at.aau.monopoly.klagenfurt.model.enums.CardAction

/**
 * Displays an action card in the center of the screen with an "Execute Action" button.
 * The player must press the button to execute the action before the game can continue.
 */
@Composable
fun ActionCardOverlay(
    isVisible: Boolean,
    card: Card? = null,
    isExecuting: Boolean = false,
    onExecuteAction: () -> Unit
) {
    if (!isVisible || card == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(width = 400.dp, height = 550.dp)
                .padding(16.dp),
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Card Type
                val (cardTypeText, headerColor) = when (card) {
                    is at.aau.monopoly.klagenfurt.model.card.ChanceCard -> {
                        "🎰 CHANCE CARD" to Color(0xFFF57C00)
                    }
                    is at.aau.monopoly.klagenfurt.model.card.CommunityChestCard -> {
                        "⭐ COMMUNITY CHEST" to Color(0xFF1565C0)
                    }
                    else -> "ACTION CARD" to Color(0xFF5E35B1)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerColor, RoundedCornerShape(8.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cardTypeText,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Card Description
                Text(
                    text = card.description,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Details Box
                ActionDetailBox(card = card)

                Spacer(modifier = Modifier.weight(1f))

                // Status Text
                if (isExecuting) {
                    Text(
                        text = "Executing action...",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Execute Action Button
                val buttonColor by animateColorAsState(
                    targetValue = if (isExecuting) Color.Gray else Color(0xFF1B7F1C),
                    animationSpec = tween(300)
                )

                Button(
                    onClick = onExecuteAction,
                    enabled = !isExecuting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        disabledContainerColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (isExecuting) "⏳ Executing..." else "✓ Execute Action",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note Text
                Text(
                    text = "Press the button above to execute this action",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Displays the specific action details (amount, target, etc.) based on CardAction type.
 */
@Composable
private fun ActionDetailBox(card: Card) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (card.action) {
                CardAction.COLLECT_MONEY -> {
                    Text(
                        text = "Collect Money",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "+$${card.amount}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32)
                    )
                }

                CardAction.PAY_MONEY -> {
                    Text(
                        text = "Pay Money",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "-$${card.amount}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC62828)
                    )
                }

                CardAction.COLLECT_FROM_EACH -> {
                    Text(
                        text = "Collect From Each Player",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "+$${card.amount} each",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32)
                    )
                }

                CardAction.PAY_EACH_PLAYER -> {
                    Text(
                        text = "Pay Each Player",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "-$${card.amount} each",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC62828)
                    )
                }

                CardAction.MOVE_TO -> {
                    Text(
                        text = "Advance to Field",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "#${card.targetFieldId ?: "?"}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF5E35B1)
                    )
                }

                CardAction.MOVE_FORWARD -> {
                    Text(
                        text = "Move Forward",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${card.moveSpaces} spaces",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF5E35B1)
                    )
                }

                CardAction.GO_TO_JAIL -> {
                    Text(
                        text = "🚔",
                        fontSize = 40.sp
                    )
                    Text(
                        text = "Go to Jail",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }

                CardAction.GET_OUT_OF_JAIL -> {
                    Text(
                        text = "🔓",
                        fontSize = 40.sp
                    )
                    Text(
                        text = "Get Out of Jail Free",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}



