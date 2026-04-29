package at.aau.monopoly.klagenfurt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import at.aau.monopoly.klagenfurt.ui.GameViewModel
import at.aau.monopoly.klagenfurt.ui.GameboardUI
import com.example.myapplication.R
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.launch


class GameActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels {
        GameViewModel.Factory(ServiceLocator.provideGameService())
    }
    private lateinit var btnGameBoard: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvEventLog: TextView
    private lateinit var etPlayerName: EditText
    private lateinit var etGameId: EditText
    private lateinit var spinnerAction: Spinner
    private lateinit var btnSend: Button
    private lateinit var btnConnect: Button
    private lateinit var btnClear: Button
    private lateinit var scrollEvents: android.widget.FrameLayout

    enum class GameActionItem(val label: String) {
        CREATE_GAME("Create Game"),
        JOIN_GAME("Join Game"),
        START_GAME("Start Game"),
        ROLL_DICE("Roll Dice"),
        END_TURN("End Turn"),
        GET_STATE("Get State"),
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        initViews()
        setupSpinner()
        setupClickListeners()
        observeViewModel()
    }

    private fun initViews() {
        btnGameBoard = findViewById(R.id.button_gameboard)
        tvStatus = findViewById(R.id.tv_status)
        tvEventLog = findViewById(R.id.tv_event_log)
        etPlayerName = findViewById(R.id.et_player_name)
        etGameId = findViewById(R.id.et_game_id)
        spinnerAction = findViewById(R.id.spinner_action)
        btnSend = findViewById(R.id.btn_send)
        btnConnect = findViewById(R.id.btn_connect)
        btnClear = findViewById(R.id.btn_clear)
        scrollEvents = findViewById<android.widget.FrameLayout>(R.id.scroll_events)

    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            GameActionItem.entries.map { it.label }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAction.adapter = adapter
    }

    private fun setupClickListeners() {
        btnConnect.setOnClickListener {
            tvStatus.text = getString(R.string.status_connecting)
            viewModel.connect()
        }
        btnGameBoard.setOnClickListener {
            val gameId = etGameId.text.toString().trim()

            if (gameId.isEmpty()) {
                toast(getString(R.string.error_enter_game_id))
                return@setOnClickListener
            }

            viewModel.setGameId(gameId)

            val intent = Intent(this, GameboardUI::class.java).apply {
                putExtra("GAME_ID", gameId)
            }
            Log.d("DiceDebug", "Opening GameboardUI with GAME_ID=$gameId")
            startActivity(intent)
        }
        btnClear.setOnClickListener { tvEventLog.text = "" }

        btnSend.setOnClickListener {
            val selected = GameActionItem.entries[spinnerAction.selectedItemPosition]
            val playerName = etPlayerName.text.toString().ifBlank { "Player1" }
            val gameId = etGameId.text.toString().trim()

            when (selected) {
                GameActionItem.CREATE_GAME -> {
                    viewModel.createGame(playerName)
                    appendLog("→ CREATE_GAME player=$playerName")


                }
                GameActionItem.JOIN_GAME -> {
                    if (gameId.isEmpty()) { toast(getString(R.string.error_enter_game_id)); return@setOnClickListener }
                    viewModel.setGameId(gameId)  // ✨ Set gameId immediately
                    viewModel.joinGame(gameId, playerName)
                    appendLog("→ JOIN_GAME gameId=$gameId player=$playerName")

                }
                else -> {
                    if (gameId.isEmpty()) { 
                        toast(getString(R.string.error_enter_game_id))
                        appendLog("❌ Cannot execute ${selected.name} - please join a game first or click 'GameBoard'")
                        return@setOnClickListener 
                    }
                    viewModel.setGameId(gameId)
                    when (selected) {
                        GameActionItem.START_GAME -> viewModel.startGame()
                        GameActionItem.ROLL_DICE -> viewModel.rollDice()
                        GameActionItem.END_TURN -> viewModel.endTurn()
                        GameActionItem.GET_STATE -> viewModel.requestState()
                       // else -> {}
                    }
                    appendLog("→ ${selected.name} gameId=$gameId")
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.status.collect { message -> tvStatus.text = message }
                }

                launch {
                    viewModel.events.collect { rawJson -> handleGameEvent(rawJson) }
                }
                launch {
                    viewModel.isGameReady.collect { isGameReady ->
                        btnGameBoard.isEnabled = isGameReady
                    }
                }
            }
        }
    }

    private val prettyMapper = jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    private fun handleGameEvent(rawJson: String) {
        try {
            val node: JsonNode = prettyMapper.readTree(rawJson)
            val event = node.get("event")?.asText() ?: ""
            val gameId = node.get("gameId")?.asText() ?: ""
            if (event == "GAME_CREATED" && gameId.isNotEmpty()) {
                etGameId.setText(gameId)
                viewModel.setGameId(gameId)
            }
        } catch (e: Exception) {
            appendLog("handleGameEvent exception: $e")
        }

        val pretty = try {
            val node = prettyMapper.readTree(rawJson)
            prettyMapper.writeValueAsString(node)
        } catch (_: Exception) { rawJson }

        appendLog("← ${getEventTag(rawJson)}\n$pretty\n${"─".repeat(36)}")
    }

    private fun getEventTag(raw: String): String = try {
        prettyMapper.readTree(raw).get("event")?.asText() ?: "EVENT"
    } catch (_: Exception) { "EVENT" }

    private fun appendLog(text: String) {
        val current = tvEventLog.text.toString()
        tvEventLog.text = if (current.isEmpty()) text else "$text\n\n$current"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun navigateToGameBoard(gameId: String) {
        val intent = Intent(this, GameboardUI::class.java).apply {
            putExtra("GAME_ID", gameId)
        }
        startActivity(intent)
    }
}
