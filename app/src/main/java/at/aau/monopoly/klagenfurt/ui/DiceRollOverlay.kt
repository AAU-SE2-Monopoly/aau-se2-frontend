package at.aau.monopoly.klagenfurt.ui

import android.os.SystemClock
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
    // Allow user to manually dismiss the overlay; resets when overlay leaves composition
    var userDismissed by remember { mutableStateOf(false) }

    if (!isVisible || userDismissed) return

    // displayRolling is controlled solely by the LaunchedEffect below,
    // NOT by remember(isRolling). This prevents the animation from being
    // aborted prematurely when isRolling flips to false on server response.
    // Reset when overlay reappears (isVisible toggles) so a second roll starts fresh
    var displayRolling by remember(isVisible) { mutableStateOf(true) }
    var displayResult by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var rollStartMs by remember { mutableStateOf(0L) }
    val minOverlayMs = 1200L

    // Control display state: start rolling → show animation,
    // server responds → wait minOverlayMs → show result.
    LaunchedEffect(diceResult, isRolling) {
        if (!isRolling && diceResult != null) {
            // Ensure the overlay stays visible for a minimum duration
            val elapsed = SystemClock.elapsedRealtime() - rollStartMs
            if (elapsed < minOverlayMs) {
                delay(minOverlayMs - elapsed)
            }
            displayResult = diceResult
            displayRolling = false
        } else if (isRolling) {
            // Start rolling animation
            rollStartMs = SystemClock.elapsedRealtime()
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
                    text = "🎲 Roll Dice",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Dice display (uses displayRolling so animation isn't cancelled early)
                DiceDisplay(
                    die1 = displayResult?.first ?: 1,
                    die2 = displayResult?.second ?: 1,
                    isRolling = displayRolling
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Result display (only when not rolling and result available)
                if (!displayRolling) {
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
                                text = "🎉 DOUBLE! 🎉",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B6B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Instructions – differentiate sensor-active vs waiting for minOverlayMs
                Text(
                    text = when {
                        displayRolling && isRolling -> "Shake your phone! 📱"
                        displayRolling -> "Waiting for result..."
                        displayResult != null -> "Dice result shown ✓"
                        else -> "Rolling..."
                    },
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.weight(1f))

                // Close button (only enabled when not rolling)
                Button(
                    onClick = {
                        userDismissed = true
                        onClose()
                    },
                    enabled = !displayRolling,
                    modifier = Modifier
                        .size(width = 150.dp, height = 40.dp)
                ) {
                    Text("Close")
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

    // Counter as LaunchedEffect key: increments on each roll so animateTo
    // runs the full 1500ms without being cancelled mid-roll by key changes.
    var animKey by remember { mutableStateOf(0) }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            animKey++  // trigger animation below
        }
    }

    LaunchedEffect(animKey) {
        if (animKey > 0) {
            rotation1.snapTo(0f)
            rotation2.snapTo(0f)
            rotation1.animateTo(
                targetValue = 360f * 3,
                animationSpec = tween(1500, easing = LinearEasing)
            )
            rotation2.animateTo(
                targetValue = 360f * 3,
                animationSpec = tween(1500, easing = LinearEasing)
            )
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
