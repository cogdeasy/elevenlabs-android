package io.elevenlabs

import io.elevenlabs.models.ConversationStatus
import io.elevenlabs.models.toConversationStatus
import io.elevenlabs.network.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionStatusMappingTest {

    @Test
    fun `connected maps to CONNECTED`() {
        assertEquals(ConversationStatus.CONNECTED, ConnectionState.CONNECTED.toConversationStatus())
    }

    @Test
    fun `connecting maps to CONNECTING`() {
        assertEquals(ConversationStatus.CONNECTING, ConnectionState.CONNECTING.toConversationStatus())
    }

    @Test
    fun `disconnected maps to DISCONNECTED`() {
        assertEquals(ConversationStatus.DISCONNECTED, ConnectionState.DISCONNECTED.toConversationStatus())
    }

    @Test
    fun `error maps to ERROR`() {
        assertEquals(ConversationStatus.ERROR, ConnectionState.ERROR.toConversationStatus())
    }

    @Test
    fun `idle and reconnecting map to DISCONNECTED`() {
        assertEquals(ConversationStatus.DISCONNECTED, ConnectionState.IDLE.toConversationStatus())
        assertEquals(ConversationStatus.DISCONNECTED, ConnectionState.RECONNECTING.toConversationStatus())
    }
}
