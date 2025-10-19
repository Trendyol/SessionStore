package com.trendyol.sessionstore.database

import androidx.room.Entity

@Entity(
    tableName = "object_chunk",
    primaryKeys = ["key", "session", "chunkIndex"]
)
internal data class StoredObjectChunk(
    val key: String,
    val session: Int,
    val chunkIndex: Int,
    val totalChunks: Int,
    val byteArray: ByteArray?,
    val json: String?,
    val type: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StoredObjectChunk

        if (key != other.key) return false
        if (session != other.session) return false
        if (chunkIndex != other.chunkIndex) return false
        if (totalChunks != other.totalChunks) return false
        if (byteArray != null && other.byteArray != null && !byteArray.contentEquals(other.byteArray)) return false
        if (byteArray != null && other.byteArray == null) return false
        if (byteArray == null && other.byteArray != null) return false
        if (json != other.json) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + session
        result = 31 * result + chunkIndex
        result = 31 * result + totalChunks
        result = 31 * result + (byteArray?.contentHashCode() ?: 0)
        result = 31 * result + (json?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        return result
    }
}
