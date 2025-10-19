package com.trendyol.sessionstore.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.trendyol.sessionstore.SessionStoreEditor
import com.trendyol.sessionstore.adapter.SerializationAdapter
import com.trendyol.sessionstore.database.SessionCounter
import com.trendyol.sessionstore.database.SessionDatabase
import com.trendyol.sessionstore.impl.SessionStoreEditorImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class SessionLifecycleCallbacks(
    private val sessionDatabase: SessionDatabase,
    private val serializationAdapter: SerializationAdapter,
    private val coroutineScope: CoroutineScope,
    private val onReady: (SessionStoreEditor) -> Unit,
) : Application.ActivityLifecycleCallbacks {

    private var isFirstActivity = true

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (isFirstActivity) {
            isFirstActivity = false

            val reason = when {
                savedInstanceState == null -> Reason.FIRST_LAUNCH
                activity.isChangingConfigurations -> Reason.ACTIVITY_RECREATION
                else -> Reason.PROCESS_DEATH
            }

            initializeSession(reason)
        }
    }

    private fun initializeSession(reason: Reason) {
        coroutineScope.launch {
            val sessionNum = when (reason) {
                Reason.FIRST_LAUNCH -> {
                    val last = sessionDatabase.counterDao().getSession() ?: 0
                    val next = last + 1

                    sessionDatabase.counterDao().upsert(SessionCounter(session = next))
                    sessionDatabase.objectDao().deleteOlderThan(next)
                    next
                }

                Reason.ACTIVITY_RECREATION,
                Reason.PROCESS_DEATH -> {
                    sessionDatabase.counterDao().getSession() ?: 0
                }
            }

            val editor = SessionStoreEditorImpl(
                database = sessionDatabase,
                serializationAdapter = serializationAdapter,
                session = sessionNum,
            )
            onReady(editor)
        }
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
