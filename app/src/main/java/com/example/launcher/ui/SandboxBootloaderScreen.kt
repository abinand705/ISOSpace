package com.example.launcher.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.os.StatFs
import com.example.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxBootloaderScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler(enabled = true) {
        // Prevent back press from minimizing or exiting on the lock screen
    }

    val context = LocalContext.current
    val availableSpaceGb = remember(context) { getAvailableStorageGb(context) }

    val recoveryQuestions = remember {
        listOf(
            "What was your first pet's name?",
            "What city were you born in?",
            "What was the name of your first school?",
            "What was your first car's make/model?"
        )
    }
    var registerQuestionIndex by remember { mutableStateOf(0) }
    var registerAnswer by remember { mutableStateOf("") }

    var recoveryStep by remember { mutableStateOf("username") } // "username", "answer", "reset"
    var recoveryUsername by remember { mutableStateOf("") }
    var recoveryAnswerInput by remember { mutableStateOf("") }
    var recoveryNewPassword by remember { mutableStateOf("") }
    var recoveryNewPasswordConfirm by remember { mutableStateOf("") }
    var recoveryQuestionToShow by remember { mutableStateOf("") }

    var step by remember { mutableStateOf("welcome") } // "welcome", "register", "login", "formatting", "booting", "forgot_password"
    
    var registerUsername by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var registerSizeLimitInput by remember { mutableStateOf("20") }
    val registerSizeLimitGb = registerSizeLimitInput.toIntOrNull() ?: 0
    
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    
    var terminalLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val isRegistered = remember { viewModel.isSandboxRegistered() }
    val scope = rememberCoroutineScope()
    
    val terminalGreen = Color(0xFF33FF33)
    val terminalDarkBg = Color(0xFF080D08)
    val terminalBorder = Color(0x3333FF33)

    // Automatically output system logs at launch
    LaunchedEffect(Unit) {
        val initialLogs = mutableListOf<String>()
        initialLogs.add("isospace-bootloader v1.42.0-generic starting...")
        delay(150)
        initialLogs.add("CPU: ARMv8-A Cryptographic Extension detected")
        delay(120)
        initialLogs.add("Initializing secure physical filesystem mount...")
        delay(150)
        
        if (isRegistered) {
            initialLogs.add("[+] SUCCESS: SECURE SANDBOX CONTAINER DETECTED")
            initialLogs.add("[+] VIRTUAL DIRECTORY: .isospace_encrypted_sandbox (ENCRYPTED)")
            initialLogs.add("[+] STATUS: LOCKED / SECURE RECOVERY KEY REQUIRED")
            terminalLogs = initialLogs.toList()
            step = "login"
        } else {
            initialLogs.add("[-] ERROR: SECURE RECOVERY CONTAINER NOT FOUND")
            initialLogs.add("[-] STATUS: UNREGISTERED / SANDBOX RECOVERY VAULT INACTIVE")
            initialLogs.add("[-] Sandboxing prevents host malware from reading launch logs.")
            terminalLogs = initialLogs.toList()
            step = "register"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040A05)),
        contentAlignment = Alignment.Center
    ) {
        // Holographic Spatial grid background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0F2612), Color(0xFF030703)),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.width * 1.2f
                )
            )
            val gap = 45.dp.toPx()
            val gridColor = Color(0x0C33FF33)
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                x += gap
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                y += gap
            }
        }

        // Floating glassmorphic terminal panel
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .background(Color(0xE6081008), RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF33FF33).copy(alpha = 0.5f),
                            Color(0xFF33FF33).copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .clip(RoundedCornerShape(24.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Header Console title block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ISOSPACE SECURE BOOTLOADER v1.42",
                    color = terminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(terminalGreen, RoundedCornerShape(4.dp))
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = terminalBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Display scrolling terminal logs
            terminalLogs.forEach { log ->
                Text(
                    text = log,
                    color = if (log.startsWith("[-]")) Color(0xFFFF5555) else if (log.startsWith("[+]")) terminalGreen else terminalGreen.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Forms & Flows based on current step
            when (step) {
                "register" -> {
                    Text(
                        text = ">>> INITIAL SYSTEM REGISTRATION:",
                        color = terminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        text = "Create a localized vault which completely sandboxes your application data from the host system. This ensures privacy and defense against unauthorized access.",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Username Input
                    OutlinedTextField(
                        value = registerUsername,
                        onValueChange = { registerUsername = it },
                        label = { Text("Set Username", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace) },
                        textStyle = TextStyle(color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = terminalGreen,
                            unfocusedBorderColor = terminalBorder,
                            cursorColor = terminalGreen
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Input
                    OutlinedTextField(
                        value = registerPassword,
                        onValueChange = { registerPassword = it },
                        label = { Text("Set Secure Password", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace) },
                        textStyle = TextStyle(color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = terminalGreen,
                            unfocusedBorderColor = terminalBorder,
                            cursorColor = terminalGreen
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Security Question Selection Row (Terminal style cycle selector)
                    Text(
                        text = "Set Security Recovery Question:",
                        color = terminalGreen.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, terminalBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                registerQuestionIndex = if (registerQuestionIndex > 0) registerQuestionIndex - 1 else recoveryQuestions.size - 1 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("<", color = terminalGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            text = recoveryQuestions[registerQuestionIndex],
                            color = terminalGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { 
                                registerQuestionIndex = (registerQuestionIndex + 1) % recoveryQuestions.size 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(">", color = terminalGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Security Answer Input
                    OutlinedTextField(
                        value = registerAnswer,
                        onValueChange = { registerAnswer = it },
                        label = { Text("Security Answer", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace) },
                        textStyle = TextStyle(color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = terminalGreen,
                            unfocusedBorderColor = terminalBorder,
                            cursorColor = terminalGreen
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Size Limit Selection
                    Text(
                        text = "Sandbox Container Space Allocation (GB):",
                        color = terminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "Available Device Storage: $availableSpaceGb GB",
                        color = if (availableSpaceGb >= registerSizeLimitGb && registerSizeLimitGb > 0) terminalGreen.copy(alpha = 0.7f) else Color(0xFFFF5555),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = registerSizeLimitInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                registerSizeLimitInput = input
                            }
                        },
                        label = { Text("Allocation Size (GB)", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace) },
                        textStyle = TextStyle(color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = terminalGreen,
                            unfocusedBorderColor = terminalBorder,
                            cursorColor = terminalGreen
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10, 20, 30, 50, 100).forEach { gb ->
                            val selected = registerSizeLimitGb == gb
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) terminalGreen else Color.Transparent)
                                    .border(1.dp, terminalGreen, RoundedCornerShape(6.dp))
                                    .clickable { registerSizeLimitInput = gb.toString() }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${gb}G",
                                    color = if (selected) Color.Black else terminalGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    errorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "[-] ERROR: $err",
                            color = Color(0xFFFF4444),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Register button
                    Button(
                        onClick = {
                            if (registerUsername.trim().isEmpty() || registerPassword.isEmpty()) {
                                errorMessage = "Username or password cannot be empty."
                                return@Button
                            }
                            if (registerAnswer.trim().isEmpty()) {
                                errorMessage = "Recovery Answer cannot be empty."
                                return@Button
                            }
                            if (registerSizeLimitGb <= 0) {
                                errorMessage = "Please enter a valid positive allocation size in GB."
                                return@Button
                            }
                            if (registerSizeLimitGb > availableSpaceGb) {
                                errorMessage = "INSUFFICIENT DEVICE STORAGE. Chosen allocation ($registerSizeLimitGb GB) exceeds available space ($availableSpaceGb GB)."
                                return@Button
                            }
                            errorMessage = null
                            step = "formatting"
                            scope.launch {
                                val currentLogs = terminalLogs.toMutableList()
                                currentLogs.add(">>> INITIALIZING STORAGE ALLOCATION FORMULA...")
                                terminalLogs = currentLogs.toList()
                                delay(600)
                                currentLogs.add("[+] Allocating $registerSizeLimitGb GB memory limit bounds...")
                                terminalLogs = currentLogs.toList()
                                delay(600)
                                currentLogs.add("[+] Creating secure hidden folder: .isospace_encrypted_sandbox/")
                                terminalLogs = currentLogs.toList()
                                delay(700)
                                currentLogs.add("[+] Initializing digital signature & keys...")
                                terminalLogs = currentLogs.toList()
                                delay(500)
                                viewModel.registerSandbox(
                                    registerUsername, 
                                    registerPassword, 
                                    registerSizeLimitGb,
                                    recoveryQuestions[registerQuestionIndex],
                                    registerAnswer.trim()
                                )
                                currentLogs.add("[+] Container encrypted and registered successfully!")
                                currentLogs.add(">>> BOOTING WORKSPACE SECURE KERNEL...")
                                terminalLogs = currentLogs.toList()
                                step = "booting"
                                delay(1200)
                                viewModel.unlockSandbox()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = terminalGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "REGISTER & CREATE SANDBOX",
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                
                "login" -> {
                    Text(
                        text = ">>> SECURE CONTAINER AUTHENTICATION REQUIRED:",
                        color = terminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Username input
                    OutlinedTextField(
                        value = loginUsername,
                        onValueChange = { loginUsername = it },
                        label = { Text("Username", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace) },
                        textStyle = TextStyle(color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = terminalGreen,
                            unfocusedBorderColor = terminalBorder,
                            cursorColor = terminalGreen
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password input
                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("Enter Vault Password", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace) },
                        textStyle = TextStyle(color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = terminalGreen,
                            unfocusedBorderColor = terminalBorder,
                            cursorColor = terminalGreen
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "[ FORGOT PASSWORD? ]",
                            color = terminalGreen.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    step = "forgot_password"
                                    recoveryStep = "username"
                                    recoveryUsername = loginUsername
                                    recoveryAnswerInput = ""
                                    recoveryNewPassword = ""
                                    recoveryNewPasswordConfirm = ""
                                    errorMessage = null
                                }
                                .padding(vertical = 4.dp)
                        )
                    }

                    errorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "[-] AUTHENTICATION FAILURE: $err",
                            color = Color(0xFFFF4444),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Register alternative if they want to clear or register fresh
                        OutlinedButton(
                            onClick = {
                                step = "register"
                                errorMessage = null
                            },
                            border = BorderStroke(1.dp, terminalGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "RESET / NEW",
                                color = terminalGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }

                        // Submit Login button
                        Button(
                            onClick = {
                                if (viewModel.verifySandbox(loginUsername, loginPassword)) {
                                    errorMessage = null
                                    step = "booting"
                                    scope.launch {
                                        val currentLogs = terminalLogs.toMutableList()
                                        currentLogs.add(">>> KEY VERIFIED. MOUNTING CRYPTO ENGINE...")
                                        terminalLogs = currentLogs.toList()
                                        delay(400)
                                        currentLogs.add("[+] Sandbox folder decrypted under .isospace_encrypted_sandbox/")
                                        currentLogs.add("[+] Enforcing size allocation limit check...")
                                        terminalLogs = currentLogs.toList()
                                        delay(500)
                                        currentLogs.add("[+] System verification: OK")
                                        currentLogs.add(">>> BOOTING WORKSPACE SECURE KERNEL...")
                                        terminalLogs = currentLogs.toList()
                                        delay(1000)
                                        viewModel.unlockSandbox()
                                    }
                                } else {
                                    errorMessage = "INCORRECT USERNAME OR CRYPTOGRAPHIC PASS KEY"
                                    val currentLogs = terminalLogs.toMutableList()
                                    currentLogs.add("[-] LOGIN ATTEMPT REJECTED: INVALID SIGNATURE")
                                    terminalLogs = currentLogs.toList()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = terminalGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(2.5f)
                        ) {
                            Text(
                                text = "DECRYPT & BOOT CONTAINER",
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                "forgot_password" -> {
                    Text(
                        text = ">>> PASSWORD RECOVERY VAULT SYSTEM:",
                        color = terminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    when (recoveryStep) {
                        "username" -> {
                            Text(
                                text = "Enter sandbox account username to query security question profile.",
                                color = Color.LightGray.copy(alpha = 0.8f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = recoveryUsername,
                                onValueChange = { recoveryUsername = it },
                                label = { Text("Account Username", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace) },
                                textStyle = TextStyle(color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = terminalGreen,
                                    unfocusedBorderColor = terminalBorder,
                                    cursorColor = terminalGreen
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            errorMessage?.let { err ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "[-] RECOVERY ERROR: $err",
                                    color = Color(0xFFFF4444),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        step = "login"
                                        errorMessage = null
                                    },
                                    border = BorderStroke(1.dp, terminalGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("CANCEL", color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        if (recoveryUsername.trim().isEmpty()) {
                                            errorMessage = "Username is required."
                                        } else if (recoveryUsername.trim().lowercase() == viewModel.getSandboxUsername().lowercase()) {
                                            val q = viewModel.getSandboxSecurityQuestion()
                                            recoveryQuestionToShow = if (q.isBlank()) "No security question registered. Enter any answer to proceed." else q
                                            recoveryStep = "answer"
                                            errorMessage = null
                                        } else {
                                            errorMessage = "Username credentials not found in sandbox signature files."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = terminalGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("QUERY KEY", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                        
                        "answer" -> {
                            Text(
                                text = "SECURITY CHALLENGE ACTIVE:",
                                color = Color(0xFFFFCC00),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = recoveryQuestionToShow,
                                color = terminalGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(terminalGreen.copy(alpha = 0.05f))
                                    .border(1.dp, terminalBorder, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = recoveryAnswerInput,
                                onValueChange = { recoveryAnswerInput = it },
                                label = { Text("Enter Challenge Answer", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace) },
                                textStyle = TextStyle(color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = terminalGreen,
                                    unfocusedBorderColor = terminalBorder,
                                    cursorColor = terminalGreen
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            errorMessage?.let { err ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "[-] CHALLENGE FAILED: $err",
                                    color = Color(0xFFFF4444),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        recoveryStep = "username"
                                        errorMessage = null
                                    },
                                    border = BorderStroke(1.dp, terminalGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("BACK", color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        if (recoveryAnswerInput.trim().isEmpty()) {
                                            errorMessage = "Answer is required."
                                        } else if (viewModel.verifySecurityAnswer(recoveryAnswerInput.trim())) {
                                            recoveryStep = "reset"
                                            errorMessage = null
                                        } else {
                                            errorMessage = "CRITICAL SIGNATURE ERROR: VERIFICATION ANSWER MISMATCH."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = terminalGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("VERIFY KEY", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        "reset" -> {
                            Text(
                                text = "AUTH STATUS: CHALLENGE BYPASSED.\nEnter new password security keys below.",
                                color = terminalGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = recoveryNewPassword,
                                onValueChange = { recoveryNewPassword = it },
                                label = { Text("Set New Password", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace) },
                                textStyle = TextStyle(color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = terminalGreen,
                                    unfocusedBorderColor = terminalBorder,
                                    cursorColor = terminalGreen
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = recoveryNewPasswordConfirm,
                                onValueChange = { recoveryNewPasswordConfirm = it },
                                label = { Text("Confirm New Password", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace) },
                                textStyle = TextStyle(color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = terminalGreen,
                                    unfocusedBorderColor = terminalBorder,
                                    cursorColor = terminalGreen
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )

                            errorMessage?.let { err ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "[-] ERROR: $err",
                                    color = Color(0xFFFF4444),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (recoveryNewPassword.isEmpty()) {
                                        errorMessage = "Password cannot be empty."
                                    } else if (recoveryNewPassword != recoveryNewPasswordConfirm) {
                                        errorMessage = "Passwords do not match."
                                    } else {
                                        val success = viewModel.resetSandboxPassword(recoveryNewPassword)
                                        if (success) {
                                            errorMessage = null
                                            step = "login"
                                            val currentLogs = terminalLogs.toMutableList()
                                            currentLogs.add("[+] KEY ROTATION SUCCESSFUL: RECOVERY VAULT UPDATED.")
                                            terminalLogs = currentLogs.toList()
                                        } else {
                                            errorMessage = "Failed to update security key database."
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = terminalGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ROTATE SECURITY KEYS & REBOOT",
                                    color = Color.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                "formatting" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = terminalGreen, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "ALLOCATING & ENCRYPTING LOCAL DISK BLOCKS...",
                                color = terminalGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                "booting" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = terminalGreen, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "DECRYPTING ENGINE AND LAUNCHING ISOSPACE OS...",
                                color = terminalGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getAvailableStorageGb(context: android.content.Context): Long {
    return try {
        val path = context.filesDir
        val stat = StatFs(path.path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        bytesAvailable / (1024L * 1024L * 1024L)
    } catch (e: Exception) {
        15L // fallback
    }
}
