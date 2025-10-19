package com.trendyol.sessionstore.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface CounterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(counter: SessionCounter)

    @Query("SELECT session FROM counter WHERE id = 0")
    suspend fun getSession(): Int?
}
