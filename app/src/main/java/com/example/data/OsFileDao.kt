package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OsFileDao {
    @Query("SELECT * FROM os_files ORDER BY importDate DESC")
    fun getAllOsFiles(): Flow<List<OsFileEntity>>

    @Query("SELECT * FROM os_files WHERE osType = :osType LIMIT 1")
    suspend fun getOsFileByType(osType: String): OsFileEntity?

    @Query("SELECT * FROM os_files WHERE consoleTarget = :console LIMIT 1")
    suspend fun getOsFileByConsole(console: String): OsFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOsFile(osFile: OsFileEntity): Long

    @Delete
    suspend fun deleteOsFile(osFile: OsFileEntity)
}
