package com.trendyol.sessionstore

import com.trendyol.sessionstore.impl.SessionStoreEditorImpl

/**
 * Editor for reading and writing session-scoped data.
 *
 * This interface provides a simple key-value API for storing and retrieving objects
 * that are scoped to the current user session. All data stored via this editor is
 * automatically associated with the current session number and will be cleaned up
 * when a new session begins.
 *
 * #### Session Isolation
 * Objects are isolated by session. If the session counter increments (e.g., on a fresh app launch),
 * all data from previous sessions is automatically deleted, and new data is stored in the new session.
 *
 * @see SessionStore.getEditor to obtain an editor instance
 * @see com.trendyol.sessionstore.adapter.SerializationAdapter for implementing serialization logic
 */
public interface SessionStoreEditor {
    /**
     * Stores an object in the current session under the specified key.
     *
     * The object is serialized using the [com.trendyol.sessionstore.adapter.SerializationAdapter] provided during
     * [SessionStore.install] and stored in the Room database. If an object already
     * exists with the same key in the current session, it will be replaced.
     *
     * @param key The unique key to associate with this object. Used to retrieve the object later.
     * @param value The object to store. Must be serializable by the configured [com.trendyol.sessionstore.adapter.SerializationAdapter].
     *
     * @throws IllegalArgumentException if the object cannot be serialized by the adapter
     */
    public suspend fun setObject(key: String, value: Any)

    /**
     * Retrieves an object from the current session by its key.
     *
     * The object is deserialized using the [com.trendyol.sessionstore.adapter.SerializationAdapter] provided during
     * [SessionStore.install]. Only objects stored in the current session are accessible;
     * objects from previous sessions are automatically cleaned up and will not be found.
     *
     * @param key The unique key associated with the object when it was stored via [setObject].
     * @param type The class type of the object to deserialize.
     *
     * @return The deserialized object if found, or `null` if:
     *         - No object exists with the given key in the current session
     *         - The object was stored in a previous session and has been cleaned up
     *         - Deserialization failed (adapter returned null)
     *
     */
    public suspend fun <T : Any> getObject(key: String, type: Class<T>): T?
}
