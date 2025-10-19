package com.trendyol.sessionstore.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SessionCounter::class, StoredObjectChunk::class],
    version = 1,
    exportSchema = false
)
internal abstract class SessionDatabase : RoomDatabase() {
    abstract fun counterDao(): CounterDao
    abstract fun objectDao(): ObjectDao

    companion object {
        @Volatile
        private var INSTANCE: SessionDatabase? = null

        fun getInstance(context: Context): SessionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SessionDatabase::class.java,
                    "session_db"
                )
                    .fallbackToDestructiveMigration(false)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
