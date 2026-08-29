package com.zaaam.nettra.tabs.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zaaam.nettra.tabs.model.CustomFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomFilterDao {
    @Query("SELECT * FROM custom_filters ORDER BY createdAt DESC")
    fun observe(): Flow<List<CustomFilterEntity>>
    @Query("SELECT * FROM custom_filters")
    suspend fun getAll(): List<CustomFilterEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: CustomFilterEntity)
    @Query("DELETE FROM custom_filters WHERE domain = :domain")
    suspend fun delete(domain: String)
    @Query("DELETE FROM custom_filters")
    suspend fun clearAll()
}
