package io.elevenlabs

import io.elevenlabs.network.ConversationEventParser
import io.elevenlabs.network.OutgoingEvent
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wire format of outgoing events: field names must use snake_case and the
 * type discriminator must match the ConvAI protocol.
 */
class OutgoingEventSerializationTest {

    private fun serialize(event: OutgoingEvent): JSONObject =
        JSONObject(ConversationEventParser.serializeOutgoingEvent(event))

    @Test
    fun `user message serializes text and type`() {
        val json = serialize(OutgoingEvent.UserMessage(text = "hello"))

        assertEquals("user_message", json.getString("type"))
        assertEquals("hello", json.getString("text"))
    }

    @Test
    fun `user activity serializes type only`() {
        val json = serialize(OutgoingEvent.UserActivity())

        assertEquals("user_activity", json.getString("type"))
    }

    @Test
    fun `feedback serializes score and snake_case event_id`() {
        val json = serialize(OutgoingEvent.Feedback(score = "like", eventId = 12))

        assertEquals("feedback", json.getString("type"))
        assertEquals("like", json.getString("score"))
        assertEquals(12, json.getInt("event_id"))
        assertFalse(json.has("eventId"))
    }

    @Test
    fun `contextual update serializes text`() {
        val json = serialize(OutgoingEvent.ContextualUpdate(text = "context"))

        assertEquals("contextual_update", json.getString("type"))
        assertEquals("context", json.getString("text"))
    }

    @Test
    fun `client tool result serializes snake_case fields and string result`() {
        val json = serialize(
            OutgoingEvent.ClientToolResult(
                toolCallId = "call_9",
                result = """{"ok":true}""",
                isError = false
            )
        )

        assertEquals("client_tool_result", json.getString("type"))
        assertEquals("call_9", json.getString("tool_call_id"))
        assertEquals("""{"ok":true}""", json.getString("result"))
        assertFalse(json.getBoolean("is_error"))
    }

    @Test
    fun `client tool result serializes error flag`() {
        val json = serialize(
            OutgoingEvent.ClientToolResult(toolCallId = "call_9", result = "failed", isError = true)
        )

        assertTrue(json.getBoolean("is_error"))
    }

    @Test
    fun `pong serializes snake_case event_id`() {
        val json = serialize(OutgoingEvent.Pong(eventId = 3))

        assertEquals("pong", json.getString("type"))
        assertEquals(3, json.getInt("event_id"))
    }
}
