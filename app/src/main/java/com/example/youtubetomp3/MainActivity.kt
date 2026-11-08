package com.example.youtubetomp3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.youtubetomp3.ui.HomeScreen
import com.example.youtubetomp3.ui.theme.YouTubeToMp3Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YouTubeToMp3Theme {
                HomeScreen()
            }
        }
    }
}
