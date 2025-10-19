package com.trendyol.sessionstore.sample.dataselection

import com.trendyol.sessionstore.sample.common.model.DataSize
import com.trendyol.sessionstore.sample.common.model.SessionData

data class SessionUiState(
    val isLoading: Boolean = false,
    val currentData: SessionData? = null,
    val selectedSize: DataSize = DataSize.SMALL,
    val message: String? = null,
)
