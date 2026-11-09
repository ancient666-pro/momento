package com.momento.feature.pod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momento.core.common.AppResult
import com.momento.domain.model.PodFeedItem
import com.momento.domain.model.PodPollCard
import com.momento.domain.model.PodTopic
import com.momento.domain.repository.PodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PodUiState(
    val topics: List<PodTopic> = emptyList(),
    val selectedTopicId: String? = null,
    val feed: List<PodFeedItem> = emptyList(),
    val liveListenerCount: Long = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class PodViewModel @Inject constructor(
    private val podRepository: PodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PodUiState())
    val uiState: StateFlow<PodUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val feedResult = podRepository.getPodFeed()
            val topicsResult = podRepository.getTopics()
            val liveCountResult = podRepository.getLiveListenerCount()

            val feed = (feedResult as? AppResult.Success)?.data.orEmpty()
            val topics = (topicsResult as? AppResult.Success)?.data.orEmpty()
            val liveCount = (liveCountResult as? AppResult.Success)?.data ?: 0L
            val error = when {
                feedResult is AppResult.Failure -> feedResult.error.message
                topicsResult is AppResult.Failure -> topicsResult.error.message
                else -> null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    feed = feed,
                    topics = topics,
                    selectedTopicId = it.selectedTopicId ?: topics.firstOrNull()?.id,
                    liveListenerCount = liveCount,
                    errorMessage = if (feed.isEmpty()) error else null
                )
            }
        }
    }

    fun selectTopic(topicId: String) {
        _uiState.update { it.copy(selectedTopicId = topicId) }
        // Topic filtering is a backend query; re-fetch arrives with the hashtag phase.
    }

    fun vote(poll: PodPollCard, optionId: String) {
        if (poll.myVoteOptionId != null) return
        viewModelScope.launch {
            val result = podRepository.vote(poll.podId, poll.id, optionId)
            if (result is AppResult.Success) {
                _uiState.update { state ->
                    state.copy(
                        feed = state.feed.map {
                            if (it is PodPollCard && it.id == poll.id) result.data else it
                        }
                    )
                }
            }
        }
    }

    fun joinPod(podId: String) {
        viewModelScope.launch {
            podRepository.joinPod(podId)
            // Navigation into the live room arrives with the Nexconn Call SDK phase.
        }
    }
}
