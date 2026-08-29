package com.zaaam.nettra.tabs.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zaaam.nettra.tabs.model.BookmarkEntity
import com.zaaam.nettra.tabs.model.CustomFilterEntity
import com.zaaam.nettra.tabs.model.HistoryEntity
import com.zaaam.nettra.tabs.model.TabEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_filters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `domain` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
    }
}

@Database(
    entities = [TabEntity::class, BookmarkEntity::class, HistoryEntity::class, CustomFilterEntity::class],
    version = 2,
    exportSchema = true
)
abstract class NetTraDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun customFilterDao(): CustomFilterDao

    companion object {
        @Volatile private var INSTANCE: NetTraDatabase? = null
        fun get(context: Context): NetTraDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetTraDatabase::class.java, "nettra.db"
                ).addMigrations(MIGRATION_1_2)
                 .fallbackToDestructiveMigrationOnDowngrade()
                 .build().also { INSTANCE = it }
            }
    }
}
