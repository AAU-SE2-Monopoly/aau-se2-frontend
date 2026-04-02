package at.aau.serg.websocketbrokerdemo

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.serg.websocketbrokerdemo.networking.Testing.FakeGameService
import com.example.myapplication.R
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameActivityTest {

    private lateinit var fakeService: FakeGameService
    private lateinit var scenario: ActivityScenario<GameActivity>

    @Before
    fun setup() {

        fakeService = FakeGameService()
        ServiceLocator.injectFakeGameService(fakeService)
        

        scenario = ActivityScenario.launch(GameActivity::class.java)
    }

    @After
    fun tearDown() {

        scenario.close()
        ServiceLocator.reset()
    }

    @Test
    fun clickConnectButton_showsConnectingInStatus_and_callsConnect() {
        onView(withId(R.id.et_player_name))
            .perform(typeText("Player1"), closeSoftKeyboard())
        
        onView(withId(R.id.btn_connect)).perform(click())
        
        onView(withId(R.id.tv_status))
            .check(matches(withText(containsString("Connecting"))))
        assertEquals(true, fakeService.connectCalled)
    }

    @Test
    fun test_Create_GameFlow_should_match_with_String_CREATE_GAME_and_call_service_createGame() {
        onView(withId(R.id.spinner_action)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("Create Game")))
            .perform(click())

        onView(withId(R.id.btn_send)).perform(click())
        
        onView(withId(R.id.tv_event_log))
            .check(matches(withText(containsString("CREATE_GAME"))))
        assertEquals(1, fakeService.createGameCalls)
    }

    @Test
    fun test_Join_GameFlow_should_match_with_String_JOIN_GAME_and_call_service_joinGame() {
        onView(withId(R.id.spinner_action)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("Join Game")))
            .perform(click())
        onView(withId(R.id.et_game_id))
            .perform(typeText("gameId"), closeSoftKeyboard())
        onView(withId(R.id.btn_send)).perform(click())

       onView(withId(R.id.tv_event_log))
           .check(matches(withText(containsString("JOIN_GAME"))))
        assertEquals(1, fakeService.joinGameCalls)
    }

    @Test
    fun test_Join_GameFlow_without_GameId_should_not_send_request() {
        onView(withId(R.id.spinner_action)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("Join Game")))
            .perform(click())

        onView(withId(R.id.btn_send)).perform(click())

        assertEquals(0, fakeService.joinGameCalls)
    }
    @Test
    fun test_ReceivingGameCreatedEvent_automatically_sets_GameId() {
        val serverGameId = "SERVER-GENERATED-ID"
        val jsonEvent = """{"event": "GAME_CREATED", "gameId": "$serverGameId"}"""

        // 1. Simulate the server sending a GAME_CREATED event
        runBlocking {
            fakeService.emitTestEvent(jsonEvent)
        }

        // 2. Verify the EditText was updated automatically
        onView(withId(R.id.et_game_id)).check(matches(withText(serverGameId)))

        // 3. Verify the service was notified of the new ID
        assertEquals(serverGameId, fakeService.lastSubscribedGameId)
    }
    @Test
    fun test_StartGame_calls_service_startGame() {
        onView(withId(R.id.spinner_action)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("Start Game"))).perform(click())
        onView(withId(R.id.et_game_id)).perform(typeText("TEST-ID"), closeSoftKeyboard())

        onView(withId(R.id.btn_send)).perform(click())

        assertEquals(true, fakeService.startGameCalled)
    }

    @Test
    fun test_RollDice_calls_service_rollDice() {
        onView(withId(R.id.spinner_action)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("Roll Dice"))).perform(click())
        onView(withId(R.id.et_game_id)).perform(typeText("TEST-ID"), closeSoftKeyboard())

        onView(withId(R.id.btn_send)).perform(click())

        assertEquals(true, fakeService.rollDiceCalled)
    }

    @Test
    fun test_EndTurn_calls_service_endTurn() {
        onView(withId(R.id.spinner_action)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("End Turn"))).perform(click())
        onView(withId(R.id.et_game_id)).perform(typeText("TEST-ID"), closeSoftKeyboard())

        onView(withId(R.id.btn_send)).perform(click())

        assertEquals(true, fakeService.endTurnCalled)
    }
    @Test
    fun test_GetState_calls_service_requestState() {
        onView(withId(R.id.spinner_action)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("Get State"))).perform(click())
        onView(withId(R.id.et_game_id)).perform(typeText("TEST-ID"), closeSoftKeyboard())

        onView(withId(R.id.btn_send)).perform(click())

        assertEquals(true, fakeService.requestStateCalled)
    }
}
