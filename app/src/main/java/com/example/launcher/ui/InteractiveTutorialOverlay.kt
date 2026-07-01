package com.example.launcher.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InteractiveGestureTutorialOverlay(
    accentColor: Color,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    // Dynamic color values
    val bgDark = Color(0xFF121212)
    val borderDark = Color(0x33FFFFFF)
    val terminalGreen = Color(0xFF33FF33)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp)
                .testTag("gesture_tutorial_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = bgDark),
            border = BorderStroke(1.dp, borderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gesture,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ISOSpace Gesture Core Manual",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Exit button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("tutorial_skip_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Skip", tint = Color.Gray)
                    }
                }

                // Progress Indicator
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(totalSteps) { index ->
                        val isCompleted = index < currentStep
                        val isActive = index == currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isCompleted) accentColor
                                    else if (isActive) accentColor.copy(alpha = 0.5f)
                                    else Color(0xFF333333)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step content switcher with animations
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() with
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() with
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }.using(SizeTransform(clip = false))
                        },
                        label = "step_content_anim"
                    ) { step ->
                        when (step) {
                            0 -> SwipeDownTutorialStep(accentColor, onComplete = { currentStep = 1 })
                            1 -> EdgeSwipeTutorialStep(accentColor, onComplete = { currentStep = 2 })
                            2 -> SwipeUpTutorialStep(accentColor, onComplete = { currentStep = 3 })
                            3 -> MultiGestureTutorialStep(accentColor, onComplete = onDismiss)
                        }
                    }
                }

                // Bottom Footer Buttons
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Interactive Calibration OS v1.2",
                        color = terminalGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    if (currentStep > 0) {
                        TextButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.testTag("tutorial_back_button")
                        ) {
                            Text(
                                "BACK",
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
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

@Composable
fun SwipeDownTutorialStep(
    accentColor: Color,
    onComplete: () -> Unit
) {
    var swipeProgressY by remember { mutableFloatStateOf(0f) }
    var isGestureCompleted by remember { mutableStateOf(false) }
    val maxSwipeDistance = 250f

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_hand")
    val handAnimOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hand_y"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "01 // SWIPE DOWN GESTURE",
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Swipe down anywhere on the home workspace to reveal the System Settings and Control Center nodes.",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }

        // Interactive Sandbox Gesture Area
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(260.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                .border(2.dp, if (isGestureCompleted) Color(0xFF33FF33) else accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (swipeProgressY >= maxSwipeDistance) {
                                isGestureCompleted = true
                            } else {
                                swipeProgressY = 0f
                            }
                        },
                        onDragCancel = { swipeProgressY = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            if (!isGestureCompleted) {
                                swipeProgressY = (swipeProgressY + dragAmount).coerceIn(0f, maxSwipeDistance)
                                if (swipeProgressY >= maxSwipeDistance) {
                                    isGestureCompleted = true
                                }
                            }
                            change.consume()
                        }
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // Simulated Control Center Panel sliding down
            val panelProgress = swipeProgressY / maxSwipeDistance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .offset { IntOffset(0, ((-220 * (1f - panelProgress)).roundToInt())) }
                    .background(Color(0xFF2E2E2E))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SYSTEM NODES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Wifi, null, tint = accentColor, modifier = Modifier.size(12.dp))
                            Icon(Icons.Default.Bluetooth, null, tint = accentColor, modifier = Modifier.size(12.dp))
                            Icon(Icons.Default.BatteryChargingFull, null, tint = Color(0xFF33FF33), modifier = Modifier.size(12.dp))
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .background(Color(0xFF3E3E3E), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(if (it == 0) Icons.Default.Wifi else Icons.Default.Bluetooth, null, tint = accentColor, modifier = Modifier.size(14.dp))
                                    Text(if (it == 0) "Wi-Fi: ON" else "BT: ON", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Visual guide overlay when not completed
            if (!isGestureCompleted) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = handAnimOffset.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "DRAG DOWNWARD TO CALIBRATE",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF33FF33).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF33FF33), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF33FF33),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "[ CALIBRATION SUCCESS ]",
                            color = Color(0xFF33FF33),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Control Center hook validated.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Action Button
        Button(
            onClick = onComplete,
            enabled = isGestureCompleted,
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                disabledContainerColor = Color(0xFF222222)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(46.dp)
                .testTag("tutorial_step1_next")
        ) {
            Text(
                text = "CONTINUE TO NEXT NODE",
                color = if (isGestureCompleted) Color.Black else Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun EdgeSwipeTutorialStep(
    accentColor: Color,
    onComplete: () -> Unit
) {
    var swipeProgressX by remember { mutableFloatStateOf(0f) }
    var isGestureCompleted by remember { mutableStateOf(false) }
    val maxSwipeDistance = 180f

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_hand_edge")
    val handAnimOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hand_x"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "02 // EDGE SWIPE TO DOCK",
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Swipe inward from the left screen edge to dynamically toggle the autohide Side Applications Dock.",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }

        // Interactive Sandbox Gesture Area
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(260.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                .border(2.dp, if (isGestureCompleted) Color(0xFF33FF33) else accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeProgressX >= maxSwipeDistance) {
                                isGestureCompleted = true
                            } else {
                                swipeProgressX = 0f
                            }
                        },
                        onDragCancel = { swipeProgressX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!isGestureCompleted) {
                                swipeProgressX = (swipeProgressX + dragAmount).coerceIn(0f, maxSwipeDistance)
                                if (swipeProgressX >= maxSwipeDistance) {
                                    isGestureCompleted = true
                                }
                            }
                            change.consume()
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Simulated side dock panel sliding out
            val dockProgress = swipeProgressX / maxSwipeDistance
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(60.dp)
                    .offset { IntOffset(((-60 * (1f - dockProgress)).roundToInt()), 0) }
                    .background(Color(0xFF2E2E2E))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.3f))
                                .border(1.dp, accentColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (it) {
                                    0 -> Icons.Default.Terminal
                                    1 -> Icons.Default.FolderOpen
                                    else -> Icons.Default.PhotoLibrary
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Visual handle guide
            if (!isGestureCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(10.dp)
                        .background(accentColor.copy(alpha = 0.3f))
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = handAnimOffset.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SWIPE INWARD FROM LEFT EDGE",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF33FF33).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF33FF33), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF33FF33),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "[ CALIBRATION SUCCESS ]",
                            color = Color(0xFF33FF33),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Side Dock dock-swipe verified.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Action Button
        Button(
            onClick = onComplete,
            enabled = isGestureCompleted,
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                disabledContainerColor = Color(0xFF222222)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(46.dp)
                .testTag("tutorial_step2_next")
        ) {
            Text(
                text = "CONTINUE TO NEXT NODE",
                color = if (isGestureCompleted) Color.Black else Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SwipeUpTutorialStep(
    accentColor: Color,
    onComplete: () -> Unit
) {
    var swipeProgressY by remember { mutableFloatStateOf(0f) }
    var isGestureCompleted by remember { mutableStateOf(false) }
    val maxSwipeDistance = 250f

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_hand_up")
    val handAnimOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hand_y"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "03 // SWIPE UP TO DASH / APPS",
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Swipe up from anywhere on the home screen to access your fully modular Linux Gnome Dash and desktop search grid.",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }

        // Interactive Sandbox Gesture Area
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(260.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                .border(2.dp, if (isGestureCompleted) Color(0xFF33FF33) else accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (swipeProgressY <= -maxSwipeDistance) {
                                isGestureCompleted = true
                            } else {
                                swipeProgressY = 0f
                            }
                        },
                        onDragCancel = { swipeProgressY = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            if (!isGestureCompleted) {
                                swipeProgressY = (swipeProgressY + dragAmount).coerceIn(-maxSwipeDistance, 0f)
                                if (swipeProgressY <= -maxSwipeDistance) {
                                    isGestureCompleted = true
                                }
                            }
                            change.consume()
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Simulated Dash sliding up
            val dashProgress = -swipeProgressY / maxSwipeDistance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .offset { IntOffset(0, ((240 * (1f - dashProgress)).roundToInt())) }
                    .background(Color(0xFF2E2E2E))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WORKSPACE SEARCH", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Search packages...", color = Color.Gray, fontSize = 10.sp)
                    }

                    Text("APPLICATIONS GRID", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(Color(0xFF3E3E3E), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(Icons.Default.Apps, null, tint = accentColor, modifier = Modifier.size(14.dp))
                                    Text("App Nodes", color = Color.LightGray, fontSize = 7.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Visual guide overlay
            if (!isGestureCompleted) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = handAnimOffset.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "DRAG UPWARD TO ACTIVATE DASH",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF33FF33).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF33FF33), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF33FF33),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "[ CALIBRATION SUCCESS ]",
                            color = Color(0xFF33FF33),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Application Dash workspace unlocked.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Action Button
        Button(
            onClick = onComplete,
            enabled = isGestureCompleted,
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                disabledContainerColor = Color(0xFF222222)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(46.dp)
                .testTag("tutorial_step3_next")
        ) {
            Text(
                text = "CONTINUE TO NEXT NODE",
                color = if (isGestureCompleted) Color.Black else Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun MultiGestureTutorialStep(
    accentColor: Color,
    onComplete: () -> Unit
) {
    var doubleTapTriggered by remember { mutableStateOf(false) }
    var longPressTriggered by remember { mutableStateOf(false) }
    val isGestureCompleted = doubleTapTriggered && longPressTriggered

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "04 // UTILITY INTERACTION KEYCODES",
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Double Tap to open System customizer settings. Long Press to manage and drag widget layouts.",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }

        // Interactive Sandbox Gesture Area
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(260.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                .border(2.dp, if (isGestureCompleted) Color(0xFF33FF33) else accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            doubleTapTriggered = true
                        },
                        onLongPress = {
                            longPressTriggered = true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Double Tap Status Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .background(
                                if (doubleTapTriggered) Color(0xFF33FF33).copy(alpha = 0.1f)
                                else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (doubleTapTriggered) Color(0xFF33FF33) else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = if (doubleTapTriggered) Icons.Default.CheckCircle else Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = if (doubleTapTriggered) Color(0xFF33FF33) else accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Double Tap Me", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                if (doubleTapTriggered) "[ VERIFIED ]" else "[ WAITING ]",
                                color = if (doubleTapTriggered) Color(0xFF33FF33) else Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Long Press Status Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .background(
                                if (longPressTriggered) Color(0xFF33FF33).copy(alpha = 0.1f)
                                else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (longPressTriggered) Color(0xFF33FF33) else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = if (longPressTriggered) Icons.Default.CheckCircle else Icons.Default.SettingsAccessibility,
                                contentDescription = null,
                                tint = if (longPressTriggered) Color(0xFF33FF33) else accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Long Press Me", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                if (longPressTriggered) "[ VERIFIED ]" else "[ WAITING ]",
                                color = if (longPressTriggered) Color(0xFF33FF33) else Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                if (isGestureCompleted) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ALL CALIBRATIONS SUCCESSFUL",
                        color = Color(0xFF33FF33),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PERFORM ACTIONS INSIDE SANDBOX",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Action Button
        Button(
            onClick = onComplete,
            enabled = isGestureCompleted,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF33FF33),
                disabledContainerColor = Color(0xFF222222)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(46.dp)
                .testTag("tutorial_step4_finish")
        ) {
            Text(
                text = "COMPLETE SYSTEM SETUP",
                color = if (isGestureCompleted) Color.Black else Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
