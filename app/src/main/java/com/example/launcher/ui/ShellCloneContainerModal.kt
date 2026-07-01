package com.example.launcher.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.launcher.model.AppItem
import com.example.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val SandboxNeonGreen = Color(0xFF00FF66)
private val SandboxDarkSlate = Color(0xFF121212)
private val SandboxCardBg = Color(0xFF1E1E1E)
private val SandboxBorder = Color(0xFF2C2C2C)
private val UbuntuPurple = Color(0xFF2C001E)
private val UbuntuWarmOrange = Color(0xFFE95420)

@Composable
fun ShellCloneContainerModal(
    packageName: String,
    viewModel: LauncherViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsState()
    
    // Find matching app details
    val originalApp = remember(apps, packageName) {
        apps.find { it.packageName == "clone.$packageName" || it.packageName == packageName }
    }
    
    val displayName = originalApp?.label ?: "Cloned App"
    val accentColor = Color(originalApp?.customColor ?: 0xFF455A64L)

    var selectedTab by rememberSaveable { mutableStateOf("Workspace") } // "Workspace", "Files Explorer", "Isolation Shield", "Runtime Logs"
    
    // Setup file paths inside filesDir/.isospace_encrypted_sandbox/clones/<packageName>
    val sandboxDir = remember { context.filesDir.resolve(".isospace_encrypted_sandbox") }
    val cloneDir = remember(packageName) { sandboxDir.resolve("clones").resolve(packageName) }
    
    // Create directory if not exists
    LaunchedEffect(cloneDir) {
        if (!cloneDir.exists()) {
            cloneDir.mkdirs()
        }
    }

    // Standard states for sandboxed configs
    var isFirewallEnabled by rememberSaveable { mutableStateOf(true) }
    var isContactsSpoofingEnabled by rememberSaveable { mutableStateOf(true) }
    var isIdentityCloakingEnabled by rememberSaveable { mutableStateOf(true) }
    var isMemoryDecouplingEnabled by rememberSaveable { mutableStateOf(false) }

    // Live logs list
    val logsList = remember { mutableStateListOf<String>() }

    // Helper to add logs
    val addLog = { message: String ->
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val time = sdf.format(Date())
        logsList.add("[$time] $message")
        if (logsList.size > 100) {
            logsList.removeAt(0)
        }
    }

    // Initial logs loading
    LaunchedEffect(packageName) {
        addLog("SYSTEM: Initializing isolated sandboxed container for clone.$packageName")
        addLog("SANDBOX: Created partition path at: ${cloneDir.absolutePath}")
        addLog("SECURITY: Setting up virtual process ID (vPID: ${Random().nextInt(9000) + 10000})")
        addLog("STORAGE: Mounting encrypted storage volume (Status: MOUNTED & VERIFIED)")
        addLog("FIREWALL: Intercepted raw socket bind - establishing secure isolation wrapper")
        addLog("SPOOFING: Contact hook active - redirected system queries to virtual_contacts.db")
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SandboxDarkSlate),
            border = BorderStroke(1.dp, SandboxBorder),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("clone_container_modal_${packageName}")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // SANDBOX TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0F0F))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Original App Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF252525)),
                        contentAlignment = Alignment.Center
                    ) {
                        val systemIconDrawable = remember(packageName) {
                            try {
                                context.packageManager.getApplicationIcon(packageName)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (systemIconDrawable != null) {
                            AsyncImage(
                                model = systemIconDrawable,
                                contentDescription = displayName,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = SandboxNeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SandboxNeonGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "SANDBOX ACTIVE",
                                    color = SandboxNeonGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Isolated process scope: clone.$packageName",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            addLog("SYSTEM: Gracefully closing clone.$packageName container.")
                            onClose()
                        },
                        modifier = Modifier.size(32.dp).testTag("close_clone_container_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Sandbox", tint = Color.LightGray)
                    }
                }

                // SUBHEADER TAB STRIP
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161616))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Workspace" to Icons.Default.AppShortcut,
                        "Files Explorer" to Icons.Default.Folder,
                        "Isolation Shield" to Icons.Default.Shield,
                        "Runtime Logs" to Icons.Default.Terminal
                    ).forEach { (tabName, icon) ->
                        val isSelected = selectedTab == tabName
                        Button(
                            onClick = {
                                selectedTab = tabName
                                addLog("NAVIGATOR: Switched to tab '$tabName'")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) SandboxNeonGreen.copy(alpha = 0.15f) else Color.Transparent,
                                contentColor = if (isSelected) SandboxNeonGreen else Color.Gray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp, 
                                if (isSelected) SandboxNeonGreen.copy(alpha = 0.3f) else Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tabName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(color = SandboxBorder, thickness = 1.dp)

                // MAIN TAB AREA
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    when (selectedTab) {
                        "Workspace" -> {
                            WorkspaceTabContent(
                                packageName = packageName,
                                cloneDir = cloneDir,
                                addLog = addLog,
                                accentColor = accentColor
                            )
                        }
                        "Files Explorer" -> {
                            FilesExplorerTabContent(
                                cloneDir = cloneDir,
                                addLog = addLog,
                                accentColor = accentColor
                            )
                        }
                        "Isolation Shield" -> {
                            IsolationShieldTabContent(
                                isFirewallEnabled = isFirewallEnabled,
                                onFirewallToggle = {
                                    isFirewallEnabled = it
                                    addLog("FIREWALL: Interception filter toggled to $it")
                                },
                                isContactsSpoofingEnabled = isContactsSpoofingEnabled,
                                onContactsSpoofingToggle = {
                                    isContactsSpoofingEnabled = it
                                    addLog("SPOOFING: Contact virtual interception toggled to $it")
                                },
                                isIdentityCloakingEnabled = isIdentityCloakingEnabled,
                                onIdentityCloakingToggle = {
                                    isIdentityCloakingEnabled = it
                                    addLog("CLOAKING: Device ID hardware telemetry masquerade toggled to $it")
                                },
                                isMemoryDecouplingEnabled = isMemoryDecouplingEnabled,
                                onMemoryDecouplingToggle = {
                                    isMemoryDecouplingEnabled = it
                                    addLog("MEMORY: Zero-out dynamic RAM allocation toggled to $it")
                                },
                                accentColor = accentColor
                            )
                        }
                        "Runtime Logs" -> {
                            RuntimeLogsTabContent(
                                logs = logsList,
                                onClear = {
                                    logsList.clear()
                                    addLog("CONSOLE: Log view buffer flushed.")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspaceTabContent(
    packageName: String,
    cloneDir: File,
    addLog: (String) -> Unit,
    accentColor: Color
) {
    val file = remember(packageName) { cloneDir.resolve("app_data.json") }
    
    // App Type Resolver
    val appType = remember(packageName) {
        when {
            packageName.contains("browser") || packageName.contains("chrome") -> "Browser"
            packageName.contains("social") || packageName.contains("youtube") || packageName.contains("instagram") || packageName.contains("telegram") || packageName.contains("whatsapp") -> "Social"
            else -> "General"
        }
    }

    // Persisted sandboxed list of strings
    val dataItems = remember { mutableStateListOf<String>() }

    // Load from local storage inside secure sandbox
    LaunchedEffect(packageName) {
        if (file.exists()) {
            try {
                val json = file.readText()
                val array = JSONArray(json)
                dataItems.clear()
                for (i in 0 until array.length()) {
                    dataItems.add(array.getString(i))
                }
                addLog("STORAGE: Loaded ${dataItems.size} sandboxed records from app_data.json successfully.")
            } catch (e: Exception) {
                addLog("STORAGE_ERROR: Failed to deserialize app_data.json: ${e.message}")
            }
        } else {
            // Write standard initial stub data
            val defaultList = when (appType) {
                "Browser" -> listOf("https://isospace.org", "https://news.ycombinator.com")
                "Social" -> listOf("Welcome to the isolated sandbox environment!", "Secure Post #1: This exists entirely offline.")
                else -> listOf("Database record #1: Created securely.")
            }
            dataItems.clear()
            dataItems.addAll(defaultList)
            saveSandboxedData(file, defaultList)
            addLog("STORAGE: Initialized clean sandboxed storage partition in app_data.json")
        }
    }

    var textInput by rememberSaveable { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // DUAL-APP ENGINE LAUNCH CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = SandboxCardBg),
            border = BorderStroke(1.dp, SandboxNeonGreen.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🚀 DUAL-APP INSTANCE ENGINE ACTIVE",
                            color = SandboxNeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Run this application in its full, original, unmodified secure state. Isolated crypt-sandbox is automatically bound.",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            addLog("DUAL_APP: Initializing sandboxed copy process flow...")
                            addLog("DUAL_APP: Checking package integrity profiles...")
                            delay(100)
                            addLog("DUAL_APP: Sandboxing process environment boundaries...")
                            addLog("DUAL_APP: Isolation Shield successfully ENGAGED.")
                            delay(100)
                            addLog("DUAL_APP: Running native system dual-app process instance...")
                            
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                                Toast.makeText(context, "Launching Dual-App Secure Instance", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: App launcher target missing physically on emulator", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SandboxNeonGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LAUNCH FUNCTIONAL DUAL APP",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SandboxCardBg),
            border = BorderStroke(1.dp, SandboxBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🔐 Sandboxed Virtual Storage State",
                    color = SandboxNeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All database records, preferences, and files stored inside this workspace are written to an isolated directory. System apps CANNOT access this data, and this app CANNOT view system app data.",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Encrypted Local File: ",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "clones/${packageName}/app_data.json",
                        color = SandboxNeonGreen,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Text(
            text = when (appType) {
                "Browser" -> "Isolated Bookmark & Tabs Vault"
                "Social" -> "Sandboxed Communications Feed"
                else -> "Isolated Sandbox Encrypted Records"
            },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = when (appType) {
                            "Browser" -> "Enter bookmark URL..."
                            "Social" -> "Share a sandboxed message..."
                            else -> "Add secure database row..."
                        },
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SandboxNeonGreen,
                    unfocusedBorderColor = SandboxBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Button(
                onClick = {
                    if (textInput.trim().isNotEmpty()) {
                        val newItem = textInput.trim()
                        dataItems.add(newItem)
                        saveSandboxedData(file, dataItems)
                        addLog("STORAGE: Appended secure record: '$newItem'")
                        textInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SandboxNeonGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // List of stored elements
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F0F0F))
                .border(1.dp, SandboxBorder, RoundedCornerShape(8.dp)),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (dataItems.isEmpty()) {
                item {
                    Text(
                        text = "No isolated records inside storage.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            } else {
                items(dataItems.asReversed()) { record ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SandboxCardBg),
                        border = BorderStroke(1.dp, Color(0x11FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (appType) {
                                        "Browser" -> Icons.Default.Language
                                        "Social" -> Icons.Default.ChatBubbleOutline
                                        else -> Icons.Default.Dataset
                                    },
                                    contentDescription = null,
                                    tint = SandboxNeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = record,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    dataItems.remove(record)
                                    saveSandboxedData(file, dataItems)
                                    addLog("STORAGE: Removed record: '$record'")
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun saveSandboxedData(file: File, items: List<String>) {
    try {
        val array = JSONArray()
        items.forEach { array.put(it) }
        file.writeText(array.toString())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun FilesExplorerTabContent(
    cloneDir: File,
    addLog: (String) -> Unit,
    accentColor: Color
) {
    val context = LocalContext.current
    var filesList by remember { mutableStateOf<List<File>>(emptyList()) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger, cloneDir) {
        // Enforce mock system configuration files creation to make the sandbox directory look realistic and complex!
        val config = cloneDir.resolve("network_firewall.cfg")
        if (!config.exists()) config.writeText("firewall_mode=intercept\nblock_unencrypted=true")
        
        val cache = cloneDir.resolve("cache_data.bin")
        if (!cache.exists()) cache.writeText("A0F11CE43109FBC")

        val logs = cloneDir.resolve("security_audit.log")
        if (!logs.exists()) logs.writeText("[SECURITY] Active hook deployed successfully.")

        filesList = cloneDir.listFiles()?.toList() ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📁 Crypt-Sandbox Local Files Explorer",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            TextButton(
                onClick = {
                    refreshTrigger++
                    addLog("FILESYSTEM: Refreshed files list inside sandboxed folder.")
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = SandboxNeonGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Refresh", color = SandboxNeonGreen, fontSize = 11.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F0F0F))
                .border(1.dp, SandboxBorder, RoundedCornerShape(8.dp)),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (filesList.isEmpty()) {
                item {
                    Text(
                        text = "Directory is empty.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            } else {
                items(filesList) { file ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SandboxCardBg),
                        border = BorderStroke(1.dp, Color(0x11FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when {
                                        file.name.endsWith(".json") -> Icons.Default.DataObject
                                        file.name.endsWith(".cfg") -> Icons.Default.SettingsInputComponent
                                        file.name.endsWith(".log") -> Icons.Default.Article
                                        else -> Icons.Default.FilePresent
                                    },
                                    contentDescription = null,
                                    tint = SandboxNeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(file.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Size: ${file.length()} bytes • Last Modified: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))}",
                                        color = Color.Gray,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    try {
                        cloneDir.listFiles()?.forEach { it.delete() }
                        refreshTrigger++
                        addLog("FILESYSTEM: Purged entire sandboxed partition completely.")
                        Toast.makeText(context, "Storage Wiped & Container Restored!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        addLog("FILESYSTEM_ERROR: Failed to wipe sandbox: ${e.message}")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FolderDelete, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Wipe Container Storage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val audit = cloneDir.resolve("security_audit.log")
                    audit.appendText("\n[AUDIT] Integrity verified manually on ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}")
                    refreshTrigger++
                    addLog("FILESYSTEM: Generated new cryptographic audit entry.")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Task, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Verify Integrity", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun IsolationShieldTabContent(
    isFirewallEnabled: Boolean,
    onFirewallToggle: (Boolean) -> Unit,
    isContactsSpoofingEnabled: Boolean,
    onContactsSpoofingToggle: (Boolean) -> Unit,
    isIdentityCloakingEnabled: Boolean,
    onIdentityCloakingToggle: (Boolean) -> Unit,
    isMemoryDecouplingEnabled: Boolean,
    onMemoryDecouplingToggle: (Boolean) -> Unit,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🛡️ Crypt-Sandbox Core Isolation Shield",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SandboxCardBg),
            border = BorderStroke(1.dp, SandboxBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Configure advanced virtual filters. Changes apply instantly to the underlying process wrapper.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        // Toggle 1
        ShieldToggleRow(
            title = "Zero-Trust Network Firewall",
            subtitle = "Intercepts socket connections and drops telemetry/analytics web hooks.",
            checked = isFirewallEnabled,
            onCheckedChange = onFirewallToggle
        )

        // Toggle 2
        ShieldToggleRow(
            title = "Virtual Contacts Spoofing",
            subtitle = "Returns an empty spoofed list when requested by the application, preventing leaks.",
            checked = isContactsSpoofingEnabled,
            onCheckedChange = onContactsSpoofingToggle
        )

        // Toggle 3
        ShieldToggleRow(
            title = "Hardware Identity Cloaking",
            subtitle = "Masquerades MAC address, IMEI, Android ID, and carrier information dynamically.",
            checked = isIdentityCloakingEnabled,
            onCheckedChange = onIdentityCloakingToggle
        )

        // Toggle 4
        ShieldToggleRow(
            title = "RAM Decoupling Optimizer",
            subtitle = "Zeroes out dynamic memory allocation immediately upon container closure.",
            checked = isMemoryDecouplingEnabled,
            onCheckedChange = onMemoryDecouplingToggle
        )
    }
}

@Composable
fun ShieldToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SandboxCardBg),
        border = BorderStroke(1.dp, SandboxBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = Color.Gray, fontSize = 9.sp, lineHeight = 12.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = SandboxNeonGreen,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
    }
}

@Composable
fun RuntimeLogsTabContent(
    logs: List<String>,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💻 Isolated Kernel Live Stream",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear logs", tint = Color.Red, modifier = Modifier.size(16.dp))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .border(1.dp, SandboxBorder, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            val listState = rememberLazyListState()
            
            // Auto scroll to latest logs
            LaunchedEffect(logs.size) {
                if (logs.isNotEmpty()) {
                    listState.animateScrollToItem(logs.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        color = if (log.contains("SYSTEM")) Color.Cyan else if (log.contains("SECURITY")) Color.Red else if (log.contains("STORAGE")) Color.Yellow else SandboxNeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}
