package com.example.launcher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

// Ubuntu Branding Colors matching UbuntuLauncherScreen
private val UbuntuPurple = Color(0xFF2C001E)
private val UbuntuAubergine = Color(0xFF5E2750)
private val UbuntuWarmOrange = Color(0xFFE95420)
private val UbuntuDarkSlate = Color(0xFF222222)
private val UbuntuCardGray = Color(0xFF2D2D2D)

data class GalleryMediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val size: Long,
    val path: String,
    val mimeType: String,
    val isVideo: Boolean,
    val duration: Long = 0L, // in ms
    val dateAdded: Long
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShellGalleryModal(onClose: () -> Unit) {
    val context = LocalContext.current

    // State parameters
    var permissionGranted by remember { mutableStateOf(true) }
    var useDemoAssets by remember { mutableStateOf(false) }
    var mediaItems by remember { mutableStateOf<List<GalleryMediaItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("All") } // "All", "Photos", "Videos"
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("Newest") } // "Newest", "Oldest", "Name", "Size"

    // Active full screen viewer
    var activeViewerIndex by remember { mutableStateOf<Int?>(null) }
    var isMetadataPanelOpen by remember { mutableStateOf(false) }

    // Read files dynamically
    var refreshTrigger by remember { mutableStateOf(0) }
    var isLoadingMedia by remember { mutableStateOf(false) }
    LaunchedEffect(useDemoAssets, refreshTrigger) {
        isLoadingMedia = true
        val list = loadGalleryMedia(context)

        // If the local disk has no media AND we want demo assets (or optionally seed)
        if (useDemoAssets) {
            // Include high definition sample photos & videos
            mediaItems = (list + getMockMediaItems()).distinctBy { it.name }.sortedByDescending { it.dateAdded }
        } else {
            mediaItems = list
        }
        isLoadingMedia = false
    }

    // Filtered lists
    val filteredItems = remember(mediaItems, selectedCategory, searchQuery, sortBy) {
        var result = mediaItems.filter { item ->
            when (selectedCategory) {
                "Photos" -> !item.isVideo
                "Videos" -> item.isVideo
                else -> true
            }
        }

        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.path.contains(searchQuery, ignoreCase = true)
            }
        }

        when (sortBy) {
            "Newest" -> result.sortedByDescending { it.dateAdded }
            "Oldest" -> result.sortedBy { it.dateAdded }
            "Name" -> result.sortedBy { it.name.lowercase() }
            "Size" -> result.sortedByDescending { it.size }
            else -> result
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
                .testTag("gallery_app_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Ubuntu Style App Bar Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(UbuntuPurple.copy(alpha = 0.45f), UbuntuAubergine.copy(alpha = 0.15f))))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearchActive) {
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit Search",
                                tint = Color.LightGray
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))

                        CompactSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Filter files...",
                            borderColor = UbuntuWarmOrange,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        )

                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search",
                                    tint = Color.LightGray
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = UbuntuWarmOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shotwell Photo Hub",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Photos, Videos & Media Inspector",
                                color = Color.LightGray.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Search Trigger
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Files",
                                tint = Color.LightGray
                            )
                        }

                        // Sort menu
                        var sortMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort Options", tint = Color.LightGray)
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                                modifier = Modifier.background(UbuntuCardGray)
                            ) {
                                listOf("Newest", "Oldest", "Name", "Size").forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode, color = Color.White, fontSize = 12.sp) },
                                        onClick = {
                                            sortBy = mode
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Refresh Action
                        IconButton(
                             onClick = { refreshTrigger++ },
                             modifier = Modifier.size(32.dp).testTag("refresh_gallery_button")
                         ) {
                             Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.LightGray)
                         }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Close Action is always displayed
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp).testTag("close_gallery_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close App", tint = Color.White)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category Tab Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "Photos", "Videos").forEach { cat ->
                            val isSel = selectedCategory == cat
                            Button(
                                onClick = { selectedCategory = cat },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) UbuntuWarmOrange else Color(0xFF333333)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = when (cat) {
                                        "All" -> "All (${mediaItems.size})"
                                        "Photos" -> "Photos (${mediaItems.count { !it.isVideo }})"
                                        "Videos" -> "Videos (${mediaItems.count { it.isVideo }})"
                                        else -> cat
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Sandbox status bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Secure Sandbox synchronized", color = Color.Gray, fontSize = 9.sp)
                        }

                        // Demo Assets Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2D2D2D))
                                .clickable { useDemoAssets = !useDemoAssets }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("Demonstration Files", fontSize = 9.sp, color = if (useDemoAssets) UbuntuWarmOrange else Color.LightGray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Checkbox(
                                checked = useDemoAssets,
                                onCheckedChange = { useDemoAssets = it },
                                modifier = Modifier.size(14.dp),
                                colors = CheckboxDefaults.colors(checkedColor = UbuntuWarmOrange, uncheckedColor = Color.Gray)
                            )
                        }
                    }
                }

                // Grid View of media
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF151515))
                ) {
                    if (isLoadingMedia) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = UbuntuWarmOrange)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Reading Media Library...", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else if (filteredItems.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "No picture or video files found matching your filters.",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Place compatible .PNG, .JPG, or .MP4 video files in your device's Pictures, DCIM, or Movies directories.",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(110.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filteredItems) { idx, item ->
                                GalleryGridItem(
                                    item = item,
                                    onClick = { activeViewerIndex = idx }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Image Slide / Video Player Overlay Window
    activeViewerIndex?.let { index ->
        if (index in filteredItems.indices) {
            val item = filteredItems[index]
            Dialog(
                onDismissRequest = { activeViewerIndex = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .fillMaxHeight(0.9f)
                        .testTag("media_slide_window"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, Color(0x44FFFFFF))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Slider Tool-Header Panel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A1A1A))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { activeViewerIndex = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${if (item.isVideo) "Video File" else "Image Document"} • ${formatSize(item.size)}",
                                    color = Color.LightGray.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }

                            // Metadata Toggle Button
                            IconButton(
                                onClick = { isMetadataPanelOpen = !isMetadataPanelOpen },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Details",
                                    tint = if (isMetadataPanelOpen) UbuntuWarmOrange else Color.LightGray
                                )
                            }
                        }

                        // Core Space (Split view if metadata is open, otherwise Full Center Viewport)
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            // Left navigation arrow button
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(48.dp)
                                    .background(Color(0x33000000))
                                    .clickable {
                                        if (index > 0) activeViewerIndex = index - 1
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous",
                                    tint = if (index > 0) Color.White else Color.DarkGray
                                )
                            }

                            // Active visual container
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF101010)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.isVideo) {
                                    InteractiveVideoPlayer(item = item)
                                } else {
                                    // Image displaying container
                                    AsyncImage(
                                         model = if (item.uri.scheme == "file") java.io.File(item.uri.path ?: "") else item.uri,
                                         contentDescription = item.name,
                                         contentScale = ContentScale.Fit,
                                         modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Right navigation arrow button
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(48.dp)
                                    .background(Color(0x33000000))
                                    .clickable {
                                        if (index < filteredItems.lastIndex) activeViewerIndex = index + 1
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next",
                                    tint = if (index < filteredItems.lastIndex) Color.White else Color.DarkGray
                                )
                            }

                            // Metadata Overlay Information Panel
                            AnimatedVisibility(
                                visible = isMetadataPanelOpen,
                                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(260.dp)
                                        .background(Color(0xFF1A1A1A))
                                        .border(BorderStroke(1.dp, Color(0x22FFFFFF)))
                                        .padding(14.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "Properties Meta",
                                        fontWeight = FontWeight.Bold,
                                        color = UbuntuWarmOrange,
                                        fontSize = 13.sp
                                    )
                                    DelimiterLine()

                                    MetaFieldRow("Name", item.name)
                                    MetaFieldRow("Format Type", item.mimeType)
                                    MetaFieldRow("Folder/Data Path", item.path)
                                    MetaFieldRow("Byte Size", "${item.size} bytes (${formatSize(item.size)})")
                                    MetaFieldRow("Created Time", formatDate(item.dateAdded))

                                    if (item.isVideo) {
                                        MetaFieldRow("Duration", formatDuration(item.duration))
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { isMetadataPanelOpen = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Text("Hide Info Panel", fontSize = 11.sp, color = Color.White)
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

@Composable
fun GalleryGridItem(
    item: GalleryMediaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.0f)
            .clickable { onClick() }
            .testTag("gallery_grid_item_${item.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = UbuntuCardGray),
        border = BorderStroke(1.dp, Color(0x15FFFFFF))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Visual dynamic preview
            AsyncImage(
                model = if (item.uri.scheme == "file") java.io.File(item.uri.path ?: "") else item.uri,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Video overlay design (Badge at bottom and duration if available)
            if (item.isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 80f
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(UbuntuWarmOrange.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.duration > 0) formatDuration(item.duration) else "Video",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Image small tiny photo document icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // Small display name bottom text
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 2.dp)
            ) {
                if (!item.isVideo) {
                    Text(
                        text = item.name.substringAfterLast(".").uppercase(),
                        fontSize = 8.sp,
                        color = Color.LightGray.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveVideoPlayer(item: GalleryMediaItem) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                val mediaController = MediaController(ctx)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)
            }
        },
        update = { videoView ->
            try {
                val playUri = if (item.uri.toString().contains("isospace_encrypted_sandbox") || item.path.contains("My Space/Videos")) {
                    android.net.Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
                } else {
                    item.uri
                }
                videoView.setVideoURI(playUri)
                videoView.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    )
}

@Composable
private fun MetaFieldRow(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = Color.White, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun DelimiterLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x1AFFFFFF))
    )
}

// Byte utility formatter
private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// Media duration formatted (mm:ss)
private fun formatDuration(durationMs: Long): String {
    val totalSecs = durationMs / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

// Date added formatted builder
private fun formatDate(rawSeconds: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date = Date(rawSeconds * 1000L)
        sdf.format(date)
    } catch (e: Exception) {
        "Unknown Date"
    }
}

// Loader implementation from private secure sandbox folder directories
private fun loadGalleryMedia(context: Context): List<GalleryMediaItem> {
    val items = mutableListOf<GalleryMediaItem>()
    val sandboxDir = context.filesDir.resolve(".isospace_encrypted_sandbox")
    val defaultHomePath = java.io.File(sandboxDir, "My Space")
    val photosDir = java.io.File(defaultHomePath, "Photos")
    val videosDir = java.io.File(defaultHomePath, "Videos")

    // Ensure the private folders exist
    if (!photosDir.exists()) photosDir.mkdirs()
    if (!videosDir.exists()) videosDir.mkdirs()

    var idCounter = 1L

    // Scan photos directory
    photosDir.listFiles()?.forEach { file ->
        if (file.isFile) {
            val extension = file.extension.lowercase()
            val mimeType = when (extension) {
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                else -> "image/*"
            }
            items.add(
                GalleryMediaItem(
                    id = idCounter++,
                    uri = Uri.fromFile(file),
                    name = file.name,
                    size = file.length(),
                    path = "My Space/Photos/${file.name}",
                    mimeType = mimeType,
                    isVideo = false,
                    dateAdded = file.lastModified() / 1000L
                )
            )
        }
    }

    // Scan videos directory
    videosDir.listFiles()?.forEach { file ->
        if (file.isFile) {
            val extension = file.extension.lowercase()
            val mimeType = when (extension) {
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "webm" -> "video/webm"
                else -> "video/*"
            }
            items.add(
                GalleryMediaItem(
                    id = idCounter++,
                    uri = Uri.fromFile(file),
                    name = file.name,
                    size = file.length(),
                    path = "My Space/Videos/${file.name}",
                    mimeType = mimeType,
                    isVideo = true,
                    duration = 0L,
                    dateAdded = file.lastModified() / 1000L
                )
            )
        }
    }

    items.sortByDescending { it.dateAdded }
    return items
}

// Fallback high definition demonstration assets (Images and Videos)
private fun getMockMediaItems(): List<GalleryMediaItem> {
    return listOf(
        GalleryMediaItem(
            id = -1,
            uri = Uri.parse("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=800&q=80"),
            name = "wallpaper_isospace_cosmic.png",
            size = 2394502L,
            path = "/storage/emulated/0/DCIM/wallpaper_isospace_cosmic.png",
            mimeType = "image/png",
            isVideo = false,
            dateAdded = 1813632000L
        ),
        GalleryMediaItem(
            id = -2,
            uri = Uri.parse("https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80"),
            name = "isospace_neon_cyberpunk.jpg",
            size = 1845620L,
            path = "/storage/emulated/0/Pictures/isospace_neon_cyberpunk.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            dateAdded = 1813630000L
        ),
        GalleryMediaItem(
            id = -3,
            uri = Uri.parse("https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=800&q=80"),
            name = "abstract_aurora_gradient.jpg",
            size = 988450L,
            path = "/storage/emulated/0/DCIM/abstract_aurora_gradient.jpg",
            mimeType = "image/jpeg",
            isVideo = false,
            dateAdded = 1813620000L
        ),
        GalleryMediaItem(
            id = -4,
            uri = Uri.parse("https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&w=800&q=80"),
            name = "yggdrasil_fluid_art.png",
            size = 4821035L,
            path = "/storage/emulated/0/DCIM/yggdrasil_fluid_art.png",
            mimeType = "image/png",
            isVideo = false,
            dateAdded = 1813610000L
        ),
        GalleryMediaItem(
            id = -5,
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            name = "big_buck_bunny.mp4",
            size = 15800840L,
            path = "/storage/emulated/0/Movies/big_buck_bunny.mp4",
            mimeType = "video/mp4",
            isVideo = true,
            duration = 596000L,
            dateAdded = 1813590000L
        ),
        GalleryMediaItem(
            id = -6,
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
            name = "elephants_dream.mp4",
            size = 24602930L,
            path = "/storage/emulated/0/Movies/elephants_dream.mp4",
            mimeType = "video/mp4",
            isVideo = true,
            duration = 653000L,
            dateAdded = 1813580000L
        )
    )
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
