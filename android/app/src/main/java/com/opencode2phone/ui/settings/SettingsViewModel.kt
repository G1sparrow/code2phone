package com.opencode2phone.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode2phone.data.repository.ModelRepository
import com.opencode2phone.di.ServerConfig
import com.opencode2phone.domain.model.OpencodeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val host: String = "100.100.100.100",
    val port: String = "3001",
    val models: List<OpencodeModel> = emptyList(),
    val selectedModel: OpencodeModel? = null,
    val isLoadingModels: Boolean = false,
    val modelError: String? = null,
    val connectionTestResult: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverConfig: ServerConfig,
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(
        host = serverConfig.host,
        port = serverConfig.port.toString()
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onHostChanged(host: String) {
        _uiState.value = _uiState.value.copy(host = host)
    }

    fun onPortChanged(port: String) {
        _uiState.value = _uiState.value.copy(port = port)
    }

    fun saveConfig() {
        val host = _uiState.value.host.trim()
        val port = _uiState.value.port.trim().toIntOrNull() ?: 3001
        serverConfig.host = host
        serverConfig.port = port
    }

    fun testConnection() {
        saveConfig()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingModels = true,
                modelError = null,
                connectionTestResult = "Testing..."
            )
            val result = modelRepository.getModels(forceRefresh = true)
            result.fold(
                onSuccess = { models ->
                    _uiState.value = _uiState.value.copy(
                        models = models,
                        isLoadingModels = false,
                        connectionTestResult = "Connected! ${models.size} models available",
                        selectedModel = models.firstOrNull()
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingModels = false,
                        modelError = e.message,
                        connectionTestResult = "Failed: ${e.message}"
                    )
                }
            )
        }
    }
}
