package io.elevenlabs.example

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.elevenlabs.example.models.TextChatMessage
import io.elevenlabs.example.ui.AppTheme
import io.elevenlabs.example.ui.TextChatScreen
import io.elevenlabs.models.ConversationStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the text chat surface. No network or API keys required —
 * the screen is rendered directly with stubbed state and callbacks.
 */
@RunWith(AndroidJUnit4::class)
class TextChatScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        status: ConversationStatus = ConversationStatus.CONNECTED,
        messages: List<TextChatMessage> = emptyList(),
        isAgentTyping: Boolean = false,
        errorMessage: String? = null,
        onSend: (String) -> Unit = {},
        onRetry: () -> Unit = {},
        onDisconnect: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                TextChatScreen(
                    status = status,
                    messages = messages,
                    isAgentTyping = isAgentTyping,
                    errorMessage = errorMessage,
                    onSend = onSend,
                    onRetry = onRetry,
                    onDisconnect = onDisconnect,
                )
            }
        }
    }

    @Test
    fun header_showsConnectedStatus() {
        setScreen(status = ConversationStatus.CONNECTED)

        composeTestRule.onNodeWithText("Chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
    }

    @Test
    fun messages_areRendered() {
        setScreen(
            messages =
                listOf(
                    TextChatMessage("1", "Hello agent", isFromUser = true),
                    TextChatMessage("2", "Hello user", isFromUser = false),
                ),
        )

        composeTestRule.onNodeWithText("Hello agent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello user").assertIsDisplayed()
    }

    @Test
    fun composer_sendsTrimmedText_andClearsInput() {
        var sent: String? = null
        setScreen(onSend = { sent = it })

        composeTestRule.onNodeWithText("Type a message…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Type a message…").performTextInput("  hi there  ")
        composeTestRule.onNodeWithContentDescription("Send").performClick()

        assertEquals("hi there", sent)
        composeTestRule.onNodeWithText("Type a message…").assertIsDisplayed()
    }

    @Test
    fun composer_showsConnectingPlaceholder_whenNotConnected() {
        setScreen(status = ConversationStatus.CONNECTING)

        // Header status label and composer placeholder both show "Connecting…"
        composeTestRule.onAllNodesWithText("Connecting…").assertCountEquals(2)
    }

    @Test
    fun errorState_showsMessage_andRetryInvokesCallback() {
        var retried = 0
        setScreen(
            status = ConversationStatus.ERROR,
            messages = emptyList(),
            errorMessage = "Connection failed",
            onRetry = { retried++ },
        )

        composeTestRule.onNodeWithText("Connection failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()

        assertEquals(1, retried)
    }

    @Test
    fun disconnectButton_invokesCallback() {
        var disconnected = 0
        setScreen(onDisconnect = { disconnected++ })

        composeTestRule.onNodeWithText("Disconnect").performClick()

        assertEquals(1, disconnected)
    }
}
