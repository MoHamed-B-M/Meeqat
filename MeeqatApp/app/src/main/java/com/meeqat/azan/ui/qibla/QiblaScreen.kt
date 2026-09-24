package com.meeqat.azan.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meeqat.azan.data.repo.LocationRepository
import com.meeqat.azan.domain.model.QiblaInfo
import com.meeqat.azan.ui.theme.MeeqatRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val KAABA_LAT = 21.4225
private const val KAABA_LNG = 39.8262
private const val LOW_PASS_ALPHA = 0.15f

class LowPassFilter(private val alpha: Float = LOW_PASS_ALPHA) {
    private var last: FloatArray? = null
    fun filter(input: FloatArray): FloatArray {
        val out = last
        if (out == null) { last = input.copyOf(); return input }
        for (i in input.indices) out[i] = out[i] + alpha * (input[i] - out[i])
        return out.copyOf()
    }
}

class QiblaViewModel(
    private val locationRepository: LocationRepository = com.meeqat.azan.di.ServiceLocator.locationRepository
) : ViewModel() {
    private val _qibla = MutableStateFlow<QiblaInfo?>(null)
    val qibla: StateFlow<QiblaInfo?> = _qibla
    init {
        viewModelScope.launch {
            locationRepository.observeLocation().collect { loc ->
                if (loc != null) {
                    val b = bearing(loc.latitude, loc.longitude, KAABA_LAT, KAABA_LNG)
                    val d = distanceKm(loc.latitude, loc.longitude, KAABA_LAT, KAABA_LNG)
                    _qibla.value = QiblaInfo(b, d, loc.latitude, loc.longitude)
                }
            }
        }
    }
    private fun bearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val phi1 = Math.toRadians(lat1); val phi2 = Math.toRadians(lat2)
        val delta = Math.toRadians(lng2 - lng1)
        val y = sin(delta)*cos(phi2); val x = cos(phi1)*sin(phi2)-sin(phi1)*cos(phi2)*cos(delta)
        var b = Math.toDegrees(atan2(y,x)); b = (b+360)%360; return b.toFloat()
    }
    private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r=6371.0; val dLat=Math.toRadians(lat2-lat1); val dLng=Math.toRadians(lng2-lng1)
        val a= sin(dLat/2)*sin(dLat/2)+cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(dLng/2)*sin(dLng/2)
        val c=2*atan2(sqrt(a), sqrt(1-a)); return r*c
    }
}

