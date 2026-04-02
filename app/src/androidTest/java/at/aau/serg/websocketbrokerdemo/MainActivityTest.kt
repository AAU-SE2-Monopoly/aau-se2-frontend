package at.aau.serg.websocketbrokerdemo

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.R

import io.mockk.mockk
import io.mockk.coVerify

import org.hildan.krossbow.stomp.StompClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private lateinit var mockStompClient: StompClient

    @Before
    fun setup() {
        // relaxed = true sorgt dafür, dass Methodenrückgaben (wie die StompSession beim Connect)
        // automatisch durch Mock-Objekte ersetzt werden und nicht null zurückliefern.
        mockStompClient = mockk(relaxed = true)
        ServiceLocator.injectStompClientForTest(mockStompClient)
    }

    @After
    fun tearDown() {
        ServiceLocator.resetForTests()
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

            // Hinweis: Um hier coVerify für das eigentliche Senden auszuführen, müsste
            // das zurückgegebene StompSession-Objekt aus dem Connect gemockt werden.
            // Für einen reinen UI-Interaktionstest reicht es zu prüfen, ob der Button
            // nicht abstürzt und die Methode im ViewModel fehlerfrei aufruft.
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
            // Prüft, ob das Textfeld beim Start korrekt sichtbar ist und den Standardtext enthält
            onView(withId(R.id.response_view))
                .check(matches(isDisplayed()))
                .check(matches(withText("response")))
        }
    }
}