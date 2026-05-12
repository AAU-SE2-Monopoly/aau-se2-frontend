package at.aau.monopoly.klagenfurt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.model.GameJoinStatus
import at.aau.monopoly.klagenfurt.ui.GameboardUI
import at.aau.monopoly.klagenfurt.ui.JoinViewModel
import at.aau.monopoly.klagenfurt.ui.theme.MyApplicationTheme
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlue
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlueLight
import com.example.myapplication.R

/**
 * Session-scoped set of game IDs the player has joined.
 * Survives activity restarts within the same process so returning
 * players are correctly detected even after [JoinActivity] is recreated.
 */
private val joinedGameIds = mutableSetOf<String>()

class JoinActivity : ComponentActivity() {


    private val viewModel: JoinViewModel by viewModels {
        JoinViewModel.Factory(ServiceLocator.provideGameService())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val joinStatus = intent.getStringExtra("JOIN_STATUS")
            ?.let { try { GameJoinStatus.valueOf(it) } catch (_: Exception) {null} }
            ?: GameJoinStatus.OPEN
        val gameId    = intent.getStringExtra("GAME_ID") ?: intent.getStringExtra("gameId") ?: ""
        val isNewGame = intent.getBooleanExtra("isNewGame", false)



        // Detect returning player – tracked per session
        val isReturningPlayer = !isNewGame && gameId in joinedGameIds

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                val joinState by viewModel.joinState.collectAsState()

                // React to terminal states: navigate on success, stay on error
                LaunchedEffect(joinState) {
                    when (val state = joinState) {
                        is JoinViewModel.JoinState.Success -> {
                            // Track this game as joined for future reconnection detection
                            joinedGameIds.add(state.gameId)
                            startActivity(
                                Intent(this@JoinActivity, GameboardUI::class.java)
                                    .putExtra("GAME_ID", state.gameId)
                            )
                            finish()
                        }
                        else -> Unit
                    }
                }

                val isConnected by viewModel.isConnected.collectAsState()
                val reconnectFailed by viewModel.reconnectFailed.collectAsState()

                JoinScreen(
                    gameId      = gameId,
                    isNewGame   = isNewGame,
                    joinState   = joinState,
                    joinStatus  = joinStatus,
                    isReturningPlayer = isReturningPlayer,
                    isConnected = isConnected,
                    reconnectFailed = reconnectFailed,
                    onReconnect = { viewModel.reconnect() },
                    onBackClicked = {
                        viewModel.resetState()
                        finish()
                    },
                    onJoin = { playerName, iconIndex ->
                        val iconId = GameJoinStatus.iconIdForIndex(iconIndex)
                        when {
                            isNewGame && joinStatus == GameJoinStatus.OPEN ->
                                viewModel.createGame(playerName, iconId)
                            else -> viewModel.joinGame(gameId, playerName, iconId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun JoinScreen(
    gameId: String,
    isNewGame: Boolean,
    joinState: JoinViewModel.JoinState,
    joinStatus: GameJoinStatus,
    isReturningPlayer: Boolean = false,
    isConnected: Boolean = true,
    reconnectFailed: Boolean = false,
    onBackClicked: () -> Unit,
    onJoin: (playerName: String, iconIndex: Int) -> Unit,
    onReconnect: () -> Unit = {}
) {
    val darkBackground = Color(0xFF0A0A2E)

    BackHandler(enabled = !joinState.let { it is JoinViewModel.JoinState.Loading }) {
        onBackClicked()
    }

    // Early return for FINISHED – show static message
    if (joinStatus == GameJoinStatus.FINISHED) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(darkBackground, Color(0xFF16213E), darkBackground)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GAME FINISHED",
                    color = Color.Gray,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This game has already ended.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }

            // Back button for finished screen
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
        }
        return
    }

    val isFull = joinStatus == GameJoinStatus.FULL
    val isInProgress = joinStatus == GameJoinStatus.IN_PROGRESS
    val isReconnectFlow = isInProgress || isReturningPlayer

    val playerIcons = listOf(
        R.drawable.lindwurm,
        R.drawable.woertherseemandl,
        R.drawable.gti,
        R.drawable.ironman,
        R.drawable.josef
    )

    var playerName by rememberSaveable { mutableStateOf("") }
    var selectedIconIndex by rememberSaveable { mutableIntStateOf(0) }

    val isLoading = joinState is JoinViewModel.JoinState.Loading
    val errorMessage = (joinState as? JoinViewModel.JoinState.Error)?.message
    val interactionDisabled = !isConnected || isLoading || isFull

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(darkBackground, Color(0xFF16213E), darkBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when {
                    isReconnectFlow -> "RECONNECT"
                    isNewGame -> "CREATE GAME"
                    else -> "JOIN GAME"
                },
                color = PrimaryBlueLight,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("ScreenTitle")
            )

            if (!isNewGame && gameId.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Game: ${gameId.take(8)}…",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            // Status-specific messages
            if (isFull) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This game is currently full.",
                    color = Color(0xFFEF9A9A),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isReconnectFlow) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isInProgress) {
                        "Game already in progress – you can rejoin as your previous player."
                    } else {
                        "You have already joined this game. You can rejoin."
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Connection warning – shown only when idle and not connected
            if (!isConnected && joinState is JoinViewModel.JoinState.Idle) {
                if (reconnectFailed) {
                    Button(
                        onClick = onReconnect,
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(56.dp)
                            .testTag("ReconnectButton"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(
                            text = "RECONNECT",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = if (reconnectFailed) "Connection lost" else "Connecting to server…",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Error message from server (e.g. game full)
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF9A9A),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Icon chooser and name input – hidden during reconnect flow
            if (!isReconnectFlow) {
                // Icon chooser
                Button(
                    onClick = { selectedIconIndex = (selectedIconIndex + 1) % playerIcons.size },
                    enabled = !interactionDisabled,
                    modifier = Modifier.size(90.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A237E).copy(alpha = 0.6f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = playerIcons[selectedIconIndex]),
                            contentDescription = "Selected Icon",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to change icon",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Player name text field
                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    label = { Text("Player Name") },
                    singleLine = true,
                    enabled = !interactionDisabled,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .testTag("PlayerNameInput"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlueLight,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = PrimaryBlueLight,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = PrimaryBlueLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            Button(
                onClick = {
                    val name = if (isReconnectFlow) "" else playerName.ifBlank { "Player" }
                    onJoin(name, selectedIconIndex)
                },
                enabled = !interactionDisabled,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(56.dp)
                    .testTag("ActionButton"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when {
                            isReconnectFlow -> "RECONNECT"
                            isNewGame -> "CREATE & JOIN"
                            else -> "JOIN GAME"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Back button – always rendered and functional
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
    }
}