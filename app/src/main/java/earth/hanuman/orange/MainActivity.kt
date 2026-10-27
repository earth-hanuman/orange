package earth.hanuman.orange

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import earth.hanuman.orange.ui.theme.OrangeTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transparent scrims (not the default auto scrim) so our dark Surface
        // paints seamlessly under the nav bar on Android 15+ / gesture + 3-button.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            OrangeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0E0E10)) {
                    DialScreen()
                }
            }
        }
    }
}

@Composable
private fun DialScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    var secureGranted by remember { mutableStateOf(FilterController.hasPermission(context)) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    var active by remember { mutableStateOf(prefs.active) }
    var warmthHue by remember { mutableIntStateOf(prefs.warmthHue) }
    var warmthIntensity by remember { mutableIntStateOf(prefs.warmthIntensity) }
    var dimEnabled by remember { mutableStateOf(prefs.dimEnabled) }
    var grayscaleEnabled by remember { mutableStateOf(prefs.grayscaleEnabled) }
    var scheduleEnabled by remember { mutableStateOf(prefs.scheduleEnabled) }
    var startMinutes by remember { mutableIntStateOf(prefs.startMinutes) }
    var endMinutes by remember { mutableIntStateOf(prefs.endMinutes) }
    var showSchedule by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                secureGranted = FilterController.hasPermission(context)
                overlayGranted = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun persist() {
        prefs.active = active
        prefs.warmthEnabled = warmthIntensity > 0
        prefs.warmthIntensity = warmthIntensity
        prefs.warmthHue = warmthHue
        prefs.dimEnabled = dimEnabled
        prefs.grayscaleEnabled = grayscaleEnabled
    }

    fun apply() {
        persist()
        Orchestrator.apply(context)
    }

    val tint = hueColor(warmthHue)
    val glow = if (active) tint else Color(0xFF55555C)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 20.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Orange", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "⚙︎",
                color = Color(0xFF8A8A8E),
                fontSize = 22.sp,
                modifier = Modifier.clickable { showSchedule = true },
            )
        }

        if (!secureGranted) {
            Spacer(Modifier.height(16.dp))
            SecureSetupCard(context)
        }
        if (!overlayGranted) {
            Spacer(Modifier.height(16.dp))
            OverlaySetupCard(context)
        }

        // Dial (hero toggle)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(216.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glow.copy(alpha = 0.55f),
                                glow.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .border(2.dp, glow.copy(alpha = 0.35f), CircleShape)
                    .clickable {
                        active = !active
                        apply()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141416)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏻", color = Color.White, fontSize = 30.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (active) "ON" else "OFF",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (active) "${hueLabel(warmthHue)} · $warmthIntensity%"
                            else "Tap to turn on",
                            color = Color(0xFF8A8A8E),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        // Sliders
        SliderRow(
            label = "Hue",
            valueLabel = hueLabel(warmthHue),
            value = warmthHue,
            range = 0..60,
            color = tint,
        ) { v, done ->
            warmthHue = v
            if (done && active) apply() else persist()
        }
        Spacer(Modifier.height(18.dp))
        SliderRow(
            label = "Intensity",
            valueLabel = "$warmthIntensity%",
            value = warmthIntensity,
            range = 0..100,
            color = Color(0xFFFF7A1A),
        ) { v, done ->
            warmthIntensity = v
            if (done && active) apply() else persist()
        }

        Spacer(Modifier.height(22.dp))

        // Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Chip("Dim", selected = dimEnabled, modifier = Modifier.weight(1f)) {
                dimEnabled = !dimEnabled
                if (active) apply() else persist()
            }
            Chip("Grayscale", selected = grayscaleEnabled, modifier = Modifier.weight(1f)) {
                grayscaleEnabled = !grayscaleEnabled
                if (active) apply() else persist()
            }
            Chip("Bedtime", selected = false, modifier = Modifier.weight(1f)) {
                prefs.applyBedtimePreset()
                active = prefs.active
                warmthHue = prefs.warmthHue
                warmthIntensity = prefs.warmthIntensity
                dimEnabled = prefs.dimEnabled
                grayscaleEnabled = prefs.grayscaleEnabled
                Orchestrator.apply(context)
            }
        }
    }

    if (showSchedule) {
        ScheduleDialog(
            context = context,
            enabled = scheduleEnabled,
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            onEnabledChange = {
                scheduleEnabled = it
                prefs.scheduleEnabled = it
                ScheduleManager.reschedule(context)
            },
            onStartChange = {
                startMinutes = it
                prefs.startMinutes = it
                ScheduleManager.reschedule(context)
            },
            onEndChange = {
                endMinutes = it
                prefs.endMinutes = it
                ScheduleManager.reschedule(context)
            },
            onDismiss = { showSchedule = false },
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueLabel: String,
    value: Int,
    range: IntRange,
    color: Color,
    onChange: (value: Int, done: Boolean) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = Color(0xFF8A8A8E), fontSize = 13.sp)
            Text(valueLabel, color = Color(0xFF8A8A8E), fontSize = 13.sp)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt(), false) },
            onValueChangeFinished = { onChange(value, true) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0xFF3A3A40),
            ),
        )
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = if (selected) Color(0xFFFF7A1A) else Color(0xFF232327)
    val fg = if (selected) Color(0xFF1A1200) else Color.White
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ScheduleDialog(
    context: Context,
    enabled: Boolean,
    startMinutes: Int,
    endMinutes: Int,
    onEnabledChange: (Boolean) -> Unit,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Schedule") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Auto on/off")
                    Switch(checked = enabled, onCheckedChange = onEnabledChange)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { pickTime(context, startMinutes, onStartChange) },
                        enabled = enabled,
                    ) { Text("Start ${formatTime(startMinutes)}") }
                    OutlinedButton(
                        onClick = { pickTime(context, endMinutes, onEndChange) },
                        enabled = enabled,
                    ) { Text("End ${formatTime(endMinutes)}") }
                }
            }
        },
    )
}

@Composable
private fun SecureSetupCard(context: Context) {
    val command =
        "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Enable dim & grayscale", style = MaterialTheme.typography.titleMedium)
            Text(
                "Dim and grayscale need the WRITE_SECURE_SETTINGS permission. " +
                    "With USB/Wireless debugging on, run this once:",
            )
            Text(command, fontFamily = FontFamily.Monospace)
            Button(onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("adb command", command))
                Toast.makeText(context, "Command copied", Toast.LENGTH_SHORT).show()
            }) { Text("Copy command") }
        }
    }
}

@Composable
private fun OverlaySetupCard(context: Context) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Enable color tint", style = MaterialTheme.typography.titleMedium)
            Text("The tint draws over the screen, which needs \"Display over other apps\".")
            Button(onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            }) { Text("Grant overlay permission") }
        }
    }
}

private fun hueColor(hue: Int): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue.coerceIn(0, 360).toFloat(), 1f, 1f)))

private fun hueLabel(hue: Int): String = when {
    hue <= 8 -> "Red"
    hue <= 20 -> "Red-orange"
    hue <= 38 -> "Orange"
    else -> "Amber"
}

private fun pickTime(context: Context, initialMinutes: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        initialMinutes / 60,
        initialMinutes % 60,
        true,
    ).show()
}

private fun formatTime(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)
