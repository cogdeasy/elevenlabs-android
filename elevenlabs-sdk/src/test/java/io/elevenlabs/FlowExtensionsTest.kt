package io.elevenlabs

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.elevenlabs.models.ConversationStatus
import io.elevenlabs.utils.asLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlowExtensionsTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `asLiveData mirrors the StateFlow's current value`() {
        val flow = MutableStateFlow(ConversationStatus.DISCONNECTED)
        val liveData = flow.asLiveData()

        val observed = mutableListOf<ConversationStatus>()
        val observer = Observer<ConversationStatus> { observed.add(it) }
        liveData.observeForever(observer)

        try {
            assertEquals(ConversationStatus.DISCONNECTED, liveData.value)
        } finally {
            liveData.removeObserver(observer)
        }
    }

    @Test
    fun `asLiveData receives subsequent StateFlow updates`() {
        val flow = MutableStateFlow(0)
        val liveData = flow.asLiveData()

        val observed = mutableListOf<Int>()
        val observer = Observer<Int> { observed.add(it) }
        liveData.observeForever(observer)

        try {
            flow.value = 1
            flow.value = 2
            assertEquals(listOf(0, 1, 2), observed)
            assertEquals(2, liveData.value)
        } finally {
            liveData.removeObserver(observer)
        }
    }
}
