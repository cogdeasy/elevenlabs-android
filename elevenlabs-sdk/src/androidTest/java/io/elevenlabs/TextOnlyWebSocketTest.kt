package io.elevenlabs

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.elevenlabs.network.ConnectionState
import io.elevenlabs.network.WebSocketConnection
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Instrumented integration tests for the text-only WebSocket transport,
 * exercised against a local MockWebServer on the device/emulator.
 */
@RunWith(AndroidJUnit4::class)
class TextOnlyWebSocketTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private val serverSockets = ConcurrentLinkedQueue<WebSocket>()
    private val connections = ConcurrentLinkedQueue<WebSocketConnection>()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient()
    }

    @After
    fun tearDown() {
        connections.forEach { runCatching { it.cleanup() } }
        serverSockets.forEach { runCatching { it.close(1000, "test teardown") } }
        client.dispatcher.executorService.shutdownNow()
        client.connectionPool.evictAll()
        server.shutdown()
    }

    private fun apiBaseUrl(): String = server.url("/").toString().removeSuffix("/")

    private fun newConnection(): WebSocketConnection = WebSocketConnection(client = client).also { connections.add(it) }

    private fun enqueueServerWs(
        onOpen: ((WebSocket) -> Unit)? = null,
        onMessage: ((WebSocket, String) -> Unit)? = null,
    ) {
        server.enqueue(
            MockResponse().withWebSocketUpgrade(
                object : WebSocketListener() {
                    override fun onOpen(
                        webSocket: WebSocket,
                        response: Response,
                    ) {
                        serverSockets.add(webSocket)
                        onOpen?.invoke(webSocket)
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String,
                    ) {
                        onMessage?.invoke(webSocket, text)
                    }
                },
            ),
        )
    }

    @Test
    fun connectsAndReceivesConversationId() {
        val connectedLatch = CountDownLatch(1)
        val conversationId = AtomicReference<String>()

        enqueueServerWs(onOpen = { ws ->
            ws.send(
                """{"type":"conversation_initiation_metadata",
                    "conversation_initiation_metadata_event":{
                      "conversation_id":"conv_device_1",
                      "agent_output_audio_format":"pcm_16000",
                      "user_input_audio_format":"pcm_16000"}}""",
            )
        })

        val connection = newConnection()
        val config =
            ConversationConfig(
                agentId = "agent_device",
                textOnly = true,
                onConnect = { id ->
                    conversationId.set(id)
                    connectedLatch.countDown()
                },
            )

        runBlocking { connection.connect(apiBaseUrl(), config) }

        assertTrue(
            "Expected onConnect within 10s",
            connectedLatch.await(10, TimeUnit.SECONDS),
        )
        assertEquals("conv_device_1", conversationId.get())
        assertEquals(ConnectionState.CONNECTED, connection.connectionState)
    }

    @Test
    fun sendsUserMessageAndReceivesAgentResponse() {
        val initiationReceived = CountDownLatch(1)
        val userMessageReceived = CountDownLatch(1)
        val agentResponseReceived = CountDownLatch(1)
        val receivedUserText = AtomicReference<String>()
        val receivedAgentText = AtomicReference<String>()

        enqueueServerWs(
            onOpen = { ws ->
                ws.send(
                    """{"type":"conversation_initiation_metadata",
                        "conversation_initiation_metadata_event":{"conversation_id":"conv_device_2"}}""",
                )
            },
            onMessage = { ws, text ->
                val obj = JSONObject(text)
                when (obj.optString("type")) {
                    "conversation_initiation_client_data" -> initiationReceived.countDown()
                    "user_message" -> {
                        receivedUserText.set(obj.optString("text"))
                        userMessageReceived.countDown()
                        ws.send(
                            """{"type":"agent_response",
                                "agent_response_event":{"agent_response":"Hello from mock","event_id":1}}""",
                        )
                    }
                }
            },
        )

        val connection = newConnection()
        val connectedLatch = CountDownLatch(1)
        val config =
            ConversationConfig(
                agentId = "agent_device",
                textOnly = true,
                onConnect = { connectedLatch.countDown() },
            )

        connection.setOnMessageListener { messageJson ->
            val obj = JSONObject(messageJson)
            if (obj.optString("type") == "agent_response") {
                receivedAgentText.set(
                    obj.getJSONObject("agent_response_event").optString("agent_response"),
                )
                agentResponseReceived.countDown()
            }
        }

        runBlocking { connection.connect(apiBaseUrl(), config) }

        assertTrue("Expected onConnect", connectedLatch.await(10, TimeUnit.SECONDS))
        assertTrue(
            "Server should receive initiation payload",
            initiationReceived.await(10, TimeUnit.SECONDS),
        )

        connection.sendMessage(
            io.elevenlabs.network.OutgoingEvent
                .UserMessage(text = "hi agent"),
        )

        assertTrue(
            "Server should receive user_message",
            userMessageReceived.await(10, TimeUnit.SECONDS),
        )
        assertEquals("hi agent", receivedUserText.get())
        assertTrue(
            "Client should receive agent_response",
            agentResponseReceived.await(10, TimeUnit.SECONDS),
        )
        assertEquals("Hello from mock", receivedAgentText.get())
    }

    @Test
    fun disconnectResetsConnectionState() {
        val connectedLatch = CountDownLatch(1)
        enqueueServerWs(onOpen = { ws ->
            ws.send(
                """{"type":"conversation_initiation_metadata",
                    "conversation_initiation_metadata_event":{"conversation_id":"conv_device_3"}}""",
            )
        })

        val connection = newConnection()
        val config =
            ConversationConfig(
                agentId = "agent_device",
                textOnly = true,
                onConnect = { connectedLatch.countDown() },
            )

        runBlocking { connection.connect(apiBaseUrl(), config) }
        assertTrue("Expected onConnect", connectedLatch.await(10, TimeUnit.SECONDS))
        assertEquals(ConnectionState.CONNECTED, connection.connectionState)

        connection.disconnect()

        assertEquals(ConnectionState.IDLE, connection.connectionState)
    }
}
