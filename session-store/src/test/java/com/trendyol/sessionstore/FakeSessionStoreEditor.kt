package com.trendyol.sessionstore

/**
 * Fake implementation for testing: stores objects in-memory.
 */
class FakeSessionStoreEditor(
    private val initialSession: Int = 0
) : SessionStoreEditor {
    private val store = mutableMapOf<String, Any>()

    override suspend fun setObject(key: String, value: Any) {
        store[key] = value
    }

    override suspend fun <T : Any> getObject(key: String, type: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return store[key] as? T
    }
}
