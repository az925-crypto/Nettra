package com.zaaam.nettra.inspector.model

data class ConsoleEntry(
    val level: String, // LOG, WARN, ERROR, INFO
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceUrl: String? = null,
    val tabId: String
)
