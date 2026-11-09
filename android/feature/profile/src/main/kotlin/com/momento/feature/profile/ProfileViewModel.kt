package com.momento.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momento.core.common.AppResult
import com.momento.domain.model.Moment
import com.momento.domain.model.Post
import com.momento.domain.model.UserProfile
import com.momento.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileTab { POST, REELS }

data class ProfileUiState(
    val profile: UserProfile? = null,
    val moments: List<Moment> = emptyList(),
    val posts: List<Post> = emptyList(),
    val selectedTab: ProfileTab = ProfileTab.REELS,
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // The authenticated user's own profile until account support lands with the auth phase.
    private val profileUserId = "me"

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val profileResult = profileRepository.getProfile(profileUserId)
            val momentsResult = profileRepository.getMoments(profileUserId)
            val postsResult = profileRepository.getPosts(profileUserId)

            val profile = (profileResult as? AppResult.Success)?.data
            val moments = (momentsResult as? AppResult.Success)?.data.orEmpty()
            val posts = (postsResult as? AppResult.Success)?.data.orEmpty()
            val error = when {
                profileResult is AppResult.Failure -> profileResult.error.message
                momentsResult is AppResult.Failure -> momentsResult.error.message
                else -> null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    profile = profile,
                    moments = moments,
                    posts = posts,
                    errorMessage = if (profile == null) error else null
                )
            }
        }
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleFollow() {
        val profile = _uiState.value.profile ?: return
        val target = !_uiState.value.isFollowing
        _uiState.update {
            it.copy(
                isFollowing = target,
                profile = profile.copy(
                    followerCount = profile.followerCount + if (target) 1 else -1
                )
            )
        }
        viewModelScope.launch {
            if (target) profileRepository.follow(profile.id) else profileRepository.unfollow(profile.id)
        }
    }
}
