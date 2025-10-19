package com.trendyol.sessionstore.sample.common.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SessionData(
    val id: String,
    val timestamp: Long,
    val dataSize: DataSize,
    val payload: String,
) : Parcelable
