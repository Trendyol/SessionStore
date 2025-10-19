package com.trendyol.sessionstore

import com.google.common.truth.Truth.assertThat
import com.trendyol.sessionstore.adapter.ByteArraySerializationAdapter
import com.trendyol.sessionstore.adapter.JsonSerializationAdapter
import com.trendyol.sessionstore.adapter.SerializationAdapter
import com.trendyol.sessionstore.database.ObjectDao
import com.trendyol.sessionstore.database.SessionDatabase
import com.trendyol.sessionstore.database.StoredObjectChunk
import com.trendyol.sessionstore.impl.SessionStoreEditorImpl
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionStoreEditorTest {

    private lateinit var database: SessionDatabase
    private lateinit var objectDao: ObjectDao
    private lateinit var byteArrayAdapter: ByteArraySerializationAdapter
    private lateinit var jsonAdapter: JsonSerializationAdapter
    private val testDispatcher = StandardTestDispatcher()
    private val testSession = 1

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        database = mockk()
        objectDao = mockk(relaxed = true)
        byteArrayAdapter = mockk(relaxed = true)
        jsonAdapter = mockk(relaxed = true)
        every { database.objectDao() } returns objectDao
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createEditor(
        adapter: SerializationAdapter,
        session: Int = testSession
    ): SessionStoreEditor {
        return SessionStoreEditorImpl(
            database = database,
            serializationAdapter = adapter,
            session = session,
            dispatcher = testDispatcher
        )
    }

    // region ByteArraySerializationAdapter Tests

    @Test
    fun `given small data, when setObject with ByteArrayAdapter, then creates single chunk`() = runTest {
        // Given
        val testData = TestData("test")
        val serializedData = byteArrayOf(1, 2, 3, 4, 5)
        every { byteArrayAdapter.serialize(testData) } returns serializedData
        coEvery { objectDao.deleteChunks(any(), any()) } returns Unit
        coEvery { objectDao.upsertChunks(any()) } returns Unit

        val editor: SessionStoreEditor = createEditor(byteArrayAdapter)

        // When
        editor.setObject("key", testData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { objectDao.deleteChunks("key", testSession) }
        coVerify {
            objectDao.upsertChunks(match { chunks ->
                assertThat(chunks).hasSize(1)
                assertThat(chunks[0].key).isEqualTo("key")
                assertThat(chunks[0].session).isEqualTo(testSession)
                assertThat(chunks[0].chunkIndex).isEqualTo(0)
                assertThat(chunks[0].totalChunks).isEqualTo(1)
                assertThat(chunks[0].byteArray).isEqualTo(serializedData)
                assertThat(chunks[0].json).isNull()
                assertThat(chunks[0].type).isEqualTo(TestData::class.java.name)
                true
            })
        }
    }

    @Test
    fun `given large data, when setObject with ByteArrayAdapter, then creates multiple chunks`() = runTest {
        // Given
        val testData = TestData("large")
        val chunkSize = 1024 * 1024 // 1MB
        val largeData = ByteArray(chunkSize * 2 + 500) { it.toByte() } // 2.5 MB
        every { byteArrayAdapter.serialize(testData) } returns largeData

        val editor: SessionStoreEditor = createEditor(byteArrayAdapter)

        // When
        editor.setObject("key", testData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { objectDao.deleteChunks("key", testSession) }
        coVerify {
            objectDao.upsertChunks(match { chunks ->
                assertThat(chunks).hasSize(3)
                assertThat(chunks.map { it.totalChunks }).containsExactly(3, 3, 3)
                assertThat(chunks[0].byteArray).hasLength(chunkSize)
                assertThat(chunks[1].byteArray).hasLength(chunkSize)
                assertThat(chunks[2].byteArray).hasLength(500)
                assertThat(chunks.map { it.json }).containsExactly(null, null, null)
                true
            })
        }
    }

    @Test
    fun `given existing key, when getObject with ByteArrayAdapter, then returns deserialized object`() = runTest {
        // Given
        val expectedData = TestData("test")
        val serializedData = byteArrayOf(1, 2, 3, 4, 5)
        val chunk = StoredObjectChunk(
            key = "key",
            session = testSession,
            chunkIndex = 0,
            totalChunks = 1,
            byteArray = serializedData,
            json = null,
            type = TestData::class.java.name
        )
        coEvery { objectDao.getChunks("key", testSession) } returns listOf(chunk)
        every { byteArrayAdapter.deserialize(serializedData, TestData::class.java) } returns expectedData

        val editor: SessionStoreEditor = createEditor(byteArrayAdapter)

        // When
        val result = editor.getObject("key", TestData::class.java)

        // Then
        assertThat(result).isEqualTo(expectedData)
        coVerify { objectDao.getChunks("key", testSession) }
    }

    @Test
    fun `given non-existent key, when getObject with ByteArrayAdapter, then returns null`() = runTest {
        // Given
        coEvery { objectDao.getChunks("nonexistent", testSession) } returns emptyList()

        val editor: SessionStoreEditor = createEditor(byteArrayAdapter)

        // When
        val result = editor.getObject("nonexistent", TestData::class.java)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `given multiple chunks, when getObject with ByteArrayAdapter, then reassembles correctly`() = runTest {
        // Given
        val expectedData = TestData("large")
        val chunk1 = byteArrayOf(1, 2, 3)
        val chunk2 = byteArrayOf(4, 5, 6)
        val chunk3 = byteArrayOf(7, 8, 9)
        val fullData = chunk1 + chunk2 + chunk3

        val chunks = listOf(
            StoredObjectChunk("key", testSession, 0, 3, chunk1, null, TestData::class.java.name),
            StoredObjectChunk("key", testSession, 1, 3, chunk2, null, TestData::class.java.name),
            StoredObjectChunk("key", testSession, 2, 3, chunk3, null, TestData::class.java.name)
        )
        coEvery { objectDao.getChunks("key", testSession) } returns chunks
        every { byteArrayAdapter.deserialize(fullData, TestData::class.java) } returns expectedData

        val editor: SessionStoreEditor = createEditor(byteArrayAdapter)

        // When
        val result = editor.getObject("key", TestData::class.java)

        // Then
        assertThat(result).isEqualTo(expectedData)
    }

    // endregion

    // region JsonSerializationAdapter Tests

    @Test
    fun `given small JSON, when setObject with JsonAdapter, then creates single chunk`() = runTest {
        // Given
        val testData = TestData("test")
        val jsonData = """{"value":"test"}"""
        every { jsonAdapter.serialize(testData) } returns jsonData

        val editor: SessionStoreEditor = createEditor(jsonAdapter)

        // When
        editor.setObject("key", testData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { objectDao.deleteChunks("key", testSession) }
        coVerify {
            objectDao.upsertChunks(match { chunks ->
                assertThat(chunks).hasSize(1)
                assertThat(chunks[0].key).isEqualTo("key")
                assertThat(chunks[0].session).isEqualTo(testSession)
                assertThat(chunks[0].chunkIndex).isEqualTo(0)
                assertThat(chunks[0].totalChunks).isEqualTo(1)
                assertThat(chunks[0].json).isEqualTo(jsonData)
                assertThat(chunks[0].byteArray).isNull()
                assertThat(chunks[0].type).isEqualTo(TestData::class.java.name)
                true
            })
        }
    }

    @Test
    fun `given large JSON, when setObject with JsonAdapter, then creates multiple chunks`() = runTest {
        // Given
        val testData = TestData("large")
        val chunkSize = 1024 * 1024 // 1MB in characters
        val largeJson = "x".repeat(chunkSize * 2 + 500) // 2.5 MB worth of characters
        every { jsonAdapter.serialize(testData) } returns largeJson

        val editor: SessionStoreEditor = createEditor(jsonAdapter)

        // When
        editor.setObject("key", testData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { objectDao.deleteChunks("key", testSession) }
        coVerify {
            objectDao.upsertChunks(match { chunks ->
                assertThat(chunks).hasSize(3)
                assertThat(chunks.map { it.totalChunks }).containsExactly(3, 3, 3)
                assertThat(chunks[0].json).hasLength(chunkSize)
                assertThat(chunks[1].json).hasLength(chunkSize)
                assertThat(chunks[2].json).hasLength(500)
                assertThat(chunks.map { it.byteArray }).containsExactly(null, null, null)
                true
            })
        }
    }

    @Test
    fun `given existing key, when getObject with JsonAdapter, then returns deserialized object`() = runTest {
        // Given
        val expectedData = TestData("test")
        val jsonData = """{"value":"test"}"""
        val chunk = StoredObjectChunk(
            key = "key",
            session = testSession,
            chunkIndex = 0,
            totalChunks = 1,
            byteArray = null,
            json = jsonData,
            type = TestData::class.java.name
        )
        coEvery { objectDao.getChunks("key", testSession) } returns listOf(chunk)
        every { jsonAdapter.deserialize(jsonData, TestData::class.java) } returns expectedData

        val editor: SessionStoreEditor = createEditor(jsonAdapter)

        // When
        val result = editor.getObject("key", TestData::class.java)

        // Then
        assertThat(result).isEqualTo(expectedData)
    }

    @Test
    fun `given non-existent key, when getObject with JsonAdapter, then returns null`() = runTest {
        // Given
        coEvery { objectDao.getChunks("nonexistent", testSession) } returns emptyList()

        val editor: SessionStoreEditor = createEditor(jsonAdapter)

        // When
        val result = editor.getObject("nonexistent", TestData::class.java)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `given multiple JSON chunks, when getObject with JsonAdapter, then reassembles correctly`() = runTest {
        // Given
        val expectedData = TestData("large")
        val jsonPart1 = """{"val"""
        val jsonPart2 = """ue":"""
        val jsonPart3 = """"large"}"""
        val fullJson = jsonPart1 + jsonPart2 + jsonPart3

        val chunks = listOf(
            StoredObjectChunk("key", testSession, 0, 3, null, jsonPart1, TestData::class.java.name),
            StoredObjectChunk("key", testSession, 1, 3, null, jsonPart2, TestData::class.java.name),
            StoredObjectChunk("key", testSession, 2, 3, null, jsonPart3, TestData::class.java.name)
        )
        coEvery { objectDao.getChunks("key", testSession) } returns chunks
        every { jsonAdapter.deserialize(fullJson, TestData::class.java) } returns expectedData

        val editor: SessionStoreEditor = createEditor(jsonAdapter)

        // When
        val result = editor.getObject("key", TestData::class.java)

        // Then
        assertThat(result).isEqualTo(expectedData)
    }

    // endregion

    // region Edge Cases

    @Test
    fun `given empty data, when setObject with ByteArrayAdapter, then creates no chunks`() = runTest {
        // Given
        val testData = TestData("empty")
        val emptyData = byteArrayOf()
        every { byteArrayAdapter.serialize(testData) } returns emptyData

        val editor: SessionStoreEditor = createEditor(byteArrayAdapter)

        // When
        editor.setObject("key", testData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - empty data creates 0 chunks
        coVerify {
            objectDao.upsertChunks(match { chunks ->
                assertThat(chunks).isEmpty()
                true
            })
        }
    }

    @Test
    fun `given data exactly chunk size, when setObject with ByteArrayAdapter, then creates one chunk`() = runTest {
        // Given
        val testData = TestData("exact")
        val chunkSize = 1024 * 1024
        val exactData = ByteArray(chunkSize) { it.toByte() }
        every { byteArrayAdapter.serialize(testData) } returns exactData

        val editor: SessionStoreEditor = createEditor(byteArrayAdapter)

        // When
        editor.setObject("key", testData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            objectDao.upsertChunks(match { chunks ->
                assertThat(chunks).hasSize(1)
                assertThat(chunks[0].byteArray).hasLength(chunkSize)
                true
            })
        }
    }

    @Test
    fun `given data just over chunk size, when setObject with ByteArrayAdapter, then creates two chunks`() = runTest {
        // Given
        val testData = TestData("oversize")
        val chunkSize = 1024 * 1024
        val oversizeData = ByteArray(chunkSize + 1) { it.toByte() }
        every { byteArrayAdapter.serialize(testData) } returns oversizeData

        val editor: SessionStoreEditor = createEditor(byteArrayAdapter)

        // When
        editor.setObject("key", testData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            objectDao.upsertChunks(match { chunks ->
                assertThat(chunks).hasSize(2)
                assertThat(chunks[0].byteArray).hasLength(chunkSize)
                assertThat(chunks[1].byteArray).hasLength(1)
                true
            })
        }
    }

    @Test
    fun `given unknown adapter, when setObject, then throws error`() = runTest {
        // Given
        val adapter = object : SerializationAdapter {}
        val testData = TestData("test")
        val editor: SessionStoreEditor = createEditor(adapter)

        // When/Then
        try {
            editor.setObject("key", testData)
            testDispatcher.scheduler.advanceUntilIdle()
            throw AssertionError("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertThat(e).isInstanceOf(IllegalStateException::class.java)
        }
    }
    // endregion

    // region Database Interaction Tests

    @Test
    fun `given any data, when setObject, then deletes existing chunks before upserting`() = runTest {
        // Given
        val testData = TestData("test")
        every { byteArrayAdapter.serialize(testData) } returns byteArrayOf(1, 2, 3)

        val editor: SessionStoreEditor = createEditor(byteArrayAdapter)

        // When
        editor.setObject("key", testData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - verify delete is called before upsert
        coVerify(ordering = Ordering.ORDERED) {
            objectDao.deleteChunks("key", testSession)
            objectDao.upsertChunks(any())
        }
    }

    @Test
    fun `given custom session, when setObject, then uses correct session ID`() = runTest {
        // Given
        val customSession = 42
        val testData = TestData("test")
        val serializedData = byteArrayOf(1, 2, 3)
        every { byteArrayAdapter.serialize(testData) } returns serializedData

        val editor: SessionStoreEditor = createEditor(byteArrayAdapter, customSession)

        // When
        editor.setObject("key", testData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { objectDao.deleteChunks("key", customSession) }
        coVerify {
            objectDao.upsertChunks(match { chunks ->
                assertThat(chunks.all { it.session == customSession }).isTrue()
                true
            })
        }
    }

    // endregion

    // Test data class
    private data class TestData(val value: String)
}

