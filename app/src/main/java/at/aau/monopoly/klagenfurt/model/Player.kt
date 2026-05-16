package at.aau.monopoly.klagenfurt.model

data class Player(
    val id: String,
    var name: String,
    var position: Int = 0,
    var money: Int = 1500,
    var iconId: String = "lindwurm",
    var inJail: Boolean = false,
    var jailTurns: Int = 0,
    var getOutOfJailCards: Int = 0,
    var consecutiveDoublets: Int = 0, // Zählt die Paschs im aktuellen Zug
    val ownedPropertyIds: MutableList<Int> = mutableListOf()
) {
    /** Returns true if the player is bankrupt (no money and no properties). */
    fun isBankrupt(): Boolean = money <= 0 && ownedPropertyIds.isEmpty()


    fun goToJail(jailPosition: Int = 10) {
        position = jailPosition
        inJail = true
        jailTurns = 0
        consecutiveDoublets = 0
    }
}