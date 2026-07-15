package com.simpleiptv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.simpleiptv.player.core.repository.AppSettingsStore
import com.simpleiptv.player.core.repository.ThemeMode
import com.simpleiptv.player.navigation.SimpleIPTVApp
import com.simpleiptv.player.ui.theme.SimpleIPTVTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsStore = remember { AppSettingsStore(this@MainActivity) }

            var themeMode by remember {
                mutableStateOf(settingsStore.getThemeMode())
            }

            SimpleIPTVTheme(
                themeMode = themeMode
            ) {
                SimpleIPTVApp(
                    onThemeChanged = { newMode ->
                        settingsStore.setThemeMode(newMode)
                        themeMode = newMode
                    }
                )
            }
        }
    }
}