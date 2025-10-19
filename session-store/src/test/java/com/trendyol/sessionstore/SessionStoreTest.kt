package com.trendyol.sessionstore

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.trendyol.sessionstore.adapter.JsonSerializationAdapter
import com.trendyol.sessionstore.impl.SessionStoreImpl
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SessionStoreTest {

    private lateinit var sessionStore: SessionStore
    private lateinit var mockApplication: Application
    private lateinit var serializationAdapter: JsonSerializationAdapter

    @Before
    fun setup() {
        sessionStore = SessionStoreImpl()
        mockApplication = mockk(relaxed = true)
        serializationAdapter = mockk(relaxed = true)
    }

    @Test
    fun `given application, when install, then registers ActivityLifecycleCallbacks`() {
        // When
        sessionStore.install(mockApplication, serializationAdapter)

        // Then
        verify { mockApplication.registerActivityLifecycleCallbacks(any()) }
    }

    @Test
    fun `given multiple install calls, when install, then only registers callbacks once`() {
        // When
        sessionStore.install(mockApplication, serializationAdapter)
        sessionStore.install(mockApplication, serializationAdapter)
        sessionStore.install(mockApplication, serializationAdapter)

        // Then
        verify(exactly = 1) { mockApplication.registerActivityLifecycleCallbacks(any()) }
    }

    @Test
    fun `given uninitialized SessionStore, when getEditor, then throws IllegalStateException`() = runTest {
        // Given
        val uninitializedStore: SessionStore = SessionStoreImpl()

        // When&Then
        try {
            uninitializedStore.getEditor()
            throw AssertionError("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertThat(e.message).isEqualTo(
                "SessionStore.install() must be called before getEditor(). " +
                        "Make sure to call install() in your Application class."
            )
        }
    }

    class TestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
        }
    }
}
