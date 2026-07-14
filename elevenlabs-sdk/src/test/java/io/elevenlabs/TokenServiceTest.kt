package io.elevenlabs

import io.elevenlabs.network.TokenService
import io.elevenlabs.network.TokenServiceException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class TokenServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: TokenService

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        service = TokenService(baseUrl = server.url("/").toString().removeSuffix("/"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchPublicAgentToken returns token on success`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"token":"tok_abc123"}""")
        )

        val response = service.fetchPublicAgentToken(
            agentId = "agent_1",
            source = "android_sdk",
            version = "0.11"
        )

        assertEquals("tok_abc123", response.token)
    }

    @Test
    fun `fetchPublicAgentToken builds request URL with query parameters`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"token":"tok"}""")
        )

        service.fetchPublicAgentToken(
            agentId = "agent_42",
            source = "android_sdk",
            version = "0.11",
            environment = "staging"
        )

        val request = server.takeRequest()
        assertEquals("/v1/convai/conversation/token", request.requestUrl?.encodedPath)
        assertEquals("agent_42", request.requestUrl?.queryParameter("agent_id"))
        assertEquals("android_sdk", request.requestUrl?.queryParameter("source"))
        assertEquals("0.11", request.requestUrl?.queryParameter("version"))
        assertEquals("staging", request.requestUrl?.queryParameter("environment"))
    }

    @Test
    fun `fetchPublicAgentToken omits environment when not provided`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"token":"tok"}""")
        )

        service.fetchPublicAgentToken(agentId = "agent_1", source = "s", version = "v")

        val request = server.takeRequest()
        assertEquals(null, request.requestUrl?.queryParameter("environment"))
    }

    @Test
    fun `fetchPublicAgentToken throws on HTTP error with body in message`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401).setBody("""{"detail":"unauthorized"}""")
        )

        try {
            service.fetchPublicAgentToken(agentId = "agent_1", source = "s", version = "v")
            fail("Expected TokenServiceException")
        } catch (e: TokenServiceException) {
            assertTrue(e.message!!.contains("HTTP 401"))
            assertTrue(e.message!!.contains("unauthorized"))
        }
    }

    @Test
    fun `fetchPublicAgentToken throws on malformed JSON`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("not json at all {{{")
        )

        try {
            service.fetchPublicAgentToken(agentId = "agent_1", source = "s", version = "v")
            fail("Expected TokenServiceException")
        } catch (e: TokenServiceException) {
            assertTrue(e.message!!.contains("Failed to parse token response"))
        }
    }
}
