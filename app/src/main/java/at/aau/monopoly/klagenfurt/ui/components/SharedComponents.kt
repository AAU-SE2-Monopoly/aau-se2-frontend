package at.aau.monopoly.klagenfurt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlue

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

