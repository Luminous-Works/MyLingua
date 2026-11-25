package com.luminousworksllc.mylingual

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.luminousworksllc.mylingual.ui.theme.MyLingualTheme
import com.luminousworksllc.mylingual.ui.translate.TranslateScreen

/**
 * Single-screen app. App Check is installed in [MyLinguaApp], before any Activity runs.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyLingualTheme {
                TranslateScreen()
            }
        }
    }
}
