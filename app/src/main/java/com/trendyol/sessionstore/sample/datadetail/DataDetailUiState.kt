package com.trendyol.sessionstore.sample.datadetail

import com.trendyol.sessionstore.sample.common.model.SessionData

data class DataDetailUiState(
    val isLoading: Boolean = false,
    val currentData: SessionData? = null
)
