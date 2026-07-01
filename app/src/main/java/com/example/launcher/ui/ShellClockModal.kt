package com.example.launcher.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val UbuntuPurple = Color(0xFF2C001E)
private val UbuntuWarmOrange = Color(0xFFE95420)
private val UbuntuDarkSlate = Color(0xFF222222)
private val UbuntuCardGray = Color(0xFF2D2D2D)

@Composable
fun ShellClockModal(onClose: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("World Clock") } // "World Clock", "Stopwatch", "Timer", "Alarms"

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEC0E0E12)),
            border = BorderStroke(
                width = 1.5.dp,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            ),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.75f)
                .testTag("clock_app_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // UBUNTU APPBARTOP
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(UbuntuPurple.copy(alpha = 0.45f), Color(0xFF5E2750).copy(alpha = 0.15f))))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = UbuntuWarmOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ubuntu Timer & Timepiece",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Shotwell Global Clock, Stopwatch & Alarms Panel",
                            color = Color.LightGray.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }

                    // System close
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp).testTag("close_clock_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // SUBHEADER TAB SWITCHES
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("World Clock", "Stopwatch", "Timer", "Alarms").forEach { tab ->
                        val isSel = selectedTab == tab
                        Button(
                            onClick = { selectedTab = tab },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) UbuntuWarmOrange else Color(0xFF333333)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(tab, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // WORKSPACE CORE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF151515))
                        .padding(14.dp)
                ) {
                    when (selectedTab) {
                        "World Clock" -> WorldClockPane()
                        "Stopwatch" -> StopwatchPane()
                        "Timer" -> TimerPane()
                        "Alarms" -> AlarmsPane()
                    }
                }
            }
        }
    }
}

