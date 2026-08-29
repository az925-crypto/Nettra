package com.zaaam.nettra.tabs.db

import androidx.room.*
import com.zaaam.nettra.tabs.model.BookmarkEntity
import com.zaaam.nettra.tabs.model.HistoryEntity
import com.zaaam.nettra.tabs.model.TabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY lastActiveAt DESC")
    fun observeTabs(): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs ORDER BY lastActiveAt DESC")
    suspend fun getAllTabs(): List<TabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE tabs SET lastActiveAt = :ts, url = :url, title = :title WHERE id = :id")
    suspend fun updateActive(id: String, url: String, title: String, ts: Long)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(b: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT :limit")
    fun observeHistory(limit: Int = 200): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 200): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(h: HistoryEntity): Long

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): HistoryEntity?
}
