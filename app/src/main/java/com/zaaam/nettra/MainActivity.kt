package com.zaaam.nettra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Nettra — Privacy Browser entry point.
 * FR-1..FR-7 implemented via modular cores; UI delegated to feature-browser-ui after mockup approval.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NettraApp()
                }
            }
        }
    }
}

@Composable
fun NettraApp() {
    Text("Nettra — com.zaaam.nettra\nMockup approved, Compose UI next")
}
