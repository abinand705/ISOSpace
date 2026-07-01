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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import com.example.launcher.viewmodel.LauncherViewModel
import com.example.launcher.model.WidgetType
import com.example.launcher.model.AppItem

private val UbuntuPurple = Color(0xFF2C001E)
private val UbuntuWarmOrange = Color(0xFFE95420)
private val UbuntuDarkSlate = Color(0xFF222222)
private val UbuntuCardGray = Color(0xFF2D2D2D)

data class SoftwarePackage(
    val id: String,
    val title: String,
    val description: String,
    val developer: String,
    val size: String,
    val rating: Float,
    val category: String, // "Applications", "Widgets", "Updates"
    val isSystemApp: Boolean = false,
    val packageName: String = "" // maps to physical/dummy package
)

@Composable
fun ShellSoftwareCenterModal(
    viewModel: LauncherViewModel,
    onClose: () -> Unit,
    onInstallWidget: (WidgetType) -> Unit
) {
    val context = LocalContext.current
    var selectedCategoryTab by remember { mutableStateOf("Apps Catalogue") } // "Apps Catalogue", "System Widgets", "Updates Manager"
    var searchQuery by remember { mutableStateOf("") }

    // Collect apps from launcher viewmodel to dynamically query installed status
    val appsList by viewModel.apps.collectAsState()

    // Apps repository database
    val appPackages = remember {
        listOf(
            SoftwarePackage("pkg_1", "ISOSpace Media Player", "A powerful port of the open source cross-platform multimedia audio & video container.", "ISOSpace Foundation", "42 MB", 4.7f, "Apps Catalogue", packageName = "com.dummy.vlc"),
            SoftwarePackage("pkg_2", "Telegram Messaging Client", "Secure, ultra-fast global messaging channel focusing on terminal encryption API.", "Telegram FZ-LLC", "28 MB", 4.5f, "Apps Catalogue", packageName = "com.dummy.telegram"),
            SoftwarePackage("pkg_3", "Instagram Story Streamer", "Dynamic modern visual photograph sharing backdrop for viewing aesthetic streams.", "Meta", "36 MB", 4.2f, "Apps Catalogue", packageName = "com.dummy.instagram"),
            SoftwarePackage("pkg_4", "WhatsApp Messenger", "Failsafe real-time communication platform bridging encryption keys.", "Meta", "32 MB", 4.6f, "Apps Catalogue", packageName = "com.dummy.whatsapp"),
            SoftwarePackage("pkg_5", "Cheese Camera Lens", "Hardware photo capture and vintage filter generator utility.", "ISOSpace Desktop", "12 MB", 4.4f, "Apps Catalogue", packageName = "com.dummy.camera"),
            SoftwarePackage("pkg_6", "Shotwell Photo Hub", "Shotwell viewer displaying image and video format filemanager data.", "ISOSpace Desktop", "15 MB", 4.8f, "Apps Catalogue", packageName = "com.dummy.gallery"),
            SoftwarePackage("pkg_7", "Notes Sticky Ledger", "Sticky note drafting pad to capture fast diary notes instantly.", "ISOSpace Desktop", "8 MB", 4.5f, "Apps Catalogue", packageName = "com.dummy.notes")
        )
    }

    // Filtered lists
    val filteredCatalog = remember(selectedCategoryTab, searchQuery, appsList) {
        appPackages.filter { pkg ->
            val matchesQuery = pkg.title.contains(searchQuery, ignoreCase = true) ||
                    pkg.description.contains(searchQuery, ignoreCase = true)
            matchesQuery
        }
    }

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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("store_app_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // UBUNTU TOOLBAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(UbuntuPurple.copy(alpha = 0.45f), Color(0xFF5E2750).copy(alpha = 0.15f))))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = UbuntuWarmOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ISOSpace Software Center",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "ISOSpace Repository for Widgets, Packages & Utilities",
                            color = Color.LightGray.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }

                    // Search notes
                    CompactSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search catalogue...",
                        borderColor = UbuntuWarmOrange,
                        modifier = Modifier
                            .width(180.dp)
                            .height(34.dp)
                            .padding(end = 8.dp)
                    )

                    // Close Action
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp).testTag("close_store_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // TAB NAVIGATION SUB-BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Apps Catalogue", "System Widgets", "Updates Manager").forEach { tab ->
                        val isSel = selectedCategoryTab == tab
                        Button(
                            onClick = { selectedCategoryTab = tab },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) UbuntuWarmOrange else Color(0xFF333333)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(tab, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // WORKSPACE LAYER
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF151515))
                        .padding(14.dp)
                ) {
                    when (selectedCategoryTab) {
                        "Apps Catalogue" -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(filteredCatalog) { pkg ->
                                    // Determine if currently installed(not hidden) in viewmodel state
                                    val matchedApp = appsList.find { it.packageName == pkg.packageName }
                                    val isInstalled = matchedApp != null && !matchedApp.isHidden

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = UbuntuCardGray),
                                        border = BorderStroke(1.dp, Color(0x11FFFFFF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Circular Icon representator
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        Color(matchedApp?.customColor ?: 0xFF4CAF50L)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (pkg.packageName) {
                                                        "com.dummy.vlc" -> Icons.Default.PlayArrow
                                                        "com.dummy.telegram" -> Icons.Default.Send
                                                        "com.dummy.instagram" -> Icons.Default.CameraAlt
                                                        "com.dummy.whatsapp" -> Icons.Default.Call
                                                        "com.dummy.camera" -> Icons.Default.PhotoCamera
                                                        "com.dummy.gallery" -> Icons.Default.Image
                                                        "com.dummy.notes" -> Icons.Default.Description
                                                        else -> Icons.Default.Memory
                                                    },
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(pkg.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text("Developer: ${pkg.developer} • Grade: ★ ${pkg.rating}", color = UbuntuWarmOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(pkg.description, color = Color.Gray, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            // Install/Uninstall Control
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

                                                    viewModel.customizeApp(
                                                        packageName = appToModify.packageName,
                                                        newLabel = appToModify.label,
                                                        category = appToModify.category,
                                                        isHidden = isInstalled // toggle hidability
                                                    )

                                                    Toast.makeText(
                                                        context,
                                                        "${pkg.title} has been ${if (isInstalled) "purged from Launcher" else "unlocked and appended"}!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isInstalled) Color(0xFFC62828) else UbuntuWarmOrange
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text(
                                                    text = if (isInstalled) "Purge" else "Install",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "System Widgets" -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            }
                        }

                        "Updates Manager" -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = UbuntuWarmOrange, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("System packages up-to-date", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Linux Kernel 6.8.0-canonical firmware synchronized and fully optimized.", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { Toast.makeText(context, "Checking mirror keys...", Toast.LENGTH_SHORT).show() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Re-index Repository Keys", fontSize = 11.sp)
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
        colors = CardDefaults.cardColors(containerColor = UbuntuCardGray),
        border = BorderStroke(1.dp, Color(0x11FFFFFF)),
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
                tint = UbuntuWarmOrange,
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
                colors = ButtonDefaults.buttonColors(containerColor = UbuntuWarmOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Append", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
    borderColor: Color = UbuntuWarmOrange,
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
            .background(containerColor, RoundedCornerShape(6.dp))
            .border(
                1.dp,
                if (isFocused) borderColor else unfocusedBorderColor,
                RoundedCornerShape(6.dp)
            )
            .onFocusChanged { isFocused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
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
                        modifier = Modifier.size(16.dp)
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
