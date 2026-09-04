package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "os_files")
data class OsFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileUri: String,
    val fileSize: Long,
    val osType: String, // "PBP", "BIN", "PUP"
    val consoleTarget: String, // "PSP", "PS2", "PS3"
    val description: String = "",
    val importDate: Long = System.currentTimeMillis()
)
