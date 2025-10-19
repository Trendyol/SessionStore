package com.trendyol.sessionstore.sample.datadetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendyol.sessionstore.SessionStore
import com.trendyol.sessionstore.sample.common.model.SessionData
import com.trendyol.sessionstore.sample.datadetail.DataDetailFragment.Companion.KEY_SESSION_DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val key: String = savedStateHandle.get<String>(KEY_SESSION_DATA)!!

    private val _uiState = MutableStateFlow(DataDetailUiState())
    val uiState: StateFlow<DataDetailUiState> = _uiState
        .onStart { retrieveData() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 30_000L),
            initialValue = DataDetailUiState()
        )

    private fun retrieveData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val editor = SessionStore.getEditor()
            val data = editor.getObject(key, SessionData::class.java)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentData = data
                )
            }
        }
    }
}
