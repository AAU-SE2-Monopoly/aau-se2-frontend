package at.aau.monopoly.klagenfurt.ui.board

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.ui.util.getPlayerTokenResource

@Composable
fun PlayerToken(
    player: Player,
    playerIndex: Int,
    sw: Float,
    sh: Float
) {
    val bounds = remember(player.position, sw, sh) {
        calculateFieldBounds(player.position, sw, sh)
    }

    val maxCols = 3
    val maxRows = 2

    val tokenSizeX = (bounds.width / maxCols) * 0.8f
    val tokenSizeY = (bounds.height / maxRows) * 0.8f
    val tokenSize = minOf(tokenSizeX, tokenSizeY).coerceAtMost(16f)

    val column = playerIndex % maxCols
    val row = playerIndex / maxCols

    val gridWidth = maxCols * tokenSize
    val gridHeight = maxRows * tokenSize
    val startX = (bounds.width - gridWidth) / 2
    val startY = (bounds.height - gridHeight) / 2

    val offsetX = startX + (column * tokenSize)
    val offsetY = startY + (row * tokenSize)

    Image(
        painter = painterResource(id = getPlayerTokenResource(player.iconId)),
        contentDescription = player.name,
        modifier = Modifier
            .offset(
                x = (bounds.x + offsetX).dp,
                y = (bounds.y + offsetY).dp
            )
            .size(tokenSize.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White)
    )
}
