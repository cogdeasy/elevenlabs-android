package io.elevenlabs

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.elevenlabs.models.ConversationEvent
import io.elevenlabs.network.ConversationEventParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke tests for SDK initialization on a real device/emulator.
 */
@RunWith(AndroidJUnit4::class)
class SdkInitializationTest {

    @Test
    fun instrumentationContextIsAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(context)
        assertEquals("io.elevenlabs.test", context.packageName)
    }

    @Test
    fun conversationConfigValidatesOnDevice() {
        val config = ConversationConfig(agentId = "agent_123", textOnly = true)
        assertEquals("agent_123", config.agentId)
        assertTrue(config.textOnly)
        assertEquals(false, config.isPrivateAgent)

        assertThrows(IllegalArgumentException::class.java) {
            ConversationConfig(agentId = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConversationConfig(signedUrl = "https://not-a-websocket.example")
        }
    }

    @Test
    fun clientToolRegistryRegistersAndUnregistersTools() {
        val registry = ClientToolRegistry()
        val tool = object : ClientTool {
            override suspend fun execute(parameters: Map<String, Any>): ClientToolResult {
                return ClientToolResult.success("ok")
            }
        }

        registry.registerTool("echo", tool)
        assertTrue(registry.isToolRegistered("echo"))

        registry.unregisterTool("echo")
        assertEquals(false, registry.isToolRegistered("echo"))
    }

    @Test
    fun eventParserParsesRealPayloadOnDevice() {
        val json = """{"type":"agent_response","agent_response_event":{"agent_response":"Hi","event_id":7}}"""
        val event = ConversationEventParser.parseIncomingEvent(json)
        assertTrue(event is ConversationEvent.AgentResponse)
        assertEquals("Hi", (event as ConversationEvent.AgentResponse).agentResponse)
        assertEquals(7, event.eventId)
    }
}
