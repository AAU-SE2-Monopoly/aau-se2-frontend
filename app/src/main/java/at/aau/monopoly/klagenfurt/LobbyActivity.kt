package at.aau.monopoly.klagenfurt

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import at.aau.monopoly.klagenfurt.messaging.dtos.GameLobbyInfo
import at.aau.monopoly.klagenfurt.messaging.dtos.joinStatusFor
import at.aau.monopoly.klagenfurt.model.GameCardStatus
import at.aau.monopoly.klagenfurt.model.cardStatus
import at.aau.monopoly.klagenfurt.networking.SessionPreferences
import at.aau.monopoly.klagenfurt.ui.GameboardUI
import at.aau.monopoly.klagenfurt.ui.LobbyViewModel
import at.aau.monopoly.klagenfurt.ui.theme.MyApplicationTheme
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlue
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlueLight
import kotlinx.coroutines.launch

class LobbyActivity : ComponentActivity() {

    private val viewModel: LobbyViewModel by viewModels {
        LobbyViewModel.Factory(ServiceLocator.provideGameService())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionPreferences.init(this)
        enableEdgeToEdge()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.rejoinNavigation.collect { gameId ->
                        startActivity(
                            Intent(this@LobbyActivity, GameboardUI::class.java)
                                .putExtra("GAME_ID", gameId)
                        )
                    }
                }
                launch {
                    viewModel.rejoinErrors.collect { message ->
                        Toast.makeText(this@LobbyActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                LobbyScreen(
                    viewModel = viewModel,
                    onBackClicked = { finish() },
                    onGameClicked = { game ->
                        val isInGame = game.playerIds.contains(viewModel.currentPlayerId)
                        if(isInGame){
                            viewModel.rejoinGame(game.gameId)
                        }else {
                            startActivity(
                                Intent(this,JoinActivity::class.java)
                                    .putExtra("gameId", game.gameId)
                                    .putExtra("isNewGame", false)
                                    .putExtra("JOIN_STATUS", game.joinStatusFor(viewModel.currentPlayerId).name)
                            )
                        }

                    },
                    onCreateGame = {
                        startActivity(
                            Intent(this, JoinActivity::class.java)
                                .putExtra("isNewGame", true)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel,
    onBackClicked: () -> Unit,
    onGameClicked: (GameLobbyInfo) -> Unit,
    onCreateGame: () -> Unit
) {
    val darkBackground = Color(0xFF0A0A2E)
    val isConnected by viewModel.isConnected.collectAsState()
    val reconnectFailed by viewModel.reconnectFailed.collectAsState()
    val games by viewModel.games.collectAsState()

    // When connected, subscribe to lobby and request game list
    LaunchedEffect(isConnected) {
        if (isConnected) {
            viewModel.onConnected()
        }
    }

    // Refresh lobby subscription and game list every time we come back to this screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLobby()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        darkBackground,
                        Color(0xFF16213E),
                        darkBackground
                    )
                )
            )
    ) {
        // Center content – game list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "OPEN GAMES",
                color = PrimaryBlueLight,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isConnected) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(
                    color = PrimaryBlueLight,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Looking for games…",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "+" card to create a new game
                    item {
                        CreateGameCard(onClick = onCreateGame, enabled = isConnected)
                    }

                    // Existing open games
                    items(games, key = { it.gameId }) { game ->
                        GameCard(
                            game = game,
                            isOwnGame = game.hostPlayerId == viewModel.currentPlayerId,
                            currentPlayerId = viewModel.currentPlayerId,
                            isConnected = isConnected,
                            onClick = { onGameClicked(game) },
                            onClose = { viewModel.closeGame(game.gameId) }
                        )
                    }
                }
            }
        }

        // Back button – top-left
        Button(
            onClick = onBackClicked,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
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

        // Connection indicator – top-right
        if (reconnectFailed && !isConnected) {
            Button(
                onClick = { viewModel.reconnect() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Reconnect",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isConnected) Color(0xFF2E7D32).copy(alpha = 0.8f)
                        else Color(0xFFE65100).copy(alpha = 0.8f)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isConnected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Connected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Connected",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Connecting…",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun CreateGameCard(onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) PrimaryBlue.copy(alpha = 0.3f) else Color(0xFF424242).copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Game",
                tint = PrimaryBlueLight,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NEW GAME",
                color = PrimaryBlueLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun GameCard(
    game: GameLobbyInfo,
    isOwnGame: Boolean,
    currentPlayerId: String,
    isConnected: Boolean = true,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    var showCloseDialog by remember { mutableStateOf(false) }
    val isInGame = game.playerIds.contains(currentPlayerId)
    val status = game.cardStatus()
    val isInteractable = when (status) {
        GameCardStatus.Open       -> true
        GameCardStatus.Full       -> isInGame
        GameCardStatus.InProgress -> isInGame
        GameCardStatus.Finished   -> false
    }

    val cardBackground = when (status) {
        GameCardStatus.Open       -> Color(0xFF1A237E).copy(alpha = 0.6f)
        GameCardStatus.Full       -> Color(0xFF424242).copy(alpha = 0.7f)
        GameCardStatus.InProgress -> Color(0xFF1B5E20).copy(alpha = 0.7f)
        GameCardStatus.Finished   -> Color(0xFF212121).copy(alpha = 0.5f)
    }

    val statusBadgeText = when (status) {
        GameCardStatus.Open       -> null
        GameCardStatus.Full       -> "FULL"
        GameCardStatus.InProgress -> "IN PROGRESS"
        GameCardStatus.Finished   -> "FINISHED"
    }

    val statusBadgeColor = when (status) {
        GameCardStatus.Full       -> Color(0xFFB71C1C).copy(alpha = 0.85f)
        GameCardStatus.InProgress -> Color(0xFFF57F17).copy(alpha = 0.85f)
        GameCardStatus.Finished   -> Color(0xFF424242).copy(alpha = 0.85f)
        else                      -> Color.Transparent
    }

    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            title = { Text("Close Game") },
            text = { Text("Are you sure you want to close this game?") },
            confirmButton = {
                TextButton(onClick = {
                    showCloseDialog = false
                    onClose()
                }) {
                    Text("Yes, close")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val clickable = isInteractable && isConnected

    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (clickable) cardBackground else Color(0xFF424242).copy(alpha = 0.5f))
            .clickable(enabled = clickable, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status badge
            if (statusBadgeText != null) {
                Text(
                    text = statusBadgeText,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .background(statusBadgeColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = "🎲",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = game.hostPlayerName.ifBlank { "Unknown" },
                color = if (isInteractable) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${game.playerCount}/${game.maxPlayers} Players",
                color = Color.White.copy(alpha = if (isInteractable) 0.7f else 0.4f),
                fontSize = 12.sp
            )
        }

        // Close button for own games – also requires connection
        if (isOwnGame && isConnected) {
            IconButton(
                onClick = { showCloseDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.7f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Game",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
