package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class AppPermissionItem(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isRequired: Boolean,
    val minSdk: Int = 0,
    val maxSdk: Int = Int.MAX_VALUE
)

object PermissionManager {

    fun getRequiredPermissionsList(): List<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
            list.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return list
    }

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun getMissingPermissions(context: Context): List<String> {
        return getRequiredPermissionsList().filter { !isPermissionGranted(context, it) }
    }

    fun areAllRequiredGranted(context: Context): Boolean {
        return getMissingPermissions(context).isEmpty()
    }

    fun getPermissionDetails(context: Context): List<AppPermissionItem> {
        val items = mutableListOf<AppPermissionItem>()

        // Bluetooth Connect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            items.add(
                AppPermissionItem(
                    permission = Manifest.permission.BLUETOOTH_CONNECT,
                    title = "Bluetooth Controller Connection",
                    description = "Required to discover, pair, and communicate with physical Bluetooth gamepads (DualShock, Xbox, Switch Pro, 8BitDo) without input latency.",
                    isGranted = isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT),
                    isRequired = true,
                    minSdk = Build.VERSION_CODES.S
                )
            )
            items.add(
                AppPermissionItem(
                    permission = Manifest.permission.BLUETOOTH_SCAN,
                    title = "Bluetooth Controller Discovery",
                    description = "Required to scan for nearby wireless game controllers when initiating pairing in Controller Settings.",
                    isGranted = isPermissionGranted(context, Manifest.permission.BLUETOOTH_SCAN),
                    isRequired = true,
                    minSdk = Build.VERSION_CODES.S
                )
            )
        }

        // Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            items.add(
                AppPermissionItem(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    title = "System Alerts & Progress Notifications",
                    description = "Used to report background game ROM directory scanning progress, controller battery warnings, and firmware verification status.",
                    isGranted = isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS),
                    isRequired = false,
                    minSdk = Build.VERSION_CODES.TIRAMISU
                )
            )
        }

        // Storage for older Android versions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            items.add(
                AppPermissionItem(
                    permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                    title = "ROM Storage Access",
                    description = "Required on Android 12 and below to scan external directories and SD cards for ISO, CSO, and PKG files.",
                    isGranted = isPermissionGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE),
                    isRequired = true,
                    maxSdk = Build.VERSION_CODES.S_V2
                )
            )
        }

        return items
    }

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }
}
