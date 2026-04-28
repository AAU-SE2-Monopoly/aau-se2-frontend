package at.aau.monopoly.klagenfurt.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun DiceRollOverlay(
    isVisible: Boolean,
    diceResult: Pair<Int, Int>? = null,
    isRolling: Boolean = false,
    onClose: () -> Unit
) {
    if (!isVisible) return

    var displayRolling by remember(isRolling) { mutableStateOf(isRolling) }
    var displayResult by remember { mutableStateOf(diceResult) }

    // Wait for backend response before stopping animation
    LaunchedEffect(diceResult, isRolling) {
        if (!isRolling && diceResult != null) {
            // Backend response received - show with 500ms delay for smooth transition
            delay(500)
            displayResult = diceResult
            displayRolling = false
        } else if (isRolling) {
            // Start rolling animation
            displayRolling = true
            displayResult = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .testTag("dice_roll_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(300.dp, 400.dp)
                .padding(16.dp),
            color = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎲 Würfeln",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Dice display
                DiceDisplay(
                    die1 = displayResult?.first ?: 1,
                    die2 = displayResult?.second ?: 1,
                    isRolling = displayRolling
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Result display (only when not rolling and result available)
                if (!displayRolling && displayResult != null) {
                    val result = displayResult
                    if (result != null) {
                        val total = result.first + result.second
                        val isDouble = result.first == result.second

                        Text(
                            text = "Total: $total",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        if (isDouble) {
                            Text(
                                text = "🎉 DOPPEL! 🎉",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B6B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Instructions
                Text(
                    text = if (displayRolling) {
                        "Handy schütteln! 📱"
                    } else if (displayResult != null) {
                        "Würfelergebnis angezeigt ✓"
                    } else {
                        "Würfeln..."
                    },
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.weight(1f))

                // Close button (only enabled when not rolling)
                Button(
                    onClick = onClose,
                    enabled = !displayRolling,
                    modifier = Modifier
                        .size(width = 150.dp, height = 40.dp)
                ) {
                    Text("Schließen")
                }
            }
        }
    }
}

@Composable
private fun DiceDisplay(
    die1: Int,
    die2: Int,
    isRolling: Boolean
) {
    val rotation1 = remember { Animatable(0f) }
    val rotation2 = remember { Animatable(0f) }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            // Start fast rotation
            rotation1.animateTo(
                targetValue = 360f * 3,
                animationSpec = tween(1500, easing = LinearEasing)
            )
            rotation2.animateTo(
                targetValue = 360f * 3,
                animationSpec = tween(1500, easing = LinearEasing)
            )
        } else {
            // Animation finished - reset for next roll
            rotation1.snapTo(0f)
            rotation2.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .size(200.dp, 100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Die 1 (Red)
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = (-50).dp)
                .background(Color(0xFFE0000F), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = die1.toString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Die 2 (Blue)
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = 50.dp)
                .background(Color(0xFF000080), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = die2.toString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
