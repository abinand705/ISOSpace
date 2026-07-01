package com.example.launcher.ui

import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.BatteryManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.launcher.model.*
import com.example.launcher.model.WidgetType
import com.example.launcher.viewmodel.LauncherViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ISOSpaceLauncherScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val widgets by viewModel.widgets.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val isDashOpen by viewModel.isDashOpen.collectAsState()
    val isControlCenterOpen by viewModel.isControlCenterOpen.collectAsState()
    val isCustomizerOpen by viewModel.isCustomizerOpen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val terminalHistory by viewModel.terminalNotesHistory.collectAsState()

    val isSandboxUnlocked by viewModel.isSandboxUnlocked.collectAsState()
    if (!isSandboxUnlocked) {
        SandboxBootloaderScreen(viewModel = viewModel)
        return
    }

    // Inner dialogs for interactive preset apps
    var activeModalApp by rememberSaveable { mutableStateOf<String?>(null) }
    val saveableStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(isSandboxUnlocked) {
        if (!isSandboxUnlocked) {
            activeModalApp = null
        }
    }

    // Intercept back presses to dismiss modals first, or prevent exiting on homescreen
    BackHandler(enabled = true) {
        if (activeModalApp != null) {
            activeModalApp = null
        } else if (isDashOpen) {
            viewModel.setDashOpen(false)
        } else if (isControlCenterOpen) {
            viewModel.setControlCenterOpen(false)
        } else if (isCustomizerOpen) {
            viewModel.setCustomizerOpen(false)
        } else {
            // Do nothing on back press on homescreen! Exiting is only allowed via dock exit button.
        }
    }

    val activeAccentColor = Color(if (settings.useCustomColors) settings.customAccentColorHex else settings.accentColor.hexAccent)
    val activeBgColor = Color(if (settings.useCustomColors) settings.customBgColorHex else settings.accentColor.hexThemeBg)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.updateSettings(settings.copy(backgroundWallpaperUrl = uri.toString()))
        }
    }

    // Inner dialogs for interactive preset apps
    var contextMenuApp by remember { mutableStateOf<AppItem?>(null) }
    var isAutohideDockRevealed by remember { mutableStateOf(false) }
    var isDockMinimizedByUser by remember { mutableStateOf(false) }
    
    var showTutorialOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(isSandboxUnlocked) {
        if (isSandboxUnlocked && !viewModel.isTutorialCompleted()) {
            showTutorialOverlay = true
        }
    }

    // Dynamic background moving wave variables
    val infiniteTransition = rememberInfiniteTransition(label = "wallpaper_ambient")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    // Gesture helper values
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("isospace_launcher_root")
            .pointerInput(Unit) {
                // Unified screen drag/tap detector for smooth OS gestures
                detectTapGestures(
                    onDoubleTap = {
                        viewModel.handleGestureTrigger(LauncherGesture.DOUBLE_TAP)
                    },
                    onLongPress = {
                        viewModel.handleGestureTrigger(LauncherGesture.LONG_PRESS)
                    },
                    onTap = {
                        // Close floating menus
                        if (isDashOpen) viewModel.setDashOpen(false)
                        if (isControlCenterOpen) viewModel.setControlCenterOpen(false)
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val dragAmount = event.changes.firstOrNull()?.position ?: Offset.Zero
                        val prevDragAmount = event.changes.firstOrNull()?.previousPosition ?: Offset.Zero
                        
                        if (event.changes.firstOrNull()?.pressed == true) {
                            totalDragY += (dragAmount.y - prevDragAmount.y)
                            totalDragX += (dragAmount.x - prevDragAmount.x)
                        } else {
                            // Drag released, process vector gestures
                            if (abs(totalDragY) > 120) {
                                if (totalDragY > 0) {
                                    viewModel.handleGestureTrigger(LauncherGesture.SWIPE_DOWN)
                                } else {
                                    viewModel.handleGestureTrigger(LauncherGesture.SWIPE_UP)
                                }
                            } else if (abs(totalDragX) > 150) {
                                // Pinch / clear signal simulation
                                viewModel.handleGestureTrigger(LauncherGesture.TWO_FINGER_PINCH)
                            }
                            totalDragY = 0f
                            totalDragX = 0f
                        }
                    }
                }
            }
    ) {
        // 1. Dynamic Canvas background or custom gallery image
        if (settings.backgroundWallpaperUrl.startsWith("content://") ||
            settings.backgroundWallpaperUrl.startsWith("file://") ||
            settings.backgroundWallpaperUrl.contains("/")
        ) {
            AsyncImage(
                model = settings.backgroundWallpaperUrl,
                contentDescription = "Custom Backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val themeBg = activeBgColor
                val accentHex = activeAccentColor
                
                when (settings.backgroundWallpaperUrl) {
                "misty_mountain" -> {
                    // Sky base gradient: soft pale desaturated misty gray-green
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFCFDCD5), Color(0xFFAFBFA0), Color(0xFF7D957E))
                        )
                    )

                    // Large glowing soft sun filtering through mist
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                            center = Offset(size.width * 0.55f, size.height * 0.28f),
                            radius = size.width * 0.7f
                        ),
                        radius = size.width * 0.7f,
                        center = Offset(size.width * 0.55f, size.height * 0.28f)
                    )

                    // Birds flying high in the mist
                    val drawBird = { bx: Float, by: Float, sizeS: Float ->
                        val birdPath = Path().apply {
                            moveTo(bx, by)
                            quadraticTo(bx - 8f * sizeS, by - 10f * sizeS, bx - 20f * sizeS, by - 2f * sizeS)
                            quadraticTo(bx - 10f * sizeS, by - 14f * sizeS, bx, by - 4f * sizeS)
                            quadraticTo(bx + 10f * sizeS, by - 14f * sizeS, bx + 20f * sizeS, by - 2f * sizeS)
                            quadraticTo(bx + 8f * sizeS, by - 10f * sizeS, bx, by)
                            close()
                        }
                        drawPath(path = birdPath, color = Color(0xFF3B563D).copy(alpha = 0.7f))
                    }
                    drawBird(size.width * 0.20f, size.height * 0.16f, 0.7f)
                    drawBird(size.width * 0.31f, size.height * 0.19f, 0.5f)
                    drawBird(size.width * 0.16f, size.height * 0.21f, 0.6f)
                    drawBird(size.width * 0.08f, size.height * 0.18f, 0.4f)
                    drawBird(size.width * 0.26f, size.height * 0.22f, 0.5f)

                    // Far Mountains contour (Misty silhouette)
                    val farMountainPath = Path().apply {
                        moveTo(0f, size.height * 0.50f)
                        cubicTo(
                            size.width * 0.25f, size.height * 0.38f,
                            size.width * 0.55f, size.height * 0.21f,
                            size.width * 0.75f, size.height * 0.28f
                        )
                        cubicTo(
                            size.width * 0.88f, size.height * 0.31f,
                            size.width * 0.95f, size.height * 0.42f,
                            size.width, size.height * 0.46f
                        )
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path = farMountainPath, color = Color(0xFF8FA695).copy(alpha = 0.85f))

                    // Floating Valley Mist Level 1
                    drawOval(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)
                        ),
                        topLeft = Offset(-size.width * 0.2f, size.height * 0.40f),
                        size = Size(size.width * 1.4f, size.height * 0.16f)
                    )

                    // Mid Mountains (Layer 2 - desaturated pine forest slopes)
                    val midMountainPath = Path().apply {
                        moveTo(0f, size.height * 0.62f)
                        lineTo(size.width * 0.30f, size.height * 0.44f)
                        lineTo(size.width * 0.60f, size.height * 0.55f)
                        lineTo(size.width * 0.85f, size.height * 0.40f)
                        lineTo(size.width, size.height * 0.54f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path = midMountainPath, color = Color(0xFF5D7A65))

                    // Floating Valley Mist Level 2
                    drawOval(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        topLeft = Offset(-size.width * 0.1f, size.height * 0.52f),
                        size = Size(size.width * 1.2f, size.height * 0.14f)
                    )

                    // Custom Pine Tree Drawing Helper
                    val drawPineTree = { x: Float, y: Float, scale: Float ->
                        val treePath = Path().apply {
                            moveTo(x, y - 48f * scale)
                            lineTo(x - 16f * scale, y)
                            lineTo(x + 16f * scale, y)
                            close()
                            moveTo(x, y - 30f * scale)
                            lineTo(x - 22f * scale, y + 16f * scale)
                            lineTo(x + 22f * scale, y + 16f * scale)
                            close()
                            moveTo(x, y - 12f * scale)
                            lineTo(x - 28f * scale, y + 32f * scale)
                            lineTo(x + 28f * scale, y + 32f * scale)
                            close()
                        }
                        drawPath(path = treePath, color = Color(0xFF263C2A))
                    }

                    // Place trees on far/mid ridges
                    drawPineTree(size.width * 0.08f, size.height * 0.58f, 0.6f)
                    drawPineTree(size.width * 0.22f, size.height * 0.59f, 0.5f)
                    drawPineTree(size.width * 0.55f, size.height * 0.56f, 0.4f)
                    drawPineTree(size.width * 0.72f, size.height * 0.52f, 0.7f)

                    // Foreground Meadow Hills (Layer 1 - Richer earthy greens)
                    val foregroundLeftPath = Path().apply {
                        moveTo(0f, size.height * 0.61f)
                        cubicTo(
                            size.width * 0.32f, size.height * 0.65f,
                            size.width * 0.45f, size.height * 0.76f,
                            size.width * 0.48f, size.height * 0.82f
                        )
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path = foregroundLeftPath, color = Color(0xFF3A593E))

                    val foregroundRightPath = Path().apply {
                        moveTo(size.width, size.height * 0.66f)
                        cubicTo(
                            size.width * 0.74f, size.height * 0.70f,
                            size.width * 0.58f, size.height * 0.79f,
                            size.width * 0.52f, size.height * 0.85f
                        )
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(path = foregroundRightPath, color = Color(0xFF425E42))

                    // Cascading stream in the valley center
                    val streamPath = Path().apply {
                        moveTo(size.width * 0.48f, size.height * 0.62f)
                        cubicTo(
                            size.width * 0.45f, size.height * 0.68f,
                            size.width * 0.51f, size.height * 0.74f,
                            size.width * 0.48f, size.height * 0.80f
                        )
                        cubicTo(
                            size.width * 0.53f, size.height * 0.86f,
                            size.width * 0.47f, size.height * 0.92f,
                            size.width * 0.50f, size.height
                        )
                        lineTo(size.width * 0.58f, size.height)
                        cubicTo(
                            size.width * 0.54f, size.height * 0.92f,
                            size.width * 0.60f, size.height * 0.86f,
                            size.width * 0.55f, size.height * 0.80f
                        )
                        cubicTo(
                            size.width * 0.58f, size.height * 0.74f,
                            size.width * 0.52f, size.height * 0.68f,
                            size.width * 0.54f, size.height * 0.62f
                        )
                        close()
                    }
                    drawPath(path = streamPath, color = Color(0xFF6B8C87))

                    // Water foam highlights representing small mountain waterfalls
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = 5.dp.toPx(),
                        center = Offset(size.width * 0.48f, size.height * 0.72f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = 7.dp.toPx(),
                        center = Offset(size.width * 0.51f, size.height * 0.81f)
                    )

                    // Rocky Pathway with stone stepping contours on left meadow
                    val trailPath = Path().apply {
                        moveTo(size.width * 0.08f, size.height * 0.73f)
                        quadraticTo(
                            size.width * 0.23f, size.height * 0.82f,
                            size.width * 0.38f, size.height * 0.94f
                        )
                        lineTo(size.width * 0.44f, size.height * 0.94f)
                        quadraticTo(
                            size.width * 0.27f, size.height * 0.81f,
                            size.width * 0.11f, size.height * 0.71f
                        )
                        close()
                    }
                    drawPath(path = trailPath, color = Color(0xFF756F5E))

                    // Draw rocks on the hiking trail side
                    drawCircle(color = Color(0xFF555555), radius = 3.dp.toPx(), center = Offset(size.width * 0.20f, size.height * 0.81f))
                    drawCircle(color = Color(0xFF666666), radius = 4.dp.toPx(), center = Offset(size.width * 0.26f, size.height * 0.85f))
                    drawCircle(color = Color(0xFF4A4A4A), radius = 5.dp.toPx(), center = Offset(size.width * 0.35f, size.height * 0.92f))

                    // Pine Trees in foreground
                    drawPineTree(size.width * 0.10f, size.height * 0.66f, 1.2f)
                    drawPineTree(size.width * 0.18f, size.height * 0.70f, 0.9f)
                    drawPineTree(size.width * 0.04f, size.height * 0.69f, 1.4f)
                    drawPineTree(size.width * 0.88f, size.height * 0.71f, 1.1f)
                    drawPineTree(size.width * 0.82f, size.height * 0.75f, 0.8f)
                    drawPineTree(size.width * 0.95f, size.height * 0.76f, 1.5f)
                    drawPineTree(size.width * 0.76f, size.height * 0.83f, 0.9f)
                }
                "starry" -> {
                    // Deep galactic cosmic theme
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F0C1B), Color(0xFF15102A), Color(0xFF08060E))
                        )
                    )

                    // Nebula cosmic dust glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF8E2DE2).copy(alpha = 0.2f), Color.Transparent),
                            center = Offset(size.width * 0.3f, size.height * 0.4f),
                            radius = size.width * 1.1f
                        ),
                        radius = size.width * 1.1f,
                        center = Offset(size.width * 0.3f, size.height * 0.4f)
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF4A00E0).copy(alpha = 0.2f), Color.Transparent),
                            center = Offset(size.width * 0.7f, size.height * 0.7f),
                            radius = size.width * 1.3f
                        ),
                        radius = size.width * 1.3f,
                        center = Offset(size.width * 0.7f, size.height * 0.7f)
                    )

                    // Constellation / starry particles (seeded pseudo-randomly to avoid external states)
                    for (i in 1..40) {
                        val seedX = (723 * i % 1000) / 1000f * size.width
                        val seedY = (517 * i % 1000) / 1000f * size.height
                        val magnitude = (i * 13 % 4) + 1.2f
                        val alpha = if (i % 2 == 0) 0.8f else 0.4f
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = magnitude.dp.toPx() / 2f,
                            center = Offset(seedX, seedY)
                        )
                    }

                    // A glowing Crescent Moon
                    drawCircle(
                        color = Color(0xFFFFFDD0),
                        radius = 24.dp.toPx(),
                        center = Offset(size.width * 0.8f, size.height * 0.15f)
                    )
                    // Subtract overlay circle to create the perfect crescent shape
                    drawCircle(
                        color = Color(0xFF0F0C1B),
                        radius = 24.dp.toPx(),
                        center = Offset(size.width * 0.75f, size.height * 0.13f)
                    )
                }
                else -> {
                    // "dynamic_gradient" - Classic Aubergine wave
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(themeBg, themeBg, Color(0xFF150212))
                        )
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentHex.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(size.width * 0.2f + waveOffset / 2f, size.height * 0.3f),
                            radius = size.width * 0.9f
                        ),
                        radius = size.width * 0.9f,
                        center = Offset(size.width * 0.2f + waveOffset / 2f, size.height * 0.3f)
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF77216F).copy(alpha = 0.25f), Color.Transparent),
                            center = Offset(size.width * 0.8f - waveOffset, size.height * 0.8f),
                            radius = size.width * 1.2f
                        ),
                        radius = size.width * 1.2f,
                        center = Offset(size.width * 0.8f - waveOffset, size.height * 0.8f)
                    )
                }
            }
        }
    }

        // 2. Main Workspace Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Conditionally show persistent Left Dock with Swipe-to-Minimize
            AnimatedVisibility(
                visible = settings.dockPosition == DockPosition.LEFT && !settings.isDockAutohideEnabled && !isDockMinimizedByUser,
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
            ) {
                ISOSpaceSideDock(
                    viewModel = viewModel,
                    apps = apps,
                    onAppClick = { app ->
                        launchOrSimulateApp(context, app, viewModel) { modal ->
                            activeModalApp = modal
                        }
                    },
                    onAppLongClick = { app ->
                        contextMenuApp = app
                    },
                    dockSettings = settings,
                    modifier = Modifier.pointerInput(settings.dockPosition) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount < -12f) {
                                isDockMinimizedByUser = true
                            }
                            change.consume()
                        }
                    }
                )
            }

            // Desktop widgets and grids content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                // Interactive desktop grid layout for customizable widgets
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // System Info mini top status row
                    TopSystemStatusRow(
                        viewModel = viewModel,
                        onSystemMenuClick = {
                            viewModel.setControlCenterOpen(true)
                        },
                        accentColor = activeAccentColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Desktop Grid hosting widgets
                    val columns = settings.desktopColumns
                    val rows = settings.desktopRows

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = widgets,
                                key = { it.id },
                                span = { widget ->
                                    val spanCount = widget.widthSpan.coerceIn(1, columns)
                                    GridItemSpan(spanCount)
                                }
                            ) { widget ->
                                ISOSpaceWidgetRegistry(
                                    config = widget,
                                    onRemove = {
                                        viewModel.removeWidget(widget.id)
                                    },
                                    onResize = { w, h ->
                                        viewModel.resizeWidget(widget.id, w, h)
                                    },
                                    onSubmitCmd = { cmd ->
                                        viewModel.submitTerminalCommand(cmd)
                                    },
                                    terminalHistory = terminalHistory,
                                    modifier = Modifier
                                        .height((130 * widget.heightSpan).dp)
                                        .animateItem()
                                )
                            }
                        }
                    }

                    // Help tip at bottom
                    Text(
                        text = "Swipe UP for Desktop Drawer • Swipe DOWN for Control Panel • Double Tap for Configurations",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 9.sp,
                            color = Color(0x66FFFFFF),
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }

            // Right side dock option with Swipe-to-Minimize
            AnimatedVisibility(
                visible = settings.dockPosition == DockPosition.RIGHT && !settings.isDockAutohideEnabled && !isDockMinimizedByUser,
                enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
            ) {
                ISOSpaceSideDock(
                    viewModel = viewModel,
                    apps = apps,
                    onAppClick = { app ->
                        launchOrSimulateApp(context, app, viewModel) { modal ->
                            activeModalApp = modal
                        }
                    },
                    onAppLongClick = { app ->
                        contextMenuApp = app
                    },
                    dockSettings = settings,
                    modifier = Modifier.pointerInput(settings.dockPosition) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount > 12f) {
                                isDockMinimizedByUser = true
                            }
                            change.consume()
                        }
                    }
                )
            }
        }

        // 3. Sliding GNOME "Dash" Application Drawer overlay (Swipe Up gesture)
        AnimatedVisibility(
            visible = isDashOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            GnomeDashOverlay(
                viewModel = viewModel,
                allApps = apps,
                customCategories = customCategories,
                searchQuery = searchQuery,
                onClose = { viewModel.setDashOpen(false) },
                onLaunchApp = { app ->
                    viewModel.setDashOpen(false)
                    launchOrSimulateApp(context, app, viewModel) { modal ->
                        activeModalApp = modal
                    }
                },
                onAppLongClick = { app ->
                    contextMenuApp = app
                },
                accentColor = activeAccentColor
            )
        }

        // 4. GNOME Top-bar Control Center panel drop down (Swipe Down gesture)
        AnimatedVisibility(
            visible = isControlCenterOpen,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .wrapContentHeight()
                .statusBarsPadding()
        ) {
            ISOSpaceControlCenterOverlay(
                viewModel = viewModel,
                onClose = { viewModel.setControlCenterOpen(false) },
                accentColor = activeAccentColor
            )
        }

        // 5. Interactive full GUI Customizer Editor (Double Tap gesture / Settings)
        if (isCustomizerOpen) {
            ISOSpaceCustomizerDialog(
                viewModel = viewModel,
                settings = settings,
                customCategories = customCategories,
                onClose = { viewModel.setCustomizerOpen(false) },
                onLaunchTutorial = {
                    viewModel.setCustomizerOpen(false)
                    showTutorialOverlay = true
                }
            )
        }

        // Gesture Interactive Tutorial Overlay
        if (showTutorialOverlay) {
            InteractiveGestureTutorialOverlay(
                accentColor = activeAccentColor,
                onDismiss = {
                    showTutorialOverlay = false
                    viewModel.setTutorialCompleted(true)
                }
            )
        }

        // 6. Interactive App Context Menu Dialog for renaming/moving apps
        contextMenuApp?.let { app ->
            val openedPackages by viewModel.openedApps.collectAsState()
            val isRunning = openedPackages.contains(app.packageName)
            AppContextSettingsDialog(
                app = app,
                allCategories = listOf("System", "Social", "Media", "Utilities", "Games", "Development") + customCategories,
                isPinnedToDock = false,
                onToggleDockPin = { },
                onClose = { contextMenuApp = null },
                onSaveCustomValues = { newLabel, newCategory, isHidden ->
                    viewModel.customizeApp(app.packageName, newLabel, newCategory, isHidden)
                    contextMenuApp = null
                },
                onUninstallApp = {
                    if (app.packageName.startsWith("clone.")) {
                        val originalPackage = app.packageName.substringAfter("clone.")
                        viewModel.setDeviceAppEnabled(originalPackage, false)
                        Toast.makeText(context, "${app.label} removed successfully", Toast.LENGTH_SHORT).show()
                    } else if (app.packageName.startsWith("com.dummy") || app.packageName.startsWith("com.isospace")) {
                        viewModel.uninstallVirtualApp(app.packageName)
                        Toast.makeText(context, "${app.label} uninstalled successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            val uri = android.net.Uri.fromParts("package", app.packageName, null)
                            val uninstallIntent = Intent(Intent.ACTION_DELETE, uri)
                            context.startActivity(uninstallIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot uninstall system virtual shortcut", Toast.LENGTH_SHORT).show()
                        }
                    }
                    contextMenuApp = null
                },
                isAppRunning = isRunning,
                onCloseApp = {
                    viewModel.closeApp(app.packageName)
                    val modalId = if (app.packageName.startsWith("clone.")) {
                        app.packageName
                    } else {
                        when (app.packageName) {
                            "com.dummy.calculator" -> "calculator"
                            "com.dummy.browser" -> "browser"
                            "com.dummy.gallery" -> "gallery"
                            "com.dummy.camera" -> "camera"
                            "com.dummy.notes" -> "notes"
                            "com.dummy.clock" -> "clock"
                            "com.dummy.files" -> "files"
                            "com.dummy.vlc" -> "vlc"
                            "com.dummy.terminal" -> "terminal"
                            "com.dummy.settings", "com.isospace.settings" -> "settings"
                            "com.dummy.store", "com.isospace.software" -> "software"
                            else -> null
                        }
                    }
                    if (activeModalApp == modalId) {
                        activeModalApp = null
                    }
                }
            )
        }

        // 6b. Autohide Slide-in side dock overlay and edge indicator handle
        if (settings.isDockAutohideEnabled) {
            // Invisible/semi-transparent background scrim for closing when clicking outside the dock
            AnimatedVisibility(
                visible = isAutohideDockRevealed,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { isAutohideDockRevealed = false }
                            )
                        }
                        .testTag("autohide_dock_scrim")
                )
            }

            // The animated side dock overlay matching settings.dockPosition
            AnimatedVisibility(
                visible = isAutohideDockRevealed,
                enter = slideInHorizontally(
                    initialOffsetX = { if (settings.dockPosition == DockPosition.LEFT) -it else it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = { if (settings.dockPosition == DockPosition.LEFT) -it else it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeOut(),
                modifier = Modifier
                    .fillMaxHeight()
                    .align(if (settings.dockPosition == DockPosition.LEFT) Alignment.CenterStart else Alignment.CenterEnd)
                    .statusBarsPadding()
            ) {
                // Intercept clicks on the dock area itself to prevent tap-out closures
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .wrapContentWidth()
                        .pointerInput(settings.dockPosition) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                if (settings.dockPosition == DockPosition.LEFT && dragAmount < -8f) {
                                    isAutohideDockRevealed = false
                                } else if (settings.dockPosition == DockPosition.RIGHT && dragAmount > 8f) {
                                    isAutohideDockRevealed = false
                                }
                                change.consume()
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // block click-through
                        )
                ) {
                    ISOSpaceSideDock(
                        viewModel = viewModel,
                        apps = apps,
                        onAppClick = { app ->
                            isAutohideDockRevealed = false // auto-close on launching app
                            launchOrSimulateApp(context, app, viewModel) { modal ->
                                activeModalApp = modal
                            }
                        },
                        onAppLongClick = { app ->
                            contextMenuApp = app
                        },
                        dockSettings = settings
                    )
                }
            }

            // Visible edge-mounted swipe handle bar (only when dock is hidden)
            if (!isAutohideDockRevealed && (settings.dockPosition == DockPosition.LEFT || settings.dockPosition == DockPosition.RIGHT)) {
                Box(
                    modifier = Modifier
                        .align(if (settings.dockPosition == DockPosition.LEFT) Alignment.CenterStart else Alignment.CenterEnd)
                        .width(14.dp)
                        .height(180.dp)
                        .clip(
                            if (settings.dockPosition == DockPosition.LEFT)
                                RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            else
                                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        )
                        .background(activeAccentColor.copy(alpha = 0.5f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = if (settings.dockPosition == DockPosition.LEFT)
                                RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            else
                                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        )
                        .clickable { isAutohideDockRevealed = true }
                        .pointerInput(settings.dockPosition) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                if (settings.dockPosition == DockPosition.LEFT && dragAmount > 5f) {
                                    isAutohideDockRevealed = true
                                } else if (settings.dockPosition == DockPosition.RIGHT && dragAmount < -5f) {
                                    isAutohideDockRevealed = true
                                }
                                change.consume()
                            }
                        }
                        .testTag("autohide_dock_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    // Visual indicator: accent/white glowing pill in center
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        }

        // 6c. Standard Persistent Dock Minimized Handle / Sidebar
        if (!settings.isDockAutohideEnabled && isDockMinimizedByUser) {
            Box(
                modifier = Modifier
                    .align(if (settings.dockPosition == DockPosition.LEFT) Alignment.CenterStart else Alignment.CenterEnd)
                    .width(16.dp)
                    .height(180.dp)
                    .clip(
                        if (settings.dockPosition == DockPosition.LEFT)
                            RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        else
                            RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    .background(activeAccentColor.copy(alpha = 0.65f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = if (settings.dockPosition == DockPosition.LEFT)
                            RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        else
                            RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    .clickable { isDockMinimizedByUser = false }
                    .pointerInput(settings.dockPosition) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (settings.dockPosition == DockPosition.LEFT && dragAmount > 4f) {
                                isDockMinimizedByUser = false
                            } else if (settings.dockPosition == DockPosition.RIGHT && dragAmount < -4f) {
                                isDockMinimizedByUser = false
                            }
                            change.consume()
                        }
                    }
                    .testTag("persistent_dock_minimized_handle"),
                contentAlignment = Alignment.Center
            ) {
                // Visual indicator: glowing white pill in center
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }
        }

        // 7. Simulated virtual apps dashboards when clicked (Web browser, calculator, etc.)
        val currentModal = activeModalApp
        if (currentModal != null) {
            saveableStateHolder.SaveableStateProvider(key = currentModal) {
                if (currentModal.startsWith("clone.")) {
                    val targetPkg = currentModal.substringAfter("clone.")
                    ShellCloneContainerModal(
                        packageName = targetPkg,
                        viewModel = viewModel,
                        onClose = { activeModalApp = null }
                    )
                } else {
                    when (currentModal) {
                        "calculator" -> ShellCalculatorModal { activeModalApp = null }
                        "browser" -> ShellWebBrowserModal { activeModalApp = null }
                        "gallery" -> ShellGalleryModal { activeModalApp = null }
                        "camera" -> ShellCameraModal { activeModalApp = null }
                        "notes" -> ShellNotesModal { activeModalApp = null }
                        "clock" -> ShellClockModal { activeModalApp = null }
                        "files" -> ShellFilesModal { activeModalApp = null }
                        "vlc" -> ShellVlcModal { activeModalApp = null }
                        "terminal" -> ShellTerminalModal(viewModel = viewModel, onClose = { activeModalApp = null })
                        "software" -> ShellSoftwareCenterModal(viewModel = viewModel, onClose = { activeModalApp = null }, onInstallWidget = { widgetType ->
                            viewModel.addWidget(widgetType)
                            activeModalApp = null
                        })
                        "settings" -> {
                            activeModalApp = null
                            viewModel.setCustomizerOpen(true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopSystemStatusRow(
    viewModel: LauncherViewModel,
    onSystemMenuClick: () -> Unit,
    accentColor: Color
) {
    val context = LocalContext.current
    val isWifiEnabled by viewModel.isWifiEnabled.collectAsState()

    var sysTime by remember { mutableStateOf("09:15") }
    LaunchedEffect(Unit) {
        while (true) {
            sysTime = SimpleDateFormat("hh:mm", Locale.getDefault()).format(Date())
            delay(5000)
        }
    }

    // Real system measurements
    var systemBatteryPercent by remember { mutableStateOf(100) }
    var systemIsCharging by remember { mutableStateOf(false) }
    var systemWifiLevel by remember { mutableStateOf(4) }
    var systemCellLevel by remember { mutableStateOf(4) }
    var systemIsCellConnected by remember { mutableStateOf(true) }

    // Interactive user override states for demonstration & testing
    var userBatteryOverride by remember { mutableStateOf<Int?>(null) }
    var userIsChargingOverride by remember { mutableStateOf<Boolean?>(null) }
    var userWifiOverride by remember { mutableStateOf<Int?>(null) }
    var userCellOverride by remember { mutableStateOf<Int?>(null) }

    val batteryPercent = userBatteryOverride ?: systemBatteryPercent
    val isCharging = userIsChargingOverride ?: systemIsCharging
    val wifiSignalLevel = if (isWifiEnabled) (userWifiOverride ?: systemWifiLevel) else 0
    val cellSignalLevel = userCellOverride ?: systemCellLevel

    // Listen to Battery broadcasts
    DisposableEffect(context) {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    systemBatteryPercent = (level * 100 / scale.toFloat()).toInt()
                }
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                systemIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
        onDispose {
            context.unregisterReceiver(batteryReceiver)
        }
    }

    // Monitor Network type & Signal Strength dynamically
    LaunchedEffect(context, isWifiEnabled) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        while (true) {
            // Check Connectivity
            if (connectivityManager != null) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (capabilities != null) {
                    systemIsCellConnected = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        // Scan Wifi signal strength from active connection RSSI inside dynamic try-catch logic
                        try {
                            val rssi = wifiManager?.connectionInfo?.rssi ?: -50
                            // wifi signal level is standard index 0..4
                            systemWifiLevel = WifiManager.calculateSignalLevel(rssi, 5).coerceIn(0, 4)
                        } catch (e: SecurityException) {
                            // Suppress exception and fallback gracefully to default positive strength on restricted systems
                            systemWifiLevel = 4
                        } catch (e: Exception) {
                            systemWifiLevel = 4
                        }
                    } else {
                        systemWifiLevel = 0
                    }
                } else {
                    systemIsCellConnected = false
                    systemWifiLevel = 0
                }
            }

            // Periodically check (every 8 seconds)
            delay(8000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onSystemMenuClick() }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Area: ISOSpace Logo and Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { 
                onSystemMenuClick()
            }
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE95420)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ISOSpace Mobile",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            )
        }

        // Right Area: Live Status Indicators Tray
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Interactive Network Signal Indicator
            Box(
                modifier = Modifier
                    .clickable {
                        // Cycle simulated cell signal strength
                        userCellOverride = when (cellSignalLevel) {
                            4 -> 3
                            3 -> 2
                            2 -> 1
                            1 -> 0
                            else -> 4
                        }
                        Toast.makeText(context, "Cell Signal strength: $cellSignalLevel bars", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 4.dp)
            ) {
                CellularSignalStrength(level = cellSignalLevel, isConnected = systemIsCellConnected || userCellOverride != null)
            }

            // Interactive Battery Indicator
            Box(
                modifier = Modifier
                    .clickable {
                        // Cycle battery level overrides: 100% (charging) -> 85% (not charging) -> 50% -> 18% (low) -> 5% (critical) -> auto
                        val nextPct = when (batteryPercent) {
                            100 -> 85
                            85 -> 50
                            50 -> 18
                            18 -> 5
                            5 -> 100
                            else -> 100
                        }
                        userBatteryOverride = nextPct
                        userIsChargingOverride = (nextPct == 100 || nextPct == 5)
                        Toast.makeText(context, "Simulated Battery: $nextPct% ${if (nextPct == 100 || nextPct == 5) "(Charging)" else ""}", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$batteryPercent%",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    
                    Canvas(modifier = Modifier.size(width = 22.dp, height = 12.dp)) {
                        val strokeWidthPx = 1.dp.toPx()
                        val cornerRadiusPx = 2.dp.toPx()
                        val mHeight = size.height
                        val mWidth = size.width
                        
                        // Draw outline
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.5f),
                            size = Size(mWidth - 2.5.dp.toPx(), mHeight),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                        
                        // Terminal cap
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.5f),
                            topLeft = Offset(mWidth - 2.dp.toPx(), mHeight * 0.3f),
                            size = Size(2.dp.toPx(), mHeight * 0.4f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )
                        
                        // Fill block
                        val fillPercentage = batteryPercent.coerceIn(0, 100) / 100f
                        val fillWidth = (mWidth - 5.dp.toPx()) * fillPercentage
                        val fillColor = when {
                            isCharging -> Color(0xFF4CAF50) // Green when charging
                            batteryPercent <= 15 -> Color(0xFFF44336) // Red when critical
                            batteryPercent <= 30 -> Color(0xFFFF9800) // Orange when low
                            else -> Color.White // Normal white
                        }
                        
                        if (fillWidth > 0f) {
                            drawRoundRect(
                                color = fillColor,
                                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                                size = Size(fillWidth, mHeight - 3.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
                            )
                        }
                    }
                    if (isCharging) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Chg",
                            tint = Color(0xFFFFEB3B),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Real Time Clock Display
            Text(
                text = sysTime,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "System Menu",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// Custom Cellular Signal strength view
@Composable
fun CellularSignalStrength(level: Int, isConnected: Boolean) {
    Canvas(modifier = androidx.compose.ui.Modifier.size(width = 16.dp, height = 11.dp)) {
        val spacing = 1.5.dp.toPx()
        val totalBars = 4
        val barWidth = (size.width - (spacing * (totalBars - 1))) / totalBars
        
        for (i in 0 until totalBars) {
            val barHeight = size.height * ((i + 1) / totalBars.toFloat())
            val isActive = isConnected && level > i
            val color = if (isActive) Color.White else Color.White.copy(alpha = 0.25f)
            
            drawRect(
                color = color,
                topLeft = Offset(i * (barWidth + spacing), size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ISOSpaceSideDock(
    viewModel: LauncherViewModel,
    apps: List<AppItem>,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    dockSettings: LauncherSettings,
    modifier: Modifier = Modifier
) {
    val openedPackages by viewModel.openedApps.collectAsState()
    val dockItems = remember(apps, openedPackages) {
        openedPackages.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
    }
    var showExitConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 12.dp)
            .width(62.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(30.dp))
            .background(Color(dockSettings.dockColorHex).copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(30.dp)
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // App Launcher Icon (Dash Launcher - Grid icon)
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE95420))
                .clickable { viewModel.setDashOpen(true) }
                .testTag("dock_dash_launcher"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Open Dash Menu",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Launcher items matching traditional icons (Nautilus, Firefox, Terminal, settings)
        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dockItems) { app ->
                ISOSpaceDockAppIcon(
                    app = app,
                    onAppClick = { onAppClick(app) },
                    onAppLongClick = { onAppLongClick(app) },
                    iconSize = dockSettings.dockIconSizeDp
                )
            }
        }

        // Bottom Config Trigger icon
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF))
                .clickable { viewModel.setCustomizerOpen(true) }
                .testTag("dock_settings_launcher"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Launcher Configurations",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Exit Application Button
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFE95420).copy(alpha = 0.2f))
                .border(1.dp, Color(0xFFE95420), CircleShape)
                .clickable {
                    showExitConfirm = true
                }
                .testTag("dock_exit_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Exit Application",
                tint = Color(0xFFFF5555),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showExitConfirm) {
        val context = LocalContext.current
        Dialog(onDismissRequest = { showExitConfirm = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
                    .testTag("exit_confirmation_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE95420).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFE95420), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power Off",
                            tint = Color(0xFFFF5555),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Exit ISOSpace Launcher?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to shut down and exit the launcher environment?",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showExitConfirm = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("exit_cancel_button")
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showExitConfirm = false
                                val activity = context as? android.app.Activity
                                activity?.finish()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE95420)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("exit_confirm_button")
                        ) {
                            Text("Exit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ISOSpaceDockAppIcon(
    app: AppItem,
    onAppClick: () -> Unit,
    onAppLongClick: () -> Unit,
    iconSize: Int
) {
    // Fetch custom pack icon colors or standard Ubuntu
    val containerColor = when (app.packageName) {
        "com.isospace.terminal" -> Color(0xFF0C0C0C)
        "com.isospace.files" -> Color(0xFFE95420)
        "com.isospace.browser" -> Color(0xFFE05A10)
        "com.isospace.software" -> Color(0xFFA62D68)
        "com.isospace.settings" -> Color(0xFF4C4C4C)
        else -> Color(app.customColor ?: 0xFF77216F)
    }

    val iconVector = when (app.packageName) {
        "com.dummy.gallery" -> Icons.Default.Image
        "com.dummy.notes" -> Icons.Default.Description
        "com.dummy.files" -> Icons.Default.Folder
        "com.dummy.store" -> Icons.Default.ShoppingBag
        "com.dummy.camera" -> Icons.Default.PhotoCamera
        "com.dummy.clock" -> Icons.Default.Schedule
        "com.dummy.settings" -> Icons.Default.Settings
        "com.dummy.terminal" -> Icons.Default.Terminal
        "com.dummy.browser" -> Icons.Default.Language
        "com.dummy.telegram" -> Icons.Default.Send
        "com.dummy.instagram" -> Icons.Default.CameraAlt
        "com.dummy.whatsapp" -> Icons.Default.Call
        "com.dummy.vlc" -> Icons.Default.PlayArrow
        "com.isospace.terminal" -> Icons.Default.Terminal
        "com.isospace.files" -> Icons.Default.Folder
        "com.isospace.browser" -> Icons.Default.Language
        "com.isospace.software" -> Icons.Default.ShoppingBag
        "com.isospace.settings" -> Icons.Default.DisplaySettings
        "com.isospace.music" -> Icons.Default.MusicNote
        "com.isospace.email" -> Icons.Default.Email
        else -> Icons.Default.AppShortcut
    }

    Box(
        modifier = Modifier
            .size(iconSize.dp)
            .combinedClickable(
                onClick = onAppClick,
                onLongClick = onAppLongClick
            )
            .testTag("dock_app_${app.packageName}"),
        contentAlignment = Alignment.Center
    ) {
        // App Base Card Shaped with classic Ubuntu rounded squircles
        Box(
            modifier = Modifier
                .size((iconSize - 10).dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val isCustomPreset = app.packageName.startsWith("com.dummy") || app.packageName.startsWith("com.isospace")
            val systemIconDrawable = remember(app.packageName) {
                if (isCustomPreset) null else {
                    try {
                        val realPkg = if (app.packageName.startsWith("clone.")) app.packageName.substringAfter("clone.") else app.packageName
                        context.packageManager.getApplicationIcon(realPkg)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            if (systemIconDrawable != null) {
                AsyncImage(
                    model = systemIconDrawable,
                    contentDescription = app.label,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = iconVector,
                    contentDescription = app.label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Notification active badge dot
        if (app.counter > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .background(Color(0xFFE95420), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
                    .size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${app.counter}",
                    style = TextStyle(fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                )
            }
        }

    }
}

@Composable
fun GnomeDashOverlay(
    viewModel: LauncherViewModel,
    allApps: List<AppItem>,
    customCategories: List<String>,
    searchQuery: String,
    onClose: () -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    accentColor: Color
) {
    val filteredApps = allApps.filter { app ->
        !app.isHidden &&
        (searchQuery.isBlank() || app.label.contains(searchQuery, ignoreCase = true))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.25f),
                        Color(0xFF0F0F12).copy(alpha = 0.96f)
                    ),
                    center = Offset(400f, 1600f),
                    radius = 1800f
                )
            )
            .statusBarsPadding()
            .padding(top = 8.dp)
            .testTag("gnome_dash_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Close dash header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desktop Dash Search",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                )

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Dash", tint = Color.LightGray)
                }
            }

            // High priority search bar input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Type application name to search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedContainerColor = Color(0xFF222222),
                    unfocusedContainerColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("dash_search_input")
            )

            // Header for simplified single collection of apps
            Text(
                text = "Applications",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                ),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Apps vertical scroll grid
            Box(modifier = Modifier.weight(1f)) {
                if (filteredApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No applications match your filtering criteria.\nTap settings icon to append custom tags.",
                            style = TextStyle(color = Color.Gray, textAlign = TextAlign.Center, fontSize = 11.sp)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredApps) { app ->
                            AppLaunchGridCard(
                                app = app,
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onAppLongClick(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppLaunchGridCard(
    app: AppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val containerColor = when (app.packageName) {
        "com.isospace.terminal" -> Color(0xFF0A0A0A)
        "com.isospace.files" -> Color(0xFFE95420)
        "com.isospace.browser" -> Color(0xFFE05A10)
        "com.isospace.software" -> Color(0xFFA62D68)
        "com.isospace.settings" -> Color(0xFF4C4C4C)
        "com.isospace.music" -> Color(0xFFF57C00)
        else -> Color(app.customColor ?: 0xFF8A8A8A)
    }

    val iconVector = when (app.packageName) {
        "com.dummy.gallery" -> Icons.Default.Image
        "com.dummy.notes" -> Icons.Default.Description
        "com.dummy.files" -> Icons.Default.Folder
        "com.dummy.store" -> Icons.Default.ShoppingBag
        "com.dummy.camera" -> Icons.Default.PhotoCamera
        "com.dummy.clock" -> Icons.Default.Schedule
        "com.dummy.settings" -> Icons.Default.Settings
        "com.dummy.terminal" -> Icons.Default.Terminal
        "com.dummy.browser" -> Icons.Default.Language
        "com.dummy.telegram" -> Icons.Default.Send
        "com.dummy.instagram" -> Icons.Default.CameraAlt
        "com.dummy.whatsapp" -> Icons.Default.Call
        "com.dummy.vlc" -> Icons.Default.PlayArrow
        "com.isospace.terminal" -> Icons.Default.Terminal
        "com.isospace.files" -> Icons.Default.Folder
        "com.isospace.browser" -> Icons.Default.Language
        "com.isospace.software" -> Icons.Default.ShoppingBag
        "com.isospace.settings" -> Icons.Default.DisplaySettings
        "com.isospace.music" -> Icons.Default.MusicNote
        "com.isospace.email" -> Icons.Default.Email
        else -> Icons.Default.Android
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp)
            .testTag("drawer_app_${app.packageName}"),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            containerColor.copy(alpha = 0.95f),
                            containerColor.copy(alpha = 0.7f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val isCustomPreset = app.packageName.startsWith("com.dummy") || app.packageName.startsWith("com.isospace")
            val systemIconDrawable = remember(app.packageName) {
                if (isCustomPreset) null else {
                    try {
                        val realPkg = if (app.packageName.startsWith("clone.")) app.packageName.substringAfter("clone.") else app.packageName
                        context.packageManager.getApplicationIcon(realPkg)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            if (systemIconDrawable != null) {
                AsyncImage(
                    model = systemIconDrawable,
                    contentDescription = app.label,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = iconVector,
                    contentDescription = app.label,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(74.dp)
        )
    }
}

@Composable
fun ISOSpaceControlCenterOverlay(
    viewModel: LauncherViewModel,
    onClose: () -> Unit,
    accentColor: Color
) {
    val brightness by viewModel.brightness.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val isWifiEnabled by viewModel.isWifiEnabled.collectAsState()
    val isBlTEnabled by viewModel.isBluetoothEnabled.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("isospace_control_center"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFA1E1E1E)),
        border = BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Drop zone slide close hook anchor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .clickable { onClose() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "System Toggles & Controls",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // WiFi Bluetooth quick actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickToggleActionItem(
                    label = "Wi-Fi Network",
                    subtext = if (isWifiEnabled) "Connected" else "Offline",
                    isActive = isWifiEnabled,
                    icon = Icons.Default.Wifi,
                    onClick = { viewModel.toggleWifi() },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                QuickToggleActionItem(
                    label = "Bluetooth Link",
                    subtext = if (isBlTEnabled) "Active" else "Disabled",
                    isActive = isBlTEnabled,
                    icon = Icons.Default.Bluetooth,
                    onClick = { viewModel.toggleBluetooth() },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Brightness range slide bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LightMode, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Slider(
                    value = brightness,
                    onValueChange = { viewModel.updateBrightness(it) },
                    colors = SliderDefaults.colors(
                        activeTrackColor = accentColor,
                        thumbColor = accentColor
                    ),
                    modifier = Modifier.weight(1f).testTag("brightness_slider")
                )
            }

            // Volume range slide bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Slider(
                    value = volume,
                    onValueChange = { viewModel.updateVolume(it) },
                    colors = SliderDefaults.colors(
                        activeTrackColor = accentColor,
                        thumbColor = accentColor
                    ),
                    modifier = Modifier.weight(1f).testTag("volume_slider")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ubuntu customized dialog trigger button inside tray
            Button(
                onClick = {
                    onClose()
                    viewModel.setCustomizerOpen(true)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_settings_adjust_button")
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Modify Desktop & Gestures Dashboard", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onClose()
                    viewModel.logout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_settings_logout_button")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lock & Logout Secure Sandbox", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun QuickToggleActionItem(
    label: String,
    subtext: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.2f) else Color(0xFF292929))
            .border(1.dp, if (isActive) accentColor else Color(0x11FFFFFF), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isActive) accentColor else Color(0xFF383838)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtext, color = Color.Gray, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
fun ISOSpaceCustomizerDialog(
    viewModel: LauncherViewModel,
    settings: LauncherSettings,
    customCategories: List<String>,
    onClose: () -> Unit,
    onLaunchTutorial: () -> Unit
) {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.updateSettings(settings.copy(backgroundWallpaperUrl = uri.toString()))
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Layout", "Theme & Style", "Gestures Control", "Add Widgets", "Additional")
    
    val activeAccentColor = Color(if (settings.useCustomColors) settings.customAccentColorHex else settings.accentColor.hexAccent)
    val activeBgColor = Color(if (settings.useCustomColors) settings.customBgColorHex else settings.accentColor.hexThemeBg)

    // Custom category input
    var newCatName by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
                .testTag("isospace_customizer_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEC0E0E12)),
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ISOSpace Customization Configuration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Delimiter()

                // Horizontal menu selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(settings.accentColor.hexAccent),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            color = Color(settings.accentColor.hexAccent),
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab])
                        )
                    }
                ) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            unselectedContentColor = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Customizer Body
                Box(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> { // Layout Tab
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                CustomizerHeaderLabel("Workspace Grid Configurations")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    GridSizeSelector(
                                        label = "Desktop Columns (${settings.desktopColumns})",
                                        value = settings.desktopColumns,
                                        onSelected = { viewModel.setDesktopLayout(it, settings.desktopRows) }
                                    )
                                    GridSizeSelector(
                                        label = "Desktop Rows (${settings.desktopRows})",
                                        value = settings.desktopRows,
                                        onSelected = { viewModel.setDesktopLayout(settings.desktopColumns, it) }
                                    )
                                }

                                CustomizerHeaderLabel("Side Applications Dock")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Dock Position Side:", color = Color.LightGray, fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (pos in listOf(DockPosition.LEFT, DockPosition.RIGHT, DockPosition.BOTTOM, DockPosition.HIDDEN)) {
                                            val isSelected = settings.dockPosition == pos
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) Color(settings.accentColor.hexAccent) else Color(0xFF333333))
                                                    .clickable { viewModel.setDockPosition(pos) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(pos.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Dock Icons Zoom (dp): ${settings.dockIconSizeDp}", color = Color.LightGray, fontSize = 12.sp)
                                    Slider(
                                        value = settings.dockIconSizeDp.toFloat(),
                                        onValueChange = { viewModel.updateSettings(settings.copy(dockIconSizeDp = it.roundToInt())) },
                                        valueRange = 36f..72f,
                                        colors = SliderDefaults.colors(activeTrackColor = Color(settings.accentColor.hexAccent)),
                                        modifier = Modifier.width(140.dp)
                                    )
                                }

                                ToggleCustomizerOption(
                                    label = "Hide side-mounted Dock unless swiped",
                                    checked = settings.isDockAutohideEnabled,
                                    onCheckedChange = { viewModel.setDockAutohide(it) }
                                )

                                ToggleCustomizerOption(
                                    label = "Show desktop applications labels",
                                    checked = settings.showIconLabels,
                                    onCheckedChange = { viewModel.setShowIconLabels(it) }
                                )
                            }
                        }
                        1 -> { // Theme & styling tab
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                CustomizerHeaderLabel("Select ISOSpace Accent Palette Theme")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (colorPreset in AccentColor.values()) {
                                        val isCurrent = !settings.useCustomColors && settings.accentColor == colorPreset
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.updateSettings(settings.copy(
                                                        accentColor = colorPreset,
                                                        useCustomColors = false
                                                    ))
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(colorPreset.hexAccent))
                                                    .border(
                                                        2.dp,
                                                        if (isCurrent) Color.White else Color.Transparent,
                                                        CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(colorPreset.label, color = Color.White, fontSize = 9.sp)
                                        }
                                    }
                                }

                                ToggleCustomizerOption(
                                    label = "Enable Custom RGB Color Mode",
                                    checked = settings.useCustomColors,
                                    onCheckedChange = { viewModel.updateSettings(settings.copy(useCustomColors = it)) }
                                )

                                if (settings.useCustomColors) {
                                    val rAccent = ((settings.customAccentColorHex shr 16) and 0xFF).toInt()
                                    val gAccent = ((settings.customAccentColorHex shr 8) and 0xFF).toInt()
                                    val bAccent = (settings.customAccentColorHex and 0xFF).toInt()

                                    val rBg = ((settings.customBgColorHex shr 16) and 0xFF).toInt()
                                    val gBg = ((settings.customBgColorHex shr 8) and 0xFF).toInt()
                                    val bBg = (settings.customBgColorHex and 0xFF).toInt()

                                    RgbColorChooser(
                                        label = "Custom Accent RGB Color",
                                        red = rAccent,
                                        green = gAccent,
                                        blue = bAccent,
                                        onValueChange = { r, g, b ->
                                            val newHex = 0xFF000000L or ((r.toLong() and 0xFF) shl 16) or ((g.toLong() and 0xFF) shl 8) or (b.toLong() and 0xFF)
                                            viewModel.updateSettings(settings.copy(customAccentColorHex = newHex))
                                        },
                                        activeColor = activeAccentColor
                                    )

                                    RgbColorChooser(
                                        label = "Custom Background RGB Color",
                                        red = rBg,
                                        green = gBg,
                                        blue = bBg,
                                        onValueChange = { r, g, b ->
                                            val newHex = 0xFF000000L or ((r.toLong() and 0xFF) shl 16) or ((g.toLong() and 0xFF) shl 8) or (b.toLong() and 0xFF)
                                            viewModel.updateSettings(settings.copy(customBgColorHex = newHex))
                                        },
                                        activeColor = activeAccentColor
                                    )
                                }

                                CustomizerHeaderLabel("Configure Dock Panel Color")
                                val rDock = ((settings.dockColorHex shr 16) and 0xFF).toInt()
                                val gDock = ((settings.dockColorHex shr 8) and 0xFF).toInt()
                                val bDock = (settings.dockColorHex and 0xFF).toInt()

                                RgbColorChooser(
                                    label = "Custom Dock Panel RGB Color",
                                    red = rDock,
                                    green = gDock,
                                    blue = bDock,
                                    onValueChange = { r, g, b ->
                                        val newHex = 0xE6000000L or ((r.toLong() and 0xFF) shl 16) or ((g.toLong() and 0xFF) shl 8) or (b.toLong() and 0xFF)
                                        viewModel.updateSettings(settings.copy(dockColorHex = newHex))
                                    },
                                    activeColor = activeAccentColor
                                )

                                CustomizerHeaderLabel("Select Icon Pack Design")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Custom Icon style Set:", color = Color.LightGray, fontSize = 12.sp)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        for (ip in IconPack.values()) {
                                            val isSelected = settings.iconPack == ip
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) activeAccentColor else Color(0xFF333333))
                                                    .clickable { viewModel.setIconPack(ip) }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(ip.label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                CustomizerHeaderLabel("Custom Workspace Ambient Background")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Select Wallpaper Canvas:", color = Color.LightGray, fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            "misty_mountain" to "Misty Alpine",
                                            "dynamic_gradient" to "Smooth Fluid",
                                            "starry" to "Starry Night"
                                        ).forEach { (code, title) ->
                                            val isSelected = settings.backgroundWallpaperUrl == code
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) activeAccentColor else Color(0xFF333333))
                                                    .clickable { viewModel.setWallpaper(code) }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(title, color = Color.White, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Custom Photo Wallpaper:", color = Color.LightGray, fontSize = 12.sp)
                                    Button(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pick from Gallery", fontSize = 11.sp, color = Color.White)
                                    }
                                }

                                if (settings.backgroundWallpaperUrl.startsWith("content://") ||
                                    settings.backgroundWallpaperUrl.startsWith("file://") ||
                                    settings.backgroundWallpaperUrl.contains("/")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Selected Image URI:", color = Color.LightGray, fontSize = 11.sp)
                                        Text(
                                            text = "Custom Wallpaper Active",
                                            color = activeAccentColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                        )
                                        IconButton(
                                            onClick = { viewModel.setWallpaper("dynamic_gradient") },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear Wallpaper", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                        2 -> { // Gestures tab
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                CustomizerHeaderLabel("Gestures System Actions")
                                GestureTriggerRow("Swipe UP overlay", settings.gestureSwipeUp) {
                                    viewModel.updateSettings(settings.copy(gestureSwipeUp = it))
                                }
                                GestureTriggerRow("Swipe DOWN panels", settings.gestureSwipeDown) {
                                    viewModel.updateSettings(settings.copy(gestureSwipeDown = it))
                                }
                                GestureTriggerRow("Double TAB screens", settings.gestureDoubleTap) {
                                    viewModel.updateSettings(settings.copy(gestureDoubleTap = it))
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onLaunchTutorial,
                                    colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("launch_tutorial_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Launch Gesture Interactive Tutorial", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        3 -> { // Add Widgets Tab
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CustomizerHeaderLabel("Add Widgets on Home Grid")
                                WidgetAddRow(WidgetType.ISOSPACE_CLOCK, "Bold ISOSpace Date/Clock", Icons.Default.AccessTime, Color(settings.accentColor.hexAccent)) {
                                    viewModel.addWidget(WidgetType.ISOSPACE_CLOCK)
                                }
                                WidgetAddRow(WidgetType.SYSTEM_MONITOR, "Advanced Mon conky monitor", Icons.Default.Analytics, Color(settings.accentColor.hexAccent)) {
                                    viewModel.addWidget(WidgetType.SYSTEM_MONITOR)
                                }
                                WidgetAddRow(WidgetType.BASH_TERMINAL, "Interactive ISOSpace Linux Terminal note", Icons.Default.Terminal, Color(settings.accentColor.hexAccent)) {
                                    viewModel.addWidget(WidgetType.BASH_TERMINAL)
                                }
                                WidgetAddRow(WidgetType.WEATHER_INFO, "Micro Weather card", Icons.Default.CloudQueue, Color(settings.accentColor.hexAccent)) {
                                    viewModel.addWidget(WidgetType.WEATHER_INFO)
                                }
                            }
                        }
                        4 -> { // Additional tab (Hardcore settings)
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                CustomizerHeaderLabel("Hardcore Additional Settings")
                                
                                ToggleCustomizerOption(
                                    label = "Show Linux Kernel boot logs on startup",
                                    checked = settings.kernelBootLogsEnabled,
                                    onCheckedChange = { viewModel.updateSettings(settings.copy(kernelBootLogsEnabled = it)) }
                                )

                                ToggleCustomizerOption(
                                    label = "Enable Turbo Performance Overclocking Mode",
                                    checked = settings.turboPerformanceMode,
                                    onCheckedChange = { viewModel.updateSettings(settings.copy(turboPerformanceMode = it)) }
                                )

                                var passwordText by remember { mutableStateOf(settings.terminalPassword) }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Simulated Terminal Root Password", color = Color.LightGray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = passwordText,
                                        onValueChange = {
                                            passwordText = it
                                            viewModel.updateSettings(settings.copy(terminalPassword = it))
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = activeAccentColor,
                                            unfocusedBorderColor = Color.Gray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    )
                                }

                                Delimiter()

                                CustomizerHeaderLabel("Secure Sandbox Credentials")

                                var sandboxUserText by remember { mutableStateOf(viewModel.getSandboxUsername()) }
                                var sandboxPassText by remember { mutableStateOf(viewModel.getSandboxPassword()) }

                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column {
                                        Text("Sandbox Login Username", color = Color.LightGray, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        OutlinedTextField(
                                            value = sandboxUserText,
                                            onValueChange = { sandboxUserText = it },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = activeAccentColor,
                                                unfocusedBorderColor = Color.Gray,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            textStyle = TextStyle(fontSize = 11.sp)
                                        )
                                    }

                                    Column {
                                        Text("Sandbox Login Password", color = Color.LightGray, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        OutlinedTextField(
                                            value = sandboxPassText,
                                            onValueChange = { sandboxPassText = it },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = activeAccentColor,
                                                unfocusedBorderColor = Color.Gray,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            textStyle = TextStyle(fontSize = 11.sp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (sandboxUserText.trim().isNotEmpty() && sandboxPassText.trim().isNotEmpty()) {
                                                viewModel.updateSandboxCredentials(sandboxUserText.trim(), sandboxPassText)
                                                Toast.makeText(context, "Sandbox credentials updated successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Username or password cannot be empty!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Save Sandbox Credentials", fontSize = 11.sp)
                                    }
                                }

                                Delimiter()

                                CustomizerHeaderLabel("Secure Sandbox Storage Analyzer")

                                val sandboxDir = remember { context.filesDir.resolve(".isospace_encrypted_sandbox") }
                                var triggerStorageReload by remember { mutableStateOf(0) }

                                val storageStats by produceState(
                                    initialValue = SandboxStorageStats(),
                                    key1 = triggerStorageReload
                                ) {
                                    value = calculateSandboxStorageStats(sandboxDir, viewModel.getSandboxSizeLimitGb())
                                }

                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val usedFraction = if (storageStats.limitBytes > 0) {
                                        storageStats.totalUsedBytes.toFloat() / storageStats.limitBytes.toFloat()
                                    } else 0f
                                    
                                    LinearProgressIndicator(
                                        progress = { usedFraction },
                                        color = activeAccentColor,
                                        trackColor = Color.Gray.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Used: ${formatBytes(storageStats.totalUsedBytes)} / ${viewModel.getSandboxSizeLimitGb()} GB",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Free: ${formatBytes(storageStats.availableBytes)}",
                                            color = Color.Green,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(
                                                "Photos" to Icons.Default.Image,
                                                "Videos" to Icons.Default.Videocam,
                                                "Notes" to Icons.Default.StickyNote2,
                                                "Audio" to Icons.Default.Audiotrack,
                                                "Documents" to Icons.Default.Description,
                                                "Other" to Icons.Default.FolderOpen
                                            ).forEach { (category, icon) ->
                                                val count = storageStats.fileCounts[category] ?: 0
                                                val size = storageStats.fileSizes[category] ?: 0L
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(icon, contentDescription = null, tint = activeAccentColor, modifier = Modifier.size(16.dp))
                                                        Text(category, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                    Text(
                                                        text = "$count files (${formatBytes(size)})",
                                                        color = Color.LightGray,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text("Delete Individual Sandbox Files", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                    if (storageStats.allFiles.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(60.dp)
                                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No files present in Sandbox.", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    } else {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                                        ) {
                                            LazyColumn(
                                                contentPadding = PaddingValues(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                items(storageStats.allFiles) { itemStats ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color(0xFF222222), RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                            Text(
                                                                text = itemStats.file.name,
                                                                color = Color.White,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "${itemStats.category} • ${formatBytes(itemStats.size)}",
                                                                color = Color.LightGray,
                                                                fontSize = 9.sp
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                try {
                                                                    if (itemStats.file.delete()) {
                                                                        triggerStorageReload++
                                                                        Toast.makeText(context, "Deleted file: ${itemStats.file.name}", Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        Toast.makeText(context, "Could not delete file.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                } catch (e: java.lang.Exception) {
                                                                    e.printStackTrace()
                                                                    Toast.makeText(context, "Error deleting file.", Toast.LENGTH_SHORT).show()
                                                                }
                                                            },
                                                            modifier = Modifier.size(24.dp).testTag("delete_sandbox_file_${itemStats.file.name}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete File",
                                                                tint = Color.Red,
                                                                modifier = Modifier.size(16.dp)
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
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Edge Swipe Sensitivity (${settings.edgeSwipeSensitivity} dp):", color = Color.LightGray, fontSize = 12.sp)
                                    Slider(
                                        value = settings.edgeSwipeSensitivity.toFloat(),
                                        onValueChange = { viewModel.updateSettings(settings.copy(edgeSwipeSensitivity = it.roundToInt())) },
                                        valueRange = 5f..40f,
                                        colors = SliderDefaults.colors(activeTrackColor = activeAccentColor),
                                        modifier = Modifier.width(140.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Animation Speed Factor (${settings.animationSpeedMultiplier}x):", color = Color.LightGray, fontSize = 12.sp)
                                    Slider(
                                        value = settings.animationSpeedMultiplier,
                                        onValueChange = { viewModel.updateSettings(settings.copy(animationSpeedMultiplier = it)) },
                                        valueRange = 0.1f..4.0f,
                                        colors = SliderDefaults.colors(activeTrackColor = activeAccentColor),
                                        modifier = Modifier.width(140.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Delimiter()

                // Final Button
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Layout Transformations")
                }
            }
        }
    }
}

@Composable
fun GridSizeSelector(
    label: String,
    value: Int,
    onSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.width(140.dp)) {
        Text(label, color = Color.LightGray, fontSize = 11.sp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 3..7) {
                val isSelected = value == i
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) UbuntuWarmOrange else Color(0xFF333333))
                        .clickable { onSelected(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("$i", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RgbColorChooser(
    label: String,
    red: Int,
    green: Int,
    blue: Int,
    onValueChange: (Int, Int, Int) -> Unit,
    activeColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF222222))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF000000L or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()))
                    .border(1.dp, Color.White, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        // Red slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R", color = Color.Red, fontSize = 10.sp, modifier = Modifier.width(14.dp))
            Slider(
                value = red.toFloat(),
                onValueChange = { onValueChange(it.roundToInt(), green, blue) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(activeTrackColor = Color.Red),
                modifier = Modifier.weight(1f)
            )
            Text(red.toString(), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
        }
        // Green slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G", color = Color.Green, fontSize = 10.sp, modifier = Modifier.width(14.dp))
            Slider(
                value = green.toFloat(),
                onValueChange = { onValueChange(red, it.roundToInt(), blue) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(activeTrackColor = Color.Green),
                modifier = Modifier.weight(1f)
            )
            Text(green.toString(), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
        }
        // Blue slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B", color = Color.Blue, fontSize = 10.sp, modifier = Modifier.width(14.dp))
            Slider(
                value = blue.toFloat(),
                onValueChange = { onValueChange(red, green, it.roundToInt()) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(activeTrackColor = Color.Blue),
                modifier = Modifier.weight(1f)
            )
            Text(blue.toString(), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
        }
    }
}

@Composable
fun ToggleCustomizerOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.LightGray, fontSize = 12.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = UbuntuWarmOrange,
                checkedTrackColor = UbuntuWarmOrange.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun GestureTriggerRow(
    label: String,
    currentAction: GestureAction,
    onSelected: (GestureAction) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (act in listOf(GestureAction.OPEN_DRAWER, GestureAction.OPEN_CONTROL_CENTER, GestureAction.OPEN_SETTINGS, GestureAction.LOCK_SCREEN)) {
                val isSelected = currentAction == act
                val actLabel = when(act) {
                    GestureAction.OPEN_DRAWER -> "Dash"
                    GestureAction.OPEN_CONTROL_CENTER -> "Tray"
                    GestureAction.OPEN_SETTINGS -> "Config"
                    GestureAction.LOCK_SCREEN -> "Lock"
                    else -> "NoOp"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) UbuntuWarmOrange else Color(0xFF333333))
                        .clickable { onSelected(act) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(actLabel, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WidgetAddRow(
    type: WidgetType,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2B2B2B))
            .clickable { onAdd() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.Default.AddCircle, contentDescription = "Add Widget", tint = accentColor, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun CustomizerHeaderLabel(txt: String) {
    Text(
        text = txt,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = UbuntuWarmOrange,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun Delimiter() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .background(Color(0x1AFFFFFF))
    )
}

@Composable
fun AppContextSettingsDialog(
    app: AppItem,
    allCategories: List<String>,
    isPinnedToDock: Boolean,
    onToggleDockPin: (Boolean) -> Unit,
    onClose: () -> Unit,
    onSaveCustomValues: (String, String, Boolean) -> Unit,
    onUninstallApp: () -> Unit,
    isAppRunning: Boolean = false,
    onCloseApp: () -> Unit = {}
) {
    var editLabel by remember { mutableStateOf(app.label) }
    var selectedCategory by remember { mutableStateOf(app.category) }
    var isHidden by remember { mutableStateOf(app.isHidden) }
    var isPinned by remember { mutableStateOf(isPinnedToDock) }

    Dialog(onDismissRequest = onClose) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Configure Application Node",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editLabel,
                    onValueChange = { editLabel = it },
                    label = { Text("Custom App Label") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = UbuntuWarmOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pin to ISOSpace side dock panel:", color = Color.LightGray, fontSize = 12.sp)
                    Switch(
                        checked = isPinned,
                        onCheckedChange = {
                            isPinned = it
                            onToggleDockPin(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = UbuntuWarmOrange)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hide application from Dash drawer:", color = Color.LightGray, fontSize = 12.sp)
                    Switch(
                        checked = isHidden,
                        onCheckedChange = { isHidden = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = UbuntuWarmOrange)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onUninstallApp,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uninstall", fontSize = 11.sp)
                        }

                        if (isAppRunning) {
                            Button(
                                onClick = {
                                    onCloseApp()
                                    onClose()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Terminate", fontSize = 11.sp)
                            }
                        }

                        TextButton(onClick = onClose) {
                            Text("Discard", color = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (app.packageName.startsWith("clone.")) {
                        val context = LocalContext.current
                        Button(
                            onClick = {
                                val targetPkg = app.packageName.substringAfter("clone.")
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPkg)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                    onClose()
                                } else {
                                    Toast.makeText(context, "Launch failed: Original app not found on system", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Functional Dual App", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { onSaveCustomValues(editLabel, selectedCategory, isHidden) },
                        colors = ButtonDefaults.buttonColors(containerColor = UbuntuWarmOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Settings", color = Color.White)
                    }
                }
            }
        }
    }
}

// Simulated App calculator Modal dialog
@Composable
fun ShellCalculatorModal(onClose: () -> Unit) {
    var display by rememberSaveable { mutableStateOf("0") }
    Dialog(onDismissRequest = onClose) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEC0E0E12)),
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            ),
            modifier = Modifier.size(290.dp, 395.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Calculator title bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("ISOSpace Calculator", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x59000000))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(display, color = Color(0xFF33FF33), fontSize = 26.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mini Keys Matrix
                val keys = listOf(
                    listOf("7", "8", "9", "/"),
                    listOf("4", "5", "6", "*"),
                    listOf("1", "2", "3", "-"),
                    listOf("C", "0", "=", "+")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in keys) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            for (key in row) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (key == "=") Brush.verticalGradient(listOf(Color(0xFFE95420), Color(0xFFB03A10)))
                                            else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (key == "=") Color(0xFFE95420).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            when (key) {
                                                "C" -> display = "0"
                                                "=" -> {
                                                    // Simple expression evaluate simulation
                                                    try {
                                                        val result = if (display.contains("+")) {
                                                            val parts = display.split("+")
                                                            (parts[0].toDouble() + parts[1].toDouble()).toString()
                                                        } else if (display.contains("-")) {
                                                            val parts = display.split("-")
                                                            (parts[0].toDouble() - parts[1].toDouble()).toString()
                                                        } else if (display.contains("*")) {
                                                            val parts = display.split("*")
                                                            (parts[0].toDouble() * parts[1].toDouble()).toString()
                                                        } else if (display.contains("/")) {
                                                            val parts = display.split("/")
                                                            (parts[0].toDouble() / parts[1].toDouble()).toString()
                                                        } else display
                                                        display = result.removeSuffix(".0")
                                                    } catch (e: Exception) {
                                                        display = "Error"
                                                    }
                                                }

                                                else -> {
                                                    if (display == "0" || display == "Error") {
                                                        display = key
                                                    } else {
                                                        display += key
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(key, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simulated App Web Browser Modal dialog
@Composable
fun ShellWebBrowserModal(onClose: () -> Unit) {
    var webUrl by rememberSaveable { mutableStateOf("https://isospace.org") }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEC0E0E12)),
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            ),
            modifier = Modifier.fillMaxWidth(0.92f).height(485.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Browser URL address bar row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = UbuntuWarmOrange, modifier = Modifier.size(18.dp))
                    
                    var isUrlFocused by remember { mutableStateOf(false) }
                    androidx.compose.foundation.text.BasicTextField(
                        value = webUrl,
                        onValueChange = { webUrl = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(UbuntuWarmOrange),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(Color(0x33000000), RoundedCornerShape(6.dp))
                            .border(
                                1.dp,
                                if (isUrlFocused) UbuntuWarmOrange else Color.DarkGray,
                                RoundedCornerShape(6.dp)
                            )
                            .onFocusChanged { isUrlFocused = it.isFocused },
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (webUrl.isEmpty()) {
                                    Text(
                                        text = "Enter website URL or search...",
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated webpage body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Custom vector logo for simulated page
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE95420)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Adjust, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ISOSpace Enterprise Mobile",
                            color = Color(0xFF2C001E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connected in Desktop browser mode successfully.",
                            color = Color.DarkGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { webUrl = "https://wiki.isospace.org" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C001E))
                        ) {
                            Text("Browse Documentation wiki")
                        }
                    }
                }
            }
        }
    }
}



// Logic to launch or simulate preset virtual apps
private fun launchOrSimulateApp(
    context: Context,
    app: AppItem,
    viewModel: LauncherViewModel,
    onSimulate: (String) -> Unit
) {
    viewModel.openApp(app.packageName)
    if (app.packageName.startsWith("clone.")) {
        viewModel.clearNotificationBadge(app.packageName)
        onSimulate(app.packageName)
    } else if (app.packageName.startsWith("com.dummy") || app.packageName.startsWith("com.isospace")) {
        // Handle virtual/simulated apps inside launcher to eliminate dead ends
        viewModel.clearNotificationBadge(app.packageName)
        when (app.packageName) {
            "com.dummy.terminal", "com.isospace.terminal" -> {
                onSimulate("terminal")
            }
            "com.dummy.settings", "com.isospace.settings" -> {
                viewModel.setCustomizerOpen(true)
            }
            "com.dummy.files", "com.isospace.files" -> {
                onSimulate("files")
            }
            "com.dummy.browser", "com.isospace.browser" -> {
                onSimulate("browser")
            }
            "com.dummy.store", "com.isospace.software" -> {
                onSimulate("software")
            }
            "com.dummy.clock" -> {
                onSimulate("clock")
            }
            "com.dummy.gallery" -> {
                onSimulate("gallery")
            }
            "com.dummy.notes" -> {
                onSimulate("notes")
            }
            "com.dummy.camera" -> {
                onSimulate("camera")
            }
            "com.dummy.calculator" -> {
                onSimulate("calculator")
            }
            "com.dummy.telegram" -> {
                Toast.makeText(context, "Telegram Chat initialized! Notification badge cleared.", Toast.LENGTH_SHORT).show()
            }
            "com.dummy.instagram" -> {
                Toast.makeText(context, "Instagram: Loading stories feed...", Toast.LENGTH_SHORT).show()
            }
            "com.dummy.whatsapp" -> {
                Toast.makeText(context, "WhatsApp: Chat engine online! Notification badge cleared.", Toast.LENGTH_SHORT).show()
            }
            "com.dummy.vlc" -> {
                onSimulate("vlc")
            }
            else -> {
                onSimulate("calculator")
            }
        }
    } else {
        // Launch real physical Android app
        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            viewModel.clearNotificationBadge(app.packageName)
        } else {
            Toast.makeText(context, "Error: App launcher target missing physically on emulator", Toast.LENGTH_SHORT).show()
        }
    }
}

// --- Sandbox Storage Analyzer Helper Utilities ---

data class SandboxStorageStats(
    val totalUsedBytes: Long = 0L,
    val limitBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val fileCounts: Map<String, Int> = emptyMap(),
    val fileSizes: Map<String, Long> = emptyMap(),
    val allFiles: List<FileItemStats> = emptyList()
)

data class FileItemStats(
    val file: java.io.File,
    val category: String,
    val size: Long,
    val relativePath: String
)

fun calculateSandboxStorageStats(sandboxDir: java.io.File, sizeLimitGb: Int): SandboxStorageStats {
    val limitBytes = sizeLimitGb * 1024L * 1024L * 1024L
    if (!sandboxDir.exists()) {
        return SandboxStorageStats(limitBytes = limitBytes, availableBytes = limitBytes)
    }
    
    val allFiles = mutableListOf<FileItemStats>()
    var totalUsedBytes = 0L
    
    val counts = mutableMapOf(
        "Photos" to 0,
        "Videos" to 0,
        "Notes" to 0,
        "Audio" to 0,
        "Documents" to 0,
        "Other" to 0
    )
    
    val sizes = mutableMapOf(
        "Photos" to 0L,
        "Videos" to 0L,
        "Notes" to 0L,
        "Audio" to 0L,
        "Documents" to 0L,
        "Other" to 0L
    )
    
    fun walk(file: java.io.File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { walk(it) }
        } else {
            val size = file.length()
            totalUsedBytes += size
            
            val parentName = file.parentFile?.name ?: ""
            val ext = file.extension.lowercase()
            val category = when {
                parentName == "Photos" || ext in listOf("png", "jpg", "jpeg", "webp", "gif") -> "Photos"
                parentName == "Videos" || ext in listOf("mp4", "mkv", "webm", "avi") -> "Videos"
                parentName == "Notes" -> "Notes"
                parentName == "Audio" || ext in listOf("mp3", "wav", "m4a", "ogg") -> "Audio"
                parentName == "Documents" || ext in listOf("txt", "pdf", "doc", "docx", "json") -> "Documents"
                else -> "Other"
            }
            
            counts[category] = (counts[category] ?: 0) + 1
            sizes[category] = (sizes[category] ?: 0L) + size
            
            val relPath = file.absolutePath.substringAfter(".isospace_encrypted_sandbox/", file.name)
            allFiles.add(FileItemStats(file, category, size, relPath))
        }
    }
    
    walk(sandboxDir)
    allFiles.sortByDescending { it.file.lastModified() }
    
    val availableBytes = (limitBytes - totalUsedBytes).coerceAtLeast(0L)
    
    return SandboxStorageStats(
        totalUsedBytes = totalUsedBytes,
        limitBytes = limitBytes,
        availableBytes = availableBytes,
        fileCounts = counts,
        fileSizes = sizes,
        allFiles = allFiles
    )
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
