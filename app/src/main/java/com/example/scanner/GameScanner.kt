package com.example.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.GameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class DiscoveredGame(
    val title: String,
    val platform: String,
    val fileFormat: String,
    val fileSize: Long,
    val uriString: String,
    val compatibilityStatus: String
)

data class ScanProgress(
    val currentFolder: String,
    val scannedFilesCount: Int,
    val foundGamesCount: Int,
    val isComplete: Boolean,
    val discoveredGames: List<DiscoveredGame> = emptyList()
)

object GameScanner {

    fun scanFolder(context: Context, treeUri: Uri): Flow<ScanProgress> = flow {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
        if (rootDoc == null || !rootDoc.canRead()) {
            emit(ScanProgress("Unable to access directory", 0, 0, true))
            return@flow
        }

        val discovered = mutableListOf<DiscoveredGame>()
        var scannedCount = 0

        fun scanRecursive(dir: DocumentFile) {
            val files = dir.listFiles()
            for (file in files) {
                scannedCount++
                if (file.isDirectory) {
                    val dirName = file.name ?: ""
                    // Check for PS3 folder format (PS3_GAME directory)
                    if (dirName.equals("PS3_GAME", ignoreCase = true) ||
                        file.findFile("PARAM.SFO") != null
                    ) {
                        val parentTitle = dir.name ?: "PS3 Game"
                        discovered.add(
                            DiscoveredGame(
                                title = parentTitle,
                                platform = "PS3",
                                fileFormat = "FOLDER",
                                fileSize = 4294967296L,
                                uriString = file.uri.toString(),
                                compatibilityStatus = "Ready for PS3 Core"
                            )
                        )
                    } else {
                        scanRecursive(file)
                    }
                } else {
                    val name = file.name ?: continue
                    val ext = name.substringAfterLast('.', "").uppercase()
                    val size = file.length()
                    val title = name.substringBeforeLast('.')

                    val detectedGame = when (ext) {
                        "CSO", "PBP" -> DiscoveredGame(
                            title = title,
                            platform = "PSP",
                            fileFormat = ext,
                            fileSize = size,
                            uriString = file.uri.toString(),
                            compatibilityStatus = "Ready for PPSSPP Core"
                        )
                        "CHD", "ZSO" -> DiscoveredGame(
                            title = title,
                            platform = "PS2",
                            fileFormat = ext,
                            fileSize = size,
                            uriString = file.uri.toString(),
                            compatibilityStatus = "Ready for PCSX2-EE Core"
                        )
                        "PKG" -> DiscoveredGame(
                            title = title,
                            platform = "PS3",
                            fileFormat = ext,
                            fileSize = size,
                            uriString = file.uri.toString(),
                            compatibilityStatus = "Ready for RPCS3 Core"
                        )
                        "PCK", "APK" -> DiscoveredGame(
                            title = title,
                            platform = "GODOT",
                            fileFormat = ext,
                            fileSize = size,
                            uriString = file.uri.toString(),
                            compatibilityStatus = "Ready for Godot Engine"
                        )
                        "ISO" -> {
                            // Smart platform determination based on parent folder or size
                            val parentName = dir.name?.uppercase() ?: ""
                            val platform = when {
                                parentName.contains("PSP") -> "PSP"
                                parentName.contains("PS3") -> "PS3"
                                size < 1800000000L -> "PSP"
                                size > 8000000000L -> "PS3"
                                else -> "PS2"
                            }
                            DiscoveredGame(
                                title = title,
                                platform = platform,
                                fileFormat = "ISO",
                                fileSize = size,
                                uriString = file.uri.toString(),
                                compatibilityStatus = "Ready for $platform Core"
                            )
                        }
                        else -> null
                    }

                    if (detectedGame != null) {
                        discovered.add(detectedGame)
                    }
                }
            }
        }

        scanRecursive(rootDoc)
        emit(
            ScanProgress(
                currentFolder = rootDoc.name ?: "Root",
                scannedFilesCount = scannedCount,
                foundGamesCount = discovered.size,
                isComplete = true,
                discoveredGames = discovered
            )
        )
    }.flowOn(Dispatchers.IO)
}
