package at.aau.serg.websocketbrokerdemo

interface GameCallbacks {
    fun onStatus(message: String)
    fun onGameEvent(rawJson: String)
}

