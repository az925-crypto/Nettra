package com.zaaam.nettra.tabs.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_filters")
data class CustomFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val createdAt: Long = System.currentTimeMillis()
)
