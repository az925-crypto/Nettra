package com.zaaam.nettra.privacy

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("nettra_prefs")

class CustomBlocklistManager(private val context: Context) {
    private val KEY = stringSetPreferencesKey("custom_blocklist")
    val flow: Flow<Set<String>> = context.dataStore.data.map { it[KEY] ?: emptySet() }
    suspend fun save(list: Set<String>) { context.dataStore.edit { it[KEY] = list.map { s-> s.trim().lowercase() }.filter { it.isNotBlank() }.toSet() } }
    suspend fun add(domain: String) { val cur = flowValue(); save(cur + domain.trim().lowercase()) }
    private suspend fun flowValue(): Set<String> = flow.first()
    fun isValid(domain: String): Boolean = Regex("^[a-z0-9.-]+$").matches(domain.lowercase()) && domain.contains(".")
}
