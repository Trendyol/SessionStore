package com.trendyol.sessionstore.impl

import com.trendyol.sessionstore.SessionStoreEditor
import com.trendyol.sessionstore.adapter.ByteArraySerializationAdapter
import com.trendyol.sessionstore.adapter.JsonSerializationAdapter
import com.trendyol.sessionstore.adapter.SerializationAdapter
import com.trendyol.sessionstore.database.SessionDatabase
import com.trendyol.sessionstore.database.StoredObjectChunk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CHUNK_SIZE = 1024 * 1024 // 1MB per chunk

internal class SessionStoreEditorImpl(
    private val database: SessionDatabase,
    private val serializationAdapter: SerializationAdapter,
    private val session: Int,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : SessionStoreEditor {

    override suspend fun setObject(key: String, value: Any) = withContext(dispatcher) {
        val typeName = value.javaClass.name
        val chunks = when (serializationAdapter) {
            is ByteArraySerializationAdapter -> {
                val byteArray = serializationAdapter.serialize(value)
                createByteArrayChunks(key, byteArray, typeName)
            }

            is JsonSerializationAdapter -> {
                val json = serializationAdapter.serialize(value)
                createJsonChunks(key, json, typeName)
            }

            else -> error("Unknown SerializationAdapter type: ${serializationAdapter::class.java.name}")
        }

        database.objectDao().deleteChunks(key, session)
        database.objectDao().upsertChunks(chunks)
    }

    override suspend fun <T : Any> getObject(key: String, type: Class<T>): T? =
        withContext(dispatcher) {
            val chunks = database.objectDao().getChunks(key, session)
            if (chunks.isEmpty()) return@withContext null

            return@withContext when (serializationAdapter) {
                is ByteArraySerializationAdapter -> {
                    val reassembled = reassembleByteArrayChunks(chunks)
                    serializationAdapter.deserialize(reassembled, type)
                }

                is JsonSerializationAdapter -> {
                    val reassembled = reassembleJsonChunks(chunks)
                    serializationAdapter.deserialize(reassembled, type)
                }

                else -> error("Unknown SerializationAdapter type: ${serializationAdapter::class.java.name}")
            }
        }

    private fun createByteArrayChunks(
        key: String,
        byteArray: ByteArray,
        type: String
    ): List<StoredObjectChunk> {
        val totalChunks = (byteArray.size + CHUNK_SIZE - 1) / CHUNK_SIZE
        return (0 until totalChunks).map { chunkIndex ->
            val start = chunkIndex * CHUNK_SIZE
            val end = minOf(start + CHUNK_SIZE, byteArray.size)
            val chunkData = byteArray.copyOfRange(start, end)

            StoredObjectChunk(
                key = key,
                session = session,
                chunkIndex = chunkIndex,
                totalChunks = totalChunks,
                byteArray = chunkData,
                json = null,
                type = type
            )
        }
    }

    private fun createJsonChunks(key: String, json: String, type: String): List<StoredObjectChunk> {
        val totalChunks = (json.length + CHUNK_SIZE - 1) / CHUNK_SIZE
        return (0 until totalChunks).map { chunkIndex ->
            val start = chunkIndex * CHUNK_SIZE
            val end = minOf(start + CHUNK_SIZE, json.length)
            val chunkData = json.substring(start, end)

            StoredObjectChunk(
                key = key,
                session = session,
                chunkIndex = chunkIndex,
                totalChunks = totalChunks,
                byteArray = null,
                json = chunkData,
                type = type
            )
        }
    }

    private fun reassembleByteArrayChunks(chunks: List<StoredObjectChunk>): ByteArray {
        val totalSize = chunks.sumOf { it.byteArray?.size ?: 0 }
        val result = ByteArray(totalSize)
        var offset = 0

        for (chunk in chunks) {
            chunk.byteArray?.let {
                it.copyInto(result, offset)
                offset += it.size
            }
        }

        return result
    }

    private fun reassembleJsonChunks(chunks: List<StoredObjectChunk>): String {
        return chunks.mapNotNull { it.json }.joinToString("")
    }
}
