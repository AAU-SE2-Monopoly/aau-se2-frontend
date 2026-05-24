package at.aau.monopoly.klagenfurt.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.ui.GameViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CHAT_COLLAPSED_TAG = "ingame_chat_collapsed"
private const val CHAT_EXPANDED_TAG = "ingame_chat_expanded"
private const val CHAT_LINES_TAG = "ingame_chat_lines"

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
    var isExpanded by remember { mutableStateOf(false) }
    val overlayStateTag = if (isExpanded) CHAT_EXPANDED_TAG else CHAT_COLLAPSED_TAG
    val visibleEntries = entries.filter { !it.isTechnical }
    val lastEntry = visibleEntries.lastOrNull()

    Column(
        modifier = modifier
            .testTag(overlayStateTag)
            .fillMaxWidth(if (isExpanded) 0.65f else 0.35f)
            .animateContentSize(animationSpec = tween(durationMillis = 150))
            .clickable { isExpanded = !isExpanded },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EventLogStatusBar(lastEntry = lastEntry, isExpanded = isExpanded)

        if (isExpanded) {
            ExpandedEventLogList(entries = visibleEntries)
        }
    }
}

private fun getLastVisibleEntry(
    entries: List<GameViewModel.LogEntry>,
    isExpanded: Boolean
): GameViewModel.LogEntry? {
    if (isExpanded) return entries.lastOrNull()
    return entries.lastOrNull { !it.isTechnical }
}

@Composable
private fun EventLogStatusBar(
    lastEntry: GameViewModel.LogEntry?,
    isExpanded: Boolean
) {
    val shape = if (isExpanded) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    } else {
        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp, max = 36.dp)
            .background(
                color = Color.Black.copy(alpha = 0.35f),
                shape = shape
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = lastEntry?.text ?: "Waiting for events...",
            color = Color.White,
            fontSize = if (isExpanded) 13.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExpandedEventLogList(entries: List<GameViewModel.LogEntry>) {
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                color = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
            .padding(12.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag(CHAT_LINES_TAG)
        ) {
            items(entries) { entry ->
                val time = timeFormat.format(Date(entry.timestampMs))
                Text(
                    text = "[$time] • ${entry.text}",
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}
