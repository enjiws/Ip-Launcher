package com.enjiws.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF080B12)
private val Glass = Color(0xB8202634)
private val Cyan = Color(0xFF9DEBFF)
private val Violet = Color(0xFFB8A7FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        setContent { EnjiwsLauncherApp() }
    }
}

private data class AppEntry(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

@Composable
private fun EnjiwsLauncherApp() {
    val context = LocalContext.current
    var showControl by remember { mutableStateOf(false) }
    var showLock by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var islandExpanded by remember { mutableStateOf(false) }

    val apps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) null
                else AppEntry(info.loadLabel(pm).toString(), pkg, info.loadIcon(pm))
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
    val filtered = apps.filter { it.label.contains(search, ignoreCase = true) }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Color(0xFF151B28), primary = Cyan,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF101B30), Color(0xFF25213D), Ink)))
                .pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, drag -> total += drag },
                        onDragEnd = { if (total < -70) page = 1 else if (total > 70) page = 0; total = 0f }
                    )
                }
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("ENJIWS", color = Color.White.copy(.72f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text(SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()), color = Color.White.copy(.75f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(20.dp))
                DynamicIsland(expanded = islandExpanded, onClick = { islandExpanded = !islandExpanded })
                Spacer(Modifier.height(28.dp))
                if (showLock) {
                    LockPreview(onClose = { showLock = false })
                } else {
                    Text(if (page == 0) "Good afternoon." else "App Library", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
                    Text("Make today yours.", color = Color.White.copy(.62f), fontSize = 14.sp)
                    Spacer(Modifier.height(22.dp))
                    if (page == 0) {
                        WidgetRow()
                        Spacer(Modifier.height(24.dp))
                        OutlinedTextField(
                            value = search, onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search apps", color = Color.White.copy(.5f)) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Cyan) },
                            singleLine = true,
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Cyan.copy(.6f), unfocusedBorderColor = Color.White.copy(.12f),
                                cursorColor = Cyan
                            )
                        )
                        Spacer(Modifier.height(18.dp))
                    } else {
                        OutlinedTextField(
                            value = search, onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Find an app", color = Color.White.copy(.5f)) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Cyan) },
                            singleLine = true, shape = RoundedCornerShape(22.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { app ->
                            AppIcon(app) {
                                val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (launch != null) context.startActivity(launch)
                            }
                        }
                    }
                    Dock(onControl = { showControl = true }, onLock = { showLock = true })
                }
            }
            AnimatedVisibility(visible = showControl, modifier = Modifier.align(Alignment.BottomCenter)) {
                ControlCenter(onClose = { showControl = false })
            }
        }
    }
}

@Composable
private fun DynamicIsland(expanded: Boolean, onClick: () -> Unit) {
    val width by animateFloatAsState(if (expanded) 0.82f else 0.40f, label = "island")
    Row(
        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
    ) {
        Row(
            Modifier.fillMaxWidth(width).height(if (expanded) 66.dp else 34.dp)
                .clip(CircleShape).background(Color.Black)
                .clickable(onClick = onClick).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (expanded) {
                Icon(Icons.Default.MusicNote, null, tint = Cyan, modifier = Modifier.size(20.dp))
                Column {
                    Text("Enjiws • Now Playing", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("Your music, your mood", color = Color.White.copy(.6f), fontSize = 9.sp)
                }
                Icon(Icons.Default.Equalizer, null, tint = Violet)
            } else {
                Box(Modifier.size(7.dp).background(Cyan, CircleShape))
                Box(Modifier.size(7.dp).background(Color.White.copy(.5f), CircleShape))
            }
        }
    }
}

@Composable
private fun WidgetRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).height(112.dp).clip(RoundedCornerShape(24.dp)).background(Glass).padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Default.WbSunny, null, tint = Color(0xFFFFD58A))
            Column {
                Text("28°", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Light)
                Text("Partly cloudy", color = Color.White.copy(.6f), fontSize = 10.sp)
            }
        }
        Column(Modifier.weight(1f).height(112.dp).clip(RoundedCornerShape(24.dp)).background(Glass).padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Default.CalendarMonth, null, tint = Cyan)
            Column {
                Text(SimpleDateFormat("d", Locale.getDefault()).format(Date()), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Light)
                Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()), color = Color.White.copy(.6f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun AppIcon(app: AppEntry, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(2.dp)) {
        androidx.compose.foundation.Image(
            bitmap = remember(app.packageName) {
                val drawable = app.icon
                val bmp = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, 96, 96); drawable.draw(canvas)
                androidx.compose.ui.graphics.asImageBitmap(bmp)
            },
            contentDescription = app.label,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(15.dp))
        )
        Spacer(Modifier.height(5.dp))
        Text(app.label, color = Color.White, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Dock(onControl: () -> Unit, onLock: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(.13f)).padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
    ) {
        DockButton(Icons.Default.Phone, "Phone") {}
        DockButton(Icons.Default.Message, "Messages") {}
        DockButton(Icons.Default.Widgets, "Widgets") { onLock() }
        DockButton(Icons.Default.Tune, "Control") { onControl() }
    }
}

@Composable
private fun DockButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(25.dp))
        Text(label, color = Color.White.copy(.68f), fontSize = 9.sp)
    }
}

@Composable
private fun LockPreview(onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(32.dp))
            .height(430.dp).background(Brush.verticalGradient(listOf(Color(0xFF364E78), Color(0xFF161C32), Ink)))
            .clickable(onClick = onClose).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), color = Color.White.copy(.8f), fontSize = 14.sp)
        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), color = Color.White, fontSize = 66.sp, fontWeight = FontWeight.ExtraLight)
        Spacer(Modifier.height(30.dp))
        Text("Tap anywhere to return", color = Color.White.copy(.65f), fontSize = 12.sp)
    }
}

@Composable
private fun ControlCenter(onClose: () -> Unit) {
    var brightness by remember { mutableFloatStateOf(.72f) }
    var volume by remember { mutableFloatStateOf(.55f) }
    var airplane by remember { mutableStateOf(false) }
    var wifi by remember { mutableStateOf(true) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Color(0xF01A2030)).padding(22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Control Center", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleTile("Airplane", Icons.Default.Flight, airplane) { airplane = !airplane }
            ToggleTile("Wi-Fi", Icons.Default.Wifi, wifi) { wifi = !wifi }
            ToggleTile("Bluetooth", Icons.Default.Bluetooth, false) {}
        }
        Spacer(Modifier.height(14.dp))
        Text("Brightness", color = Color.White.copy(.7f), fontSize = 12.sp)
        Slider(value = brightness, onValueChange = { brightness = it })
        Text("Volume", color = Color.White.copy(.7f), fontSize = 12.sp)
        Slider(value = volume, onValueChange = { volume = it })
        Text("Preview controls only — Android system toggles require system-level access.", color = Color.White.copy(.45f), fontSize = 10.sp)
    }
}

@Composable
private fun ToggleTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(96.dp).height(76.dp).clip(RoundedCornerShape(20.dp))
            .background(if (active) Color(0xFF3C7190) else Glass)
            .clickable(onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(icon, null, tint = Color.White)
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}
