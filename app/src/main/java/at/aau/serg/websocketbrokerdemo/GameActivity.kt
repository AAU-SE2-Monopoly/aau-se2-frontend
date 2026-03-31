package at.aau.serg.websocketbrokerdemo

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.R
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class GameActivity : ComponentActivity() {


    private lateinit var tvStatus: TextView
    private lateinit var tvEventLog: TextView
    private lateinit var etPlayerName: EditText
    private lateinit var etGameId: EditText
    private lateinit var spinnerAction: Spinner
    private lateinit var btnSend: Button
    private lateinit var btnConnect: Button
    private lateinit var btnClear: Button
    private lateinit var scrollEvents: ScrollView

    /** Actions that map to STOMP destinations / GameAction.action values */
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



        tvStatus     = findViewById(R.id.tv_status)
        tvEventLog   = findViewById(R.id.tv_event_log)
        etPlayerName = findViewById(R.id.et_player_name)
        etGameId     = findViewById(R.id.et_game_id)
        spinnerAction= findViewById(R.id.spinner_action)
        btnSend      = findViewById(R.id.btn_send)
        btnConnect   = findViewById(R.id.btn_connect)
        btnClear     = findViewById(R.id.btn_clear)
        scrollEvents = findViewById(R.id.scroll_events)

        // Populate spinner
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            GameActionItem.entries.map { it.label }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAction.adapter = adapter

        btnConnect.setOnClickListener {
            tvStatus.text = getString(R.string.status_connecting)
            GameStompManager.connect()
        }

        btnClear.setOnClickListener { tvEventLog.text = "" }

        btnSend.setOnClickListener {
            val selected = GameActionItem.entries[spinnerAction.selectedItemPosition]
            val playerName = etPlayerName.text.toString().ifBlank { "Player1" }
            val gameId = etGameId.text.toString().trim()

            when (selected) {
                GameActionItem.CREATE_GAME -> {
                    // Subscribe to a temp "wildcard" isn't possible with simple broker,
                    // so we subscribe AFTER we receive the GAME_CREATED event via onGameEvent.
                    GameStompManager.createGame(playerName)
                    appendLog("→ CREATE_GAME  player=$playerName")
                }

                GameActionItem.JOIN_GAME -> {
                    if (gameId.isEmpty()) {
                        toast(getString(R.string.error_enter_game_id)); return@setOnClickListener
                    }
                    GameStompManager.joinGame(gameId, playerName)
                    appendLog("→ JOIN_GAME  gameId=$gameId  player=$playerName")
                }

                GameActionItem.START_GAME -> {
                    if (gameId.isEmpty()) {
                        toast(getString(R.string.error_enter_game_id)); return@setOnClickListener
                    }
                    GameStompManager.setGameId(gameId)
                    GameStompManager.startGame()
                    appendLog("→ START_GAME  gameId=$gameId")
                }

                GameActionItem.ROLL_DICE -> {
                    if (gameId.isEmpty()) {
                        toast(getString(R.string.error_enter_game_id)); return@setOnClickListener
                    }
                    GameStompManager.setGameId(gameId)
                    GameStompManager.rollDice()
                    appendLog("→ ROLL_DICE  gameId=$gameId")
                }

                GameActionItem.END_TURN -> {
                    if (gameId.isEmpty()) {
                        toast(getString(R.string.error_enter_game_id)); return@setOnClickListener
                    }
                    GameStompManager.setGameId(gameId)
                    GameStompManager.endTurn()
                    appendLog("→ END_TURN  gameId=$gameId")
                }

                GameActionItem.GET_STATE -> {
                    if (gameId.isEmpty()) {
                        toast(getString(R.string.error_enter_game_id)); return@setOnClickListener
                    }
                    GameStompManager.setGameId(gameId)
                    GameStompManager.requestState()
                    appendLog("→ GET_STATE  gameId=$gameId")
                }
            }
        }
        observeStompFlows()
    }



    private fun observeStompFlows() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    GameStompManager.status.collect { message -> tvStatus.text = message }
                }
                launch {
                    GameStompManager.events.collect { rawJson ->
                        handleGameEvent(rawJson)
                    }
                }
            }


        }
    }
   // Auto-fill gameId from GAME_CREATED event and subscribe to the topic

    private fun handleGameEvent(rawJson: String) {
            try {
                val obj = JSONObject(rawJson)
                val event = obj.optString("event")
                val gameId = obj.optString("gameId")
                if (event == "GAME_CREATED" && gameId.isNotEmpty()) {
                    etGameId.setText(gameId)
                    GameStompManager.setGameId(gameId)
                }
            } catch (_: JSONException) { }

            val pretty = try {
                JSONObject(rawJson).toString(2)
            } catch (_: JSONException) { rawJson }

            appendLog("← ${getEventTag(rawJson)}\n$pretty\n${"─".repeat(36)}")
        }
    /** Extracts the "event" field for a concise header label. */
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