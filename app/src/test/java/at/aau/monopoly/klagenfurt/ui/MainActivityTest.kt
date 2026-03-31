package at.aau.serg.websocketbrokerdemo.ui


import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import at.aau.serg.websocketbrokerdemo.MainActivity
import at.aau.serg.websocketbrokerdemo.ServiceLocator
import com.example.myapplication.R
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hildan.krossbow.stomp.StompClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.regex.Pattern.matches

// 1. Robolectric Runner anstelle von AndroidJUnit4
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityTest {

    private lateinit var mockStompClient: StompClient
    private val testDispatcher= StandardTestDispatcher()
    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockStompClient = mockk(relaxed = true)
        ServiceLocator.injectStompClientForTest(mockStompClient)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        ServiceLocator.resetForTests()
        Dispatchers.resetMain()
    }

    @Test
    fun testConnectButton_triggersStompClientConnect() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.connectbtn))
                .check(matches(isDisplayed()))
                .perform(click())

            coVerify(exactly = 1) {
                mockStompClient.connect(url = any())
            }
        }
    }

    @Test
    fun testHelloButton_isDisplayedAndClickable() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.hellobtn))
                .check(matches(isDisplayed()))
                .perform(click())
        }
    }

    @Test
    fun testJsonButton_isDisplayedAndClickable() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.jsonbtn))
                .check(matches(isDisplayed()))
                .perform(click())
        }
    }

    @Test
    fun testResponseView_showsInitialText() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.response_view))
                .check(matches(isDisplayed()))
                .check(matches(withText("response")))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testObserveViewModel_updatesTextViewOnFlowEmission() = runTest(testDispatcher) {
        coEvery {
            mockStompClient.connect(
                any(),
                any()
            )
        } throws RuntimeException("Simulierter Netzwerkfehler")

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.connectbtn)).perform(click())

            advanceUntilIdle()

            onView(withId(R.id.response_view)).check(matches(withText("Connection error")))
        }
    }
    }
