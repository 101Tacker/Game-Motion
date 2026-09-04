package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameEntity
import com.example.data.GameRepository
import com.example.data.OsFileEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    val allGames: StateFlow<List<GameEntity>> = repository.allGames
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentGames: StateFlow<List<GameEntity>> = repository.recentGames
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allOsFiles: StateFlow<List<OsFileEntity>> = repository.allOsFiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun importGame(
        title: String,
        packageUri: String,
        packageSize: Long,
        unrealVersion: String,
        architecture: String,
        compatibilityStatus: String,
        consoleType: String,
        fileExtension: String
    ) {
        viewModelScope.launch {
            val game = GameEntity(
                title = title,
                thumbnailUri = null,
                packageUri = packageUri,
                packageSize = packageSize,
                unrealVersion = unrealVersion,
                architecture = architecture,
                compatibilityStatus = compatibilityStatus,
                consoleType = consoleType,
                fileExtension = fileExtension
            )
            repository.insertGame(game)
        }
    }

    suspend fun getGame(id: Long): GameEntity? {
        return repository.getGame(id)
    }

    suspend fun getOsFileForConsole(console: String): OsFileEntity? {
        return repository.getOsFileByConsole(console)
    }

    suspend fun getOsFileByType(osType: String): OsFileEntity? {
        return repository.getOsFileByType(osType)
    }

    fun importOsFile(
        fileName: String,
        fileUri: String,
        fileSize: Long,
        osType: String,
        consoleTarget: String,
        description: String = ""
    ) {
        viewModelScope.launch {
            val osFile = OsFileEntity(
                fileName = fileName,
                fileUri = fileUri,
                fileSize = fileSize,
                osType = osType,
                consoleTarget = consoleTarget,
                description = description
            )
            repository.insertOsFile(osFile)
        }
    }

    fun deleteOsFile(osFile: OsFileEntity) {
        viewModelScope.launch {
            repository.deleteOsFile(osFile)
        }
    }

    fun updateGame(game: GameEntity) {
        viewModelScope.launch {
            repository.updateGame(game)
        }
    }

    fun deleteGame(game: GameEntity) {
        viewModelScope.launch {
            repository.deleteGame(game)
        }
    }
}
