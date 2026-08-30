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

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY createdAt DESC")
    suspend fun getAll(): List<TabEntity>
    @Insert suspend fun insert(t: TabEntity)
    @Query("DELETE FROM tabs WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM tabs") suspend fun clearAll()
    @Query("UPDATE tabs SET url=:url, title=:title, type=:type, query=:query, blocked=:blocked, grade=:grade, secure=:secure WHERE id=:id")
    suspend fun updateTab(id: Long, url: String, title: String, type: String, query: String, blocked: Int, grade: String, secure: Boolean)
}

@Database(entities = [BookmarkEntity::class, HistoryEntity::class, TabEntity::class], version = 2, exportSchema = true, autoMigrations = [androidx.room.AutoMigration(from = 1, to = 2)])
abstract class NettraDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun tabDao(): TabDao
}
