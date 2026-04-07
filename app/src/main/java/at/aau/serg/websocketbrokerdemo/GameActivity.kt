package at.aau.serg.websocketbrokerdemo

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import at.aau.serg.websocketbrokerdemo.GameboardUI.GameboardUI
import at.aau.serg.websocketbrokerdemo.ui.GameViewModel
import com.example.myapplication.R
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

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
    private lateinit var scrollEvents: ScrollView

    enum class GameActionItem(val label: String) {
        CREATE_GAME("Create Game"),
        JOIN_GAME("Join Game"),
        START_GAME("Start Game"),
        ROLL_DICE("Roll Dice"),
        END_TURN("End Turn"),
        GET_STATE("Get State"),
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
        scrollEvents = findViewById(R.id.scroll_events)

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
            val intent = Intent(this, GameboardUI::class.java)
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
                    btnGameBoard.isEnabled = true

                }
                GameActionItem.JOIN_GAME -> {
                    if (gameId.isEmpty()) { toast(getString(R.string.error_enter_game_id)); return@setOnClickListener }
                    viewModel.joinGame(gameId, playerName)
                    appendLog("→ JOIN_GAME gameId=$gameId player=$playerName")
                }
                else -> {
                    if (gameId.isEmpty()) { toast(getString(R.string.error_enter_game_id)); return@setOnClickListener }
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
            }
        }
    }

    private fun handleGameEvent(rawJson: String) {
        try {
            val obj = JSONObject(rawJson)
            val event = obj.optString("event")
            val gameId = obj.optString("gameId")
            if (event == "GAME_CREATED" && gameId.isNotEmpty()) {
                etGameId.setText(gameId)
                viewModel.setGameId(gameId)
            }
        } catch (e: JSONException) {

            appendLog("handeGameEvent exception: $e")

        }

        val pretty = try {
            JSONObject(rawJson).toString(2)
        } catch (_: JSONException) { rawJson }

        appendLog("← ${getEventTag(rawJson)}\n$pretty\n${"─".repeat(36)}")
    }

    private fun getEventTag(raw: String): String = try {
        JSONObject(raw).optString("event", "EVENT")
    } catch (_: JSONException) { "EVENT" }

    private fun appendLog(text: String) {
        val current = tvEventLog.text.toString()
        tvEventLog.text = if (current.isEmpty()) text else "$text\n\n$current"
        scrollEvents.post { scrollEvents.fullScroll(ScrollView.FOCUS_UP) }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}


