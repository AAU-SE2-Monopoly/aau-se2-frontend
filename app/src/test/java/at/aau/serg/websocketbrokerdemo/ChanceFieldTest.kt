package at.aau.serg.websocketbrokerdemo.at.aau.serg.websocketbrokerdemo

import at.aau.serg.websocketdemoserver.model.enums.FieldType
import at.aau.serg.websocketdemoserver.model.field.ChanceField
import org.junit.Test

class ChanceFieldTest {
    @Test
    fun test_verify_chance_field_with_set_properties() {
        val chanceField = ChanceField(1,"Move",FieldType.COMMUNITY_CHEST)
        assert(chanceField.id==1)
        assert(chanceField.name=="Move")
        assert(chanceField.type==FieldType.COMMUNITY_CHEST)
    }
    @Test
    fun test_verify_chance_field_default_properties(){
        val chanceField = ChanceField(1)
        assert(chanceField.id==1)
        assert(chanceField.name=="Chance")
        assert(chanceField.type==FieldType.CHANCE)
    }
}


