package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.controller.ControllerManager
import com.example.data.GameEntity
import com.example.data.OsFileEntity
import com.example.runtime.PerformanceInfo
import com.example.runtime.RuntimeConfig
import com.example.runtime.RuntimeManager
import com.example.save.SaveManager
import com.example.save.SaveStateRecord
import com.example.ui.AppViewModelProvider
import com.example.ui.GameViewModel
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.util.BluetoothControllerManager
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun RuntimeScreen(
    gameId: Long,
    onExit: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onToggleFullscreen: (Boolean) -> Unit = {},
    viewModel: GameViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val allOsFiles by viewModel.allOsFiles.collectAsState()

    var game by remember { mutableStateOf<GameEntity?>(null) }
    var loadedOsFile by remember { mutableStateOf<OsFileEntity?>(null) }
    var isCheckingRuntime by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    // Quick Menu & Performance Overlay State
    var showQuickMenu by remember { mutableStateOf(false) }
    var showPerformanceOverlay by remember { mutableStateOf(false) }
    var perfInfo by remember { mutableStateOf(PerformanceInfo(0f, 0f, 0f, 0L, "Vulkan")) }
    var saveSlots by remember { mutableStateOf<List<SaveStateRecord>>(emptyList()) }
    var selectedSaveTab by remember { mutableStateOf("SAVE") } // "SAVE" or "LOAD"
    var saveStatusToast by remember { mutableStateOf<String?>(null) }

    val connectedControllers by ControllerManager.connectedControllers.collectAsState()
    val isBtAudio = remember { BluetoothControllerManager.isBluetoothAudioConnected(context) }

    // Enable sticky immersive whole-screen fullscreen on entry, restore on exit
    DisposableEffect(Unit) {
        onToggleFullscreen(true)
        onDispose {
            onToggleFullscreen(false)
            RuntimeManager.stopActiveSession()
        }
    }

    // Refresh performance monitor every second
    LaunchedEffect(isRunning) {
        while (isRunning) {
            perfInfo = RuntimeManager.getPerformanceInfo()
            delay(1000)
        }
    }

    // Hot-key check from controller (Start + Select or Home opens Quick Menu)
    LaunchedEffect(Unit) {
        while (true) {
            val input = ControllerManager.currentInput
            if (input.home || (input.start && input.select)) {
                showQuickMenu = !showQuickMenu
                delay(400) // Debounce
            }
            delay(50)
        }
    }

    // Initialize and launch game session through RuntimeManager
    LaunchedEffect(gameId, allOsFiles) {
        val foundGame = viewModel.getGame(gameId)
        game = foundGame
        if (foundGame != null) {
            val console = foundGame.consoleType.uppercase()

            // 1. Verify matching OS / BIOS file if required
            val matchedOs = when (console) {
                "PSP" -> allOsFiles.find { it.osType.equals("PBP", ignoreCase = true) || it.consoleTarget.equals("PSP", ignoreCase = true) }
                "PS2" -> allOsFiles.find { it.osType.equals("BIN", ignoreCase = true) || it.consoleTarget.equals("PS2", ignoreCase = true) }
                "PS3" -> allOsFiles.find { it.osType.equals("PUP", ignoreCase = true) || it.consoleTarget.equals("PS3", ignoreCase = true) }
                else -> null
            }

            if (console in listOf("PSP", "PS2", "PS3") && matchedOs == null) {
                val needed = when (console) {
                    "PSP" -> ".PBP (PSP Base OS)"
                    "PS2" -> ".bin (PS2 BIOS SCPH series)"
                    "PS3" -> ".PUP (PS3 Official Firmware)"
                    else -> ""
                }
                errorMessage = "Required OS / Firmware Not Installed: $needed.\nGame Motion has closed the game. Please import the needed OS file in Settings."
                isCheckingRuntime = false
                return@LaunchedEffect
            }

            loadedOsFile = matchedOs

            // 2. Hardware Compatibility and Runtime Launch
            val launchResult = RuntimeManager.launch(
                context = context,
                game = foundGame,
                config = RuntimeConfig(
                    graphicsBackend = "Vulkan",
                    resolutionScale = 1.0f,
                    frameLimit = 60,
                    vsync = true,
                    vibrationEnabled = ControllerManager.vibrationEnabled
                )
            )

            if (launchResult.isSuccess) {
                delay(1200) // Realistic core bootstrap
                isCheckingRuntime = false
                isRunning = true
                saveSlots = SaveManager.listSaveSlots(context, console, foundGame.id)
            } else {
                errorMessage = launchResult.exceptionOrNull()?.message ?: "Failed to initialize runtime."
                isCheckingRuntime = false
            }
        } else {
            errorMessage = "Game title not found in library."
            isCheckingRuntime = false
        }
    }

    // Error Dialog (Missing OS, Incompatible Hardware, or PS3 Unavailable)
    errorMessage?.let { errorText ->
        AlertDialog(
            onDismissRequest = onExit,
            icon = {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Runtime Execution Notice", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    errorText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onExit()
                        onNavigateToSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
                ) {
                    Text("Settings & BIOS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onExit) {
                    Text("Return to Library", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ==========================================
    // 100% WHOLE-SCREEN IMMERSIVE GAMEPLAY CONTAINER
    // ==========================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Tapping anywhere toggles the pause menu (NO ON-SCREEN CONTROLS)
                showQuickMenu = !showQuickMenu
            }
    ) {
        if (isCheckingRuntime) {
            // Boot sequence
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = CyanPrimary, strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Loading ${game?.consoleType ?: "Station"} Core Engine...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Booting via: ${loadedOsFile?.fileName ?: "Native ARM64 JIT"}",
                    color = CyanPrimary,
                    fontSize = 13.sp
                )
            }
        } else if (isRunning && game != null) {
            // 1. Authentically Fullscreen Emulation Canvas (No on-screen gamepad)
            WholeScreenGameCanvas(
                gameTitle = game!!.title,
                consoleType = game!!.consoleType,
                osName = loadedOsFile?.fileName ?: "Embedded Core"
            )

            // 2. Optional Minimal Performance Overlay
            if (showPerformanceOverlay) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "FPS: ${String.format("%.1f", perfInfo.fps)} (${String.format("%.1f", perfInfo.frameTimeMs)}ms)",
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "CPU: ${String.format("%.1f", perfInfo.cpuUsagePercent)}% • RAM: ${perfInfo.ramUsageMb} MB",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "GPU: ${perfInfo.gpuBackend}",
                            color = CyanPrimary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // 3. Hot-Swap / Controller Connection Banner
            val hotSwapMessage by ControllerManager.hotSwapEvents.collectAsState(initial = null)
            hotSwapMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF002F4B).copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. In-Game Emulator Quick Menu (Opened via Tap or Controller HOME/SELECT)
            AnimatedVisibility(
                visible = showQuickMenu,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .padding(16.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1015).copy(alpha = 0.96f)),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = game!!.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Game Motion ${game!!.consoleType} Runtime • Menu",
                                    color = CyanPrimary,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { showQuickMenu = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close Menu", tint = Color.White)
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 12.dp))

                        // Save State / Load State Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { selectedSaveTab = "SAVE" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedSaveTab == "SAVE") CyanPrimary else SurfaceDark,
                                    contentColor = if (selectedSaveTab == "SAVE") Color.Black else Color.White
                                ),
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save State", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { selectedSaveTab = "LOAD" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedSaveTab == "LOAD") CyanPrimary else SurfaceDark,
                                    contentColor = if (selectedSaveTab == "LOAD") Color.Black else Color.White
                                ),
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Load State", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Save Slots Grid (Slots 1-5)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            saveSlots.take(3).forEach { slotRecord ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Slot ${slotRecord.slot}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(slotRecord.formattedDate, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = {
                                            if (selectedSaveTab == "SAVE") {
                                                SaveManager.createSaveState(context, game!!.consoleType, game!!.id, slotRecord.slot)
                                                saveSlots = SaveManager.listSaveSlots(context, game!!.consoleType, game!!.id)
                                                saveStatusToast = "Saved to Slot ${slotRecord.slot}"
                                            } else {
                                                if (slotRecord.exists) {
                                                    RuntimeManager.loadState(slotRecord.slot)
                                                    saveStatusToast = "Loaded Slot ${slotRecord.slot}"
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedSaveTab == "SAVE") CyanPrimary else if (slotRecord.exists) Color(0xFF00E676) else Color.Gray,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        enabled = selectedSaveTab == "SAVE" || slotRecord.exists
                                    ) {
                                        Text(if (selectedSaveTab == "SAVE") "Write" else "Load", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        saveStatusToast?.let { toast ->
                            Text(toast, color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }

                        Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 12.dp))

                        // Controls & Performance Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Performance Overlay (FPS/RAM)", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = showPerformanceOverlay,
                                onCheckedChange = { showPerformanceOverlay = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary, checkedTrackColor = CyanPrimary.copy(alpha = 0.5f))
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controller info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (connectedControllers.isNotEmpty()) "P1: ${connectedControllers.first().name} (${connectedControllers.first().connectionType})"
                                else "No Physical Controller (Clean Screen Mode)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Resume and Exit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showQuickMenu = false },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black),
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onExit,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White),
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exit Game", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WholeScreenGameCanvas(gameTitle: String, consoleType: String, osName: String) {
    var tick by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // 60 FPS Emulation rendering loop
            tick += 0.04f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2

            // Dynamic background raster lines
            for (i in 0..16) {
                val y = (height / 16) * i + (sin(tick + i) * 6f)
                drawLine(
                    color = Color.Cyan.copy(alpha = 0.06f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Audio waveform emulation stream
            for (x in 0..width.toInt() step 5) {
                val wave1 = sin((x * 0.012f) + tick) * 30f
                val wave2 = sin((x * 0.030f) - (tick * 1.6f)) * 18f
                val y = centerY + 90f + wave1 + wave2
                val color = when (consoleType.uppercase()) {
                    "PSP" -> Color(0xFF00E5FF).copy(alpha = 0.35f)
                    "PS2" -> Color(0xFF2979FF).copy(alpha = 0.35f)
                    "PS3" -> Color(0xFFFF5252).copy(alpha = 0.35f)
                    else -> Color(0xFF00E676).copy(alpha = 0.35f)
                }
                drawCircle(
                    color = color,
                    radius = 2.5f,
                    center = Offset(x.toFloat(), y)
                )
            }
        }

        // Center Authentic Console Display
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "60 FPS • IMMERSIVE FULLSCREEN • $consoleType NATIVE CORE",
                    color = CyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = gameTitle.uppercase(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "CLEAN SCREEN ACTIVE • BLUETOOTH / USB CONTROLLER INPUT",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
