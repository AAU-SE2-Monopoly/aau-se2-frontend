package at.aau.monopoly.klagenfurt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlue
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlueLight

val DarkBackground = Color(0xFF0A0A2E)
val MidBackground = Color(0xFF16213E)

@Composable
fun DarkGradientBackground(
    gradientDirection: GradientDirection = GradientDirection.VERTICAL,
    content: @Composable BoxScope.() -> Unit
) {
    val gradient = when (gradientDirection) {
        GradientDirection.VERTICAL -> Brush.verticalGradient(
            colors = listOf(DarkBackground, MidBackground, DarkBackground)
        )
        GradientDirection.HORIZONTAL -> Brush.horizontalGradient(
            colors = listOf(DarkBackground, MidBackground, DarkBackground)
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        content = content
    )
}

enum class GradientDirection { VERTICAL, HORIZONTAL }

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Back",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Shared animated screen scaffold used by Settings, Credits, and similar screens.
 * Provides a dark gradient background, an enter animation for the content, and
 * an animated back button pinned to the top-start corner.
 */
@Composable
fun AnimatedScreenScaffold(
    onBackClicked: () -> Unit,
    content: @Composable () -> Unit
) {
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
                content()
            }
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -it }
        ) {
            BackButton(onClick = onBackClicked)
        }
    }
}

/**
 * Reusable screen title block with accent-colored heading and decorative dividers.
 */
@Composable
fun ScreenTitle(title: String) {
    val accentBlue = PrimaryBlueLight
    Text(
        text = title,
        color = accentBlue,
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 6.sp,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(0.3f),
        thickness = 1.dp,
        color = accentBlue.copy(alpha = 0.3f)
    )
}
