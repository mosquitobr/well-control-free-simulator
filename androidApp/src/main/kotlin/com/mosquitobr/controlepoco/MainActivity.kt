package com.mosquitobr.controlepoco

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mosquitobr.controlepoco.ui.AppRoot
import com.mosquitobr.controlepoco.ui.ControlePocoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ControlePocoTheme {
                AppRoot()
            }
        }
    }
}
