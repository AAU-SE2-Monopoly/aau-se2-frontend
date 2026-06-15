package at.aau.monopoly.klagenfurt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.model.Player

@Composable
fun GameOverOverlay(
    isVisible: Boolean,
    activePlayers: List<Player>,
    onBackToLobby: () -> Unit
) {
    if (!isVisible) return

    val showWinner = activePlayers.size == 1
    val winner = activePlayers.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .shadow(18.dp, RoundedCornerShape(26.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFF8E1),
                            Color(0xFFFFECB3),
                            Color(0xFFFFF3E0)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .border(2.dp, Color(0xFFFFC107), RoundedCornerShape(26.dp))
                .padding(horizontal = 28.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🏆", fontSize = 44.sp)

            Text(
                text = "Game Over",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3E2723),
                textAlign = TextAlign.Center
            )

            if (showWinner) {
                Text(
                    text = "Winner",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8D6E63),
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = winner?.name ?: "Unknown",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1B5E20),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Players still in game",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )

                activePlayers.forEach { player ->
                    PlayerSummaryRow(player)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onBackToLobby,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3E2723),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Back to Lobby",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlayerSummaryRow(player: Player) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = player.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3E2723)
        )

        Text(
            text = "${player.money} € · 🏠 ${player.ownedPropertyIds.size}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4E342E)
        )
    }
}