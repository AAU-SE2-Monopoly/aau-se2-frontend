package at.aau.monopoly.klagenfurt.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.ui.GameViewModel

private val TaperedTopBarShape = GenericShape { size, _ ->
    val slant = size.height * 0.7f // Adjust this value to change the "shrink" intensity
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width - slant, size.height)
    lineTo(slant, size.height)
    close()
}

@Composable
fun ChatOverlay(
    entries: List<GameViewModel.LogEntry>,
    modifier: Modifier = Modifier
) {
    EventLogOverlay(entries = entries, modifier = modifier)
}

@Composable
fun EventLogOverlay(
    entries: List<GameViewModel.LogEntry>,
    modifier: Modifier = Modifier
) {
    val lastEntry = entries.lastOrNull() ?: return

    Box(
        modifier = modifier
            .fillMaxWidth(0.35f)
            .heightIn(min = 26.dp, max = 34.dp)
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = TaperedTopBarShape
            )
            .padding(horizontal = 20.dp), // Increased horizontal padding for tapered shape
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = lastEntry.text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
