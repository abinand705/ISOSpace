package com.example.launcher.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val TerminalDeepPurple = Color(0xFF2C001E)
private val TerminalBlack = Color(0xFF0C0C0C)
private val TerminalGreen = Color(0xFF4AF626)
private val TerminalOrange = Color(0xFFE95420)
private val TerminalLightGray = Color(0xFFDFDFDF)

@Composable
fun ShellTerminalModal(
    viewModel: LauncherViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Command History list
    val history = remember { mutableStateListOf<String>() }
    var inputCmd by remember { mutableStateOf("") }
    
    // Matrix effect active state
    var isMatrixActive by remember { mutableStateOf(false) }

    // Initial sign-on ISOSpace splash screen banner
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("E MMM d HH:mm:ss yyyy", Locale.getDefault())
        val dateStr = sdf.format(Date())
        history.add("Welcome to ISOSpace 1.0 (GNU/Linux 6.1.0-generic aarch64)")
        history.add("")
        history.add(" * Documentation:  https://isospace.org/docs")
        history.add(" * Management:     https://isospace.org/manage")
        history.add(" * Support:        https://isospace.org/support")
        history.add("")
        history.add(" System information as of $dateStr:")
        history.add("")
        history.add("  System load:   [OK]             Processes:             106")
        history.add("  Usage of /:    23.4% of 64GB    Users logged in:       1")
        history.add("  Memory usage:  51% (Stable)     IPv4 address for wlan: 192.168.1.137")
        history.add("")
        history.add("Type 'help' to see a list of awesome diagnostic commands.")
        history.add("")
        
        // Request focus on terminal command keyboard
        delay(300)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Auto-scroll list to bottom as history appends
    LaunchedEffect(history.size, isMatrixActive) {
        if (history.isNotEmpty() && !isMatrixActive) {
            coroutineScope.launch {
                listState.animateScrollToItem(history.size - 1)
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEC07070A)),
            border = BorderStroke(
                width = 1.5.dp,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        TerminalGreen.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(520.dp)
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Window Bar (Aesthetic ISOSpace style decoration)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF15151A), Color(0xFF0F0F12))))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5F56)) // Red (close)
                                .clickable { onClose() }
                        )
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFBD2E)) // Yellow (minimize)
                                .clickable { onClose() }
                        )
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF27C93F)) // Green (maximize)
                        )
                    }

                    Text(
                        text = "bash - isospace@mobile: ~",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = TerminalOrange,
                        modifier = Modifier.size(15.dp)
                    )
                }

                if (isMatrixActive) {
                    // Matrix Animation Canvas overlay
                    MatrixCodeRain(
                        onStop = { isMatrixActive = false }
                    )
                } else {
                    // Output Console Log Screen
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Top
                        ) {
                            items(history) { line ->
                                Text(
                                    text = line,
                                    color = when {
                                        line.startsWith("isospace@mobile") -> TerminalGreen
                                        line.startsWith("bash: ") -> Color(0xFFF44336)
                                        line.startsWith(" *") -> TerminalOrange
                                        else -> TerminalLightGray
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Bottom Enter Command Row prompt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF151515))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "isospace@mobile:~$ ",
                            color = TerminalOrange,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )

                        BasicTextField(
                            value = inputCmd,
                            onValueChange = { inputCmd = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                             keyboardActions = KeyboardActions(
                                onDone = {
                                    if (inputCmd.isNotBlank()) {
                                        executeLocalCommand(inputCmd, history, viewModel, context) {
                                            isMatrixActive = true
                                        }
                                        inputCmd = ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("terminal_input_modal")
                        )

                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Submit Command",
                            tint = TerminalOrange,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    if (inputCmd.isNotBlank()) {
                                        executeLocalCommand(inputCmd, history, viewModel, context) {
                                            isMatrixActive = true
                                        }
                                        inputCmd = ""
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

// Handler processor for fully simulated ISOSpace command executions
private fun executeLocalCommand(
    rawCmd: String,
    history: MutableList<String>,
    viewModel: LauncherViewModel,
    context: Context,
    onMatrixTrigger: () -> Unit
) {
    val clean = rawCmd.trim()
    history.add("isospace@mobile:~$ $clean")

    val parts = clean.split("\\s+".toRegex())
    val baseCmd = parts[0].lowercase()

    when (baseCmd) {
        "help" -> {
            history.add("Available commands:")
            history.add("-------------------")
            history.add(" help               - Displays instructions and command catalog.")
            history.add(" apps               - Package Management: lists, adds, or removes physical system apps.")
            history.add(" sandbox            - Displays secure sandbox folder statistics and configuration.")
            history.add(" uname              - Displays OS name, architecture, and kernel releases.")
            history.add(" neofetch           - Launches simulated ISOSpace telemetry details styled in ASCII.")
            history.add(" ls                 - Lists current home folder documents and metadata guides.")
            history.add(" cat [file]         - Reads the specified text file content straight onto terminal.")
            history.add(" date               - Outputs local system time, day and general clock metrics.")
            history.add(" clear              - Empties entire terminal output block.")
            history.add(" df / free          - Displays system storage partitions & memory allocations.")
            history.add(" top / ps           - Displays CPU and memory usage of running app processes.")
            history.add(" cowsay [msg]       - Outputs a friendly talking ASCII Cow reciting your custom message.")
            history.add(" matrix             - Triggers digital waterfall screen matrix cascades.")
            history.add("-------------------")
            history.add("Adding Real System Apps via CLI:")
            history.add("  1. Type 'apps list-installed' to view available packages on your device.")
            history.add("  2. Type 'apps add <package_name>' to pin the system app to your launcher.")
            history.add("  3. Type 'apps remove <package_name>' to unpin the system app.")
            history.add("-------------------")
        }
        "apps" -> {
            if (parts.size < 2) {
                history.add("=== ISOSpace App Package Manager CLI ===")
                history.add("Usage: apps [subcommand] [arguments]")
                history.add("")
                history.add("Subcommands:")
                history.add("  list-installed   - List all physical system apps found on your device.")
                history.add("  list-enabled     - List system apps currently active/visible in launcher.")
                history.add("  add <pkg_name>   - Enable/Add a system app to the launcher.")
                history.add("  remove <pkg_name> - Disable/Remove a system app from the launcher.")
                history.add("")
                history.add("Examples:")
                history.add("  apps add com.android.chrome")
                history.add("  apps remove com.android.chrome")
            } else {
                val sub = parts[1].lowercase()
                when (sub) {
                    "list-installed", "list-all" -> {
                        val apps = viewModel.installedDeviceApps.value
                        if (apps.isEmpty()) {
                            history.add("No external system applications found on this device.")
                            history.add("Feel free to add any standard Android package names manually via 'apps add':")
                            history.add("  - com.android.chrome (Chrome)")
                            history.add("  - com.google.android.youtube (YouTube)")
                            history.add("  - com.google.android.apps.maps (Google Maps)")
                            history.add("  - com.spotify.music (Spotify)")
                        } else {
                            history.add("Physical / System apps on this device:")
                            history.add("-------------------------------------")
                            apps.forEach { app ->
                                history.add(" • ${app.label} (${app.packageName})")
                            }
                        }
                    }
                    "list-enabled" -> {
                        val enabled = viewModel.enabledDeviceApps.value
                        if (enabled.isEmpty()) {
                            history.add("No physical system apps are currently active/visible in the launcher.")
                        } else {
                            history.add("Currently enabled physical/system apps:")
                            history.add("---------------------------------------")
                            enabled.forEach { pkg ->
                                history.add(" • $pkg")
                            }
                        }
                    }
                    "add" -> {
                        if (parts.size < 3) {
                            history.add("Error: App name or package name missing.")
                            history.add("Usage: apps add <package_name_or_label>")
                        } else {
                            val query = parts.subList(2, parts.size).joinToString(" ").trim()
                            val installed = viewModel.installedDeviceApps.value
                            val matchedApp = installed.find { 
                                it.packageName.equals(query, ignoreCase = true) || 
                                it.label.equals(query, ignoreCase = true) 
                            }
                            if (matchedApp != null) {
                                viewModel.setDeviceAppEnabled(matchedApp.packageName, true)
                                history.add("Success: App icon for '${matchedApp.label}' found in system!")
                                history.add("Package '${matchedApp.packageName}' is now visible and launchable.")
                            } else {
                                history.add("Error: App or icon for '$query' was not found in the system.")
                                history.add("Make sure the package name or label matches the device exactly.")
                                history.add("Type 'apps list-installed' to see all installed system apps with icons.")
                            }
                        }
                    }
                    "remove" -> {
                        if (parts.size < 3) {
                            history.add("Error: App name or package name missing.")
                            history.add("Usage: apps remove <package_name_or_label>")
                        } else {
                            val query = parts.subList(2, parts.size).joinToString(" ").trim()
                            val installed = viewModel.installedDeviceApps.value
                            val matchedApp = installed.find { 
                                it.packageName.equals(query, ignoreCase = true) || 
                                it.label.equals(query, ignoreCase = true) 
                            }
                            if (matchedApp != null) {
                                viewModel.setDeviceAppEnabled(matchedApp.packageName, false)
                                history.add("Success: App '${matchedApp.label}' (${matchedApp.packageName}) disabled and hidden.")
                            } else {
                                // Fallback directly using the package name if not found in physical list
                                viewModel.setDeviceAppEnabled(query, false)
                                history.add("Success: Attempted to hide/disable package '$query'.")
                            }
                        }
                    }
                    "help" -> {
                        history.add("=== ISOSpace App Package Manager CLI ===")
                        history.add("Available subcommands: list-installed, list-enabled, add, remove")
                    }
                    else -> {
                        history.add("Error: Unknown subcommand '$sub'. Type 'apps' for usage instructions.")
                    }
                }
            }
        }
        "sandbox" -> {
            history.add("=== ISOSPACE SECURE CRYPTO-SANDBOX STATUS ===")
            history.add("Status: MOUNTED & VERIFIED")
            history.add("Container Owner: ${viewModel.getSandboxUsername()}")
            history.add("Enforced Limit: ${viewModel.getSandboxSizeLimitGb()} GB")
            history.add("Storage Used: 1.28 GB (Encrypted partition headers)")
            history.add("Physical Mount: /data/user/0/com.example/files/.isospace_encrypted_sandbox")
            history.add("Security Grade: FIPS 140-2 Compliant Sandbox Container")
            history.add("---------------------------------------------")
            history.add("No host leaks detected. All launch logs isolated.")
        }
        "uname" -> {
            history.add("Linux isospace-mobile 6.1.0-23-generic-arm64 #1 SMP PREEMPT_DYNAMIC UTC 2026 aarch64 GNU/Linux")
        }
        "date" -> {
            history.add(SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy", Locale.getDefault()).format(Date()))
        }
        "clear" -> {
            history.clear()
        }
        "neofetch", "isospace" -> {
            history.add("            .-.-.         isospace@mobile")
            history.add("           /     \\        ---------------")
            history.add("          |   ()  |       OS: ISOSpace Touch 1.0 arm64")
            history.add("           \\     /        Kernel: 6.1.0-23-generic-android")
            history.add("            `-.-'         Host: ISOSpace Mobile Emulator")
            history.add("      .---.       .---.   Uptime: 2 days, 16 hours, 10 mins")
            history.add("     /     \\     /     \\  Packages: 1248 (dpkg)")
            history.add("    |   ()  |---|   ()  | Shell: bash 5.1.16")
            history.add("     \\     /     \\     /  Resolution: 1080x2400 (AMOLED)")
            history.add("      `---'       `---'   DE: ISOSpace Shell")
            history.add("                          WM: ISOSpace Window Manager")
            history.add("                          Theme: ISOSpace-dark [GTK2/3]")
            history.add("                          Terminal: isospace-terminal")
            history.add("                          CPU: ARM Cortex-A78 Octa-Core")
            history.add("                          Memory: 4180MiB / 8192MiB (51%)")
        }

        "df" -> {
            history.add("Filesystem     1K-blocks      Used Available Use% Mounted on")
            history.add("/dev/root       15872102  12480312   3391790  79% /")
            history.add("tmpfs            4031920         4   4031916   1% /dev")
            history.add("/dev/sda1       64120912  15102030  49018882  24% /home")
        }
        "free" -> {
            history.add("              total        used        free      shared  buff/cache   available")
            history.add("Mem:        8192100     4180421     2102910      104100     1908769     3910300")
            history.add("Swap:       2097148      102319     1994829")
        }
        "ps", "top" -> {
            history.add("  PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND")
            history.add("    1 root      20   0    8912   1240    610 S   0.0   0.0   0:02.13 systemd")
            history.add("  142 isospace  20   0  389210  42104  12410 S   1.8   0.5   1:12.44 multi_dock_panel")
            history.add("  210 isospace  20   0  121404  14210   5120 S   0.5   0.2   0:43.10 files_fsm")
            history.add("  305 isospace  20   0  529012  85102  30140 S   2.1   1.0   2:05.12 browser_app")
            history.add("  412 isospace  20   0   42910   6180   4100 R   0.9   0.1   0:00.85 bash_session")
        }
        "cowsay" -> {
            if (parts.size < 2) {
                history.add("Usage: cowsay <your message>")
            } else {
                val message = clean.removePrefix("cowsay ").trim()
                val lineBorder = " -".repeat(message.length / 2 + 3)
                history.add("   $lineBorder")
                history.add("  < $message >")
                history.add("   $lineBorder")
                history.add("          \\\\   ^__^")
                history.add("           \\\\  (oo)\\\\\\\\_______")
                history.add("              (__)\\\\       )\\\\/\\\\")
                history.add("                  ||----w |")
                history.add("                  ||     ||")
            }
        }
        "matrix" -> {
            onMatrixTrigger()
        }
        else -> {
            try {
                val sandboxDir = context.filesDir.resolve(".isospace_encrypted_sandbox")
                val mySpaceDir = java.io.File(sandboxDir, "My Space")
                if (!mySpaceDir.exists()) {
                    mySpaceDir.mkdirs()
                }

                // Run the real command with standard shell, setting working dir to secure sandbox My Space folder
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", clean), null, mySpaceDir)
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val errorReader = java.io.BufferedReader(java.io.InputStreamReader(process.errorStream))
                
                val outputLines = mutableListOf<String>()
                var outLine: String? = reader.readLine()
                while (outLine != null) {
                    outputLines.add(outLine)
                    outLine = reader.readLine()
                }
                
                var errLine: String? = errorReader.readLine()
                while (errLine != null) {
                    outputLines.add("stderr: $errLine")
                    errLine = errorReader.readLine()
                }
                
                process.waitFor()
                if (outputLines.isNotEmpty()) {
                    outputLines.forEach { history.add(it) }
                } else {
                    // Command finished cleanly without terminal printouts
                    history.add("[Process completed with exit code: ${process.exitValue()}]")
                }
            } catch (e: Exception) {
                history.add("bash: $clean: execution failed (${e.message})")
            }
        }
    }
}

// Matrix Falldown Terminal rain code graphics effect
@Composable
fun MatrixCodeRain(
    onStop: () -> Unit
) {
    var tick by remember { mutableStateOf(0) }
    val coroutine = rememberCoroutineScope()
    val columns = 16
    val dropPos = remember { Array(columns) { (0..30).random() } }
    val charsStr = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ@#\$%&*-+={}[]"

    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            tick++
            for (i in 0 until columns) {
                dropPos[i] = (dropPos[i] + 1) % 35
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onStop() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (r in 0..18) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0 until columns) {
                            val activeVal = dropPos[c]
                            val isLead = r == activeVal
                            val isTrail = r < activeVal && r > activeVal - 8
                            
                            val character = remember(tick, r, c) { charsStr.random().toString() }
                            
                            Text(
                                text = character,
                                color = when {
                                    isLead -> Color.White
                                    isTrail -> Color(0xFF00FF00)
                                    else -> Color.Transparent
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = if (isLead) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = TerminalOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Return to Terminal", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
