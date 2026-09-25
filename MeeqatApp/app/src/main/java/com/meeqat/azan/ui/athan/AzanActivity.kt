package com.meeqat.azan.ui.athan

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.meeqat.azan.service.AzanForegroundService
import com.meeqat.azan.ui.theme.MeeqatTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AzanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        // Show over lockscreen for exact alarms (matches USE_FULL_SCREEN_INTENT).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val prayerName = intent.getStringExtra(AzanForegroundService.EXTRA_PRAYER) ?: "Fajr"
        val timeMillis = intent.getLongExtra(AzanForegroundService.EXTRA_TIME, System.currentTimeMillis())
        val backgroundAsset = AthanBackgrounds.assetPathFor(prayerName)

        setContent {
            MeeqatTheme(dynamicColor = false) {
                AzanFullScreen(
                    prayerName = prayerName,
                    timeMillis = timeMillis,
                    backgroundAsset = backgroundAsset,
                    onDismiss = {
                        startService(Intent(this, AzanForegroundService::class.java).apply {
                            action = AzanForegroundService.ACTION_DISMISS
                        })
                        finish()
                    },
                    onSnooze = {
                        startService(Intent(this, AzanForegroundService::class.java).apply {
                            action = AzanForegroundService.ACTION_SNOOZE
                        })
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun AzanFullScreen(
    prayerName: String,
    timeMillis: Long,
    backgroundAsset: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap = remember(backgroundAsset) {
        try {
            context.assets.open(backgroundAsset).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        } catch (_: Exception) { null }
    }
    val timeText = remember(timeMillis) {
        try {
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(timeMillis))
        } catch (_: Exception) { "--:--" }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0F2B4A))) {
        // Background image (prayer-specific) with fallback gradient.
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Readability scrim.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.15f),
                        Color.Black.copy(alpha = 0.55f)
                    )
                )
            )
        )

        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 48.dp).widthIn(max = 560.dp).align(Alignment.Center),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "AZAN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp, fontWeight = FontWeight.W600, color = Color.White.copy(alpha = 0.85f)
                    ),
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    prayerName,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.W700, fontSize = 40.sp, color = Color.White
                    ),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    timeText,
                    style = MaterialTheme.typography.headlineSmall.copy(color = Color.White.copy(alpha = 0.9f)),
                    maxLines = 1
                )
            }

            // Fluid rounded action card, like Salat minimalist white card.
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Text(
                        "It's time for $prayerName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W600, color = Color(0xFF1A2742)),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onSnooze,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Snooze 5m") }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2742))
                        ) { Text("Dismiss") }
                    }
                }
            }
        }
    }
}
