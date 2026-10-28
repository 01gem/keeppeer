package com.gem.keeppeer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gem.keeppeer.ui.navigation.KeeppeerApp
import com.gem.keeppeer.ui.theme.KeeppeerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeeppeerTheme {
                KeeppeerApp()
            }
        }
    }
}
