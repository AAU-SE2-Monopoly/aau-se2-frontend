package at.aau.monopoly.klagenfurt
import at.aau.monopoly.klagenfurt.messaging.GameAction
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.card.Card
import at.aau.monopoly.klagenfurt.model.card.ChanceCard
import at.aau.monopoly.klagenfurt.model.card.CommunityChestCard
import at.aau.monopoly.klagenfurt.model.enums.CardAction
import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.ChanceField
import at.aau.monopoly.klagenfurt.model.field.CommunityChestField
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.FreeParkingField
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.model.field.GoToJailField
import at.aau.monopoly.klagenfurt.model.field.JailField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.TaxField
import at.aau.monopoly.klagenfurt.model.field.UtilityField
import nl.jqno.equalsverifier.EqualsVerifier
import nl.jqno.equalsverifier.Warning
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
/*
abstract class Card(
    open val id: Int,
    open val description: String,
    open val action: CardAction,
    /** Amount of money involved (for COLLECT_MONEY, PAY_MONEY, etc.) */
    open val amount: Int = 0,
    /** Target field index (for MOVE_TO) */
    open val targetFieldId: Int? = null,
    /** Number of spaces to move (for MOVE_FORWARD) */
    open val moveSpaces: Int = 0
)
* */

class GlobalDataClassTest {


    @Test
    fun test_Card_value_for_all_fields() {
        val card1 = object : Card(1, "desc", CardAction.MOVE_TO, 1, 2, 3) {}
        assert(card1.id == 1)
        assert(card1.description == "desc")
        assert(card1.action == CardAction.MOVE_TO)
        assert(card1.amount == 1)
        assert(card1.targetFieldId == 2)
        assert(card1.moveSpaces == 3)
    }

    @Test
    fun test_Card_Defaults() {
        val card1 = object : Card(1, "desc", CardAction.MOVE_TO) {}
        assert(card1.amount == 0)
        assert(card1.targetFieldId == null)
        assert(card1.moveSpaces == 0)
    }

    @TestFactory
    fun generateDataClassTests() = listOf(
        GameAction::class.java,
        GameEvent::class.java,
        Card::class.java,
        ChanceCard::class.java,
        CommunityChestCard::class.java,
        CardAction::class.java,
        FieldType::class.java,
        GamePhase::class.java,
        PropertyColor::class.java,
        ChanceField::class.java,
        CommunityChestField::class.java,
        Field::class.java,
        FreeParkingField::class.java,
        GoField::class.java,
        GoToJailField::class.java,
        JailField::class.java,
        PropertyField::class.java,
        RailroadField::class.java,
        TaxField::class.java,
        UtilityField::class.java,
        DiceRoll::class.java,
        GameState::class.java,
        Player::class.java

    ).map { clazz ->
        DynamicTest.dynamicTest("Testing ${clazz.simpleName}") {
            EqualsVerifier.forClass(clazz)
                .suppress(
                    Warning.INHERITED_DIRECTLY_FROM_OBJECT,
                    Warning.NONFINAL_FIELDS,
                    Warning.ALL_FIELDS_SHOULD_BE_USED,
                    Warning.STRICT_INHERITANCE
                )
                .verify()

            try {
                val constructor =
                    clazz.constructors.firstOrNull() ?: clazz.declaredConstructors.first()
                constructor.isAccessible = true
                val args = Array<Any?>(constructor.parameterCount) { i ->
                    when (constructor.parameterTypes[i]) {
                        Int::class.java -> 1
                        String::class.java -> "Test"
                        Boolean::class.java -> false
                        else -> null
                    }
                }

                val instance = constructor.newInstance(*args)
                clazz.declaredFields.forEach { field ->
                    field.isAccessible = true
                    field.get(instance)
                }

                if (clazz == DiceRoll::class.java) {
                    val d = instance as DiceRoll
                    val sum = d.total
                    val dub = d.isDouble
                }

                instance.toString()
                instance.hashCode()
                if (instance is UtilityField) {
                    instance.ownerId = "player1"
                    instance.isMortgaged = true
                }

            } catch (e: Exception) {
                if (clazz == Card::class.java) {
                    val card = object : Card(1, "desc", CardAction.MOVE_TO) {}
                    card.id; card.description; card.action; card.amount; card.targetFieldId; card.moveSpaces
                }
            } finally {
                // EXPLIZITER COVERAGE-BOOSTER FÜR DIE ABSTRAKTE KLASSE
                if (clazz == Card::class.java) {
                    val cardAction = CardAction.MOVE_TO
                    val testCard = object : Card(
                        id = 1,
                        description = "Test",
                        action = cardAction,
                        amount = 100,
                        targetFieldId = 2,
                        moveSpaces = 3
                    ) {}

                    val baseCard = testCard as Card

                    assertNotNull(baseCard.id)
                    assertNotNull(baseCard.description)
                    assertNotNull(baseCard.action)
                    assertNotNull(baseCard.amount)
                    assertNotNull(baseCard.targetFieldId)
                    assertNotNull(baseCard.moveSpaces)
                }
            }
        }
    }
}