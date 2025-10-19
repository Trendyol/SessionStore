package com.trendyol.sessionstore.sample.dataselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendyol.sessionstore.SessionStore
import com.trendyol.sessionstore.sample.common.DefaultDispatcher
import com.trendyol.sessionstore.sample.common.model.DataSize
import com.trendyol.sessionstore.sample.common.model.SessionData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _navigateToDataDetail = Channel<String>()
    val navigateToDataDetail = _navigateToDataDetail.receiveAsFlow()

    fun storeDataAndNavigate(dataSize: DataSize) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val sessionData = create(dataSize)
        val key = sessionData.hashCode().toString()
        val editor = SessionStore.getEditor()

        editor.setObject(key, sessionData)

        _uiState.update {
            it.copy(
                isLoading = false,
                currentData = sessionData,
            )
        }
        _navigateToDataDetail.send(key)
    }

    fun updateSelectedSize(dataSize: DataSize) {
        _uiState.update { it.copy(selectedSize = dataSize) }
    }

    private suspend fun create(dataSize: DataSize): SessionData = withContext(defaultDispatcher) {
        val payload = generatePayload(dataSize)
        SessionData(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            dataSize = dataSize,
            payload = payload,
        )
    }

    private fun generatePayload(dataSize: DataSize): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return buildString {
            repeat(dataSize.bytes) {
                append(chars.random())
            }
        }
    }
}
