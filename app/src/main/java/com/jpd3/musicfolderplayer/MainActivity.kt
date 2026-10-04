package com.jpd3.musicfolderplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.jpd3.musicfolderplayer.ui.MusicFolderPlayerApp
import com.jpd3.musicfolderplayer.ui.theme.MusicFolderPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(12, 14, 15)),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(12, 14, 15))
        )
        setContent {
            MusicFolderPlayerTheme {
                MusicFolderPlayerApp()
            }
        }
    }
}
