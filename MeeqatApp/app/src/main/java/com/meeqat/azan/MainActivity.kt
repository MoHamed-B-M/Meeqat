package com.meeqat.azan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.meeqat.azan.di.ServiceLocator
import com.meeqat.azan.ui.navigation.MeeqatNavGraph
import com.meeqat.azan.ui.theme.MeeqatTheme

class MainActivity : ComponentActivity() {

    private val settingsRepository by lazy { ServiceLocator.settingsRepository }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContent {
            val dynamicColor by settingsRepository.dynamicColorFlow.collectAsState(initial = true)
            MeeqatTheme(dynamicColor = dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MeeqatNavGraph()
                }
            }
        }
    }
}
