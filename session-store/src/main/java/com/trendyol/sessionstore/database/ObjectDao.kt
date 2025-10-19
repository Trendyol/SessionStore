package com.trendyol.sessionstore.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface ObjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChunks(chunks: List<StoredObjectChunk>)

    @Query("SELECT * FROM object_chunk WHERE `key` = :key AND session = :session ORDER BY chunkIndex ASC")
    suspend fun getChunks(key: String, session: Int): List<StoredObjectChunk>

    @Query("DELETE FROM object_chunk WHERE `key` = :key AND session = :session")
    suspend fun deleteChunks(key: String, session: Int)

    @Query("DELETE FROM object_chunk WHERE session < :currentSession")
    suspend fun deleteOlderThan(currentSession: Int)
}
