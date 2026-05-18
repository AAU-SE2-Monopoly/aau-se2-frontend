package at.aau.monopoly.klagenfurt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.ui.components.BackButton
import at.aau.monopoly.klagenfurt.ui.components.DarkGradientBackground
import at.aau.monopoly.klagenfurt.ui.theme.MyApplicationTheme
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlueLight

class CreditsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                CreditsScreen(onBackClicked = { finish() })
            }
        }
    }
}

@Composable
fun CreditsScreen(onBackClicked: () -> Unit) {
    val accentBlue = PrimaryBlueLight

    val teamMembers = listOf(
        "Alwin J. Schuster",
        "David Essl",
        "Joachim Lugger",
        "Pablo Tolazzi",
        "Christian Wascher"
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    DarkGradientBackground {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 2 }
        ) {
            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CREDITS",
                color = accentBlue,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Monopoly - Klagenfurt Edition",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.3f),
                thickness = 1.dp,
                color = accentBlue.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DEVELOPMENT TEAM",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            teamMembers.forEach { name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentBlue.copy(alpha = 0.7f))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.3f),
                thickness = 1.dp,
                color = accentBlue.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "AAU · Software Engineering II · 2025",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }

        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -it }
        ) {
            BackButton(
                onClick = onBackClicked
            )
        }
    }
}
