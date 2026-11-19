package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.ThemePreferences
import com.example.ui.screens.DeepakSocialApp
import com.example.ui.theme.DeepakSocialTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreferences = remember { ThemePreferences(applicationContext) }
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()

            DeepakSocialTheme(themeMode = themeMode) {
                DeepakSocialApp(
                    themeMode = themeMode,
                    onChangeThemeMode = { newMode ->
                        scope.launch {
                            themePreferences.setThemeMode(newMode)
                        }
                    }
                )
            }
        }
    }
}
