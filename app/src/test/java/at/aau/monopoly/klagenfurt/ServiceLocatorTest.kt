package at.aau.monopoly.klagenfurt

import at.aau.monopoly.klagenfurt.networking.GameService
import io.mockk.mockk
import org.hildan.krossbow.stomp.StompClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ServiceLocatorTest {

    @AfterEach
    fun teardown() {
        // Mandatory: resets global state after each test.
        ServiceLocator.resetForTests()
    }

    @Test
    fun provideStompClient_initialCall_createsNewInstance() {
        val client = ServiceLocator.provideStompClient()

        assertNotNull(client)
    }

    @Test
    fun provideStompClient_multipleCalls_returnsSameInstance() {
        val client1 = ServiceLocator.provideStompClient()
        val client2 = ServiceLocator.provideStompClient()

        // assertSame checks for exact memory reference (singleton behavior)
        assertSame(client1, client2)
    }

    @Test
    fun provideGameService_initialCall_createsNewInstance() {
        val service = ServiceLocator.provideGameService()

        assertNotNull(service)
    }

    @Test
    fun provideGameService_multipleCalls_returnsSameInstance() {
        val service1 = ServiceLocator.provideGameService()
        val service2 = ServiceLocator.provideGameService()

        assertSame(service1, service2)
    }

    @Test
    fun injectStompClientForTest_overridesDefaultInstance() {
        val mockClient = mockk<StompClient>()

        ServiceLocator.injectStompClientForTest(mockClient)
        val providedClient = ServiceLocator.provideStompClient()

        assertSame(mockClient, providedClient)
    }

    @Test
    fun injectGameServiceForTest_overridesDefaultInstance() {
        val mockService = mockk<GameService>()

        ServiceLocator.injectGameServiceForTest(mockService)
        val providedService = ServiceLocator.provideGameService()

        assertSame(mockService, providedService)
    }

    @Test
    fun resetGameService_disconnectsAndClearsGameService() {
        val mockService = mockk<GameService>(relaxed = true)
        ServiceLocator.injectGameServiceForTest(mockService)

        ServiceLocator.resetGameService()

        val newService = ServiceLocator.provideGameService()
        assertNotSame(mockService, newService)
        io.mockk.verify { mockService.disconnect() }
    }

    @Test
    fun resetGameService_handlesNullGameServiceGracefully() {
        // No game service injected – should not throw
        ServiceLocator.resetGameService()
        assertNotNull(ServiceLocator.provideGameService())
    }

    @Test
    fun resetForTests_clearsAllInstances() {
        val mockClient = mockk<StompClient>()
        val mockService = mockk<GameService>()

        ServiceLocator.injectStompClientForTest(mockClient)
        ServiceLocator.injectGameServiceForTest(mockService)

        // Call reset
        ServiceLocator.resetForTests()

        // After reset, subsequent calls must build real, new instances,
        // not the mocks from above.
        val newClient = ServiceLocator.provideStompClient()
        val newService = ServiceLocator.provideGameService()

        assertNotSame(mockClient, newClient)
        assertNotSame(mockService, newService)
    }
}