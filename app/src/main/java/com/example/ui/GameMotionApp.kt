package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.util.PermissionManager
import kotlinx.coroutines.delay
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ImportScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.RecentScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.RuntimeScreen
import com.example.ui.theme.CyanPrimary

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Library : Screen("library", "Library", Icons.Filled.List)
    object Import : Screen("import", "Import", Icons.Filled.AddCircle)
    object Recent : Screen("recent", "Recent", Icons.Filled.PlayArrow)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object About : Screen("about", "About", Icons.Filled.Info)
    object Runtime : Screen("runtime/{id}", "Runtime", Icons.Filled.PlayArrow) {
        fun createRoute(id: Long) = "runtime/$id"
    }
}

val items = listOf(
    Screen.Library,
    Screen.Import,
    Screen.Recent,
    Screen.Settings,
    Screen.About
)

@Composable
fun GameMotionApp(
    modifier: Modifier = Modifier,
    onToggleFullscreen: (Boolean) -> Unit = {}
) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen(modifier)
    } else {
        MainAppContent(modifier, onToggleFullscreen)
    }
}

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.BackgroundDark),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME MOTION",
            style = androidx.compose.material3.MaterialTheme.typography.displayMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = CyanPrimary,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Created by\nTUCCI CYBER NATION",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun MainAppContent(
    modifier: Modifier = Modifier,
    onToggleFullscreen: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val isRuntimeScreen = currentRoute?.startsWith("runtime") == true

    var missingPermissions by remember { mutableStateOf(PermissionManager.getMissingPermissions(context)) }
    var showPermissionBanner by remember { mutableStateOf(missingPermissions.isNotEmpty()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        missingPermissions = PermissionManager.getMissingPermissions(context)
        showPermissionBanner = missingPermissions.isNotEmpty()
    }

    LaunchedEffect(Unit) {
        if (missingPermissions.isNotEmpty()) {
            val needed = PermissionManager.getRequiredPermissionsList()
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!isRuntimeScreen) {
            DynamicBackground()
        }
        
        Row(modifier = Modifier.fillMaxSize()) {
            if (!isRuntimeScreen) {
                androidx.compose.material3.NavigationRail(
                    containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f),
                    contentColor = CyanPrimary
                ) {
                    items.forEach { screen ->
                        androidx.compose.material3.NavigationRailItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                                indicatorColor = CyanPrimary.copy(alpha = 0.2f),
                                selectedIconColor = CyanPrimary,
                                selectedTextColor = CyanPrimary,
                                unselectedIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isRuntimeScreen && showPermissionBanner && missingPermissions.isNotEmpty()) {
                    PermissionAlertBanner(
                        onRequest = {
                            val needed = PermissionManager.getRequiredPermissionsList()
                            permissionLauncher.launch(needed.toTypedArray())
                        },
                        onOpenSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onDismiss = {
                            showPermissionBanner = false
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Library.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Screen.Library.route) {
                            LibraryScreen(
                                onPlayGame = { id -> navController.navigate(Screen.Runtime.createRoute(id)) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onNavigateToImport = { navController.navigate(Screen.Import.route) }
                            )
                        }
                        composable(Screen.Import.route) { ImportScreen() }
                        composable(Screen.Recent.route) {
                            RecentScreen(
                                onPlayGame = { id -> navController.navigate(Screen.Runtime.createRoute(id)) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                            )
                        }
                        composable(Screen.Settings.route) { SettingsScreen() }
                        composable(Screen.About.route) { AboutScreen() }
                        composable(Screen.Runtime.route) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                            RuntimeScreen(
                                gameId = id,
                                onExit = { navController.popBackStack() },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onToggleFullscreen = onToggleFullscreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionAlertBanner(
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = Color(0xFF1C1808),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Hardware Permissions Recommended",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Grant Bluetooth and storage permissions to enable wireless gamepad discovery and ROM scans.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Grant", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Details", fontSize = 11.sp)
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicBackground() {
    val backgrounds = listOf(
        com.example.R.drawable.gta_style_bg_1788456081421,
        com.example.R.drawable.fc_style_bg_1788456095160,
        com.example.R.drawable.mk_style_bg_1788456108275,
        com.example.R.drawable.sr_style_bg_1788456122548
    )
    var currentBg by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while(true) {
            delay(5000)
            currentBg = (currentBg + 1) % backgrounds.size
        }
    }
    
    androidx.compose.animation.Crossfade(
        targetState = backgrounds[currentBg],
        animationSpec = androidx.compose.animation.core.tween(1500),
        label = "DynamicBackgroundCrossfade"
    ) { bg ->
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = bg),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                androidx.compose.ui.graphics.BlendMode.Darken
            )
        )
    }
}
