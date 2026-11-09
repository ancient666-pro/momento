package com.momento.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momento.core.common.AppResult
import com.momento.domain.model.Post
import com.momento.domain.model.Story
import com.momento.domain.repository.FeedRepository
import com.momento.domain.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FeedTab { FOLLOWING, FOR_YOU }

data class HomeUiState(
    val selectedTab: FeedTab = FeedTab.FOR_YOU,
    val stories: List<Story> = emptyList(),
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val storyRepository: StoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val storiesResult = storyRepository.getStories()
            val postsResult = when (_uiState.value.selectedTab) {
                FeedTab.FOLLOWING -> feedRepository.getFollowingFeed(0)
                FeedTab.FOR_YOU -> feedRepository.getForYouFeed(0)
            }

            val stories = (storiesResult as? AppResult.Success)?.data.orEmpty()
            val posts = (postsResult as? AppResult.Success)?.data.orEmpty()
            val error = when {
                storiesResult is AppResult.Failure -> storiesResult.error.message
                postsResult is AppResult.Failure -> postsResult.error.message
                else -> null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    stories = stories,
                    posts = posts,
                    errorMessage = if (stories.isEmpty() && posts.isEmpty()) error else null
                )
            }
        }
    }

    fun selectTab(tab: FeedTab) {
        if (tab == _uiState.value.selectedTab) return
        _uiState.update { it.copy(selectedTab = tab) }
        refresh()
    }

    fun toggleLike(post: Post) {
        val target = !post.isLiked
        _uiState.update { state ->
            state.copy(posts = state.posts.map {
                if (it.id == post.id) it.copy(
                    isLiked = target,
                    likeCount = it.likeCount + if (target) 1 else -1
                ) else it
            })
        }
        viewModelScope.launch {
            feedRepository.toggleLike(post.id, target)
        }
    }

    fun toggleBookmark(post: Post) {
        val target = !post.isBookmarked
        _uiState.update { state ->
            state.copy(posts = state.posts.map {
                if (it.id == post.id) it.copy(isBookmarked = target) else it
            })
        }
        viewModelScope.launch {
            feedRepository.toggleBookmark(post.id, target)
        }
    }
}