// 1. World clock implementation pane with offsets
@Composable
fun WorldClockPane() {
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    val formatTime = { tzId: String ->
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone(tzId)
        sdf.format(currentTime)
    }

    val formatDate = { tzId: String ->
        val sdf = SimpleDateFormat("E, d MMM yyyy", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone(tzId)
        sdf.format(currentTime)
    }

    val cities = listOf(
        Triple("Local Location (Ubuntu)", TimeZone.getDefault().id, "System Time"),
        Triple("London (GTM / UTC+0)", "Europe/London", "Standard Baseline"),
        Triple("New York (EST / UTC-4)", "America/New_York", "Industrial Center"),
        Triple("Tokyo (JST / UTC+9)", "Asia/Tokyo", "Dynamic Tech Core"),
        Triple("Sydney (AEST / UTC+10)", "Australia/Sydney", "Pacific Coastline")
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(cities) { city ->
            Card(
                colors = CardDefaults.cardColors(containerColor = UbuntuCardGray),
                border = BorderStroke(1.dp, Color(0x11FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(city.first, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(city.third, color = Color.Gray, fontSize = 9.sp)
                        Text(formatDate(city.second), color = UbuntuWarmOrange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Text(
                        text = formatTime(city.second),
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 2. Stopwatch implementation with play, pause, lap logs scroll
@Composable
fun StopwatchPane() {
    var isRunning by remember { mutableStateOf(false) }
    var timeElapsedMs by remember { mutableStateOf(0L) }
    var lapsList by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis() - timeElapsedMs
            while (isRunning) {
                timeElapsedMs = System.currentTimeMillis() - startTime
                delay(10)
            }
        }
    }

    val formatStopwatchTime = { msCount: Long ->
        val totalSecs = msCount / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        val hunds = (msCount % 1000) / 10
        String.format(Locale.US, "%02d:%02d:%02d", mins, secs, hunds)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E1E))
                .border(2.dp, UbuntuWarmOrange, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ELAPSED TIME", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatStopwatchTime(timeElapsedMs),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lap / Reset button
            Button(
                onClick = {
                    if (isRunning) {
                        lapsList = listOf("Lap ${lapsList.size + 1} • ${formatStopwatchTime(timeElapsedMs)}") + lapsList
                    } else {
                        timeElapsedMs = 0L
                        lapsList = emptyList()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier.height(36.dp)
            ) {
                Text(if (isRunning) "Lap Split" else "Reset Chrono", fontSize = 11.sp, color = Color.White)
            }

            // Start / Stop trigger
            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFC62828) else UbuntuWarmOrange
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(if (isRunning) "Pause" else "Launch", fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Laps Scroll Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (lapsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No splits logged during this session.", color = Color.DarkGray, fontSize = 10.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(lapsList) { lap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF222222), RoundedCornerShape(4.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lap.substringBefore(" •"), color = Color.Gray, fontSize = 11.sp)
                            Text(lap.substringAfter("• "), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// 3. Countdown timer implementation
@Composable
fun TimerPane() {
    var inputSecs by remember { mutableStateOf(60L) } // 1 minute default
    var secondsLeft by remember { mutableStateOf(60L) }
    var isTimerActive by remember { mutableStateOf(false) }
    var scaleFraction by remember { mutableStateOf(1.0f) }

    LaunchedEffect(isTimerActive, secondsLeft) {
        if (isTimerActive && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
            scaleFraction = if (scaleFraction == 1.0f) 0.95f else 1.0f
            if (secondsLeft == 0L) {
                isTimerActive = false
            }
        }
    }

    val formatTimer = { secs: Long ->
        val m = secs / 60
        val s = secs % 60
        String.format(Locale.US, "%02d:%02d", m, s)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isTimerActive || secondsLeft < inputSecs) {
            // Circle Countdown Renderer
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1D1D1D))
                    .border(3.dp, UbuntuWarmOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COUNTINGDOWN", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTimer(secondsLeft),
                        fontSize = 32.sp,
                        color = if (secondsLeft <= 5) Color.Red else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        isTimerActive = !isTimerActive
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UbuntuWarmOrange),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(if (isTimerActive) "Pause" else "Resume", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        isTimerActive = false
                        secondsLeft = inputSecs
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Reset", fontSize = 11.sp, color = Color.White)
                }
            }
        } else {
            // Selecting State Wheels input UI
            Text("Set Countdown Period", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(30L, 60L, 120L, 300L, 600L).forEach { duration ->
                    Card(
                        modifier = Modifier
                            .clickable {
                                inputSecs = duration
                                secondsLeft = duration
                            }
                            .width(62.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (inputSecs == duration) UbuntuWarmOrange else Color(0xFF222222)
                        ),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Text(
                            text = "${duration / 60}m",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    secondsLeft = inputSecs
                    isTimerActive = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = UbuntuWarmOrange),
                modifier = Modifier.width(180.dp).height(42.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Timer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 4. Custom Alarms manager Pane with toggle states
data class AlarmDummyItem(
    val time: String,
    val label: String,
    val repeatDays: String,
    var isEnabled: Boolean
)

@Composable
fun AlarmsPane() {
    val context = LocalContext.current
    var alarmsList by remember {
        mutableStateOf(
            listOf(
                AlarmDummyItem("07:00 AM", "Morning Workspace Startup", "Mon, Tue, Wed, Thu, Fri", true),
                AlarmDummyItem("09:30 AM", "Cheese Camera Review Sync", "Sat, Sun", false),
                AlarmDummyItem("10:00 PM", "Commit Ledger Notes", "Everyday", true)
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ubuntu Active Alarms", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Button(
                onClick = {
                    Toast.makeText(context, "New Alarm configuration overlay triggered!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = UbuntuWarmOrange),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Alarm", fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(alarmsList) { alarm ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = UbuntuCardGray),
                    border = BorderStroke(1.dp, Color(0x19FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = alarm.time,
                                color = if (alarm.isEnabled) Color.White else Color.Gray,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(alarm.label, color = Color.LightGray, fontSize = 10.sp)
                            Text(alarm.repeatDays, color = UbuntuWarmOrange, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Switch(
                            checked = alarm.isEnabled,
                            onCheckedChange = { checked ->
                                alarm.isEnabled = checked
                                // Re-trigger state list update
                                alarmsList = alarmsList.map { if (it.label == alarm.label) it.copy(isEnabled = checked) else it }
                                Toast.makeText(context, "Alarm for ${alarm.time} ${if (checked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = UbuntuWarmOrange,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }
                }
            }
        }
    }
}
