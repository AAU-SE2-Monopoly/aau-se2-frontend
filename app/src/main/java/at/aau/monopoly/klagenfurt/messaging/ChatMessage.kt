package at.aau.monopoly.klagenfurt.messaging

import org.json.JSONObject

data class ChatMessage(
    val sender: String,
    val message: String,


){
    companion object{
        fun fromJson(json: JSONObject): ChatMessage {
            val sender = json.getString("sender")
            val message = json.getString("message")
        return ChatMessage(sender, message)
        }

    }
}

