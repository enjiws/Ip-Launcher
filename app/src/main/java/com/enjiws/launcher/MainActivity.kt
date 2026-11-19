package com.enjiws.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings
import android.media.AudioManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val Night = Color(0xFF080B14)
private val Glass = Color(0xB820293B)
private val WhiteGlass = Color(0x20FFFFFF)
private val Ice = Color(0xFFB8E9FF)
private val Lilac = Color(0xFFD2C5FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        setContent { EnjiwsLauncher() }
    }
}

private data class AppEntry(
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
)

@Composable
private fun EnjiwsLauncher() {
    val context = LocalContext.current
    var showControl by remember { mutableStateOf(false) }
    var showLock by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var islandExpanded by remember { mutableStateOf(false) }
    var battery by remember { mutableIntStateOf(readBattery(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                if (level >= 0 && scale > 0) battery = level * 100 / scale
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    val apps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) null
                else AppEntry(info.loadLabel(pm).toString(), pkg, info.loadIcon(pm))
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
    val filtered = remember(apps, search) {
        apps.filter { it.label.contains(search, ignoreCase = true) }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Night, surface = Color(0xFF171E2D), primary = Ice,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Box(
            Modifier.fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF172B49), Color(0xFF343052), Color(0xFF171D30), Night)
                    )
                )
                .pointerInput(showControl, showLock) {
                    var dx = 0f
                    var dy = 0f
                    detectDragGestures(
                        onDrag = { change, amount ->
                            dx += amount.x
                            dy += amount.y
                            change.consume()
                        },
                        onDragEnd = {
                            if (abs(dx) > abs(dy) && abs(dx) > 85f) {
                                page = if (dx < 0f) 1 else 0
                                search = ""
                            } else if (dy > 90f) {
                                showControl = true
                            } else if (dy < -90f) {
                                showControl = false
                            }
                            dx = 0f
                            dy = 0f
                        }
                    )
                }
        ) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 22.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ENJIWS", color = Color.White.copy(.78f), fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text(SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(now)),
                        color = Color.White.copy(.75f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(15.dp))
                DynamicIsland(islandExpanded) { islandExpanded = !islandExpanded }
                Spacer(Modifier.height(22.dp))

                if (showLock) {
                    LockPreview(now) { showLock = false }
                } else {
                    AnimatedContent(targetState = page, label = "page") { current ->
                        Column {
                            Text(
                                if (current == 0) "Good afternoon." else "App Library",
                                color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (current == 0) "A little more beautiful every day." else "${apps.size} apps on your device",
                                color = Color.White.copy(.62f), fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    if (page == 0) {
                        LiveWidgets(now, battery, onCalendar = { openCalendar(context) },
                            onBattery = { openBatterySettings(context) })
                        Spacer(Modifier.height(17.dp))
                    }

                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search apps", color = Color.White.copy(.48f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Ice) },
                        singleLine = true, shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Ice.copy(.65f), unfocusedBorderColor = Color.White.copy(.13f),
                            cursorColor = Ice, focusedContainerColor = WhiteGlass,
                            unfocusedContainerColor = WhiteGlass
                        )
                    )
                    Spacer(Modifier.height(13.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(17.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppIcon(app) {
                                context.packageManager.getLaunchIntentForPackage(app.packageName)
                                    ?.let(context::startActivity)
                            }
                        }
                    }
                    Dock(
                        onPhone = { launchByLabel(context, apps, listOf("phone", "dialer", "telepon")) },
                        onMessages = { launchByLabel(context, apps, listOf("message", "pesan")) },
                        onLibrary = { page = 1; search = "" },
                        onControl = { showControl = true }
                    )
                }
            }

            AnimatedVisibility(
                visible = showControl,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ControlCenter(
                    onClose = { showControl = false },
                    onInternet = { openPanel(context, Settings.Panel.ACTION_INTERNET_CONNECTIVITY) },
                    onBluetooth = { openPanel(context, Settings.ACTION_BLUETOOTH_SETTINGS) },
                    onAirplane = { openAirplaneSettings(context) },
                    onDisplay = { openDisplaySettings(context) },
                    onSound = { openSoundSettings(context) }
                )
            }
        }
    }
}

private fun readBattery(context: Context): Int {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    return if (level >= 0 && scale > 0) level * 100 / scale else 100
}

