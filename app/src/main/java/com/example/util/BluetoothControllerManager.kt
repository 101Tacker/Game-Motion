package com.example.util

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.view.InputDevice

data class ConnectedController(
    val id: Int,
    val name: String,
    val playerNumber: Int,
    val isBluetooth: Boolean = true
)

object BluetoothControllerManager {

    fun getConnectedControllers(context: Context): List<ConnectedController> {
        val deviceIds = InputDevice.getDeviceIds()
        val list = mutableListOf<ConnectedController>()
        var playerNum = 1
        for (id in deviceIds) {
            val device = InputDevice.getDevice(id) ?: continue
            val sources = device.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            val isJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if (isGamepad || isJoystick) {
                list.add(
                    ConnectedController(
                        id = id,
                        name = device.name ?: "Wireless Gamepad",
                        playerNumber = playerNum++
                    )
                )
            }
        }
        return list
    }

    @Suppress("DEPRECATION")
    fun isBluetoothAudioConnected(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return audioManager?.isBluetoothA2dpOn == true
    }

    fun openBluetoothSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }
}
