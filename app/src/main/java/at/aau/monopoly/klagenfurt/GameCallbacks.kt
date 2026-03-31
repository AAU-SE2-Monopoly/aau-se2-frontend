package at.aau.monopoly.klagenfurt

interface GameCallbacks {
    fun onStatus(message: String)
    fun onGameEvent(rawJson: String)
}

