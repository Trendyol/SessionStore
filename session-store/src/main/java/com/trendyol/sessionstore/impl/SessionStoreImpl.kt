package com.trendyol.sessionstore.impl

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.trendyol.sessionstore.SessionStore
import com.trendyol.sessionstore.SessionStoreEditor
import com.trendyol.sessionstore.adapter.SerializationAdapter
import com.trendyol.sessionstore.database.SessionDatabase
import com.trendyol.sessionstore.lifecycle.SessionLifecycleCallbacks
import kotlinx.coroutines.CompletableDeferred

internal class SessionStoreImpl : SessionStore {

    private val ready = CompletableDeferred<SessionStoreEditor>()
    private val applicationScope = ProcessLifecycleOwner.get().lifecycleScope

    private var installCalled = false

    override fun install(application: Application, serializationAdapter: SerializationAdapter) {
        if (!installCalled) {
            installCalled = true

            val sessionDatabase = SessionDatabase.getInstance(application.applicationContext)
            val callbacks = SessionLifecycleCallbacks(
                sessionDatabase = sessionDatabase,
                serializationAdapter = serializationAdapter,
                coroutineScope = applicationScope,
                onReady = { editor ->
                    if (!ready.isCompleted) {
                        ready.complete(editor)
                    }
                }
            )

            application.registerActivityLifecycleCallbacks(callbacks)
        }
    }

    override suspend fun getEditor(): SessionStoreEditor {
        check(installCalled) {
            "SessionStore.install() must be called before getEditor(). " +
                    "Make sure to call install() in your Application class."
        }
        return ready.await()
    }
}
