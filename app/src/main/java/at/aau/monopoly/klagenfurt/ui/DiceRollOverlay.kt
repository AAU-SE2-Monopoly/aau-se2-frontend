package at.aau.monopoly.klagenfurt.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun DiceRollOverlay(
    isVisible: Boolean,
    diceResult: Pair<Int, Int>? = null,
    isRolling: Boolean = false,
    hasShaken: Boolean = false,
    onShakeButton: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    // Allow user to manually dismiss the overlay; resets when overlay reappears
    var userDismissed by remember { mutableStateOf(false) }

    // Reset userDismissed when overlay reappears
    LaunchedEffect(isVisible) {
        if (isVisible) userDismissed = false
    }

    if (!isVisible || userDismissed) return

    // Three visual states:
    //   1. Idle    – overlay open, waiting for the user to shake (no animation, no result).
    //   2. Rolling – user has shaken, network request fired; dice animate.
    //   3. Result  – server response received and minimum overlay duration elapsed.
    //
    // displayRolling is controlled solely by the LaunchedEffects below,
    // NOT by remember(isRolling). This prevents the animation from being
    // aborted prematurely when isRolling flips to false on server response.
    var displayRolling by remember(isVisible) { mutableStateOf(false) }
    var displayResult by remember(isVisible) { mutableStateOf<Pair<Int, Int>?>(null) }
    var rollStartMs by remember(isVisible) { mutableStateOf(0L) }
    val minOverlayMs = 1200L

    // Start the rolling animation the moment the user shakes the device.
    LaunchedEffect(hasShaken) {
        if (hasShaken && rollStartMs == 0L) {
            rollStartMs = SystemClock.elapsedRealtime()
            displayRolling = true
            displayResult = null
        }
    }

    // Wait for the server response (diceResult populated, isRolling false),
    // then enforce the minimum overlay duration before revealing the result.
    LaunchedEffect(diceResult, isRolling, hasShaken) {
        if (hasShaken && !isRolling && diceResult != null) {
            val safeStart = rollStartMs.let {
                if (it > 0L) it
                else {
                    Log.w("DiceRollOverlay", "rollStartMs was 0 — using fallback start time")
                    SystemClock.elapsedRealtime() - 1500L
                }
            }
            val elapsed = SystemClock.elapsedRealtime() - safeStart
            if (elapsed < minOverlayMs) {
                delay(minOverlayMs - elapsed)
            }
            displayResult = diceResult
            displayRolling = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dice_roll_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(300.dp, 320.dp)
                .padding(16.dp),
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // X close button at top-right
                Text(
                    text = "✕",
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clickable(enabled = !displayRolling || !hasShaken) {
                            userDismissed = true
                            onClose()
                        }
                        .testTag("dice_close_x")
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎲 Roll Dice",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dice display (uses displayRolling so animation isn't cancelled early)
                    DiceDisplay(
                        die1 = displayResult?.first ?: 1,
                        die2 = displayResult?.second ?: 1,
                        isRolling = displayRolling
                    )

                    Spacer(modifier = Modifier.height(4.dp))

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
                                color = Color.White,
                                modifier = Modifier.testTag("dice_total_text")
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Instructions – distinguish idle (waiting for shake), rolling (network), result.
                    Text(
                        text = when {
                            !hasShaken -> "Shake your phone!"
                            displayRolling && isRolling -> "Rolling..."
                            displayRolling -> "Waiting for result..."
                            else -> ""
                        },
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.testTag("dice_instruction_text")
                    )

                    if (!hasShaken && onShakeButton != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onShakeButton,
                            modifier = Modifier
                                .size(width = 150.dp, height = 40.dp)
                                .testTag("shake_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Shake")
                        }
                    }
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
            coroutineScope {
                launch {
                    rotation1.animateTo(
                        targetValue = 360f * 3,
                        animationSpec = tween(1500, easing = LinearEasing)
                    )
                }
                launch {
                    rotation2.animateTo(
                        targetValue = 360f * 3,
                        animationSpec = tween(1500, easing = LinearEasing)
                    )
                }
            }
        }
    }

    // While rolling, animate the displayed numbers to simulate the dice tumbling.
    // After the roll result is known (displayResult becomes non-null in the parent),
    // isRolling will be false and the actual die values are shown.
    var displayDie1 by remember { mutableIntStateOf(1) }
    var displayDie2 by remember { mutableIntStateOf(1) }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            // Rapidly change the shown numbers while rolling to simulate tumbling
            while (true) {
                displayDie1 = Random.nextInt(1, 7)
                displayDie2 = Random.nextInt(1, 7)
                delay(100L)
            }
        } else {
            // When rolling ends, show the final (actual) values
            displayDie1 = die1
            displayDie2 = die2
        }
    }

    Box(
        modifier = Modifier
            .size(200.dp, 100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Die 1 (Red) – rotating during rolling
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = (-50).dp)
                .rotate(if (isRolling) rotation1.value else 0f)
                .background(Color(0xFFE0000F), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            DiceDots(value = displayDie1, dotColor = Color.White)
        }

        // Die 2 (Blue) – rotating during rolling
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = 50.dp)
                .rotate(if (isRolling) rotation2.value else 0f)
                .background(Color(0xFF000080), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            DiceDots(value = displayDie2, dotColor = Color.White)
        }
    }
}

@Composable
private fun DiceDots(value: Int, dotColor: Color) {
    Canvas(modifier = Modifier.size(60.dp)) {
        val r = size.width * 0.09f
        val p1 = size.width * 0.25f
        val p2 = size.width * 0.50f
        val p3 = size.width * 0.75f

        val positions = when (value) {
            1 -> listOf(Offset(p2, p2))
            2 -> listOf(Offset(p1, p3), Offset(p3, p1))
            3 -> listOf(Offset(p1, p3), Offset(p2, p2), Offset(p3, p1))
            4 -> listOf(Offset(p1, p1), Offset(p1, p3), Offset(p3, p1), Offset(p3, p3))
            5 -> listOf(Offset(p1, p1), Offset(p1, p3), Offset(p2, p2), Offset(p3, p1), Offset(p3, p3))
            6 -> listOf(Offset(p1, p1), Offset(p1, p2), Offset(p1, p3), Offset(p3, p1), Offset(p3, p2), Offset(p3, p3))
            else -> emptyList()
        }

        positions.forEach { pos ->
            drawCircle(color = dotColor, radius = r, center = pos)
        }
    }
}
