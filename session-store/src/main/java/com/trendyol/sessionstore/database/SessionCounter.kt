package com.trendyol.sessionstore.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counter")
internal data class SessionCounter(
    @PrimaryKey val id: Int = 0,
    val session: Int
)
