package io.elevenlabs.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.elevenlabs.example.ui.AppTheme
import io.elevenlabs.example.ui.StartScreen
import io.elevenlabs.models.ConversationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the landing surface. No network or API keys required —
 * the screen is rendered directly with stubbed state and callbacks.
 */
@RunWith(AndroidJUnit4::class)
class StartScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        textOnlyMode: Boolean = false,
        onToggleTextOnly: (Boolean) -> Unit = {},
        status: ConversationStatus = ConversationStatus.DISCONNECTED,
        onConnect: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                StartScreen(
                    textOnlyMode = textOnlyMode,
                    onToggleTextOnly = onToggleTextOnly,
                    status = status,
                    onConnect = onConnect,
                )
            }
        }
    }

    @Test
    fun connectButton_isEnabled_whenDisconnected() {
        setScreen(status = ConversationStatus.DISCONNECTED)

        composeTestRule.onNodeWithText("Connect").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun connectButton_isDisabled_andShowsConnecting_whileConnecting() {
        setScreen(status = ConversationStatus.CONNECTING)

        composeTestRule.onNodeWithText("Connecting…").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun connectButton_click_invokesCallback() {
        var connectCount = 0
        setScreen(onConnect = { connectCount++ })

        composeTestRule.onNodeWithText("Connect").performClick()

        assertEquals(1, connectCount)
    }

    @Test
    fun textOnlyToggle_reflectsState_andInvokesCallback() {
        var toggled: Boolean? = null
        setScreen(textOnlyMode = false, onToggleTextOnly = { toggled = it })

        composeTestRule.onNodeWithText("Use Text-Only Mode").assertIsDisplayed()
        composeTestRule.onNode(isToggleable()).assertIsOff()
        composeTestRule.onNode(isToggleable()).performClick()

        assertTrue(toggled == true)
    }

    @Test
    fun textOnlyToggle_showsOn_whenTextOnlyModeEnabled() {
        setScreen(textOnlyMode = true)

        composeTestRule.onNode(isToggleable()).assertIsOn()
    }
}
