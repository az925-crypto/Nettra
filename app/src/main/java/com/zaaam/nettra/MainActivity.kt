package com.zaaam.nettra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.nettra.browserui.BrowserScreen
import com.zaaam.nettra.browserui.BrowserViewModel
import com.zaaam.nettra.browserui.BrowserViewModelFactory
import com.zaaam.nettra.tabs.DatabaseProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = DatabaseProvider.getDatabase(this)
        val factory = BrowserViewModelFactory(db.historyDao(), db.tabDao())
        setContent {
            val vm: BrowserViewModel = viewModel(factory = factory)
            BrowserScreen(vm = vm)
        }
    }
}
