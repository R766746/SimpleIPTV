package com.simpleiptv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.simpleiptv.player.navigation.SimpleIPTVApp
import com.simpleiptv.player.ui.theme.SimpleIPTVTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SimpleIPTVTheme {
                SimpleIPTVApp()
            }
        }
    }
}