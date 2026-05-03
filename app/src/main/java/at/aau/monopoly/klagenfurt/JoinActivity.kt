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
import at.aau.monopoly.klagenfurt.ui.GameboardUI
import at.aau.monopoly.klagenfurt.ui.JoinViewModel
import at.aau.monopoly.klagenfurt.ui.theme.MyApplicationTheme
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlue
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlueLight
import com.example.myapplication.R

class JoinActivity : ComponentActivity() {

    private val viewModel: JoinViewModel by viewModels {
        JoinViewModel.Factory(ServiceLocator.provideGameService())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val gameId    = intent.getStringExtra("GAME_ID") ?: intent.getStringExtra("gameId") ?: ""
        val isNewGame = intent.getBooleanExtra("isNewGame", false)

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                val joinState by viewModel.joinState.collectAsState()

                // React to terminal states: navigate on success, stay on error
                LaunchedEffect(joinState) {
                    when (val state = joinState) {
                        is JoinViewModel.JoinState.Success -> {
                            startActivity(
                                Intent(this@JoinActivity, GameboardUI::class.java)
                                    .putExtra("GAME_ID", state.gameId)
                            )
                            finish()
                        }
                        else -> Unit
                    }
                }

                JoinScreen(
                    gameId      = gameId,
                    isNewGame   = isNewGame,
                    joinState   = joinState,
                    onBackClicked = {
                        viewModel.resetState()
                        finish()
                    },
                    onJoin = { playerName, iconIndex ->
                        val iconId = mapIndexToIconId(iconIndex)
                        if (isNewGame) {
                            viewModel.createGame(playerName, iconId)
                        } else {
                            viewModel.joinGame(gameId, playerName, iconId)
                        }
                    }
                )
            }
        }
    }

    companion object {
        fun mapIndexToIconId(iconIndex: Int): String = when (iconIndex) {
            0    -> "lindwurm"
            1    -> "woerthersee"
            2    -> "gti"
            3    -> "ironman"
            4    -> "josef"
            else -> "lindwurm"
        }
    }
}

@Composable
fun JoinScreen(
    gameId: String,
    isNewGame: Boolean,
    joinState: JoinViewModel.JoinState,
    onBackClicked: () -> Unit,
    onJoin: (playerName: String, iconIndex: Int) -> Unit
) {
    val darkBackground = Color(0xFF0A0A2E)

    BackHandler(enabled = !joinState.let { it is JoinViewModel.JoinState.Loading }) {
        onBackClicked()
    }

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
                text = if (isNewGame) "CREATE GAME" else "JOIN GAME",
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

            // Icon chooser
            Button(
                onClick = { selectedIconIndex = (selectedIconIndex + 1) % playerIcons.size },
                enabled = !isLoading,
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

            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("Player Name") },
                singleLine = true,
                enabled = !isLoading,
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

            Button(
                onClick = {
                    val name = playerName.ifBlank { "Player" }
                    onJoin(name, selectedIconIndex)
                },
                enabled = !isLoading,
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
                        text = if (isNewGame) "CREATE & JOIN" else "JOIN GAME",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Back button
        Button(
            onClick = onBackClicked,
            enabled = !isLoading,
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