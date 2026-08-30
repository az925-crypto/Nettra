package com.zaaam.nettra.tabs

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var INSTANCE: NettraDatabase? = null

    fun getDatabase(context: Context): NettraDatabase =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                NettraDatabase::class.java,
                "nettra.db"
            )
                // v1 fresh — no migration needed; future v2 must add Migration(1,2) without fallback
                .build()
                .also { INSTANCE = it }
        }
}