private fun openCalendar(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun openBatterySettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun openPanel(context: Context, action: String) {
    runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun openAirplaneSettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun openDisplaySettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun openSoundSettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun launchByLabel(context: Context, apps: List<AppEntry>, terms: List<String>) {
    apps.firstOrNull { app -> terms.any { app.label.contains(it, true) } }
        ?.let { context.packageManager.getLaunchIntentForPackage(it.packageName)?.let(context::startActivity) }
}

@Composable
private fun DynamicIsland(expanded: Boolean, onClick: () -> Unit) {
    val fraction by animateFloatAsState(
        if (expanded) .78f else .38f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "island-width"
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Row(
            Modifier.fillMaxWidth(fraction).height(if (expanded) 64.dp else 34.dp)
                .clip(CircleShape).background(Color.Black)
                .clickable(onClick = onClick).animateContentSize()
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (expanded) {
                Icon(Icons.Default.MusicNote, null, tint = Ice, modifier = Modifier.size(19.dp))
                Column {
                    Text("ENJIWS • ACTIVITY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text("Tap to collapse", color = Color.White.copy(.58f), fontSize = 9.sp)
                }
                Icon(Icons.Default.Equalizer, null, tint = Lilac)
            } else {
                Box(Modifier.size(6.dp).background(Ice, CircleShape))
                Box(Modifier.size(6.dp).background(Color.White.copy(.5f), CircleShape))
            }
        }
    }
}

@Composable
private fun LiveWidgets(now: Long, battery: Int, onCalendar: () -> Unit, onBattery: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        GlassWidget(Modifier.weight(1f), onClick = onCalendar) {
            Icon(Icons.Default.CalendarMonth, null, tint = Ice)
            Column {
                Text(SimpleDateFormat("d", Locale.getDefault()).format(Date(now)),
                    color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Light)
                Text(SimpleDateFormat("EEE, MMM yyyy", Locale.getDefault()).format(Date(now)),
                    color = Color.White.copy(.67f), fontSize = 10.sp)
                Text("OPEN CALENDAR", color = Ice, fontSize = 8.sp, letterSpacing = 1.sp)
            }
        }
        GlassWidget(Modifier.weight(1f), onClick = onBattery) {
            Icon(Icons.Default.BatteryFull, null, tint = Color(0xFF9AE6B4))
            Column {
                Text("$battery%", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Light)
                Text("Battery level", color = Color.White.copy(.67f), fontSize = 10.sp)
                Text("BATTERY SETTINGS", color = Color(0xFF9AE6B4), fontSize = 8.sp, letterSpacing = .7.sp)
            }
        }
    }
}

@Composable
private fun GlassWidget(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier.height(116.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xB84B6484), Color(0x9920273B))))
            .border(1.dp, Color.White.copy(.15f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick).padding(13.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        content = content
    )
}

@Composable
private fun AppIcon(app: AppEntry, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(2.dp)
    ) {
        Box(
            Modifier.size(57.dp).clip(RoundedCornerShape(17.dp))
                .background(Color.White.copy(.10f))
                .border(1.dp, Color.White.copy(.15f), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = remember(app.packageName) {
                    val bmp = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    app.icon.setBounds(0, 0, 96, 96)
                    app.icon.draw(canvas)
                    bmp.asImageBitmap()
                },
                contentDescription = app.label,
                modifier = Modifier.size(43.dp).clip(RoundedCornerShape(11.dp))
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(app.label, color = Color.White, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Dock(
    onPhone: () -> Unit,
    onMessages: () -> Unit,
    onLibrary: () -> Unit,
    onControl: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(RoundedCornerShape(27.dp))
            .background(Color.White.copy(.14f))
            .border(1.dp, Color.White.copy(.16f), RoundedCornerShape(27.dp))
            .padding(horizontal = 8.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockButton(Icons.Default.Phone, "Phone", onPhone)
        DockButton(Icons.Default.Message, "Messages", onMessages)
        DockButton(Icons.Default.Apps, "Library", onLibrary)
        DockButton(Icons.Default.Tune, "Control", onControl)
    }
}

@Composable
private fun DockButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(13.dp)).clickable(onClick = onClick)
            .padding(horizontal = 9.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(label, color = Color.White.copy(.72f), fontSize = 9.sp)
    }
}

@Composable
private fun LockPreview(now: Long, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().height(440.dp).clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF7189B0), Color(0xFF303B60), Night)))
            .clickable(onClick = onClose).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)),
            color = Color.White.copy(.85f), fontSize = 14.sp)
        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now)),
            color = Color.White, fontSize = 68.sp, fontWeight = FontWeight.ExtraLight)
        Spacer(Modifier.height(26.dp))
        Text("Launcher preview • tap to return", color = Color.White.copy(.65f), fontSize = 11.sp)
    }
}

@Composable
private fun ControlCenter(
    onClose: () -> Unit,
    onInternet: () -> Unit,
    onBluetooth: () -> Unit,
    onAirplane: () -> Unit,
    onDisplay: () -> Unit,
    onSound: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Color(0xF01A2030))
            .border(1.dp, Color.White.copy(.12f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Control Center", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ControlTile("Internet", Icons.Default.Wifi, onInternet, Modifier.weight(1f))
            ControlTile("Bluetooth", Icons.Default.Bluetooth, onBluetooth, Modifier.weight(1f))
            ControlTile("Airplane", Icons.Default.Flight, onAirplane, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ControlTile("Display", Icons.Default.Brightness6, onDisplay, Modifier.weight(1f))
            ControlTile("Sound", Icons.Default.VolumeUp, onSound, Modifier.weight(1f))
            ControlTile("Settings", Icons.Default.Settings, {
                onClose()
            }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text("Controls open Android's native settings panels where supported.",
            color = Color.White.copy(.48f), fontSize = 10.sp)
    }
}

@Composable
private fun ControlTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.height(72.dp).clip(RoundedCornerShape(19.dp))
            .background(Glass).border(1.dp, Color.White.copy(.10f), RoundedCornerShape(19.dp))
            .clickable(onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(icon, null, tint = Ice, modifier = Modifier.size(20.dp))
        Text(label, color = Color.White, fontSize = 10.sp, maxLines = 1)
    }
}
