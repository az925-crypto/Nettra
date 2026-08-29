package com.zaaam.nettra.tabs

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAll(): List<BookmarkEntity>
    @Insert suspend fun insert(b: BookmarkEntity)
    @Query("DELETE FROM bookmarks WHERE url = :url") suspend fun deleteByUrl(url: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 100")
    suspend fun getRecent(): List<HistoryEntity>
    @Insert suspend fun insert(h: HistoryEntity)
    @Query("DELETE FROM history") suspend fun clearAll()
}

@Database(entities = [BookmarkEntity::class, HistoryEntity::class, TabEntity::class], version = 1, exportSchema = true)
abstract class NettraDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
}
