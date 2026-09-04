package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val thumbnailUri: String?,
    val packageUri: String,
    val packageSize: Long,
    val unrealVersion: String,
    val architecture: String,
    val compatibilityStatus: String,
    val consoleType: String = "PSP", // "PSP", "PS2", "PS3", "Godot"
    val fileExtension: String = "ISO",
    val lastPlayed: Long = 0,
    val playCount: Int = 0,
    val importDate: Long = System.currentTimeMillis()
)