@Composable
fun QiblaScreen(viewModel: QiblaViewModel = viewModel()) {
    val qibla by viewModel.qibla.collectAsState()
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }
    var isFlat by remember { mutableStateOf(true) }
    var accuracyLow by remember { mutableStateOf(false) }
    var sensorAvailable by remember { mutableStateOf(true) }
    val filter = remember { LowPassFilter() }
    val rotationMatrix = remember { FloatArray(9) }
    val orientation = remember { FloatArray(3) }

    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorAvailable = sensor != null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (e.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val f = filter.filter(e.values)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, f)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    azimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360) % 360)
                    val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    isFlat = kotlin.math.abs(pitch) < 25 && kotlin.math.abs(roll) < 25
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) { accuracyLow = a == SensorManager.SENSOR_STATUS_UNRELIABLE || a == SensorManager.SENSOR_STATUS_ACCURACY_LOW }
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    val bearing = qibla?.bearingDegrees ?: 0f
    val needleRaw = (bearing - azimuth + 360) % 360
    val delta = remember(needleRaw) {
        val d = ((needleRaw + 180) % 360) - 180
        d
    }
    val needleAnimated by animateFloatAsState(needleRaw, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), label = "needle")
    val dialAnimated by animateFloatAsState(bearing, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), label = "dial")

    // instruction like reference: "Turn to your left" when off by >10 degrees
    val absDelta = kotlin.math.abs(delta)
    val instruction = when {
        !sensorAvailable -> "Compass unavailable"
        qibla == null -> "Waiting for location…"
        absDelta < 8 -> "You are facing Qibla"
        delta > 0 -> "Turn to your right"
        else -> "Turn to your left"
    }
    val leftPart = if (instruction.contains("left")) "Turn to your " else if (instruction.contains("right")) "Turn to your " else ""
    val rightPart = if (instruction.contains("left")) "left" else if (instruction.contains("right")) "right" else instruction

    BoxWithConstraints(Modifier.fillMaxSize().background(MeeqatRef.Navy)) {
        val maxW = 840.dp
        Column(Modifier.fillMaxSize().widthIn(max = maxW).align(Alignment.TopCenter).padding(horizontal = if (maxWidth >= 840.dp) 24.dp else 0.dp)) {
            // LOCATION pill
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("LOCATION", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8EA0B8), letterSpacing = 1.sp), maxLines = 1)
                    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFF0B1E36), modifier = Modifier.padding(top = 6.dp)) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                qibla?.let { String.format(java.util.Locale.getDefault(), "%.4f, %.4f", it.lat, it.lng) } ?: "Ruislip",
                                style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.W700), maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text("▼", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 10.sp))
                        }
                    }
                }
                Surface(shape = CircleShape, color = Color(0xFF0B1E36), modifier = Modifier.size(36.dp)) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Text("i", style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.W700)) }
                }
            }

            // Compass — big white circle with beige outer ring, like reference
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                // outer beige ring + white inner
                Box(
                    Modifier.size(300.dp).clip(CircleShape).background(Color(0xFFE8DCD2))
                ) {
                    Box(
                        Modifier.size(270.dp).clip(CircleShape).background(Color.White).align(Alignment.Center)
                    ) {
                        // Canvas for N/E/S/W and Kaaba and needle
                        Canvas(Modifier.fillMaxSize()) {
                            val cx = size.width/2; val cy = size.height/2; val r = size.width/2 - 16f; val center = Offset(cx, cy)
                            // N/E/S/W letters (light beige)
                            val paintColor = Color(0xFFE8DCD2)
                            // Kaaba small icon at Qibla bearing
                            rotate(dialAnimated, center) {
                                val kaabaAngle = -90f; val rad = Math.toRadians(kaabaAngle.toDouble())
                                val kaabaR = r - 36f; val kx = cx + kotlin.math.cos(rad).toFloat()*kaabaR; val ky = cy + kotlin.math.sin(rad).toFloat()*kaabaR
                                // Kaaba: dark gray with gold stripe
                                drawCircle(Color(0xFF3A3A4A), 12f, Offset(kx, ky))
                                drawCircle(Color(0xFFD4A85C), 12f, Offset(kx, ky), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                            }
                        }
                        // needle
                        Canvas(Modifier.fillMaxSize()) {
                            val cx = size.width/2; val cy = size.height/2
                            rotate(needleAnimated, Offset(cx, cy)) {
                                val tip = Offset(cx, cy - 68.dp.toPx())
                                drawCircle(MeeqatRef.Peach, 28f, tip)
                                // needle shape: teardrop peach
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(cx, cy - 72.dp.toPx())
                                    cubicTo(cx+18f, cy-30f, cx+12f, cy+18f, cx, cy+22f)
                                    cubicTo(cx-12f, cy+18f, cx-18f, cy-30f, cx, cy-72f)
                                    close()
                                }
                                drawPath(path, MeeqatRef.Peach)
                                drawCircle(Color.White, 6f, Offset(cx, cy))
                            }
                        }
                        // N/E/S/W text overlay via Box (simpler than canvas text)
                    }
                    // labels N/E/S/W around
                    Box(Modifier.fillMaxSize()) {
                        Text("N", Modifier.align(Alignment.TopCenter).padding(top = 18.dp), style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFFE8DCD2)), textAlign = TextAlign.Center)
                        Text("E", Modifier.align(Alignment.CenterEnd).padding(end = 18.dp), style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFFE8DCD2)))
                        Text("S", Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFFE8DCD2)))
                        Text("W", Modifier.align(Alignment.CenterStart).padding(start = 18.dp), style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFFE8DCD2)))
                    }
                }
                if (!sensorAvailable) {
                    Card(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.GpsOff, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                            Text("No compass sensor", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Instruction — Turn to your left
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
                if (leftPart.isNotEmpty()) {
                    Text(leftPart, style = MaterialTheme.typography.headlineSmall.copy(color = Color(0xFF5A6E89), fontWeight = FontWeight.W400), textAlign = TextAlign.Center)
                    Text(rightPart, style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.W700), textAlign = TextAlign.Center)
                } else {
                    Text(instruction, style = MaterialTheme.typography.headlineSmall.copy(color = if (instruction.contains("Qibla")) MeeqatRef.Peach else Color(0xFF5A6E89), fontWeight = if (instruction.contains("Qibla")) FontWeight.W700 else FontWeight.W400), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            // calibration hint
            if (accuracyLow) {
                Surface(shape = RoundedCornerShape(12.dp), color = MeeqatRef.NavyContainer, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text("Move phone in figure-8 to calibrate  ∞", style = MaterialTheme.typography.bodySmall.copy(color = MeeqatRef.Peach), modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}
