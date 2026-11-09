package com.momento.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momento.core.common.AppResult
import com.momento.domain.model.AudioRoom
import com.momento.domain.model.NearbyProfile
import com.momento.domain.model.SparkCard
import com.momento.domain.repository.DiscoverRepository
import com.momento.domain.repository.SparkFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NearbyAction { PASS, SUPER_LIKE, LIKE }

data class DiscoverUiState(
    val nearby: List<NearbyProfile> = emptyList(),
    val sparks: List<SparkCard> = emptyList(),
    val audioRooms: List<AudioRoom> = emptyList(),
    val selectedFilter: SparkFilter = SparkFilter.TRENDING,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val nearbyResult = discoverRepository.getNearbyProfiles()
            val sparksResult = discoverRepository.getTrendingSparks(_uiState.value.selectedFilter)
            val roomsResult = discoverRepository.getAudioRooms()

            val nearby = (nearbyResult as? AppResult.Success)?.data.orEmpty()
            val sparks = (sparksResult as? AppResult.Success)?.data.orEmpty()
            val rooms = (roomsResult as? AppResult.Success)?.data.orEmpty()
            val error = when {
                nearbyResult is AppResult.Failure -> nearbyResult.error.message
                sparksResult is AppResult.Failure -> sparksResult.error.message
                else -> null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    nearby = nearby,
                    sparks = sparks,
                    audioRooms = rooms,
                    errorMessage = if (nearby.isEmpty() && sparks.isEmpty()) error else null
                )
            }
        }
    }

    fun selectFilter(filter: SparkFilter) {
        if (filter == _uiState.value.selectedFilter) return
        _uiState.update { it.copy(selectedFilter = filter) }
        viewModelScope.launch {
            val result = discoverRepository.getTrendingSparks(filter)
            _uiState.update { state ->
                state.copy(
                    sparks = (result as? AppResult.Success)?.data.orEmpty(),
                    errorMessage = if (result is AppResult.Failure) result.error.message else state.errorMessage
                )
            }
        }
    }

    fun actOnTopCard(action: NearbyAction) {
        val top = _uiState.value.nearby.firstOrNull() ?: return
        _uiState.update { state ->
            state.copy(nearby = state.nearby.drop(1))
        }
        viewModelScope.launch {
            when (action) {
                NearbyAction.PASS -> discoverRepository.pass(top.profile.id)
                NearbyAction.SUPER_LIKE -> discoverRepository.superLike(top.profile.id)
                NearbyAction.LIKE -> discoverRepository.like(top.profile.id)
            }
        }
    }
}
