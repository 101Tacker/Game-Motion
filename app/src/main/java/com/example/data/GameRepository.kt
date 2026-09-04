package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(
    private val gameDao: GameDao,
    private val osFileDao: OsFileDao
) {
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()
    val recentGames: Flow<List<GameEntity>> = gameDao.getRecentGames()
    val allOsFiles: Flow<List<OsFileEntity>> = osFileDao.getAllOsFiles()

    suspend fun getGame(id: Long): GameEntity? {
        return gameDao.getGameById(id)
    }

    suspend fun insertGame(game: GameEntity): Long {
        return gameDao.insertGame(game)
    }

    suspend fun updateGame(game: GameEntity) {
        gameDao.updateGame(game)
    }

    suspend fun deleteGame(game: GameEntity) {
        gameDao.deleteGame(game)
    }

    suspend fun getOsFileByType(osType: String): OsFileEntity? {
        return osFileDao.getOsFileByType(osType)
    }

    suspend fun getOsFileByConsole(console: String): OsFileEntity? {
        return osFileDao.getOsFileByConsole(console)
    }

    suspend fun insertOsFile(osFile: OsFileEntity): Long {
        return osFileDao.insertOsFile(osFile)
    }

    suspend fun deleteOsFile(osFile: OsFileEntity) {
        osFileDao.deleteOsFile(osFile)
    }
}
