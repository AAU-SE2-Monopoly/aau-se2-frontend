package at.aau.monopoly.klagenfurt

import at.aau.monopoly.klagenfurt.model.card.CommunityChestCard
import at.aau.monopoly.klagenfurt.model.enums.CardAction
import org.junit.Test

class CommunityChestCardTest {
    @Test
    fun test_verify_community_chest_card_with_set_properties() {
        val communityChestCard= CommunityChestCard(1, "desc", CardAction.MOVE_TO, 1, 2, 3)
        assert(communityChestCard.id==1)
        assert(communityChestCard.description=="desc")
        assert(communityChestCard.action== CardAction.MOVE_TO)
        assert(communityChestCard.amount==1)
        assert(communityChestCard.targetFieldId==2)
        assert(communityChestCard.moveSpaces==3)

    }
    @Test
    fun test_verify_community_chest_card_default_properties(){
        val communityChestCard= CommunityChestCard(1, "desc", CardAction.MOVE_TO)
        assert(communityChestCard.id==1)
        assert(communityChestCard.description=="desc")
        assert(communityChestCard.action== CardAction.MOVE_TO)
        assert(communityChestCard.amount==0)
        assert(communityChestCard.targetFieldId==null)
        assert(communityChestCard.moveSpaces==0)
    }
}