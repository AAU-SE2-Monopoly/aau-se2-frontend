package at.aau.monopoly.klagenfurt
import at.aau.monopoly.klagenfurt.model.card.ChanceCard
import at.aau.monopoly.klagenfurt.model.enums.CardAction
import org.junit.Test

class ChanceCardTest {
    @Test
    fun test_verify_chance_card_with_set_properties() {
        val chanceCard= ChanceCard(1,"desc", CardAction.MOVE_TO,1,2,3)
        assert(chanceCard.id==1)
        assert(chanceCard.description=="desc")
        assert(chanceCard.action==CardAction.MOVE_TO)
        assert(chanceCard.amount==1)
        assert(chanceCard.targetFieldId==2)
        assert(chanceCard.moveSpaces==3)

    }
    @Test
    fun test_verify_chance_card_default_properties(){
        val chanceCard= ChanceCard(1,"desc", CardAction.MOVE_TO)
        assert(chanceCard.id==1)
        assert(chanceCard.description=="desc")
        assert(chanceCard.action==CardAction.MOVE_TO)
        assert(chanceCard.amount==0)
        assert(chanceCard.targetFieldId==null)
        assert(chanceCard.moveSpaces==0)

    }
}