package com.meeqat.azan.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meeqat.azan.data.repo.LocationRepository
import com.meeqat.azan.domain.model.QiblaInfo
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
private const val TAG = "Qibla"

class LowPassFilter(private val alpha: Float = LOW_PASS_ALPHA) {
    private var last: FloatArray? = null
    fun filter(input: FloatArray): FloatArray {
        val out = last
        if (out == null) {
            last = input.copyOf()
            return input
        }
        for (i in input.indices) out[i] = out[i] + alpha * (input[i] - out[i])
        return out.copyOf()
    }
}

class QiblaViewModel(
    private val locationRepository: LocationRepository = com.meeqat.azan.di.ServiceLocator.locationRepository
) : ViewModel() {
    private val _qibla = MutableStateFlow<QiblaInfo?>(null)
    val qibla: StateFlow<QiblaInfo?> = _qibla
    private val _hasLocation = MutableStateFlow(false)
    val hasLocation: StateFlow<Boolean> = _hasLocation

    init {
        viewModelScope.launch {
            Log.i(TAG, "QiblaViewModel observing location")
            locationRepository.observeLocation().collect { loc ->
                _hasLocation.value = loc != null
                if (loc != null) {
                    val b = bearing(loc.latitude, loc.longitude, KAABA_LAT, KAABA_LNG)
                    val d = distanceKm(loc.latitude, loc.longitude, KAABA_LAT, KAABA_LNG)
                    Log.d(TAG, "Qibla computed bearing=$b distance=$d for ${loc.latitude},${loc.longitude}")
                    _qibla.value = QiblaInfo(b, d, loc.latitude, loc.longitude)
                } else {
                    Log.w(TAG, "No location — qibla unavailable")
                    _qibla.value = null
                }
            }
        }
    }

    private fun bearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val phi1 = Math.toRadians(lat1); val phi2 = Math.toRadians(lat2)
        val delta = Math.toRadians(lng2 - lng1)
        val y = sin(delta) * cos(phi2); val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(delta)
        var b = Math.toDegrees(atan2(y, x)); b = (b + 360) % 360; return b.toFloat()
    }
    private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0; val dLat = Math.toRadians(lat2 - lat1); val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat/2)*sin(dLat/2) + cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(dLng/2)*sin(dLng/2)
        val c = 2*atan2(sqrt(a), sqrt(1-a)); return r*c
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QiblaScreen(viewModel: QiblaViewModel = viewModel()) {
    val qibla by viewModel.qibla.collectAsState()
    val hasLocation by viewModel.hasLocation.collectAsState()
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }
    var accuracyLow by remember { mutableStateOf(false) }
    var isFlat by remember { mutableStateOf(true) }
    var sensorAvailable by remember { mutableStateOf(true) }
    val filter = remember { LowPassFilter() }
    val rotationMatrix = remember { FloatArray(9) }
    val orientation = remember { FloatArray(3) }

    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorAvailable = sensor != null
        if (sensor == null) Log.w(TAG, "TYPE_ROTATION_VECTOR not available")
        else Log.i(TAG, "Rotation vector sensor registered")
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val filtered = filter.filter(event.values)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, filtered)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    azimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360) % 360)
                    val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    isFlat = kotlin.math.abs(pitch) < 25 && kotlin.math.abs(roll) < 25
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                accuracyLow = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
                if (accuracyLow) Log.w(TAG, "Sensor accuracy low: $accuracy")
            }
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener); Log.i(TAG, "Sensor listener unregistered") }
    }

    // Expressive springs for both dial and needle
    val bearing = qibla?.bearingDegrees ?: 0f
    val needleRaw = (bearing - azimuth + 360) % 360
    val needleAnimated by animateFloatAsState(needleRaw, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), label = "needle")
    val dialAnimated by animateFloatAsState(bearing, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), label = "dial")

    val distanceText = qibla?.let { String.format(java.util.Locale.getDefault(), "%.0f km", it.distanceKm) } ?: "-- km"
    val bearingText = qibla?.let { String.format(java.util.Locale.getDefault(), "%.0f°", it.bearingDegrees) } ?: "--°"

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val isCompact = maxWidth < 600.dp
        val maxW = 840.dp
        Column(
            Modifier.fillMaxSize().widthIn(max = maxW).align(Alignment.TopCenter).padding(horizontal = if (maxWidth >= 840.dp) 24.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Qibla Compass", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Loading / error states
            when {
                !sensorAvailable -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.GpsOff, null, Modifier.size(24.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Compass unavailable", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("This device has no rotation sensor. Qibla direction will use location only.", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                !hasLocation || qibla == null -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                Column(Modifier.weight(1f)) {
                                    Text("Waiting for location…", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("We need GPS to compute bearing to Kaaba (21.4225, 39.8262). Enable location.", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.Filled.Sensors, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                else -> {
                    Text("$bearingText • $distanceText to Kaaba", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CompassCanvas(needleRotation = needleAnimated, qiblaBearing = dialAnimated, modifier = Modifier.fillMaxSize())
                if (!isFlat && hasLocation) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                    ) {
                        Text("Hold flat", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
                if (accuracyLow && hasLocation) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Calibration needed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1)
                            Text("Move your phone in a figure-8", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("∞", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // Prominent status card — degrees, distance, calibration health
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp)) {
                                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Explore, null, Modifier.size(16.dp)) }
                                }
                                Column {
                                    Text("QIBLA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(bearingText, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.W700), color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(distanceText + " • 21.4225, 39.8262", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            qibla?.let { info ->
                                Text(
                                    String.format(java.util.Locale.getDefault(), "You: %.4f, %.4f", info.lat, info.lng),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = when {
                            !sensorAvailable || !hasLocation -> MaterialTheme.colorScheme.errorContainer
                            accuracyLow || !isFlat -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }, contentColor = when {
                            !sensorAvailable || !hasLocation -> MaterialTheme.colorScheme.onErrorContainer
                            accuracyLow || !isFlat -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    when {
                                        !sensorAvailable -> "No sensor"
                                        !hasLocation -> "No location"
                                        !isFlat -> "Hold flat"
                                        accuracyLow -> "Calibrate"
                                        else -> "Ready"
                                    },
                                    style = MaterialTheme.typography.labelMedium, maxLines = 1
                                )
                                Text(
                                    if (hasLocation && sensorAvailable) "Healthy" else "Waiting",
                                    style = MaterialTheme.typography.labelSmall, maxLines = 1
                                )
                            }
                        }
                    }
                    if (!hasLocation || !sensorAvailable) {
                        LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CompassCanvas(needleRotation: Float, qiblaBearing: Float, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        val cx = size.width/2; val cy = size.height/2; val radius = kotlin.math.min(cx, cy) * 0.88f; val center = Offset(cx, cy)
        drawCircle(color = outlineVariant.copy(alpha = 0.25f), radius = radius+8f, center = center)
        drawCircle(color = Color.White.copy(alpha = 0.02f), radius = radius, center = center)
        for (i in 0 until 24) {
            val angle = i*15f-90f; val isCardinal = i%6==0; val isMajor = i%2==0
            val tickLen = when { isCardinal -> 18f; isMajor -> 12f; else -> 6f }
            val stroke = if (isCardinal) 3f else 1.5f
            val color = if (isCardinal) onSurface else outlineVariant
            val rad = Math.toRadians(angle.toDouble()); val cosA = kotlin.math.cos(rad).toFloat(); val sinA = kotlin.math.sin(rad).toFloat()
            val inner = radius - tickLen; val outer = radius
            drawLine(color, Offset(cx+cosA*inner, cy+sinA*inner), Offset(cx+cosA*outer, cy+sinA*outer), strokeWidth = stroke)
        }
        rotate(degrees = qiblaBearing, pivot = center) {
            val rad = Math.toRadians((-90f).toDouble()); val kaabaR = radius-36f
            val kx = cx + kotlin.math.cos(rad).toFloat()*kaabaR; val ky = cy + kotlin.math.sin(rad).toFloat()*kaabaR
            drawCircle(tertiary, 14f, Offset(kx, ky)); drawCircle(Color.White, 5f, Offset(kx, ky))
        }
        rotate(degrees = needleRotation, pivot = center) {
            val needleLen = radius-22f; val tip = Offset(cx, cy-needleLen)
            drawLine(primary, center, tip, 4f); drawCircle(primary, 6f, tip)
            drawCircle(secondary, 18f, center); drawCircle(Color.White, 7f, center)
        }
        drawCircle(onSurface.copy(alpha = 0.08f), radius, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
    }
}
