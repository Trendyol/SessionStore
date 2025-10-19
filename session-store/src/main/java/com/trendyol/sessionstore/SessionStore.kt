package com.trendyol.sessionstore

import android.app.Application
import com.trendyol.sessionstore.adapter.SerializationAdapter
import com.trendyol.sessionstore.impl.SessionStoreImpl

/**
 * Session-scoped storage manager for Android applications.
 *
 * Store arbitrary objects that are scoped to user sessions.
 * A session is defined by a counter that increments on first app launch (after installation or process death
 * with no saved state). When a new session begins, all data from previous sessions is automatically cleaned up.
 *
 * @see SessionStoreEditor for reading and writing session data
 * @see com.trendyol.sessionstore.adapter.SerializationAdapter base interface for serialization
 * @see com.trendyol.sessionstore.adapter.ByteArraySerializationAdapter for binary serialization
 * @see com.trendyol.sessionstore.adapter.JsonSerializationAdapter for JSON serialization
 */
public interface SessionStore {
    /**
     * Installs the session store into the application.
     *
     * This method should be called once in the [Application.onCreate] method. It registers
     * [Application.ActivityLifecycleCallbacks] to detect the launch reason and initializes the session accordingly.
     *
     * Subsequent calls to `install()` on the same instance will wait for the initial
     * installation to complete but will not reinitialize.
     *
     * @param application The [Application] instance to register lifecycle callbacks with.
     * @param serializationAdapter The [com.trendyol.sessionstore.adapter.SerializationAdapter] implementation to use for serializing/deserializing objects.
     *        Can be either a [ByteArraySerializationAdapter] or [JsonSerializationAdapter].
     *
     * Example usage:
     * ```
     * class MyApplication : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *         sessionStore.install(this@MyApplication, serializationAdapter)
     *     }
     * }
     * ```
     */
    public fun install(application: Application, serializationAdapter: SerializationAdapter)

    /**
     * Returns an editor for reading and writing session-scoped data.
     *
     * This method suspends until the session store has been fully initialized via [install].
     *
     * The returned editor is bound to the current session and will read/write data
     * associated with that session number. All operations on the editor are thread-safe
     * and use coroutines for non-blocking execution.
     *
     * @return A [SessionStoreEditor] for the current session
     *
     * @throws IllegalStateException if [install] has not been called
     */
    public suspend fun getEditor(): SessionStoreEditor

    /**
     * Default singleton instance of [SessionStore].
     *
     * This companion object provides a ready-to-use instance that can be accessed as
     * `SessionStore.Default` or simply `SessionStore` throughout your application.
     *
     * Example usage:
     * ```
     * class MyApplication : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *         SessionStore.install(this, JsonSerializationAdapter())
     *     }
     * }
     *
     * // Later in your code:
     * val editor = SessionStore.getEditor()
     * ```
     */
    public companion object Default : SessionStore by SessionStoreImpl()
}
