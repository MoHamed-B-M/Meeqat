package com.vibeprayer.app.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibeprayer.app.R
import com.vibeprayer.app.data.model.Prayer
import com.vibeprayer.app.ui.components.CustomCard
import com.vibeprayer.app.ui.components.DotMatrixText
import com.vibeprayer.app.ui.components.DynamicBackground
import com.vibeprayer.app.ui.theme.VibeTheme
import com.vibeprayer.app.ui.theme.VibeType
import androidx.compose.foundation.text.BasicText
import com.vibeprayer.app.ui.theme.LocalVibeColors

class AdhanAlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        enableEdgeToEdge()
        val prayerName = intent.getStringExtra("prayer") ?: "Fajr"
        val prayer = runCatching { Prayer.valueOf(prayerName) }.getOrNull()
        val bg = when (prayer) {
            Prayer.Fajr -> R.drawable.fajer
            Prayer.Dhuhr -> R.drawable.doher
            Prayer.Asr -> R.drawable.aser
            Prayer.Maghrib -> R.drawable.magreb
            else -> R.drawable.default_bg
        }
        setContent {
            VibeTheme {
                Box(Modifier.fillMaxSize()) {
                    DynamicBackground(bgRes = bg)
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DotMatrixText(text = "$prayerName TIME")
                        BasicText(
                            text = prayerName.uppercase(),
                            style = VibeType.clock.copy(color = LocalVibeColors.current.textPrimary)
                        )
                        CustomCard(
                            onClick = { finish() },
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                        ) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                DotMatrixText(text = "DISMISS")
                            }
                        }
                    }
                }
            }
        }
    }
}
