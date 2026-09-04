package com.example.runtime

import android.content.Context
import com.example.data.GameEntity

data class RuntimeConfig(
    val graphicsBackend: String = "Vulkan",
    val resolutionScale: Float = 1.0f,
    val frameLimit: Int = 60,
    val vsync: Boolean = true,
    val audioLatencyMs: Int = 30,
    val vibrationEnabled: Boolean = true
)

data class SessionInfo(
    val gameTitle: String,
    val platform: String,
    val engineVersion: String,
    val bootedFirmware: String?,
    val startTime: Long = System.currentTimeMillis()
)

data class PerformanceInfo(
    val fps: Float,
    val frameTimeMs: Float,
    val cpuUsagePercent: Float,
    val ramUsageMb: Long,
    val gpuBackend: String,
    val temperature: Float? = null
)

data class CompatibilityResult(
    val isCompatible: Boolean,
    val message: String,
    val missingRequirements: List<String> = emptyList()
)

interface GameRuntime {
    val platform: String
    val name: String
    val coreVersion: String

    fun initialize(context: Context): Boolean
    fun shutdown()
    fun launchGame(context: Context, game: GameEntity, config: RuntimeConfig): Result<SessionInfo>
    fun pauseGame()
    fun resumeGame()
    fun stopGame()
    fun saveState(slot: Int): Boolean
    fun loadState(slot: Int): Boolean
    fun getPerformanceInformation(): PerformanceInfo
    fun getSupportedFormats(): List<String>
    fun isDeviceCompatible(context: Context): CompatibilityResult
}
