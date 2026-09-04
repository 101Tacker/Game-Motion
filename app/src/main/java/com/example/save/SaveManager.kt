package com.example.save

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SaveStateRecord(
    val slot: Int,
    val platform: String,
    val gameId: Long,
    val timestamp: Long,
    val formattedDate: String,
    val exists: Boolean
)

object SaveManager {

    private fun getBaseFolder(context: Context): File {
        val base = File(context.filesDir, "GameMotion")
        if (!base.exists()) base.mkdirs()
        return base
    }

    fun getSavesDirectory(context: Context, platform: String): File {
        val dir = File(getBaseFolder(context), "Saves/$platform")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getStatesDirectory(context: Context, platform: String): File {
        val dir = File(getBaseFolder(context), "States/$platform")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listSaveSlots(context: Context, platform: String, gameId: Long): List<SaveStateRecord> {
        val statesDir = getStatesDirectory(context, platform)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        return (1..5).map { slot ->
            val file = File(statesDir, "game_${gameId}_slot_${slot}.state")
            if (file.exists()) {
                SaveStateRecord(
                    slot = slot,
                    platform = platform,
                    gameId = gameId,
                    timestamp = file.lastModified(),
                    formattedDate = dateFormat.format(Date(file.lastModified())),
                    exists = true
                )
            } else {
                SaveStateRecord(
                    slot = slot,
                    platform = platform,
                    gameId = gameId,
                    timestamp = 0L,
                    formattedDate = "Empty Slot",
                    exists = false
                )
            }
        }
    }

    fun createSaveState(context: Context, platform: String, gameId: Long, slot: Int): Boolean {
        return try {
            val statesDir = getStatesDirectory(context, platform)
            val file = File(statesDir, "game_${gameId}_slot_${slot}.state")
            file.writeText("GameMotion State Slot $slot - $platform Game $gameId - ${System.currentTimeMillis()}")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteSaveState(context: Context, platform: String, gameId: Long, slot: Int): Boolean {
        val file = File(getStatesDirectory(context, platform), "game_${gameId}_slot_${slot}.state")
        return if (file.exists()) file.delete() else false
    }
}
