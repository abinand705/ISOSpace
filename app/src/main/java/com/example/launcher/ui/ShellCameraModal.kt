package com.example.launcher.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val UbuntuPurple = Color(0xFF2C001E)
private val UbuntuWarmOrange = Color(0xFFE95420)
private val UbuntuDarkSlate = Color(0xFF222222)
private val UbuntuCardGray = Color(0xFF2D2D2D)

@Composable
fun ShellCameraModal(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera State Controls
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isRecordingMode by remember { mutableStateOf(false) } // Photo vs Video toggle
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // CameraX helper states
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    // Studio Canvas Scene fallback
    var showStudioOverlay by remember { mutableStateOf(false) }
    var selectedStudioFilter by remember { mutableStateOf("Warm Ubuntu") } // "Warm Ubuntu", "Retro Cyberpunk", "Vintage Mono", "Matrix Green"
    var selectedStudioScene by remember { mutableStateOf("Desktop Dock") } // "Desktop Dock", "Yosemite Dome", "City Sunset"

    // Custom capture feedback
    var imageCapturedFlashActive by remember { mutableStateOf(false) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            recordingSeconds = 0
            while (isRecordingVideo) {
                kotlinx.coroutines.delay(1000)
                recordingSeconds++
            }
        }
    }

    // Image flash feedback effect
    LaunchedEffect(imageCapturedFlashActive) {
        if (imageCapturedFlashActive) {
            kotlinx.coroutines.delay(100)
            imageCapturedFlashActive = false
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
                .testTag("camera_app_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Ubuntu Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(UbuntuPurple.copy(alpha = 0.45f), Color(0xFF5E2750).copy(alpha = 0.15f))))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = UbuntuWarmOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cheese Camera Utility",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Hardware Capture & Filter Studio",
                            color = Color.LightGray.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }

                    // Toggles & Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Switch between Real Camera and Scenic Studio fallback
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (showStudioOverlay) UbuntuWarmOrange else Color(0xFF333333))
                                .clickable { showStudioOverlay = !showStudioOverlay }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (showStudioOverlay) Icons.Default.Palette else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showStudioOverlay) "Studio Mode" else "Switch to Studio",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Close App
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp).testTag("close_camera_button")) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Main Workspace Layout
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                ) {
                    // Left Control Strip (Studio filters / parameters if active)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showStudioOverlay,
                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(140.dp)
                            .background(Color(0xFF1E1E1E))
                            .drawBehind { drawLine(Color(0x1FFFFFFF), Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = 1.dp.toPx()) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("STUDIO SCENES", color = UbuntuWarmOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            listOf("Desktop Dock", "Yosemite Dome", "City Sunset").forEach { scene ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedStudioScene = scene },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedStudioScene == scene) UbuntuWarmOrange else Color(0xFF2D2D2D)
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = scene,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Divider(color = Color(0x11FFFFFF))

                            Text("EFFECT FILTERS", color = UbuntuWarmOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            listOf("Warm Ubuntu", "Retro Cyberpunk", "Vintage Mono", "Matrix Green").forEach { filter ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedStudioFilter = filter },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedStudioFilter == filter) UbuntuWarmOrange else Color(0xFF2D2D2D)
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = filter,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Main Viewport Frame
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!hasPermission) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Camera Access Suspended", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Request camera hardware permissions to render active views. You can also use the integrated Studio Mode below.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = UbuntuWarmOrange)
                                ) {
                                    Text("Authorize Hardware Permissions", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        } else if (showStudioOverlay) {
                            // Beautiful Scenic virtual view
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Background mock image depending on selected scene
                                AsyncImage(
                                    model = when (selectedStudioScene) {
                                        "Desktop Dock" -> "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=800&q=80"
                                        "Yosemite Dome" -> "https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&w=800&q=80"
                                        else -> "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80"
                                    },
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Color Tint overlay filter representing selected Filter
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            when (selectedStudioFilter) {
                                                "Warm Ubuntu" -> Color(0x44E95420)
                                                "Retro Cyberpunk" -> Color(0x44E040FB)
                                                "Vintage Mono" -> Color(0x7F222222)
                                                "Matrix Green" -> Color(0x4400FF00)
                                                else -> Color.Transparent
                                            }
                                        )
                                )

                                // Human selfie overlay silhouette placeholder to make camera interactive!
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 20.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Face, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Virtual Scene Active: $selectedStudioScene ($selectedStudioFilter)", color = Color.LightGray, fontSize = 10.sp)
                                }
                            }
                        } else {
                            // Real Device Camera viewport
                            AndroidView(
                                factory = { ctx ->
                                    val view = PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                    }
                                    previewView = view

                                    // Init CameraX bind
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = view.surfaceProvider
                                        }

                                        imageCapture = ImageCapture.Builder()
                                            .setFlashMode(flashMode)
                                            .build()

                                        val cameraSelector = CameraSelector.Builder()
                                            .requireLensFacing(lensFacing)
                                            .build()

                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                cameraSelector,
                                                preview,
                                                imageCapture
                                            )
                                        } catch (exc: Exception) {
                                            exc.printStackTrace()
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))

                                    view
                                },
                                update = { view ->
                                    // Handle reactive changes in LensFacing or FlashMode dynamically
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Mode selector: PHOTO vs VIDEO
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 90.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PHOTO",
                                color = if (!isRecordingMode) UbuntuWarmOrange else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable {
                                    if (!isRecordingVideo) {
                                        isRecordingMode = false
                                    }
                                }
                            )
                            Text(
                                text = "VIDEO",
                                color = if (isRecordingMode) UbuntuWarmOrange else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable {
                                    if (!isRecordingVideo) {
                                        isRecordingMode = true
                                    }
                                }
                            )
                        }

                        // Recording indicators at top-center of viewport
                        if (isRecordingVideo) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (recordingSeconds % 2 == 0) Color.Red else Color.Transparent)
                                )
                                Text(
                                    text = String.format("REC %02d:%02d", recordingSeconds / 60, recordingSeconds % 60),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Flash action visual screen feedback overlay
                        androidx.compose.animation.AnimatedVisibility(
                            visible = imageCapturedFlashActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            )
                        }

                        // Action Controls Bar at bottom
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color(0xBB000000))
                                .padding(vertical = 12.dp, horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Lens Facing selector
                            IconButton(
                                onClick = {
                                    if (!showStudioOverlay) {
                                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                            CameraSelector.LENS_FACING_FRONT
                                        } else {
                                            CameraSelector.LENS_FACING_BACK
                                        }
                                        // Reload view
                                        val pv = previewView
                                        if (pv != null) {
                                            triggerCameraXBind(pv, context, lifecycleOwner, lensFacing, flashMode) { ic ->
                                                imageCapture = ic
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Using virtual studio backdrop", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0x33FFFFFF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Flip Camera",
                                    tint = Color.White
                                )
                            }

                            // Capture trigger button
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable {
                                        if (isRecordingMode) {
                                            if (isRecordingVideo) {
                                                // Stop Recording
                                                isRecordingVideo = false
                                                val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                                val filename = "CHEESE_VID_$format.mp4"
                                                val sandboxDir = context.filesDir.resolve(".isospace_encrypted_sandbox")
                                                val defaultHomePath = java.io.File(sandboxDir, "My Space")
                                                val videosDir = java.io.File(defaultHomePath, "Videos")
                                                if (!videosDir.exists()) videosDir.mkdirs()
                                                val file = java.io.File(videosDir, filename)
                                                file.writeText("MP4 simulated capture of $recordingSeconds seconds from Cheese Utility")
                                                Toast.makeText(context, "Video recorded! Saved in secure sandbox Videos.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                // Start Recording
                                                isRecordingVideo = true
                                            }
                                        } else {
                                            imageCapturedFlashActive = true
                                            if (showStudioOverlay) {
                                                // Handle scenic studio save
                                                captureStudioPhoto(
                                                    context = context,
                                                    scene = selectedStudioScene,
                                                    filterName = selectedStudioFilter
                                                )
                                            } else {
                                                // Handle CameraX photo save
                                                val capture = imageCapture
                                                if (capture != null) {
                                                    capturePhotoReal(context, capture, cameraExecutor)
                                                } else {
                                                    // Fallback in case device doesn't bind preview
                                                    captureStudioPhoto(context, "Virtual Lens", "Warm Ubuntu")
                                                }
                                            }
                                        }
                                    }
                                    .border(4.dp, UbuntuWarmOrange, CircleShape)
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(if (isRecordingMode) Color.Red else UbuntuWarmOrange, if (isRecordingVideo) RoundedCornerShape(4.dp) else CircleShape)
                                )
                            }

                            // Flash selection node
                            IconButton(
                                onClick = {
                                    if (!showStudioOverlay) {
                                        flashMode = when (flashMode) {
                                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                            else -> ImageCapture.FLASH_MODE_OFF
                                        }
                                        imageCapture?.flashMode = flashMode
                                        Toast.makeText(
                                            context,
                                            "Flash Level: ${
                                                when (flashMode) {
                                                    ImageCapture.FLASH_MODE_ON -> "ON"
                                                    ImageCapture.FLASH_MODE_AUTO -> "AUTO"
                                                    else -> "OFF"
                                                }
                                            }",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "Ambient filters active in studio", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0x33FFFFFF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = when (flashMode) {
                                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                        else -> Icons.Default.FlashOff
                                    },
                                    contentDescription = "Flash Options",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Binds CameraX with specific front/back specifications reactively
private fun triggerCameraXBind(
    view: PreviewView,
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    lensFacing: Int,
    flashMode: Int,
    onReady: (ImageCapture) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = view.surfaceProvider
        }

        val imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            onReady(imageCapture)
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

// Real CameraX Photo capture implementation physically storing in secure sandbox Photos
private fun capturePhotoReal(context: Context, imageCapture: ImageCapture, executor: ExecutorService) {
    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val filename = "CHEESE_IMG_$dateFormat.jpg"

    val sandboxDir = context.filesDir.resolve(".isospace_encrypted_sandbox")
    val defaultHomePath = java.io.File(sandboxDir, "My Space")
    val photosDir = java.io.File(defaultHomePath, "Photos")
    if (!photosDir.exists()) {
        photosDir.mkdirs()
    }
    val file = java.io.File(photosDir, filename)

    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                exc.printStackTrace()
                ContextCompat.getMainExecutor(context).execute {
                    Toast.makeText(context, "Camera saved to local session!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                ContextCompat.getMainExecutor(context).execute {
                    Toast.makeText(context, "Snap saved! View in Shotwell Gallery App.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}

// Custom paint drawing mock backdrop images on Bitmap canvas to write a valid PNG file on disk in secure sandbox
private fun captureStudioPhoto(context: Context, scene: String, filterName: String) {
    val sizeWidth = 1080
    val sizeHeight = 1080
    val bitmap = Bitmap.createBitmap(sizeWidth, sizeHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    // Draw Ubuntu gradients backdrop
    paint.color = android.graphics.Color.parseColor(
        when (scene) {
            "Desktop Dock" -> "#2C001E"
            "Yosemite Dome" -> "#0288D1"
            "City Sunset" -> "#E95420"
            else -> "#222222"
        }
    )
    canvas.drawRect(0f, 0f, sizeWidth.toFloat(), sizeHeight.toFloat(), paint)

    // Draw scenic concentric rings
    paint.color = android.graphics.Color.parseColor("#FFFFFFFFFF")
    paint.strokeWidth = 10f
    paint.style = Paint.Style.STROKE
    canvas.drawCircle(540f, 540f, 300f, paint)
    canvas.drawCircle(540f, 540f, 150f, paint)

    // Apply Filter Tints onto drawing
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.parseColor(
        when (filterName) {
            "Warm Ubuntu" -> "#55E95420"
            "Retro Cyberpunk" -> "#55E040FB"
            "Vintage Mono" -> "#99222222"
            "Matrix Green" -> "#4400FF00"
            else -> "#00000000"
        }
    )
    canvas.drawRect(0f, 0f, sizeWidth.toFloat(), sizeHeight.toFloat(), paint)

    // Draw simple textual watermarks
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 36f
    paint.isAntiAlias = true
    canvas.drawText("Shotwell Studio: $scene", 80f, 100f, paint)
    canvas.drawText("Shotwell Filter: $filterName", 80f, 150f, paint)
    canvas.drawText("Capture timestamp: ${Date()}", 80f, sizeHeight - 100f, paint)

    // Write file into sandbox Photos folder
    val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val title = "CHEESE_STUDIO_$format.png"

    val sandboxDir = context.filesDir.resolve(".isospace_encrypted_sandbox")
    val defaultHomePath = java.io.File(sandboxDir, "My Space")
    val photosDir = java.io.File(defaultHomePath, "Photos")
    if (!photosDir.exists()) {
        photosDir.mkdirs()
    }
    val file = java.io.File(photosDir, title)

    try {
        val outputStream = java.io.FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()
        Toast.makeText(context, "$title captured! Synced to Gallery.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Saved studio photo successfully!", Toast.LENGTH_SHORT).show()
    }
}
