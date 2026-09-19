package com.example.homework

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.homework.core.locale.LocaleHelper
import com.example.homework.ui.app.KazantourApp
import com.example.homework.ui.uikit.theme.HomeworkTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeworkTheme {
                KazantourApp()
            }
        }
    }
}
