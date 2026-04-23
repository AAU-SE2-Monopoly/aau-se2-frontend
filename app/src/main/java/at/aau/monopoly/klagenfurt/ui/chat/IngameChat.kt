package at.aau.monopoly.klagenfurt.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.ui.GameViewModel

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
            .heightIn(min = 24.dp, max = 32.dp)
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = lastEntry.text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
