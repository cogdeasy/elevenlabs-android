package io.elevenlabs

import io.elevenlabs.audio.AudioManager
import io.elevenlabs.models.ConversationEvent
import io.elevenlabs.models.ConversationMode
import io.elevenlabs.network.OutgoingEvent
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Callback dispatch behavior of [ConversationEventHandler]: legacy (deprecated)
 * callback bridging, event-id-aware callbacks, ping/pong, feedback deduplication,
 * and conversation mode transitions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationEventHandlerCallbacksTest {
    private lateinit var audioManager: AudioManager
    private lateinit var toolRegistry: ClientToolRegistry
    private val sentEvents = mutableListOf<OutgoingEvent>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        audioManager = mockk(relaxed = true)
        toolRegistry = ClientToolRegistry()
        sentEvents.clear()
    }

    @After
    fun teardown() {
        toolRegistry.cleanup()
        Dispatchers.resetMain()
    }

    private fun handler(
        onUserTranscript: ((String) -> Unit)? = null,
        onAgentResponse: ((String) -> Unit)? = null,
        onAgentResponseCorrection: ((String, String) -> Unit)? = null,
        onUserTranscriptEvent: ((String, Int?) -> Unit)? = null,
        onAgentResponseEvent: ((String, Int?) -> Unit)? = null,
        onAgentResponseCorrectionEvent: ((String, Int?) -> Unit)? = null,
        onVadScore: ((Float) -> Unit)? = null,
        onCanSendFeedbackChange: ((Boolean) -> Unit)? = null,
        onInterruption: ((Int) -> Unit)? = null,
        onError: ((Int, String?) -> Unit)? = null,
    ) = ConversationEventHandler(
        audioManager = audioManager,
        toolRegistry = toolRegistry,
        messageCallback = { sentEvents.add(it) },
        onUserTranscript = onUserTranscript,
        onAgentResponse = onAgentResponse,
        onAgentResponseCorrection = onAgentResponseCorrection,
        onUserTranscriptEvent = onUserTranscriptEvent,
        onAgentResponseEvent = onAgentResponseEvent,
        onAgentResponseCorrectionEvent = onAgentResponseCorrectionEvent,
        onVadScore = onVadScore,
        onCanSendFeedbackChange = onCanSendFeedbackChange,
        onInterruption = onInterruption,
        onError = onError,
    )

    @Test
    fun `deprecated onUserTranscript and event-aware callback both fire`() =
        runTest {
            var legacy: String? = null
            var eventText: String? = null
            var eventId: Int? = null
            val h =
                handler(
                    onUserTranscript = { legacy = it },
                    onUserTranscriptEvent = { text, id ->
                        eventText = text
                        eventId = id
                    },
                )

            h.handleIncomingEvent(ConversationEvent.UserTranscript(userTranscript = "hello", eventId = 5))

            assertEquals("hello", legacy)
            assertEquals("hello", eventText)
            assertEquals(5, eventId)
        }

    @Test
    fun `deprecated onAgentResponse and event-aware callback both fire`() =
        runTest {
            var legacy: String? = null
            var eventText: String? = null
            var eventId: Int? = null
            val h =
                handler(
                    onAgentResponse = { legacy = it },
                    onAgentResponseEvent = { text, id ->
                        eventText = text
                        eventId = id
                    },
                )

            h.handleIncomingEvent(ConversationEvent.AgentResponse(agentResponse = "hi there", eventId = 2))

            assertEquals("hi there", legacy)
            assertEquals("hi there", eventText)
            assertEquals(2, eventId)
        }

    @Test
    fun `correction fires deprecated pair callback and corrected-text event callback`() =
        runTest {
            var legacyOriginal: String? = null
            var legacyCorrected: String? = null
            var eventText: String? = null
            val h =
                handler(
                    onAgentResponseCorrection = { original, corrected ->
                        legacyOriginal = original
                        legacyCorrected = corrected
                    },
                    onAgentResponseCorrectionEvent = { text, _ -> eventText = text },
                )

            h.handleIncomingEvent(
                ConversationEvent.AgentResponseCorrection(
                    originalAgentResponse = "wrng",
                    correctedAgentResponse = "wrong",
                    eventId = 9,
                ),
            )

            assertEquals("wrng", legacyOriginal)
            assertEquals("wrong", legacyCorrected)
            assertEquals("wrong", eventText)
        }

    @Test
    fun `a throwing callback does not break event processing`() =
        runTest {
            var eventText: String? = null
            val h =
                handler(
                    onUserTranscript = { throw RuntimeException("app bug") },
                    onUserTranscriptEvent = { text, _ -> eventText = text },
                )

            h.handleIncomingEvent(ConversationEvent.UserTranscript(userTranscript = "still works", eventId = 1))

            assertEquals("still works", eventText)
            assertEquals(1, h.messages.value.size)
        }

    @Test
    fun `ping is answered with a pong carrying the same event id`() =
        runTest {
            val h = handler()

            h.handleIncomingEvent(ConversationEvent.Ping(eventId = 42, pingMs = null))

            val pong = sentEvents.filterIsInstance<OutgoingEvent.Pong>().single()
            assertEquals(42, pong.eventId)
        }

    @Test
    fun `vad score is forwarded`() =
        runTest {
            var score: Float? = null
            val h = handler(onVadScore = { score = it })

            h.handleIncomingEvent(ConversationEvent.VadScore(score = 0.87f))

            assertEquals(0.87f, score)
        }

    @Test
    fun `server error is forwarded with code and message`() =
        runTest {
            var code: Int? = null
            var message: String? = null
            val h =
                handler(onError = { c, m ->
                    code = c
                    message = m
                })

            h.handleIncomingEvent(ConversationEvent.ServerError(code = 1011, message = "boom"))

            assertEquals(1011, code)
            assertEquals("boom", message)
        }

    @Test
    fun `agent response switches mode to SPEAKING and interruption back to LISTENING`() =
        runTest {
            var canSendFeedback: Boolean? = null
            var interruptedEventId: Int? = null
            val h =
                handler(
                    onCanSendFeedbackChange = { canSendFeedback = it },
                    onInterruption = { interruptedEventId = it },
                )
            assertEquals(ConversationMode.LISTENING, h.getCurrentMode())

            h.handleIncomingEvent(ConversationEvent.AgentResponse(agentResponse = "speaking now", eventId = 1))
            assertEquals(ConversationMode.SPEAKING, h.getCurrentMode())
            assertEquals(true, canSendFeedback)

            h.handleIncomingEvent(ConversationEvent.Interruption(eventId = 2))
            assertEquals(ConversationMode.LISTENING, h.getCurrentMode())
            assertEquals(false, canSendFeedback)
            assertEquals(2, interruptedEventId)
        }

    @Test
    fun `sendFeedback sends like for the last agent event and dedupes repeats`() =
        runTest {
            val h = handler()
            h.handleIncomingEvent(ConversationEvent.AgentResponse(agentResponse = "answer", eventId = 7))

            h.sendFeedback(isPositive = true)
            h.sendFeedback(isPositive = false)

            val feedback = sentEvents.filterIsInstance<OutgoingEvent.Feedback>()
            assertEquals(1, feedback.size)
            assertEquals("like", feedback[0].score)
            assertEquals(7, feedback[0].eventId)
        }

    @Test
    fun `sendFeedback is a no-op when no agent message exists`() =
        runTest {
            val h = handler()

            h.sendFeedback(isPositive = true)

            assertTrue(sentEvents.filterIsInstance<OutgoingEvent.Feedback>().isEmpty())
        }

    @Test
    fun `sendFeedback allows feedback for a newer agent response`() =
        runTest {
            val h = handler()
            h.handleIncomingEvent(ConversationEvent.AgentResponse(agentResponse = "first", eventId = 1))
            h.sendFeedback(isPositive = true)

            h.handleIncomingEvent(ConversationEvent.AgentResponse(agentResponse = "second", eventId = 2))
            h.sendFeedback(isPositive = false)

            val feedback = sentEvents.filterIsInstance<OutgoingEvent.Feedback>()
            assertEquals(2, feedback.size)
            assertEquals("dislike", feedback[1].score)
            assertEquals(2, feedback[1].eventId)
        }

    @Test
    fun `sendContextualUpdate and sendUserActivity emit the right outgoing events`() =
        runTest {
            val h = handler()

            h.sendContextualUpdate("user opened settings")
            h.sendUserActivity()

            val contextual = sentEvents.filterIsInstance<OutgoingEvent.ContextualUpdate>().single()
            assertEquals("user opened settings", contextual.text)
            assertEquals(1, sentEvents.filterIsInstance<OutgoingEvent.UserActivity>().size)
        }

    @Test
    fun `unregistered tool call with no handler sends an automatic error result`() =
        runTest {
            val h = handler()

            h.handleIncomingEvent(
                ConversationEvent.ClientToolCall(
                    toolName = "missing_tool",
                    parameters = emptyMap(),
                    toolCallId = "call_1",
                    expectsResponse = true,
                ),
            )

            val result = sentEvents.filterIsInstance<OutgoingEvent.ClientToolResult>().single()
            assertEquals("call_1", result.toolCallId)
            assertTrue(result.isError)
        }

    @Test
    fun `registered tool executes and returns its result to the agent`() =
        runTest {
            toolRegistry.registerTool(
                "echo",
                object : ClientTool {
                    override suspend fun execute(parameters: Map<String, Any>): ClientToolResult =
                        ClientToolResult.success("echo:" + parameters["text"])
                },
            )
            val h = handler()

            h.handleIncomingEvent(
                ConversationEvent.ClientToolCall(
                    toolName = "echo",
                    parameters = mapOf("text" to "hi"),
                    toolCallId = "call_2",
                    expectsResponse = true,
                ),
            )

            val result = sentEvents.filterIsInstance<OutgoingEvent.ClientToolResult>().single()
            assertEquals("call_2", result.toolCallId)
            assertEquals("echo:hi", result.result)
            assertFalse(result.isError)
        }
}
