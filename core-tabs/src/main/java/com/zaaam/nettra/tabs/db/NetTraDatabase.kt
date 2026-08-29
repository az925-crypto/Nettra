package com.zaaam.nettra.tabs.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zaaam.nettra.tabs.model.BookmarkEntity
import com.zaaam.nettra.tabs.model.HistoryEntity
import com.zaaam.nettra.tabs.model.TabEntity

@Database(
    entities = [TabEntity::class, BookmarkEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class NetTraDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: NetTraDatabase? = null
        fun get(context: Context): NetTraDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetTraDatabase::class.java, "nettra.db"
                ).fallbackToDestructiveMigrationOnDowngrade()
                 .build().also { INSTANCE = it }
            }
    }
}
