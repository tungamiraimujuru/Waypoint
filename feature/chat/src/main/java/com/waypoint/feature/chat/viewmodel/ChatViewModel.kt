package com.waypoint.feature.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waypoint.core.ai.model.AiRequest
import com.waypoint.core.ai.model.AiStreamEvent
import com.waypoint.core.ai.orchestrator.AiOrchestrator
import com.waypoint.core.data.ItineraryRepository
import com.waypoint.core.domain.model.ConversationId
import com.waypoint.core.domain.model.TripContext
import com.waypoint.feature.chat.state.ChatIntent
import com.waypoint.feature.chat.state.ChatMessage
import com.waypoint.feature.chat.state.ChatNavEvent
import com.waypoint.feature.chat.state.ChatUiState
import com.waypoint.feature.chat.state.ItineraryPreview
import com.waypoint.feature.chat.state.reduceStreamEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val orchestrator: AiOrchestrator,
    private val itineraryRepository: ItineraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _nav = Channel<ChatNavEvent>(Channel.BUFFERED)
    val navEvents: Flow<ChatNavEvent> = _nav.receiveAsFlow()

    private var streamJob: Job? = null
    private var lastSentPrompt: String? = null

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.DraftChanged -> _state.update { it.copy(draft = intent.text) }
            ChatIntent.Send -> send()
            ChatIntent.CancelStream -> cancelStream()
            ChatIntent.RetryLast -> retryLast()
            ChatIntent.DismissError -> _state.update { it.copy(transientError = null) }
            ChatIntent.OpenItinerary -> openItinerary()
        }
    }

    private fun send() {
        val current = _state.value
        if (!current.canSend) return

        val prompt = current.draft.trim()
        lastSentPrompt = prompt
        startStream(prompt)
    }

    private fun retryLast() {
        val prompt = lastSentPrompt ?: return
        startStream(prompt)
    }

    private fun startStream(prompt: String) {
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatMessage.Role.User,
            text = prompt
        )
        val assistantId = UUID.randomUUID().toString()
        val assistantMsg = ChatMessage(
            id = assistantId,
            role = ChatMessage.Role.Assistant,
            text = "",
            isStreaming = true
        )

        _state.update {
            it.copy(
                messages = it.messages + userMsg + assistantMsg,
                draft = "",
                stream = ChatUiState.StreamState.Connecting,
                itineraryPreview = ItineraryPreview(),
                transientError = null,
                savedItineraryId = null
            )
        }

        streamJob?.cancel()
        streamJob = orchestrator.streamItinerary(
            AiRequest(
                userPrompt = prompt,
                context = TripContext.Empty,
                conversationId = ConversationId.Default
            )
        )
            .onEach { event ->
                _state.update { current ->
                    reduceStreamEvent(current, event, assistantId)
                }
                if (event is AiStreamEvent.Done) {
                    itineraryRepository.save(event.itinerary)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun cancelStream() {
        streamJob?.cancel()
        streamJob = null
        _state.update {
            it.copy(
                messages = it.messages.map { msg ->
                    if (msg.isStreaming) msg.copy(isStreaming = false) else msg
                },
                stream = ChatUiState.StreamState.Idle
            )
        }
    }

    private fun openItinerary() {
        val id = _state.value.savedItineraryId ?: return
        _nav.trySend(ChatNavEvent.OpenItinerary(id))
    }
}