package com.example.launcher.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.widget.VideoView
import kotlinx.coroutines.delay
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import java.io.File
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.net.URL

// VLC Orange and Ubuntu Dark Theme
private val VlcOrange = Color(0xFFFF5500)
private val UbuntuPurple = Color(0xFF2C001E)
private val VlcDarkBg = Color(0xFF151515)
private val VlcPanelBg = Color(0xFF1E1E1E)
private val VlcCardBg = Color(0xFF2A2A2A)

enum class VlcTab : java.io.Serializable {
    VIDEO, AUDIO, STREAM, INFO
}

data class MediaItem(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val duration: String,
    val type: String, // "video" or "audio"
    val artist: String = "VLC Library"
) : java.io.Serializable

@Composable
fun ShellVlcModal(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentTab by rememberSaveable { mutableStateOf(VlcTab.VIDEO) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    // Custom stream URL input
    var streamUrlInput by rememberSaveable { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4") }
    
    // Selected / Active Playing item
    var activeMediaItem by rememberSaveable { mutableStateOf<MediaItem?>(null) }
    
    // Scan triggers and states
    var triggerScanCount by rememberSaveable { mutableStateOf(0) }
    
    // Built-in media library scanned dynamically
    val mediaLibrary = remember(triggerScanCount) {
        scanForMediaFiles(context)
    }

    // Download and Import states
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val resolvedTitle = selectedUri.lastPathSegment?.substringAfterLast("/") ?: "Imported File"
            val isAudioType = selectedUri.toString().contains("audio", ignoreCase = true) || 
                             resolvedTitle.endsWith(".mp3", ignoreCase = true) ||
                             resolvedTitle.endsWith(".wav", ignoreCase = true)
            activeMediaItem = MediaItem(
                id = selectedUri.toString(),
                title = resolvedTitle,
                description = "Imported from Android device storage",
                url = selectedUri.toString(),
                duration = "00:00",
                type = if (isAudioType) "audio" else "video",
                artist = "External Media"
            )
        }
    }
    
    val onDownloadSampleClick: (Boolean) -> Unit = { isAudio ->
        scope.launch {
            isDownloading = true
            downloadProgress = 0f
            val defaultHomePath = Environment.getExternalStorageDirectory() ?: context.filesDir
            val destFolder = if (isAudio) File(defaultHomePath, "Download") else File(defaultHomePath, "DCIM")
            val fileName = if (isAudio) "vlc_sample_piano.mp3" else "vlc_sample_fun.mp4"
            val destFile = File(destFolder, fileName)
            val downloadUrl = if (isAudio) {
                "https://ccrma.stanford.edu/~jos/mp3/pno-cs.mp3"
            } else {
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
            }
            
            val success = downloadSampleFile(downloadUrl, destFile) { prg ->
                downloadProgress = prg
            }
            
            isDownloading = false
            if (success) {
                Toast.makeText(context, "Sample media file saved to storage!", Toast.LENGTH_LONG).show()
                triggerScanCount++ // reload scan!
            } else {
                Toast.makeText(context, "Connection Error. Could not download sample media.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Filtered lists
    val filteredVideos = mediaLibrary.filter {
        it.type == "video" && (it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
    }
    
    val filteredAudios = mediaLibrary.filter {
        it.type == "audio" && (it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true))
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xEC0E0E12))
                .border(
                    width = 1.5.dp,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            VlcOrange.copy(alpha = 0.8f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("vlc_app_dialog")
        ) {
            val isCompact = maxWidth < 550.dp
            
            Column(modifier = Modifier.fillMaxSize()) {
                
                // TOP MAIN HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(UbuntuPurple.copy(alpha = 0.45f), Color(0xFF5E2750).copy(alpha = 0.15f))))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Cone Emblem
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VlcOrange)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Adaptive simulation of cone visual
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ISOSpace Media Player",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "ISOSpace High-Performance Open-Source Player",
                            color = Color.LightGray.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                    
                    // Close button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_vlc_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close VLC",
                            tint = Color.White
                        )
                    }
                }

                // TAB MENU & SEARCH ROW (Adapts defensively)
                if (isCompact) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VlcPanelBg)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VlcTabButton("Videos", currentTab == VlcTab.VIDEO) { currentTab = VlcTab.VIDEO }
                            VlcTabButton("Audios", currentTab == VlcTab.AUDIO) { currentTab = VlcTab.AUDIO }
                            VlcTabButton("Network Stream", currentTab == VlcTab.STREAM) { currentTab = VlcTab.STREAM }
                            VlcTabButton("About VLC", currentTab == VlcTab.INFO) { currentTab = VlcTab.INFO }
                        }
                        
                        CompactSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search VLC media library...",
                            borderColor = VlcOrange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VlcPanelBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VlcTabButton("Videos", currentTab == VlcTab.VIDEO) { currentTab = VlcTab.VIDEO }
                            VlcTabButton("Audios", currentTab == VlcTab.AUDIO) { currentTab = VlcTab.AUDIO }
                            VlcTabButton("Network Stream", currentTab == VlcTab.STREAM) { currentTab = VlcTab.STREAM }
                            VlcTabButton("About VLC", currentTab == VlcTab.INFO) { currentTab = VlcTab.INFO }
                        }
                        
                        CompactSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search library...",
                            borderColor = VlcOrange,
                            modifier = Modifier
                                .width(200.dp)
                                .height(38.dp)
                        )
                    }
                }

                // MAIN WORKSPACE INTERACTIVE WINDOW
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Render Tab Panel Content
                    when (currentTab) {
                        VlcTab.VIDEO -> {
                            VlcVideoTab(
                                videos = filteredVideos,
                                onVideoSelect = { activeMediaItem = it },
                                onImportClick = { filePickerLauncher.launch("video/*") },
                                onDownloadClick = onDownloadSampleClick,
                                isDownloading = isDownloading,
                                downloadProgress = downloadProgress
                            )
                        }
                        VlcTab.AUDIO -> {
                            VlcAudioTab(
                                audios = filteredAudios,
                                onAudioSelect = { activeMediaItem = it },
                                onImportClick = { filePickerLauncher.launch("audio/*") },
                                onDownloadClick = onDownloadSampleClick,
                                isDownloading = isDownloading,
                                downloadProgress = downloadProgress
                            )
                        }
                        VlcTab.STREAM -> {
                            VlcStreamTab(
                                streamUrl = streamUrlInput,
                                onUrlChange = { streamUrlInput = it },
                                onPlayStream = { url ->
                                    activeMediaItem = MediaItem(
                                        id = "custom_stream",
                                        title = url.substringAfterLast("/").substringBefore("?").ifBlank { "Custom Network Stream" },
                                        description = "Media Broadcast Feed Live Stream",
                                        url = url,
                                        duration = "Live 00:00",
                                        type = "video",
                                        artist = "Dynamic IP Stream"
                                    )
                                }
                            )
                        }
                        VlcTab.INFO -> {
                            VlcAboutPanel()
                        }
                    }
                }
            }

            // PLAYER VIEW HUD OVERLAY (Launches over workspace on play)
            AnimatedVisibility(
                visible = activeMediaItem != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                activeMediaItem?.let { item ->
                    VlcPlayerScreen(
                        mediaItem = item,
                        onClosePlayer = { activeMediaItem = null }
                    )
                }
            }
        }
    }
}

