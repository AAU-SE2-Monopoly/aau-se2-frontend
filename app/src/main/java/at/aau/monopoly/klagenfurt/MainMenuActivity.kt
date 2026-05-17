package at.aau.monopoly.klagenfurt

import android.content.Intent
import android.os.Bundle
import at.aau.monopoly.klagenfurt.networking.SessionPreferences
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import at.aau.monopoly.klagenfurt.ui.components.DarkGradientBackground
import at.aau.monopoly.klagenfurt.ui.components.GradientDirection
import at.aau.monopoly.klagenfurt.ui.theme.MyApplicationTheme
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlue
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlueLight
import com.example.myapplication.R

class MainMenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        SessionPreferences.init(this)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                MainMenuScreen(
                    onPlayClicked = {
                        startActivity(Intent(this, LobbyActivity::class.java))
                    },
                    onCreditsClicked = {
                        startActivity(Intent(this, CreditsActivity::class.java))
                    },
                    onSettingsClicked = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun MainMenuScreen(
    onPlayClicked: () -> Unit,
    onCreditsClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    val accentBlue = PrimaryBlue
    val primaryBlue = PrimaryBlueLight

    // Slide-in animations
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val imageOffsetX by animateFloatAsState(
        targetValue = if (visible) 0f else -800f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "imageSlide"
    )
    val imageAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "imageAlpha"
    )
    val buttonsOffsetX by animateFloatAsState(
        targetValue = if (visible) 0f else 800f,
        animationSpec = tween(durationMillis = 800, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "buttonsSlide"
    )
    val buttonsAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 200),
        label = "buttonsAlpha"
    )

    // Play button pulse
    val infiniteTransition = rememberInfiniteTransition(label = "playPulse")
    val playScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playScale"
    )
    val playGlow by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playGlow"
    )

    DarkGradientBackground(gradientDirection = GradientDirection.HORIZONTAL) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left side – Game icon
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = imageOffsetX
                        alpha = imageAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.game_menu_icon),
                    contentDescription = "Game Icon",
                    modifier = Modifier
                        .size(280.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            // Right side – Title + Buttons
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = buttonsOffsetX
                        alpha = buttonsAlpha
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "MONOPOLY",
                    color = primaryBlue,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "KLAGENFURT EDITION",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Play button
                Button(
                    onClick = onPlayClicked,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(64.dp)
                        .scale(playScale)
                        .shadow(playGlow.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentBlue
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Text(
                        text = "▶  PLAY",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Credits & Settings buttons
                Row(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = onCreditsClicked,
                        modifier = Modifier
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Credits",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 2.sp
                        )
                    }

                    TextButton(
                        onClick = onSettingsClicked,
                        modifier = Modifier
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Settings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}








