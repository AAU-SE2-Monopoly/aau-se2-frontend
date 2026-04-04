package at.aau.serg.websocketbrokerdemo.at.aau.serg.websocketbrokerdemo

import at.aau.serg.websocketdemoserver.model.enums.FieldType
import at.aau.serg.websocketdemoserver.model.field.CommunityChestField
import org.junit.jupiter.api.Test


class CommunityChestFieldTest {
    @Test
    fun verify_CommunityChestField_with_set_properties() {
        val communityChestField = CommunityChestField(1, "Move", FieldType.COMMUNITY_CHEST)
        assert(communityChestField.id==1)
        assert(communityChestField.name=="Move")
        assert(communityChestField.type==FieldType.COMMUNITY_CHEST)

    }
    @Test
    fun verify_CommunityChestField_default_properties(){
        val communityChestField = CommunityChestField(1)
        assert(communityChestField.id==1)
        assert(communityChestField.name=="Community Chest")
        assert(communityChestField.type==FieldType.COMMUNITY_CHEST)
    }
}