// STYLED TAB BUTTONS FOR VLC BRANDING
@Composable
fun VlcTabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) VlcOrange else Color(0xFF333333))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 14.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// 1. VIDEOS TAB (Displays gorgeous Grid of videos)
@Composable
fun VlcVideoTab(
    videos: List<MediaItem>,
    onVideoSelect: (MediaItem) -> Unit,
    onImportClick: () -> Unit,
    onDownloadClick: (Boolean) -> Unit,
    isDownloading: Boolean,
    downloadProgress: Float
) {
    if (videos.isEmpty()) {
        VlcEmptyState(
            isAudio = false,
            onImportClick = onImportClick,
            onDownloadClick = onDownloadClick,
            isDownloading = isDownloading,
            progress = downloadProgress
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(videos) { item ->
                VlcVideoCard(item, onVideoSelect)
            }
        }
    }
}

@Composable
fun VlcVideoCard(media: MediaItem, onPlayClick: (MediaItem) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VlcCardBg),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick(media) }
    ) {
        Column {
            // Simulated video thumbnail container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Background visual simulation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Horizontal design bands
                    drawLine(
                        color = VlcOrange.copy(alpha = 0.2f),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Medium Play Arrow
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, VlcOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = VlcOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = media.duration,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Description / Details
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = media.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = media.description,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = media.artist,
                    color = VlcOrange,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 2. AUDIO TAB (Renders music album style playlists)
@Composable
fun VlcAudioTab(
    audios: List<MediaItem>,
    onAudioSelect: (MediaItem) -> Unit,
    onImportClick: () -> Unit,
    onDownloadClick: (Boolean) -> Unit,
    isDownloading: Boolean,
    downloadProgress: Float
) {
    if (audios.isEmpty()) {
        VlcEmptyState(
            isAudio = true,
            onImportClick = onImportClick,
            onDownloadClick = onDownloadClick,
            isDownloading = isDownloading,
            progress = downloadProgress
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(audios) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(VlcCardBg)
                        .clickable { onAudioSelect(item) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio Album Cover placeholder
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(VlcOrange.copy(alpha = 0.15f))
                            .border(1.dp, VlcOrange.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = VlcOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${item.artist} • ${item.description}",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = item.duration,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = { onAudioSelect(item) }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Audio",
                            tint = VlcOrange
                        )
                    }
                }
            }
        }
    }
}

// 3. NETWORK STREAM TAB (Stream dynamic IP URLs)
@Composable
fun VlcStreamTab(
    streamUrl: String,
    onUrlChange: (String) -> Unit,
    onPlayStream: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SettingsInputAntenna,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(60.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Play Network Stream",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Enter direct HTTP/RTSP/RTMP streaming URL to broadcast in VLC",
            color = Color.Gray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = streamUrl,
            onValueChange = onUrlChange,
            label = { Text("Network URL Feed Link", color = VlcOrange) },
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = VlcOrange,
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth().height(66.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (streamUrl.isNotBlank()) {
                    onPlayStream(streamUrl)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = VlcOrange),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .width(200.dp)
                .height(44.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Stream Link", fontWeight = FontWeight.Bold, color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick presets
        Text("Standard Preset Test Video Feeds:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        listOf(
            "Big Buck Bunny (1080p MP4)" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "Tears of Steel (VFX Movie)" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            "Sintel Open Trailer" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
        ).forEach { (label, link) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(6.dp))
                    .clickable { onUrlChange(link) }
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(link, color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.ArrowForward, null, tint = VlcOrange, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// 4. ABOUT PANEL
@Composable
fun VlcAboutPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(VlcOrange.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Info, null, tint = VlcOrange, modifier = Modifier.size(44.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        Text(
            text = "ISOSpace for Android™",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 16.sp
        )
        Text(
            text = "Version 1.0 Stable Release (ISOSpace Shell Port)",
            color = Color.Gray,
            fontSize = 11.sp
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "ISOSpace media player is a free and open-source, portable, cross-platform media player software and streaming media server. ISOSpace is available for all desktop and mobile operating systems.",
            color = Color.LightGray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = VlcCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Features integrated in this port:", color = VlcOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                BulletLine("Full online MP4 Video Hardware Decoding")
                BulletLine("Live MP3 Internet Audio Player Streamer")
                BulletLine("Embedded Android framework interactive VideoView")
                BulletLine("Adaptive mobile/tablet viewport responsiveness")
                BulletLine("Equalizer graphical visualizer simulations")
            }
        }
    }
}

@Composable
fun BulletLine(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(VlcOrange))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 11.sp)
    }
}


// 5. INTERACTIVE HARDWARE-ACCELERATED MOVIE CONTROLLER
@Composable
fun VlcPlayerScreen(
    mediaItem: MediaItem,
    onClosePlayer: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) }
    var activeVideoDuration by remember { mutableStateOf(100) }
    var activeProgressMs by remember { mutableStateOf(0) }
    var isBuffering by remember { mutableStateOf(true) }
    
    // Custom volume slider state
    var volumeLevel by remember { mutableStateOf(0.8f) }
    
    // Maintain mutable references to players
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var audioPlayerInstance by remember { mutableStateOf<MediaPlayer?>(null) }
    
    // Tick progress when video or audio is actively playing
    LaunchedEffect(isPlaying, videoViewInstance, audioPlayerInstance) {
        while (isPlaying) {
            if (mediaItem.type == "video") {
                videoViewInstance?.let { view ->
                    if (view.isPlaying) {
                        val pos = view.currentPosition
                        val dur = view.duration
                        if (dur > 0) {
                            activeVideoDuration = dur
                            activeProgressMs = pos
                            currentProgress = pos.toFloat() / dur.toFloat()
                        }
                    }
                }
            } else {
                audioPlayerInstance?.let { player ->
                    if (player.isPlaying) {
                        val pos = player.currentPosition
                        val dur = player.duration
                        if (dur > 0) {
                            activeVideoDuration = dur
                            activeProgressMs = pos
                            currentProgress = pos.toFloat() / dur.toFloat()
                        }
                    }
                }
            }
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {} // block click through
    ) {
        // BACKGROUND PLAYER FOR VIDEO
        if (mediaItem.type == "video") {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        videoViewInstance = this
                        try {
                            val uri = if (mediaItem.url.startsWith("http://") || mediaItem.url.startsWith("https://") || mediaItem.url.startsWith("content://")) {
                                Uri.parse(mediaItem.url)
                            } else {
                                Uri.fromFile(File(mediaItem.url))
                            }
                            setVideoURI(uri)
                            setOnPreparedListener { mp ->
                                isBuffering = false
                                mp.isLooping = true
                                mp.setVolume(volumeLevel, volumeLevel)
                                activeVideoDuration = duration
                                start()
                                isPlaying = true
                            }
                            setOnErrorListener { _, what, extra ->
                                Toast.makeText(context, "Network Stream Timeout or Error: $what", Toast.LENGTH_SHORT).show()
                                isBuffering = false
                                false
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "VLC: Failed to parse stream URI", Toast.LENGTH_SHORT).show()
                            isBuffering = false
                        }
                    }
                },
                update = { view ->
                    videoViewInstance = view
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // AUDIO PLAYER ARTWORK BACKDROP + AUDIO GRAPHICAL WAVEFORM SIMULATOR
            DisposableEffect(mediaItem) {
                val mp = MediaPlayer().apply {
                    try {
                        val uri = if (mediaItem.url.startsWith("http://") || mediaItem.url.startsWith("https://") || mediaItem.url.startsWith("content://")) {
                            Uri.parse(mediaItem.url)
                        } else {
                            Uri.fromFile(File(mediaItem.url))
                        }
                        setDataSource(context, uri)
                        setOnPreparedListener {
                            isBuffering = false
                            activeVideoDuration = duration
                            start()
                            isPlaying = true
                        }
                        setOnErrorListener { _, what, extra ->
                            Toast.makeText(context, "Audio playback error: $what", Toast.LENGTH_SHORT).show()
                            isBuffering = false
                            false
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            currentProgress = 0f
                            activeProgressMs = 0
                        }
                        prepareAsync()
                    } catch (e: Exception) {
                        isBuffering = false
                        Toast.makeText(context, "Error opening audio file", Toast.LENGTH_SHORT).show()
                    }
                }
                audioPlayerInstance = mp
                
                onDispose {
                    try {
                        mp.stop()
                    } catch (e: Exception) {}
                    mp.release()
                    audioPlayerInstance = null
                }
            }
            
            // Render audio view panel
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(VlcOrange.copy(alpha = 0.15f))
                        .border(3.dp, VlcOrange, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = VlcOrange,
                        modifier = Modifier.size(72.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = mediaItem.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = mediaItem.artist,
                    color = VlcOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Real-time Graphic equalizer node rendering using custom row
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(50.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Cyclic heights simulated on local timer ticks
                    val wavePhases = remember { List(12) { mutableStateOf(10f) } }
                    LaunchedEffect(isPlaying) {
                        var ticks = 0
                        while (isPlaying) {
                            wavePhases.forEachIndexed { i, state ->
                                val waveFactor = if (i % 2 == 0) 15 else 30
                                state.value = (10f + Math.sin(ticks.toDouble() / 2.0 + i) * waveFactor + Math.random() * 10).toFloat().coerceIn(4f, 48f)
                            }
                            ticks++
                            delay(100)
                        }
                    }
                    
                    wavePhases.forEach { state ->
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(state.value.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(VlcOrange)
                        )
                    }
                }
            }
        }

        // BUFFERING INDICATOR
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = VlcOrange)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Opening stream or file feed...", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // CONTROL OVERLAY INTERACTIVE CANVAS (Tap to show/hide layout)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Close player button at top edge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        try {
                            videoViewInstance?.stopPlayback()
                        } catch (e: Exception) {}
                        try {
                            audioPlayerInstance?.stop()
                            audioPlayerInstance?.release()
                        } catch (e: Exception) {}
                        onClosePlayer()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Return to VLC Directory",
                        tint = Color.White
                    )
                }

                // Streaming media detail indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "VLC HW DECODING ACTIVE",
                        color = VlcOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // BOTTOM CONTROLLERS OVERLAY PANEL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    // Media details inside active player
                    if (mediaItem.type == "video") {
                        Text(
                            text = mediaItem.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Text(
                            text = mediaItem.artist,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // TIMELINE ACCURATE SEEKBAR SLIDER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatMs(activeProgressMs),
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )

                        Slider(
                            value = currentProgress,
                            onValueChange = { progress ->
                                currentProgress = progress
                                val targetPos = (progress * activeVideoDuration).toInt()
                                activeProgressMs = targetPos
                                if (mediaItem.type == "video") {
                                    videoViewInstance?.seekTo(targetPos)
                                } else {
                                    audioPlayerInstance?.seekTo(targetPos)
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = VlcOrange,
                                activeTrackColor = VlcOrange,
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        Text(
                            text = formatMs(activeVideoDuration),
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // BUTTONS BAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume Slider component
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(110.dp)
                        ) {
                            Icon(
                                imageVector = if (volumeLevel == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Slider(
                                value = volumeLevel,
                                onValueChange = { vol ->
                                    volumeLevel = vol
                                    if (mediaItem.type == "video") {
                                        // set volume on VideoView if possible, or keep as visual
                                    } else {
                                        try {
                                            audioPlayerInstance?.setVolume(vol, vol)
                                        } catch (e: Exception) {}
                                    }
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.DarkGray
                                ),
                                modifier = Modifier.height(18.dp)
                            )
                        }

                        // Play/Pause / Skip Center row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // RW -10s
                            IconButton(onClick = {
                                if (mediaItem.type == "video") {
                                    videoViewInstance?.let { view ->
                                        val newPos = (view.currentPosition - 10000).coerceAtLeast(0)
                                        view.seekTo(newPos)
                                    }
                                } else {
                                    audioPlayerInstance?.let { player ->
                                        val newPos = (player.currentPosition - 10000).coerceAtLeast(0)
                                        player.seekTo(newPos)
                                        activeProgressMs = newPos
                                        currentProgress = if (activeVideoDuration > 0) newPos.toFloat() / activeVideoDuration.toFloat() else 0f
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }

                            // PLAY PAUSE BUTTON
                            IconButton(
                                onClick = {
                                    if (mediaItem.type == "video") {
                                        videoViewInstance?.let { view ->
                                            if (view.isPlaying) {
                                                view.pause()
                                                isPlaying = false
                                            } else {
                                                view.start()
                                                isPlaying = true
                                            }
                                        }
                                    } else {
                                        audioPlayerInstance?.let { player ->
                                            if (player.isPlaying) {
                                                player.pause()
                                                isPlaying = false
                                            } else {
                                                player.start()
                                                isPlaying = true
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(VlcOrange)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // FF +10s
                            IconButton(onClick = {
                                if (mediaItem.type == "video") {
                                    videoViewInstance?.let { view ->
                                        val newPos = (view.currentPosition + 10000).coerceAtMost(view.duration)
                                        view.seekTo(newPos)
                                    }
                                } else {
                                    audioPlayerInstance?.let { player ->
                                        val newPos = (player.currentPosition + 10000).coerceAtMost(player.duration)
                                        player.seekTo(newPos)
                                        activeProgressMs = newPos
                                        currentProgress = if (activeVideoDuration > 0) newPos.toFloat() / activeVideoDuration.toFloat() else 0f
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }

                        // Aspect ratio / Fullscreen Indicator
                        IconButton(onClick = {
                            Toast.makeText(context, "VLC: Lock Aspect Ratio 16:9 configured.", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Format milliseconds to mm:ss helper
private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Composable
private fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    borderColor: Color = VlcOrange,
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

// Dynamic storage scan helper
fun scanForMediaFiles(context: Context): List<MediaItem> {
    val defaultHomePath = Environment.getExternalStorageDirectory() ?: context.filesDir
    val mediaList = mutableListOf<MediaItem>()
    val targets = listOf(
        defaultHomePath,
        File(defaultHomePath, "Documents"),
        File(defaultHomePath, "Download"),
        File(defaultHomePath, "Pictures"),
        File(defaultHomePath, "DCIM")
    )
    
    val visited = mutableSetOf<String>()
    
    fun searchDir(dir: File) {
        val canonical = try { dir.canonicalPath } catch (e: Exception) { dir.absolutePath }
        if (visited.contains(canonical)) return
        visited.add(canonical)
        
        if (!dir.exists() || !dir.isDirectory) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                searchDir(file)
            } else {
                val ext = file.extension.lowercase()
                val isVideo = ext in listOf("mp4", "mkv", "avi", "3gp", "webm")
                val isAudio = ext in listOf("mp3", "wav", "ogg", "aac", "m4a")
                if (isVideo || isAudio) {
                    val type = if (isVideo) "video" else "audio"
                    val duration = if (isVideo) "00:15" else "01:21"
                    mediaList.add(
                        MediaItem(
                            id = file.absolutePath,
                            title = file.name,
                            description = "Local file in ${file.parentFile?.name ?: "Home"}",
                            url = file.absolutePath,
                            duration = duration,
                            type = type,
                            artist = "Local Device File"
                        )
                    )
                }
            }
        }
    }
    
    for (target in targets) {
        searchDir(target)
    }
    
    return mediaList
}

// Background downloading utility
suspend fun downloadSampleFile(urlStr: String, destinationFile: File, onProgress: (Float) -> Unit): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            destinationFile.parentFile?.mkdirs()
            val url = URL(urlStr)
            val connection = url.openConnection()
            connection.connect()
            val fileLength = connection.contentLength
            val input = BufferedInputStream(url.openStream())
            val output = FileOutputStream(destinationFile)
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    onProgress(total.toFloat() / fileLength.toFloat())
                }
                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            input.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

// Elegant VLC onboarding Empty State
@Composable
fun VlcEmptyState(
    isAudio: Boolean,
    onImportClick: () -> Unit,
    onDownloadClick: (Boolean) -> Unit,
    isDownloading: Boolean,
    progress: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isAudio) Icons.Default.MusicNote else Icons.Default.Movie,
            contentDescription = null,
            tint = VlcOrange.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isAudio) "No Local Audio Files Found" else "No Local Video Files Found",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "ISOSpace scans your local storage folders (Download, Documents, DCIM, Pictures) for playable media.",
            color = Color.Gray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isDownloading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(220.dp)
            ) {
                CircularProgressIndicator(color = VlcOrange, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Downloading sample media... ${(progress * 100).toInt()}%",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = VlcOrange,
                    trackColor = Color.DarkGray,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onImportClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import File", fontSize = 11.sp, color = Color.White)
                }
                
                Button(
                    onClick = { onDownloadClick(isAudio) },
                    colors = ButtonDefaults.buttonColors(containerColor = VlcOrange),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAudio) "Get Demo Song" else "Get Demo Video",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
