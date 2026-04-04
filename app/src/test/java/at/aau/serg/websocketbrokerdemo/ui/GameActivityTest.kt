package at.aau.serg.websocketbrokerdemo.at.aau.serg.websocketbrokerdemo.ui


import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import at.aau.serg.websocketbrokerdemo.GameActivity
import at.aau.serg.websocketbrokerdemo.ServiceLocator
import at.aau.serg.websocketbrokerdemo.at.aau.serg.websocketbrokerdemo.FakeGameService
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GameActivityTest {

    private lateinit var fakeService: FakeGameService
    private lateinit var scenario: ActivityScenario<GameActivity>

    @Before
    fun setup() {
        fakeService = FakeGameService()
        ServiceLocator.injectGameServiceForTest(fakeService)
        scenario = ActivityScenario.launch(GameActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
        ServiceLocator.resetForTests()
    }

    @Test
    fun test_clearButton_clearsEventLog_afterTextWasAdded() {
        runBlocking {
            fakeService.emitTestEvent("""{"event":"TEST_MESSAGE"}""")
            ShadowLooper.idleMainLooper() // Wartet auf UI-Update durch den Flow

            onView(withId(R.id.tv_event_log))
                .check(matches(withText(containsString("TEST_MESSAGE"))))

            onView(withId(R.id.btn_clear)).perform(click())

            onView(withId(R.id.tv_event_log))
                .check(matches(withText("")))
        }
    }

    @Test
    fun test_clearButton_clearsEventLog() {
        onView(withId(R.id.btn_clear)).perform(click())
        onView(withId(R.id.tv_event_log)).check(matches(withText("")))
    }

    @Test
    fun test_setupSpinner_populatesWithGameActionItems() {
        onView(withId(R.id.spinner_action))
            .check(matches(withSpinnerText(containsString("Create Game"))))
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

        runBlocking {
            fakeService.emitTestEvent(jsonEvent)
            ShadowLooper.idleMainLooper()
        }

        onView(withId(R.id.et_game_id)).check(matches(withText(serverGameId)))
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

    @Test
    fun test_observeViewModel_collects_statusFlow_and_eventsFlow() {
        runBlocking {
            fakeService.emitTestStatus("TEST-STATUS")
            fakeService.emitTestEvent("TestEvent")
            ShadowLooper.idleMainLooper()

            onView(withId(R.id.tv_status))
                .check(matches(withText(containsString("TEST-STATUS"))))
            onView(withId(R.id.tv_event_log))
                .check(matches(withText(containsString("TestEvent"))))
        }
    }

    @Test
    fun test_handleGameEvent_withValidJson_updatesGameId_andAppendsPrettyLog() {
        runBlocking {
            val validJson = """{"event": "GAME_CREATED", "gameId": "NEW-LOBBY"}"""
            fakeService.emitTestEvent(validJson)
            ShadowLooper.idleMainLooper()

            onView(withId(R.id.et_game_id))
                .check(matches(withText("NEW-LOBBY")))

            onView(withId(R.id.tv_event_log))
                .check(matches(withText(containsString("← GAME_CREATED"))))

            onView(withId(R.id.tv_event_log))
                .check(matches(withText(containsString("\"gameId\": \"NEW-LOBBY\""))))
        }
    }

    @Test
    fun test_handleGameEvent_withInvalidJson_usesFallbackTag_andRawText() {
        runBlocking {
            val brokenJson = "Das ist einfach nur Text, kein JSON"
            fakeService.emitTestEvent(brokenJson)
            ShadowLooper.idleMainLooper()

            onView(withId(R.id.tv_event_log))
                .check(matches(withText(containsString("← EVENT"))))

            onView(withId(R.id.tv_event_log))
                .check(matches(withText(containsString(brokenJson))))
        }
    }
    @Test
    fun test_appendLog_withMultipleMessages_prependsNewText() {
        runBlocking {
            fakeService.emitTestEvent("""{"event":"ERSTE_NACHRICHT"}""")
            fakeService.emitTestEvent("""{"event":"ZWEITE_NACHRICHT"}""")
            ShadowLooper.idleMainLooper()

            onView(withId(R.id.tv_event_log))
                .check(matches(withText(containsString("ERSTE_NACHRICHT"))))

            onView(withId(R.id.tv_event_log))
                .check(matches(withText(containsString("ZWEITE_NACHRICHT"))))
        }
    }
}