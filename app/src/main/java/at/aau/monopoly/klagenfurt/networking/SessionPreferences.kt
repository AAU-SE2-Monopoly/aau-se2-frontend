package at.aau.monopoly.klagenfurt.networking

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Persists the player's session identity (playerId, playerName, gameId, iconId)
 * so that a rejoin is possible even after the app is completely closed or reinstalled
 * (as long as the app data is not wiped).
 */
object SessionPreferences {

    private const val PREFS_NAME = "monopoly_session"
    private const val KEY_PLAYER_ID = "player_id"
    private const val KEY_PLAYER_NAME = "player_name"
    private const val KEY_GAME_ID = "game_id"
    private const val KEY_ICON_ID = "icon_id"

    @Volatile
    private var prefs: SharedPreferences? = null

    /** Must be called once from Application.onCreate() or the launcher Activity. */
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Returns the persisted player ID, or generates and persists a new one
     * if none exists yet.
     */
    fun getOrCreatePlayerId(): String {
        val p = prefs ?: return UUID.randomUUID().toString()
        val existing = p.getString(KEY_PLAYER_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val newId = UUID.randomUUID().toString()
        p.edit().putString(KEY_PLAYER_ID, newId).apply()
        return newId
    }

    var playerName: String
        get() = prefs?.getString(KEY_PLAYER_NAME, "") ?: ""
        set(value) { prefs?.edit()?.putString(KEY_PLAYER_NAME, value)?.apply() }

    var gameId: String
        get() = prefs?.getString(KEY_GAME_ID, "") ?: ""
        set(value) { prefs?.edit()?.putString(KEY_GAME_ID, value)?.apply() }

    var iconId: String
        get() = prefs?.getString(KEY_ICON_ID, "lindwurm") ?: "lindwurm"
        set(value) { prefs?.edit()?.putString(KEY_ICON_ID, value)?.apply() }

    /** Clears the game session (called when a game ends or is left). */
    fun clearGameSession() {
        prefs?.edit()
            ?.remove(KEY_GAME_ID)
            ?.remove(KEY_PLAYER_NAME)
            ?.remove(KEY_ICON_ID)
            ?.apply()
    }
}

