package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GameEntity
import com.example.ui.AppViewModelProvider
import com.example.ui.GameViewModel
import com.example.ui.components.GameTile
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceDark
import com.example.util.BluetoothControllerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: GameViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onPlayGame: (Long) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToImport: () -> Unit = {}
) {
    val context = LocalContext.current
    val allGames by viewModel.allGames.collectAsState()
    val allOsFiles by viewModel.allOsFiles.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    val filters = listOf("ALL", "PSP", "PS2", "PS3", "GODOT")

    val filteredGames = remember(allGames, selectedFilter) {
        if (selectedFilter == "ALL") allGames
        else allGames.filter { it.consoleType.equals(selectedFilter, ignoreCase = true) }
    }

    // State for OS Missing Alert
    var missingOsGame by remember { mutableStateOf<GameEntity?>(null) }
    var requiredOsExtension by remember { mutableStateOf("") }

    // State for Bluetooth Controller Warning
    var pendingLaunchGame by remember { mutableStateOf<GameEntity?>(null) }

    fun tryLaunchGame(game: GameEntity) {
        val console = game.consoleType.uppercase()
        // Determine required OS file type:
        val (neededExt, hasOsFile) = when (console) {
            "PSP" -> Pair(
                ".PBP",
                allOsFiles.any { it.osType.equals("PBP", ignoreCase = true) || it.consoleTarget.equals("PSP", ignoreCase = true) }
            )
            "PS2" -> Pair(
                ".bin (PS2 BIOS)",
                allOsFiles.any { it.osType.equals("BIN", ignoreCase = true) || it.consoleTarget.equals("PS2", ignoreCase = true) }
            )
            "PS3" -> Pair(
                ".PUP (PS3 Firmware)",
                allOsFiles.any { it.osType.equals("PUP", ignoreCase = true) || it.consoleTarget.equals("PS3", ignoreCase = true) }
            )
            else -> Pair("", true) // Godot or others require no external OS file
        }

        if (!hasOsFile) {
            // Game closed automatically, notify user to install OS files
            requiredOsExtension = neededExt
            missingOsGame = game
            return
        }

        // Check connected Bluetooth / USB controllers
        val controllers = com.example.controller.ControllerManager.connectedControllers.value
        if (controllers.isEmpty()) {
            // No physical controller connected: prompt user for better experience
            pendingLaunchGame = game
        } else {
            // BT controller is connected: skip warning directly
            val updated = game.copy(
                lastPlayed = System.currentTimeMillis(),
                playCount = game.playCount + 1
            )
            viewModel.updateGame(updated)
            onPlayGame(game.id)
        }
    }

    // Missing OS Dialog
    missingOsGame?.let { game ->
        AlertDialog(
            onDismissRequest = { missingOsGame = null },
            icon = {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Required OS File Not Found",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "To run '${game.title}' on ${game.consoleType}, Game Motion requires the system operating files ($requiredOsExtension).",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Game Motion has closed the game. Please install the required OS files (.PBP for PSP, .bin for PS2, or .PUP for PS3) in Settings to execute this console title.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        missingOsGame = null
                        onNavigateToSettings()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Install OS Files in Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { missingOsGame = null }) {
                    Text("Dismiss", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // No Physical Controller Dialog (Section 37 specification)
    pendingLaunchGame?.let { game ->
        AlertDialog(
            onDismissRequest = { pendingLaunchGame = null },
            icon = {
                Icon(
                    Icons.Filled.SportsEsports,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "No Controller Detected",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Connect a Bluetooth or USB controller to play.",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Game Motion prioritizes physical controllers to keep your screen completely clear and free of virtual buttons.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingLaunchGame = null
                        onNavigateToSettings()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Controller Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val toPlay = pendingLaunchGame
                        pendingLaunchGame = null
                        toPlay?.let {
                            val updated = it.copy(
                                lastPlayed = System.currentTimeMillis(),
                                playCount = it.playCount + 1
                            )
                            viewModel.updateGame(updated)
                            onPlayGame(it.id)
                        }
                    }
                ) {
                    Text("Play Anyway (Touch Navigation)", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Row with Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Game Station Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
                Text(
                    text = "${allGames.size} Titles Available • Tap any game tile to launch",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val filterColor = when (filter) {
                        "PSP" -> Color(0xFF00E5FF)
                        "PS2" -> Color(0xFF2979FF)
                        "PS3" -> Color(0xFFFF5252)
                        "GODOT" -> Color(0xFF00E676)
                        else -> CyanPrimary
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = filterColor,
                            selectedLabelColor = Color.Black,
                            containerColor = SurfaceDark.copy(alpha = 0.7f),
                            labelColor = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredGames.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.SportsEsports,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (allGames.isEmpty()) "No games installed in your Station Library."
                        else "No $selectedFilter games found in your library.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToImport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Games Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 250.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredGames, key = { it.id }) { game ->
                    GameTile(
                        game = game,
                        onPlay = { tryLaunchGame(game) },
                        onDelete = { viewModel.deleteGame(game) }
                    )
                }
            }
        }
    }
}
