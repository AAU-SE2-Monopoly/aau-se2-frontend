package at.aau.serg.websocketbrokerdemo

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.serg.websocketbrokerdemo.networking.Testing.FakeGameService
import com.example.myapplication.R
import junit.framework.TestCase.assertEquals
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameActivityTest {

    private lateinit var fakeService: FakeGameService

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(GameActivity::class.java)

    @Before
    fun setup() {

        fakeService = FakeGameService()
        ServiceLocator.injectFakeGameService(fakeService)
    }

    @Test
    fun clickConnectButton_showsConnectingInStatus() {
        onView(withId(R.id.et_player_name))
            .perform(typeText("Player1"), closeSoftKeyboard())
        
        onView(withId(R.id.btn_connect)).perform(click())
        

        onView(withId(R.id.tv_status))
            .check(matches(withText(containsString("Connecting"))))
    }

    @Test
    fun test_Create_GameFlow_should_match_with_String_CREATE_GAME() {
        onView(withId(R.id.spinner_action)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("Create Game")))
            .perform(click())

        onView(withId(R.id.btn_send)).perform(click())
        

        onView(withId(R.id.tv_event_log))
            .check(matches(withText(containsString("CREATE_GAME"))))
    }
    @Test
    fun test_Join_GameFlow_should_match_with_String_JOIN_GAME() {
        onView(withId(R.id.spinner_action)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("Join Game")))
            .perform(click())
        onView(withId(R.id.et_game_id))
            .perform(typeText("gameId"), closeSoftKeyboard())
        onView(withId(R.id.btn_send)).perform(click())

       onView(withId(R.id.tv_event_log))
           .check(matches(withText(containsString("JOIN_GAME"))))
    }
    @Test
    fun test_Join_GameFlow_without_GameId_should_not_send_request() {
        onView(withId(R.id.spinner_action)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("Join Game")))
            .perform(click())


        onView(withId(R.id.btn_send)).perform(click())


        assertEquals(0, fakeService.joinGameCalls)
    }
}
