package com.trendyol.sessionstore.database

import androidx.room.Entity

@Entity(
    tableName = "object",
    primaryKeys = ["key", "session"]
)
internal data class StoredObject(
    val key: String,
    val session: Int,
    val byteArray: ByteArray?,
    val json: String?,
    val type: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StoredObject

        if (session != other.session) return false
        if (key != other.key) return false
        if (byteArray != null && other.byteArray != null && !byteArray.contentEquals(other.byteArray)) return false
        if (byteArray != null && other.byteArray == null) return false
        if (byteArray == null && other.byteArray != null) return false
        if (json != other.json) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = session
        result = 31 * result + key.hashCode()
        result = 31 * result + (byteArray?.contentHashCode() ?: 0)
        result = 31 * result + (json?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        return result
    }
}
