package com.trendyol.sessionstore.lifecycle

internal enum class Reason {
    FIRST_LAUNCH,
    ACTIVITY_RECREATION,
    PROCESS_DEATH
}
