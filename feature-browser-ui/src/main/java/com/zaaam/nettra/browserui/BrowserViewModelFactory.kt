package com.zaaam.nettra.browserui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zaaam.nettra.tabs.HistoryDao
import com.zaaam.nettra.tabs.TabDao

class BrowserViewModelFactory(
    private val historyDao: HistoryDao?,
    private val tabDao: TabDao?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            return BrowserViewModel(historyDao, tabDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
