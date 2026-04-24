package at.aau.monopoly.klagenfurt.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.ui.GameViewModel

private val TaperedTopBarShape = GenericShape { size, _ ->
    val slant = size.height * 0.7f
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
    var isExpanded by remember { mutableStateOf(false) }
    val overlayStateTag = if (isExpanded) "ingame_chat_expanded" else "ingame_chat_collapsed"

    // Collapsed view shows only relevant game events
    val collapsedEntries = entries.filter { !it.isTechnical }
    val lastEntry = if (isExpanded) entries.lastOrNull() else collapsedEntries.lastOrNull()

    Column(
        modifier = modifier
            .testTag(overlayStateTag)
            .fillMaxWidth(if (isExpanded) 0.65f else 0.35f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { isExpanded = !isExpanded }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Bar (Top)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 28.dp, max = 36.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = TaperedTopBarShape
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = lastEntry?.text ?: "Waiting for events...",
                color = if (lastEntry?.isTechnical == true) Color.LightGray else Color.White,
                fontSize = if (isExpanded) 13.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }


        if (isExpanded) {
            val listState = rememberLazyListState()


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
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ingame_chat_lines")
                ) {
                    items(entries) { entry ->
                        Text(
                            text = "• ${entry.text}",
                            color = if (entry.isTechnical) Color.Gray.copy(alpha = 0.8f) else Color.White,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = if (entry.isTechnical) FontWeight.Normal else FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
