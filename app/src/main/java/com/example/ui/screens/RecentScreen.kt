package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
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

@Composable
fun RecentScreen(
    viewModel: GameViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onPlayGame: (Long) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val recentGames by viewModel.recentGames.collectAsState()
    val allOsFiles by viewModel.allOsFiles.collectAsState()

    var missingOsGame by remember { mutableStateOf<GameEntity?>(null) }
    var requiredOsExtension by remember { mutableStateOf("") }
    var pendingLaunchGame by remember { mutableStateOf<GameEntity?>(null) }

    fun tryLaunchGame(game: GameEntity) {
        val console = game.consoleType.uppercase()
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
            else -> Pair("", true)
        }

        if (!hasOsFile) {
            requiredOsExtension = neededExt
            missingOsGame = game
            return
        }

        val controllers = com.example.controller.ControllerManager.connectedControllers.value
        if (controllers.isEmpty()) {
            pendingLaunchGame = game
        } else {
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
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "The game execution was halted. Please import the needed OS file in Settings.",
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

    // No Bluetooth Controller Dialog
    pendingLaunchGame?.let { game ->
        AlertDialog(
            onDismissRequest = { pendingLaunchGame = null },
            icon = {
                Icon(
                    Icons.Filled.Bluetooth,
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
                        text = "For a better console experience, please connect a Bluetooth controller (DualShock 4, DualSense, Xbox, or wireless gamepad).",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notice: Game Motion does not provide on-screen virtual touch controls in order to keep your screen completely clear and authentic.",
                        color = CyanPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
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
                    Text("Continue Anyway", color = Color.White.copy(alpha = 0.8f))
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.History, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Recently Played Games",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
                Text(
                    text = "Quick resume for your console sessions with saved OS runtime state",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (recentGames.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No games played yet.\nSelect a game from the Library to begin playing.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 250.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(recentGames, key = { it.id }) { game ->
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
