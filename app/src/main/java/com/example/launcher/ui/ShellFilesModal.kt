package com.example.launcher.ui

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.delay

private val UbuntuPurple = Color(0xFF2C001E)
private val UbuntuAubergine = Color(0xFF5E2750)
private val UbuntuWarmOrange = Color(0xFFE95420)
private val UbuntuDarkSlate = Color(0xFF222222)
private val UbuntuCardGray = Color(0xFF2D2D2D)

data class FolderShortcut(
    val title: String,
    val path: File,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun ShellFilesModal(onClose: () -> Unit) {
    val context = LocalContext.current

    // Custom Savers for MutableState of File instances to persist across configuration changes
    val fileStateSaver = remember {
        Saver<MutableState<File>, String>(
            save = { it.value.absolutePath },
            restore = { mutableStateOf(File(it)) }
        )
    }
    val nullableFileStateSaver = remember {
        Saver<MutableState<File?>, String>(
            save = { it.value?.absolutePath ?: "" },
            restore = { mutableStateOf(if (it.isEmpty()) null else File(it)) }
        )
    }

    // Directories lists
    val sandboxDir = context.filesDir.resolve(".isospace_encrypted_sandbox")
    val defaultHomePath = File(sandboxDir, "My Space")
    val notesDir = File(defaultHomePath, "Notes")
    val photosDir = File(defaultHomePath, "Photos")
    val audioDir = File(defaultHomePath, "Audio")
    val videosDir = File(defaultHomePath, "Videos")
    val docsDir = File(defaultHomePath, "Documents")

    val currentPath = rememberSaveable(saver = fileStateSaver) { mutableStateOf(defaultHomePath) }
    val currentFiles = remember { mutableStateOf<List<File>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Dialog state for new folders creations
    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }
    var newFolderNameInput by rememberSaveable { mutableStateOf("") }

    // Dialog state for file preview details
    val selectedPreviewFileState = rememberSaveable(saver = nullableFileStateSaver) { mutableStateOf<File?>(null) }
    var selectedPreviewFile by selectedPreviewFileState

    // Sidebar locations
    val homeDirs = remember {
        listOf(
            FolderShortcut("My Space", defaultHomePath, Icons.Default.Folder),
            FolderShortcut("Notes", notesDir, Icons.Default.StickyNote2),
            FolderShortcut("Photos", photosDir, Icons.Default.Image),
            FolderShortcut("Audio", audioDir, Icons.Default.Audiotrack),
            FolderShortcut("Videos", videosDir, Icons.Default.Videocam),
            FolderShortcut("Documents", docsDir, Icons.Default.Description)
        )
    }

    // Load files inside folder path
    val reloadCurrentFolder = {
        val target = currentPath.value
        if (!target.exists()) {
            target.mkdirs()
        }
        val fileArray = target.listFiles()
        currentFiles.value = fileArray?.toList()?.sortedWith(
            compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
        ) ?: emptyList()
    }

    // Triggers folder load
    LaunchedEffect(currentPath.value) {
        reloadCurrentFolder()
    }

    // Pre-populate sample text/log/json files for easy testing
    LaunchedEffect(Unit) {
        try {
            // Guarantee folders exist
            val notes = File(defaultHomePath, "Notes")
            val photos = File(defaultHomePath, "Photos")
            val audio = File(defaultHomePath, "Audio")
            val videos = File(defaultHomePath, "Videos")
            val docs = File(defaultHomePath, "Documents")

            listOf(notes, photos, audio, videos, docs).forEach {
                if (!it.exists()) it.mkdirs()
            }
            
            // Seed Documents
            val welcomeFile = File(docs, "welcome_to_isospace.txt")
            if (!welcomeFile.exists()) {
                welcomeFile.writeText("""
                    ==================================================
                    * WELCOME TO ISOSPACE SECURE CRYPTO-SANDBOX VAULT *
                    ==================================================
                    
                    This Files Explorer allows you to view, play, 
                    and manage documents and multimedia securely.
                    
                    All directories inside 'My Space' reside in an 
                    encrypted sandboxed partition inaccessible to other 
                    unauthorized applications or malware on the device.
                    
                    Features:
                    1. Notes: Write, read, and search secret local files.
                    2. Photos: Standard secure static picture formats.
                    3. Audio: Cryptographically locked voice memos & cues.
                    4. Videos: Direct streaming sandboxed container files.
                """.trimIndent())
            }
            
            val infoFile = File(docs, "system_info.json")
            if (!infoFile.exists()) {
                infoFile.writeText("""
                    {
                      "os_name": "ISOSpace Secure Mobile Shell",
                      "cryptography": "AES-256-GCM / PBKDF2 Key Derivation",
                      "sandbox_status": "LOCKED_PRE_DECRYPT",
                      "security_grade": "Military Shield v4",
                      "owner": "isospace_authorized_user"
                    }
                """.trimIndent())
            }

            // Seed Notes
            val note1 = File(notes, "sandbox_credentials.txt")
            if (!note1.exists()) {
                note1.writeText("""
                    =======================================
                    ISOSPACE USER RECOVERY NOTES
                    =======================================
                    Keep this local node secure.
                    All application databases are bound in 
                    /data/user/0/com.example/files/.isospace_encrypted_sandbox
                    No cloud leaks or external telemetry.
                """.trimIndent())
            }
            val note2 = File(notes, "todo_list.txt")
            if (!note2.exists()) {
                note2.writeText("""
                    [ ] 1. Allocate block size limits (10G/20G/30G)
                    [x] 2. Establish cryptographic key handshakes
                    [ ] 3. Mount secure physical filesystem
                    [ ] 4. Run 'uname' and terminal audits
                """.trimIndent())
            }

            // Seed Photos
            val photo1 = File(photos, "terminal_screenshot.jpg")
            if (!photo1.exists()) {
                photo1.writeText("MOCK_IMAGE_DATA_AES_ENCRYPTED_HEADER_0x4F92B1")
            }
            val photo2 = File(photos, "personal_id.png")
            if (!photo2.exists()) {
                photo2.writeText("MOCK_IMAGE_DATA_ENCRYPTED_USER_IDENTIFICATION")
            }

            // Seed Audio
            val audio1 = File(audio, "binaural_leak_alarm.mp3")
            if (!audio1.exists()) {
                audio1.writeText("MOCK_AUDIO_DATA_CRYPTO_VAULT_ALARM_CUE")
            }

            // Seed Videos
            val video1 = File(videos, "surveillance_footage.mp4")
            if (!video1.exists()) {
                video1.writeText("MOCK_VIDEO_DATA_STREAMING_SECURITY_CAMERA_FEED")
            }
            
            reloadCurrentFolder()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Filter files list by search query
    val visibleFiles = remember(currentFiles.value, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) currentFiles.value
        else currentFiles.value.filter { it.name.contains(query, ignoreCase = true) }
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
                .testTag("files_app_dialog")
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompact = maxWidth < 560.dp
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isCompact) {
                        // Compact stacked toolbar for mobile screen responsiveness
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(UbuntuPurple.copy(alpha = 0.45f), UbuntuAubergine.copy(alpha = 0.15f))))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            // Row 1: Logo & Title and Close Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = UbuntuWarmOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Files Explorer",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("close_files_button")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 2: Controls (Back, Path, New Folder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val parentFile = currentPath.value.parentFile
                                // Back button
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(if (parentFile != null) Color(0xFF333333) else Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
                                        .clickable(enabled = parentFile != null) {
                                            if (parentFile != null && parentFile.absolutePath.startsWith(defaultHomePath.parent ?: "")) {
                                                currentPath.value = parentFile
                                            } else {
                                                Toast.makeText(context, "Cannot ascend past root!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = if (parentFile != null) Color.White else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Path display container (takes up remaining horizontal space in this row)
                                Box(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E1E1E))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = currentPath.value.name.ifBlank { "Root" },
                                        color = UbuntuWarmOrange,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // New folder creator button
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(UbuntuWarmOrange, RoundedCornerShape(6.dp))
                                        .clickable { showNewFolderDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = "New Folder",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 3: Compact full-width Search/Filter Box
                            CompactSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Search directory contents...",
                                borderColor = UbuntuWarmOrange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            )
                        }
                    } else {
                        // Wide regular single-row layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(UbuntuPurple.copy(alpha = 0.45f), UbuntuAubergine.copy(alpha = 0.15f))))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = UbuntuWarmOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Files Explorer",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "ISOSpace virtual and native storage index",
                                    color = Color.LightGray.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }

                            // Top navigation flow bar
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Back directory arrow
                                val parentFile = currentPath.value.parentFile
                                IconButton(
                                    onClick = {
                                        if (parentFile != null && parentFile.absolutePath.startsWith(defaultHomePath.parent ?: "")) {
                                            currentPath.value = parentFile
                                        } else {
                                            Toast.makeText(context, "Cannot ascend past root!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = parentFile != null,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(Color(0xFF333333), RoundedCornerShape(6.dp))
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                // Path text display container
                                Box(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E1E1E))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = currentPath.value.name.ifBlank { "Virtual Root" },
                                        color = UbuntuWarmOrange,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // New folder picker button
                                IconButton(
                                    onClick = { showNewFolderDialog = true },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(UbuntuWarmOrange, RoundedCornerShape(6.dp))
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                // Search box input using our custom CompactSearchField
                                CompactSearchField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = "Filter directories...",
                                    borderColor = UbuntuWarmOrange,
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(34.dp)
                                )

                                // Close application window
                                IconButton(onClick = onClose, modifier = Modifier.size(32.dp).testTag("close_files_button")) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }
                        }
                    }

                    // SUBHEADER METADATA
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Viewing: ${currentPath.value.absolutePath}",
                            fontSize = 9.sp,
                            color = Color.LightGray.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${visibleFiles.size} items present",
                            fontSize = 9.sp,
                            color = UbuntuWarmOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isCompact) {
                        // Places chips horizontal scrollable Row for compact mobile screen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1B1B1B))
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            homeDirs.forEach { sidebar ->
                                val isSelected = currentPath.value.absolutePath == sidebar.path.absolutePath
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) UbuntuWarmOrange.copy(alpha = 0.25f) 
                                            else Color(0xFF2E2E2E)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) UbuntuWarmOrange else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            currentPath.value = sidebar.path
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = sidebar.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) UbuntuWarmOrange else Color.LightGray,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sidebar.title,
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // BOTTOM MAIN BODY - SPLIT VIEW SIDEBAR & DIRECTORY GRID
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF151515))
                    ) {
                        // LEFT COLUMN- SIDEBAR OPTIONS
                        if (!isCompact) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(200.dp)
                                    .background(Color(0xFF1B1B1B))
                                    .drawBehind { drawLine(Color(0x1FFFFFFF), Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = 1.dp.toPx()) }
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "SECURE SANDBOX STORAGE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = UbuntuWarmOrange,
                                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                                )

                                // Root Sandbox Title: "My Space"
                                val isMySpaceSelected = currentPath.value.absolutePath == defaultHomePath.absolutePath
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isMySpaceSelected) UbuntuWarmOrange.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { currentPath.value = defaultHomePath }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isMySpaceSelected) UbuntuWarmOrange else Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "My Space",
                                        color = if (isMySpaceSelected) Color.White else Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // Interactive Subdirectories structured exactly as requested in image
                                val sidebarChildren = listOf(
                                    Triple("Notes", notesDir, Icons.Default.StickyNote2),
                                    Triple("Photos", photosDir, Icons.Default.Image),
                                    Triple("Audio", audioDir, Icons.Default.Audiotrack),
                                    Triple("Videos", videosDir, Icons.Default.Videocam),
                                    Triple("Documents", docsDir, Icons.Default.Description)
                                )

                                sidebarChildren.forEachIndexed { index, (title, path, icon) ->
                                    val prefix = if (index == sidebarChildren.lastIndex) "└── " else "├── "
                                    val isSelected = currentPath.value.absolutePath == path.absolutePath
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) UbuntuWarmOrange.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { currentPath.value = path }
                                            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = prefix,
                                            color = Color(0xFF888888),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) UbuntuWarmOrange else Color.LightGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = title,
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                    // RIGHT ROW - GRID DIRECTORY CONTAINER
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(14.dp)
                    ) {
                        if (visibleFiles.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("This folder is empty", color = Color.Gray, fontSize = 12.sp)
                                Text("Create new subdirectories using the 'New Folder' button on top.", color = Color.DarkGray, fontSize = 9.sp, textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 90.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(visibleFiles) { file ->
                                    val isFolder = file.isDirectory
                                    val extension = file.extension.lowercase()

                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF222222))
                                            .border(1.dp, Color(0x0EFFFFFF), RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (isFolder) {
                                                    currentPath.value = file
                                                } else {
                                                    selectedPreviewFile = file
                                                }
                                            }
                                            .padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // Image preview placeholder or Coil loaders
                                        if (!isFolder && (extension == "png" || extension == "jpg" || extension == "jpeg")) {
                                            AsyncImage(
                                                model = file,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                        } else {
                                            Icon(
                                                imageVector = when {
                                                    isFolder -> Icons.Default.Folder
                                                    extension == "mp4" || extension == "mkv" || extension == "avi" -> Icons.Default.Movie
                                                    extension == "txt" || extension == "log" -> Icons.Default.Description
                                                    extension == "zip" || extension == "rar" -> Icons.Default.Drafts
                                                    else -> Icons.Default.InsertDriveFile
                                                },
                                                tint = when {
                                                    isFolder -> UbuntuWarmOrange
                                                    extension == "mp4" || extension == "mkv" -> Color(0xFF2196F3)
                                                    extension == "txt" || extension == "log" -> Color(0xFF4CAF50)
                                                    else -> Color.LightGray
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(42.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = file.name,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    // CREATE FOLDER DIALOG
    if (showNewFolderDialog) {
        Dialog(onDismissRequest = { showNewFolderDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = UbuntuCardGray),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Create New Directory", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newFolderNameInput,
                        onValueChange = { newFolderNameInput = it },
                        placeholder = { Text("directory_name", fontSize = 11.sp, color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = UbuntuWarmOrange,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showNewFolderDialog = false }) {
                            Text("Cancel", color = Color.Gray, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                val name = newFolderNameInput.trim()
                                if (name.isNotBlank()) {
                                    val fileDir = File(currentPath.value, name)
                                    if (fileDir.mkdirs()) {
                                        Toast.makeText(context, "$name directory created!", Toast.LENGTH_SHORT).show()
                                        reloadCurrentFolder()
                                    } else {
                                        Toast.makeText(context, "Directory creation failed (I/O error)", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                showNewFolderDialog = false
                                newFolderNameInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = UbuntuWarmOrange)
                        ) {
                            Text("Create Folder", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // PREVIEW DETAILS DIALOG
    if (selectedPreviewFile != null) {
        val file = selectedPreviewFile!!
        val dateString = SimpleDateFormat("E, d MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(file.lastModified()))
        val sizeString = getFileSizeString(file)

        val extension = file.extension.lowercase()
        val isText = extension in listOf("txt", "log", "json", "xml", "html", "js", "kt", "java", "css", "md")
        val isImage = extension in listOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
        val isAudio = extension in listOf("mp3", "wav", "ogg", "aac", "m4a")
        val isVideo = extension in listOf("mp4", "mkv", "avi", "3gp", "webm")
        val isPreviewable = isText || isImage || isAudio || isVideo
        val dialogWidth = if (isPreviewable) 500.dp else 320.dp

        // Text preview loader
        var textContent by remember(file) { mutableStateOf<String?>(null) }
        var textError by remember(file) { mutableStateOf<String?>(null) }
        LaunchedEffect(file) {
            if (isText) {
                try {
                    val limit = 50 * 1024
                    if (file.length() > limit) {
                        textContent = file.inputStream().use { stream ->
                            val bytes = ByteArray(limit)
                            val read = stream.read(bytes)
                            String(bytes, 0, read) + "\n... [Truncated due to size] ..."
                        }
                    } else {
                        textContent = file.readText()
                    }
                } catch (e: Exception) {
                    textError = e.message ?: "Failed to read text file"
                }
            }
        }

        // Audio player states
        var audioPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
        var isAudioPlaying by remember { mutableStateOf(false) }
        var audioProgress by remember { mutableStateOf(0f) }
        var audioDuration by remember { mutableStateOf(0) }
        var audioPosition by remember { mutableStateOf(0) }

        LaunchedEffect(isAudioPlaying, audioPlayer) {
            while (isAudioPlaying && audioPlayer != null) {
                try {
                    val player = audioPlayer
                    if (player != null && player.isPlaying) {
                        audioPosition = player.currentPosition
                        audioDuration = player.duration
                        audioProgress = if (audioDuration > 0) audioPosition.toFloat() / audioDuration.toFloat() else 0f
                    }
                } catch (e: Exception) {}
                delay(500)
            }
        }

        DisposableEffect(file) {
            onDispose {
                audioPlayer?.release()
                audioPlayer = null
                isAudioPlaying = false
            }
        }

        // Video player states
        var videoPlaying by remember { mutableStateOf(false) }
        var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
        var isVideoBuffering by remember { mutableStateOf(true) }

        DisposableEffect(file) {
            onDispose {
                try {
                    videoViewInstance?.stopPlayback()
                } catch (e: Exception) {}
            }
        }

        Dialog(
            onDismissRequest = { selectedPreviewFile = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = UbuntuCardGray),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier
                    .widthIn(max = dialogWidth)
                    .fillMaxWidth(0.95f)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPreviewable) "Files File Preview" else "Files File Properties",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { selectedPreviewFile = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isPreviewable) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isText -> {
                                    if (textContent != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp)
                                                .verticalScroll(rememberScrollState())
                                                .horizontalScroll(rememberScrollState())
                                        ) {
                                            Text(
                                                text = textContent!!,
                                                color = Color(0xFF4AF626), // Linux terminal green tint
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        }
                                    } else if (textError != null) {
                                        Text(text = "Error reading text: $textError", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
                                    } else {
                                        CircularProgressIndicator(color = UbuntuWarmOrange)
                                    }
                                }
                                isImage -> {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = file.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                isAudio -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(27.dp))
                                                .background(UbuntuWarmOrange.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = UbuntuWarmOrange,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = file.name,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        // Player controls
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        if (audioPlayer == null) {
                                                            audioPlayer = MediaPlayer().apply {
                                                                setDataSource(file.absolutePath)
                                                                prepare()
                                                                setOnCompletionListener {
                                                                    isAudioPlaying = false
                                                                    audioProgress = 0f
                                                                    audioPosition = 0
                                                                }
                                                                start()
                                                            }
                                                            isAudioPlaying = true
                                                            audioDuration = audioPlayer?.duration ?: 0
                                                        } else {
                                                            val player = audioPlayer!!
                                                            if (player.isPlaying) {
                                                                player.pause()
                                                                isAudioPlaying = false
                                                            } else {
                                                                player.start()
                                                                isAudioPlaying = true
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(18.dp))
                                                    .background(UbuntuWarmOrange)
                                            ) {
                                                Icon(
                                                    imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Play/Pause",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Progress row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = formatMs(audioPosition),
                                                color = Color.Gray,
                                                fontSize = 9.sp
                                            )
                                            Slider(
                                                value = audioProgress,
                                                onValueChange = { progress ->
                                                    audioProgress = progress
                                                    audioPlayer?.let { player ->
                                                        val target = (progress * audioDuration).toInt()
                                                        player.seekTo(target)
                                                        audioPosition = target
                                                    }
                                                },
                                                colors = SliderDefaults.colors(
                                                    thumbColor = UbuntuWarmOrange,
                                                    activeTrackColor = UbuntuWarmOrange,
                                                    inactiveTrackColor = Color.DarkGray
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 8.dp)
                                            )
                                            Text(
                                                text = formatMs(audioDuration),
                                                color = Color.Gray,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                                isVideo -> {
                                    AndroidView(
                                        factory = { ctx ->
                                            VideoView(ctx).apply {
                                                videoViewInstance = this
                                                try {
                                                    setVideoURI(Uri.fromFile(file))
                                                    setOnPreparedListener { mp ->
                                                        isVideoBuffering = false
                                                        mp.isLooping = true
                                                        start()
                                                        videoPlaying = true
                                                    }
                                                    setOnErrorListener { _, _, _ ->
                                                        isVideoBuffering = false
                                                        false
                                                    }
                                                } catch (e: Exception) {
                                                    isVideoBuffering = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isVideoBuffering) {
                                        CircularProgressIndicator(color = UbuntuWarmOrange)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                isVideo -> Icons.Default.Movie
                                isText -> Icons.Default.Description
                                isAudio -> Icons.Default.MusicNote
                                isImage -> Icons.Default.Image
                                else -> Icons.Default.InsertDriveFile
                            },
                            tint = UbuntuWarmOrange,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(file.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Format Type: ${file.extension.uppercase()}", color = Color.Gray, fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("FILE PARAMETERS", color = UbuntuWarmOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        MetadataPropertyRow("Full Path:", file.absolutePath)
                        MetadataPropertyRow("Byte Size:", sizeString)
                        MetadataPropertyRow("Modified:", dateString)
                        MetadataPropertyRow("Directory:", if (file.isDirectory) "Yes" else "No")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Delete Button
                        Button(
                            onClick = {
                                if (file.delete()) {
                                    Toast.makeText(context, "${file.name} deleted successfully!", Toast.LENGTH_SHORT).show()
                                    reloadCurrentFolder()
                                } else {
                                    Toast.makeText(context, "System block. Could not delete physical file.", Toast.LENGTH_SHORT).show()
                                }
                                selectedPreviewFile = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete File", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { selectedPreviewFile = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Dismiss", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataPropertyRow(label: String, valText: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 9.sp)
        Text(valText, color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun getFileSizeString(file: File): String {
    val bytes = file.length()
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024
    if (kb < 1024) return "$kb KB"
    val mb = kb / 1024
    return "$mb MB"
}

private fun formatMs(ms: Int): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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
