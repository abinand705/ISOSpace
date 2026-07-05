package com.example.launcher.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.onFocusChanged
import com.example.launcher.viewmodel.LauncherViewModel
import com.example.launcher.model.WidgetType
import com.example.launcher.model.AppItem

private val PlayStoreGreen = Color(0xFF00E676)
private val PlayStoreDarkBg = Color(0xFF111115)
private val PlayStoreCardBg = Color(0xFF1A1A22)
private val PlayStoreAccent = Color(0xFF2196F3)
private val UbuntuPurple = Color(0xFF2C001E)
private val UbuntuWarmOrange = Color(0xFFE95420)
private val UbuntuCardGray = Color(0xFF2D2D2D)

data class SoftwarePackage(
    val id: String,
    val title: String,
    val description: String,
    val developer: String,
    val size: String,
    val rating: Float,
    val category: String, // "Social", "Media", "Utilities", "Games"
    val isSystemApp: Boolean = false,
    val packageName: String = ""
)

@Composable
fun ShellSoftwareCenterModal(
    viewModel: LauncherViewModel,
    onClose: () -> Unit,
    onInstallWidget: (WidgetType) -> Unit
) {
    val context = LocalContext.current
    var selectedCategoryTab by remember { mutableStateOf("Google Play Store") } // "Google Play Store", "Device Apps (Cloner)", "System Widgets", "Updates Manager"
    var storeCategoryFilter by remember { mutableStateOf("All") } // "All", "Social", "Media", "Utilities"
    var searchQuery by remember { mutableStateOf("") }

    // Collect apps from launcher viewmodel to dynamically query installed status
    val appsList by viewModel.apps.collectAsState()
    val installedDeviceApps by viewModel.installedDeviceApps.collectAsState()
    val enabledDeviceApps by viewModel.enabledDeviceApps.collectAsState()

    // Apps repository database
    val appPackages = remember {
        listOf(
            SoftwarePackage("pkg_1", "YouTube Mobile", "Watch your favorite videos, channels, and playlists in a beautiful full-screen dynamic interface.", "Google LLC", "45 MB", 4.8f, "Media", packageName = "com.dummy.youtube"),
            SoftwarePackage("pkg_2", "Spotify Web Player", "Stream millions of songs, podcasts, and albums from around the world on the fly.", "Spotify AB", "30 MB", 4.6f, "Media", packageName = "com.dummy.spotify"),
            SoftwarePackage("pkg_3", "Google Maps", "Navigate, explore, and find local spots with real-time street and transit view.", "Google LLC", "52 MB", 4.5f, "Utilities", packageName = "com.dummy.maps"),
            SoftwarePackage("pkg_4", "Gmail Secure", "Lightning-fast secure email client with smart filtering and instant delivery keys.", "Google LLC", "25 MB", 4.4f, "Utilities", packageName = "com.dummy.gmail"),
            SoftwarePackage("pkg_5", "Discord Mobile", "Join channels, hang out, and stay connected with text, voice, and gaming hubs.", "Discord Inc.", "38 MB", 4.7f, "Social", packageName = "com.dummy.discord"),
            SoftwarePackage("pkg_6", "Reddit Client", "Explore trending communities, live threads, and endless social discussion boards.", "Reddit Inc.", "22 MB", 4.3f, "Social", packageName = "com.dummy.reddit"),
            SoftwarePackage("pkg_7", "Telegram Web", "Ultra-fast global messaging channel focusing on modern security and file transmission.", "Telegram FZ-LLC", "28 MB", 4.5f, "Social", packageName = "com.dummy.telegram"),
            SoftwarePackage("pkg_8", "Instagram View", "Photo sharing backdrop for browsing feeds, direct messaging, and social content.", "Meta Platforms", "36 MB", 4.2f, "Social", packageName = "com.dummy.instagram"),
            SoftwarePackage("pkg_9", "WhatsApp Messenger", "Failsafe instant communication service keeping you in touch with secure chats.", "Meta Platforms", "32 MB", 4.6f, "Social", packageName = "com.dummy.whatsapp"),
            SoftwarePackage("pkg_10", "ISOSpace VLC Player", "A powerful container for cross-platform audio & video file rendering.", "ISOSpace Foundation", "42 MB", 4.7f, "Media", packageName = "com.dummy.vlc"),
            SoftwarePackage("pkg_11", "Cheese Camera Lens", "Hardware photo capture and beautiful responsive vintage filters.", "ISOSpace Desktop", "12 MB", 4.4f, "Utilities", packageName = "com.dummy.camera"),
            SoftwarePackage("pkg_12", "Shotwell Photo Hub", "Shotwell media viewer displaying isolated images, albums, and logs.", "ISOSpace Desktop", "15 MB", 4.8f, "Utilities", packageName = "com.dummy.gallery"),
            SoftwarePackage("pkg_13", "Notes Sticky Ledger", "Quick-drafting note-taking canvas and notebook manager for rapid journaling.", "ISOSpace Desktop", "8 MB", 4.5f, "Utilities", packageName = "com.dummy.notes")
        )
    }

    // Filtered lists for Play Store
    val filteredCatalog = remember(searchQuery, storeCategoryFilter, appsList) {
        appPackages.filter { pkg ->
            val matchesQuery = pkg.title.contains(searchQuery, ignoreCase = true) ||
                    pkg.description.contains(searchQuery, ignoreCase = true)
            val matchesFilter = storeCategoryFilter == "All" || pkg.category == storeCategoryFilter
            matchesQuery && matchesFilter
        }
    }

    // Simulated device apps for testing cloner inside emulator if real ones are empty
    val simulatedDeviceApps = remember {
        listOf(
            AppItem("com.android.chrome", "Chrome Browser", "Chrome Browser", "Applications"),
            AppItem("com.google.android.youtube", "YouTube Native", "YouTube Native", "Applications"),
            AppItem("com.google.android.apps.maps", "Google Maps Native", "Google Maps Native", "Applications"),
            AppItem("com.google.android.gm", "Gmail Native", "Gmail Native", "Applications"),
            AppItem("com.android.camera2", "Camera Native", "Camera Native", "Applications")
        )
    }

    val displayDeviceApps = remember(installedDeviceApps) {
        if (installedDeviceApps.isEmpty()) simulatedDeviceApps else installedDeviceApps
    }

    val filteredDeviceApps = remember(searchQuery, displayDeviceApps) {
        displayDeviceApps.filter { app ->
            app.label.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFB0B0B0E)),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            ),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("store_app_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // GOOGLE PLAY / ISOSPACE REPOSITORY HEADER BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF121216),
                                    Color(0xFF1E293B).copy(alpha = 0.45f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PlayStoreGreen, PlayStoreAccent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Play Store",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "Download responsive cloud apps and clone phone-native packages into your secure sandbox",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    // Compact Search Bar
                    CompactSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search apps...",
                        borderColor = PlayStoreGreen,
                        modifier = Modifier
                            .width(220.dp)
                            .height(36.dp)
                            .padding(end = 12.dp)
                    )

                    // Close Action
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp).testTag("close_store_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // TAB NAVIGATION SUB-BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16161C))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Google Play Store", "Device Apps (Cloner)", "System Widgets", "Updates Manager").forEach { tab ->
                        val isSel = selectedCategoryTab == tab
                        Button(
                            onClick = { 
                                selectedCategoryTab = tab 
                                searchQuery = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) PlayStoreGreen else Color(0xFF23232A),
                                contentColor = if (isSel) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = when (tab) {
                                    "Google Play Store" -> Icons.Default.Shop
                                    "Device Apps (Cloner)" -> Icons.Default.AppRegistration
                                    "System Widgets" -> Icons.Default.Dashboard
                                    "Updates Manager" -> Icons.Default.SystemUpdate
                                    else -> Icons.Default.Apps
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // WORKSPACE LAYER
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(PlayStoreDarkBg)
                ) {
                    when (selectedCategoryTab) {
                        "Google Play Store" -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // PLAY STORE CATEGORY FILTER ROW
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF14141A))
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("All", "Social", "Media", "Utilities").forEach { cat ->
                                        val filterSelected = storeCategoryFilter == cat
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (filterSelected) PlayStoreGreen.copy(alpha = 0.15f) else Color.Transparent)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (filterSelected) PlayStoreGreen else Color.DarkGray,
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .clickable { storeCategoryFilter = cat }
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = cat,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (filterSelected) PlayStoreGreen else Color.LightGray
                                            )
                                        }
                                    }
                                }

                                // FEATURED APP HERO CAROUSEL
                                if (searchQuery.isEmpty() && storeCategoryFilter == "All") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF0F172A), Color(0xFF1E3A8A))
                                                )
                                            )
                                            .border(1.dp, PlayStoreGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(PlayStoreGreen)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("FEATURED WEBAPP", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Stream Premium Entertainment Offline", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Text("Get YouTube, Spotify, and Maps integrated inside the OS securely.", color = Color.Gray, fontSize = 10.sp)
                                            }
                                            Icon(
                                                imageVector = Icons.Default.OfflineBolt,
                                                contentDescription = null,
                                                tint = PlayStoreGreen,
                                                modifier = Modifier.size(52.dp)
                                            )
                                        }
                                    }
                                }

                                // GRID OF APPLICATIONS
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(filteredCatalog) { pkg ->
                                        val matchedApp = appsList.find { it.packageName == pkg.packageName }
                                        val isInstalled = matchedApp != null && !matchedApp.isHidden

                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = PlayStoreCardBg),
                                            border = BorderStroke(1.dp, Color(0xFF2A2A35)),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .padding(14.dp)
                                                    .fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    // App Icon Block
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(
                                                                Color(matchedApp?.customColor ?: 0xFF4CAF50L)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = when (pkg.packageName) {
                                                                "com.dummy.vlc" -> Icons.Default.PlayArrow
                                                                "com.dummy.telegram" -> Icons.AutoMirrored.Filled.Send
                                                                "com.dummy.instagram" -> Icons.Default.CameraAlt
                                                                "com.dummy.whatsapp" -> Icons.Default.Call
                                                                "com.dummy.camera" -> Icons.Default.PhotoCamera
                                                                "com.dummy.gallery" -> Icons.Default.Image
                                                                "com.dummy.notes" -> Icons.Default.Description
                                                                "com.dummy.youtube" -> Icons.Default.PlayCircle
                                                                "com.dummy.spotify" -> Icons.Default.MusicNote
                                                                "com.dummy.maps" -> Icons.Default.Map
                                                                "com.dummy.gmail" -> Icons.Default.Email
                                                                "com.dummy.discord" -> Icons.Default.Forum
                                                                "com.dummy.reddit" -> Icons.Default.Public
                                                                else -> Icons.Default.Memory
                                                            },
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = pkg.title,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "★ ${pkg.rating} • ${pkg.size}",
                                                            color = PlayStoreGreen,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = pkg.description,
                                                    color = Color.Gray,
                                                    fontSize = 10.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 13.sp,
                                                    modifier = Modifier.height(26.dp)
                                                )

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Install / Uninstall Action Controls
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            val appToModify = matchedApp ?: AppItem(
                                                                packageName = pkg.packageName,
                                                                originalLabel = pkg.title,
                                                                label = pkg.title,
                                                                category = "Applications",
                                                                customIconResId = null,
                                                                customColor = 0xFF455A64L,
                                                                isHidden = false,
                                                                counter = 0
                                                            )

                                                            // Toggle install status
                                                            viewModel.customizeApp(
                                                                packageName = appToModify.packageName,
                                                                newLabel = appToModify.label,
                                                                category = appToModify.category,
                                                                isHidden = isInstalled
                                                            )

                                                            Toast.makeText(
                                                                context,
                                                                "${pkg.title} has been ${if (isInstalled) "uninstalled." else "installed onto your homescreen!"}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isInstalled) Color(0xFFC62828) else PlayStoreGreen
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(30.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isInstalled) "Uninstall" else "Install",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isInstalled) Color.White else Color.Black
                                                        )
                                                    }

                                                    if (isInstalled) {
                                                        IconButton(
                                                            onClick = {
                                                                Toast.makeText(context, "Opening ${pkg.title}...", Toast.LENGTH_SHORT).show()
                                                                // Launch custom simulated app flow
                                                                onClose()
                                                                viewModel.openApp(pkg.packageName)
                                                            },
                                                            modifier = Modifier
                                                                .size(30.dp)
                                                                .background(Color(0xFF2C2C35), RoundedCornerShape(8.dp))
                                                        ) {
                                                            Icon(Icons.Default.Launch, contentDescription = "Open App", tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Device Apps (Cloner)" -> {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AppRegistration, contentDescription = null, tint = PlayStoreGreen)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Physical App Cloner", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Select native apps on your phone to clone as virtual sandboxes in ISOSpace Launcher.", color = Color.Gray, fontSize = 11.sp)
                                    }
                                }

                                if (installedDeviceApps.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .background(Color(0xFF1E1A11), RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ EMULATOR DETECTED: Displaying simulated native packages for system cloner verification.",
                                            color = Color(0xFFFFB300),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredDeviceApps) { app ->
                                        val isCloned = enabledDeviceApps.contains(app.packageName)

                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = PlayStoreCardBg),
                                            border = BorderStroke(1.dp, Color(0xFF252530)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(12.dp)
                                                    .fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF2C2C35)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Android,
                                                        contentDescription = null,
                                                        tint = PlayStoreGreen,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(app.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    Text(app.packageName, color = Color.Gray, fontSize = 9.sp)
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Button(
                                                    onClick = {
                                                        viewModel.setDeviceAppEnabled(app.packageName, !isCloned)
                                                        Toast.makeText(
                                                            context,
                                                            if (!isCloned) {
                                                                "Cloned ${app.label}! Package added to sandboxed desktop."
                                                            } else {
                                                                "Removed ${app.label} cloned workspace partition."
                                                            },
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isCloned) Color(0xFFC62828) else PlayStoreGreen,
                                                        contentColor = if (isCloned) Color.White else Color.Black
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text(
                                                        text = if (isCloned) "Uninstall" else "Clone App",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "System Widgets" -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            ) {
                                item {
                                    WidgetInstallRow("Advanced Conky panel", "Displays processor ticks, memory usages and system parameters dynamically on the dashboard feed.", WidgetType.SYSTEM_MONITOR, onInstallWidget)
                                }
                                item {
                                    WidgetInstallRow("Bash Console Terminal", "Direct input utility shell, supporting native logs, calendar and custom colors.", WidgetType.BASH_TERMINAL, onInstallWidget)
                                }
                                item {
                                    WidgetInstallRow("Sensors Weather Widget", "Tracks live coordinates, micro reports and atmospheric forecasts.", WidgetType.WEATHER_INFO, onInstallWidget)
                                }
                                item {
                                    WidgetInstallRow("Canonical Clock Dial", "Bold numeric system calendar date reporter widget layout.", WidgetType.ISOSPACE_CLOCK, onInstallWidget)
                                }
                                item {
                                    WidgetInstallRow("Battery Status Indicator", "Modern energy meter tracking live battery level, power connectivity and status.", WidgetType.BATTERY_STATUS, onInstallWidget)
                                }
                            }
                        }

                        "Updates Manager" -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = PlayStoreGreen, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                               Text("System firmware & packages are up-to-date", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("ISOSpace Kernel Core 6.8.0-canonical compiled dynamically and optimized offline.", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { Toast.makeText(context, "Checking mirror keys...", Toast.LENGTH_SHORT).show() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23232A)),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Refresh Repository Sync Keys", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetInstallRow(
    title: String,
    desc: String,
    type: WidgetType,
    onInstall: (WidgetType) -> Unit
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = PlayStoreCardBg),
        border = BorderStroke(1.dp, Color(0xFF252530)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = null,
                tint = PlayStoreGreen,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.Gray, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = {
                    onInstall(type)
                    Toast.makeText(context, "Appended $title onto Workspace grid!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PlayStoreGreen),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Append", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    borderColor: Color = PlayStoreGreen,
    unfocusedBorderColor: Color = Color.DarkGray,
    containerColor: Color = Color(0x33000000),
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp
) {
    var isFocused by remember { mutableStateOf(false) }
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color.White,
            fontSize = fontSize,
            fontFamily = FontFamily.SansSerif
        ),
        singleLine = true,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(borderColor),
        modifier = modifier
            .background(containerColor, RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (isFocused) borderColor else unfocusedBorderColor,
                RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.Gray,
                            fontSize = fontSize
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear text",
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    )
}
