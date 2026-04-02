package at.aau.serg.websocketbrokerdemo

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.R

import io.mockk.mockk
import io.mockk.verify
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
        mockStompClient = mockk(relaxed = true)
        ServiceLocator.injectStompClientForTest(mockStompClient)
    }

    @After
    fun tearDown() {
        ServiceLocator.resetForTests()
    }

    @Test
    fun testConnectButton_triggersStompClientConnect() {
        // Warnung behoben durch Entfernen von "scenario ->"
        ActivityScenario.launch(MainActivity::class.java).use {

            onView(withId(R.id.connectbtn)).perform(click())

            coVerify(exactly = 1) { // any() als Parameter bedeutet, dass der Test erfolgreich ist, // egal welche exakte URL an den StompClient übergeben wurde.
            mockStompClient.connect(url = any())
             }
        }
    }
}