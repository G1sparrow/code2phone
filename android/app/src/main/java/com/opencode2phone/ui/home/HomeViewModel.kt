package com.opencode2phone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode2phone.data.repository.SessionRepository
import com.opencode2phone.di.ServerConfig
import com.opencode2phone.domain.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val phase: HomePhase = HomePhase.DIRECTORY_PICKER,
    val directories: List<String> = emptyList(),
    val selectedDirectory: String? = null,
    val sessions: List<Session> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val serverHost: String = "100.100.100.100",
    val serverPort: Int = 3001
)

enum class HomePhase {
    DIRECTORY_PICKER,
    SESSION_LIST
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val serverConfig: ServerConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            serverHost = serverConfig.host,
            serverPort = serverConfig.port
        )
        observeSessions()
        loadDirectories()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            sessionRepository.observeSessions().collect { sessions ->
                val selectedDir = _uiState.value.selectedDirectory
                if (selectedDir != null) {
                    _uiState.value = _uiState.value.copy(
                        sessions = sessions.filter { it.directory == selectedDir }
                    )
                }
            }
        }
    }

    fun loadDirectories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = sessionRepository.listDirectories()
            result.fold(
                onSuccess = { dirs ->
                    _uiState.value = _uiState.value.copy(
                        directories = dirs,
                        isLoading = false,
                        isConnected = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isConnected = false,
                        error = e.message ?: "Failed to load directories"
                    )
                }
            )
        }
    }

    fun selectDirectory(directory: String) {
        _uiState.value = _uiState.value.copy(
            selectedDirectory = directory,
            phase = HomePhase.SESSION_LIST
        )
        refreshSessions()
    }

    fun refreshSessions() {
        val dir = _uiState.value.selectedDirectory ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            val result = sessionRepository.refreshSessions(directory = dir)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = e.message ?: "Refresh failed"
                    )
                }
            )
        }
    }

    fun changeDirectory() {
        _uiState.value = _uiState.value.copy(
            phase = HomePhase.DIRECTORY_PICKER,
            selectedDirectory = null,
            sessions = emptyList()
        )
        loadDirectories()
    }

    fun updateServerConfig(host: String, port: Int) {
        serverConfig.host = host
        serverConfig.port = port
        _uiState.value = _uiState.value.copy(
            serverHost = host,
            serverPort = port
        )
    }
}
