package io.elevenlabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationOverridesBuilderTest {

    @Test
    fun `minimal config yields only the type field`() {
        val config = ConversationConfig(agentId = "agent_1")
        val json = ConversationOverridesBuilder.constructOverrides(config)

        assertEquals("conversation_initiation_client_data", json.getString("type"))
        assertFalse(json.has("conversation_config_override"))
        assertFalse(json.has("custom_llm_extra_body"))
        assertFalse(json.has("dynamic_variables"))
        assertFalse(json.has("user_id"))
        assertFalse(json.has("source_info"))
    }

    @Test
    fun `agent overrides are nested under conversation_config_override`() {
        val config = ConversationConfig(
            agentId = "agent_1",
            overrides = Overrides(
                agent = AgentOverrides(
                    prompt = PromptOverrides(prompt = "Be terse"),
                    firstMessage = "Hello!",
                    language = Language.DE
                )
            )
        )
        val json = ConversationOverridesBuilder.constructOverrides(config)

        val agent = json.getJSONObject("conversation_config_override").getJSONObject("agent")
        assertEquals("Be terse", agent.getJSONObject("prompt").getString("prompt"))
        assertEquals("Hello!", agent.getString("first_message"))
        assertEquals("de", agent.getString("language"))
    }

    @Test
    fun `tts and conversation overrides are serialized`() {
        val config = ConversationConfig(
            agentId = "agent_1",
            overrides = Overrides(
                tts = TtsOverrides(voiceId = "voice_9"),
                conversation = ConversationOverrides(textOnly = true)
            )
        )
        val json = ConversationOverridesBuilder.constructOverrides(config)

        val override = json.getJSONObject("conversation_config_override")
        assertEquals("voice_9", override.getJSONObject("tts").getString("voice_id"))
        assertTrue(override.getJSONObject("conversation").getBoolean("text_only"))
    }

    @Test
    fun `empty overrides object emits no conversation_config_override`() {
        val config = ConversationConfig(agentId = "agent_1", overrides = Overrides())
        val json = ConversationOverridesBuilder.constructOverrides(config)

        assertFalse(json.has("conversation_config_override"))
    }

    @Test
    fun `custom llm extra body and dynamic variables are passed through`() {
        val config = ConversationConfig(
            agentId = "agent_1",
            customLlmExtraBody = mapOf("temperature" to 0.5),
            dynamicVariables = mapOf("name" to "Ada")
        )
        val json = ConversationOverridesBuilder.constructOverrides(config)

        assertEquals(0.5, json.getJSONObject("custom_llm_extra_body").getDouble("temperature"), 0.0)
        assertEquals("Ada", json.getJSONObject("dynamic_variables").getString("name"))
    }

    @Test
    fun `user id and client source info are serialized`() {
        val config = ConversationConfig(
            agentId = "agent_1",
            userId = "user_7",
            overrides = Overrides(
                client = ClientOverrides(source = "android_sdk", version = "0.11")
            )
        )
        val json = ConversationOverridesBuilder.constructOverrides(config)

        assertEquals("user_7", json.getString("user_id"))
        val sourceInfo = json.getJSONObject("source_info")
        assertEquals("android_sdk", sourceInfo.getString("source"))
        assertEquals("0.11", sourceInfo.getString("version"))
    }
}
