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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meeqat.azan.data.repo.LocationRepository
import com.meeqat.azan.domain.model.QiblaInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
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
        if (out == null) {
            last = input.copyOf()
            return input
        }
        for (i in input.indices) {
            out[i] = out[i] + alpha * (input[i] - out[i])
        }
        return out.copyOf()
    }
}

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _qibla = MutableStateFlow<QiblaInfo?>(null)
    val qibla: StateFlow<QiblaInfo?> = _qibla

    init {
        viewModelScope.launchQibla()
    }

    private fun kotlinx.coroutines.CoroutineScope.launchQibla() {
        launch {
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
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val delta = Math.toRadians(lng2 - lng1)
        val y = sin(delta) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(delta)
        var b = Math.toDegrees(atan2(y, x))
        b = (b + 360) % 360
        return b.toFloat()
    }

    private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

@Composable
fun QiblaScreen(
    viewModel: QiblaViewModel = hiltViewModel()
) {
    val qibla by viewModel.qibla.collectAsState()
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }
    var accuracyLow by remember { mutableStateOf(false) }
    var isFlat by remember { mutableStateOf(true) }
    val filter = remember { LowPassFilter(LOW_PASS_ALPHA) }
    val rotationMatrix = remember { FloatArray(9) }
    val orientation = remember { FloatArray(3) }

    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val filtered = filter.filter(event.values)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, filtered)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val az = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    azimuth = (az + 360) % 360
                    val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    isFlat = kotlin.math.abs(pitch) < 25 && kotlin.math.abs(roll) < 25
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                accuracyLow = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
            }
        }
        if (sensor != null) {
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sm.unregisterListener(listener) }
    }

    val bearing = qibla?.bearingDegrees ?: 0f
    val needleRotationRaw = (bearing - azimuth + 360) % 360
    val needleAnimated by animateFloatAsState(
        targetValue = needleRotationRaw,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "needle"
    )

    val distanceText = qibla?.let { String.format(java.util.Locale.getDefault(), "%.0f km", it.distanceKm) } ?: "-- km"
    val bearingText = qibla?.let { String.format(java.util.Locale.getDefault(), "%.0f°", it.bearingDegrees) } ?: "--°"

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Qibla Compass",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "$bearingText • $distanceText to Kaaba",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CompassCanvas(
                needleRotation = needleAnimated,
                qiblaBearing = bearing,
                modifier = Modifier.fillMaxSize()
            )
            if (!isFlat) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                ) {
                    Text(
                        text = "Hold flat",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            if (accuracyLow) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Calibration needed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Move your phone in a figure-8", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        Text("∞", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("  Kaaba  $bearingText", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W600))
                    }
                    Text(distanceText + " • 21.4225, 39.8262", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (qibla != null) {
                        Text(
                            String.format(java.util.Locale.getDefault(), "You: %.4f, %.4f", qibla.lat, qibla.lng),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CompassCanvas(
    needleRotation: Float,
    qiblaBearing: Float,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = kotlin.math.min(cx, cy) * 0.88f
        val center = Offset(cx, cy)

        drawCircle(color = outlineVariant.copy(alpha = 0.25f), radius = radius + 8f, center = center)
        drawCircle(color = Color.White.copy(alpha = 0.02f), radius = radius, center = center)

        for (i in 0 until 24) {
            val angle = i * 15f - 90f
            val isCardinal = i % 6 == 0
            val isMajor = i % 2 == 0
            val tickLen = when {
                isCardinal -> 18f
                isMajor -> 12f
                else -> 6f
            }
            val stroke = if (isCardinal) 3f else 1.5f
            val color = if (isCardinal) onSurface else outlineVariant
            val rad = Math.toRadians(angle.toDouble())
            val cosA = kotlin.math.cos(rad).toFloat()
            val sinA = kotlin.math.sin(rad).toFloat()
            val inner = radius - tickLen
            val outer = radius
            drawLine(
                color = color,
                start = Offset(cx + cosA * inner, cy + sinA * inner),
                end = Offset(cx + cosA * outer, cy + sinA * outer),
                strokeWidth = stroke
            )
        }

        val labels = listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270)
        for ((_, deg) in labels) {
            val a = Math.toRadians((deg - 90).toDouble())
            val r = radius - 28f
            val x = cx + kotlin.math.cos(a).toFloat() * r
            val y = cy + kotlin.math.sin(a).toFloat() * r
        }

        rotate(degrees = qiblaBearing, pivot = center) {
            val kaabaAngle = -90f
            val rad = Math.toRadians(kaabaAngle.toDouble())
            val kaabaR = radius - 36f
            val kx = cx + kotlin.math.cos(rad).toFloat() * kaabaR
            val ky = cy + kotlin.math.sin(rad).toFloat() * kaabaR
            drawCircle(color = tertiary, radius = 14f, center = Offset(kx, ky))
            drawCircle(color = Color.White, radius = 5f, center = Offset(kx, ky))
        }

        rotate(degrees = needleRotation, pivot = center) {
            val needleLen = radius - 22f
            val tip = Offset(cx, cy - needleLen)
            val baseLeft = Offset(cx - 10f, cy + 12f)
            val baseRight = Offset(cx + 10f, cy + 12f)

            drawLine(color = primary, start = center, end = tip, strokeWidth = 4f)
            drawCircle(color = primary, radius = 6f, center = tip)
            drawCircle(color = secondary, radius = 18f, center = center)
            drawCircle(color = Color.White, radius = 7f, center = center)
        }

        drawCircle(color = onSurface.copy(alpha = 0.08f), radius = radius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
    }
}
