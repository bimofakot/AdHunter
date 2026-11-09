/*
 * Entry point aplikasi. UI sepenuhnya memakai Jetpack Compose Material 3.
 */
package com.massbim.adhunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.massbim.adhunter.ui.AppRoot
import com.massbim.adhunter.ui.theme.AdHunterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdHunterTheme {
                AppRoot()
            }
        }
    }
}
