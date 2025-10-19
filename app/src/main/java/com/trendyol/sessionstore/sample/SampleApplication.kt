package com.trendyol.sessionstore.sample

import android.app.Application
import com.trendyol.sessionstore.SessionStore
import com.trendyol.sessionstore.sample.common.ParcelableSerializationAdapter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        SessionStore.install(
            application = this,
            serializationAdapter = ParcelableSerializationAdapter()
        )
    }
}
