package com.example.runtime

import android.content.Context
import com.example.data.GameEntity

object RuntimeManager {

    private val runtimes = mapOf<String, GameRuntime>(
        "PSP" to PSPRuntime(),
        "PS2" to PS2Runtime(),
        "PS3" to PS3Runtime(),
        "GODOT" to GodotRuntime()
    )

    var activeRuntime: GameRuntime? = null
        private set

    var activeSession: SessionInfo? = null
        private set

    fun getRuntime(platform: String): GameRuntime? {
        return runtimes[platform.uppercase()]
    }

    fun getSupportedPlatforms(): List<String> {
        return runtimes.keys.toList()
    }

    fun checkCompatibility(context: Context, platform: String): CompatibilityResult {
        val runtime = getRuntime(platform) ?: return CompatibilityResult(
            isCompatible = false,
            message = "No runtime registered for platform: $platform"
        )
        return runtime.isDeviceCompatible(context)
    }

    fun launch(
        context: Context,
        game: GameEntity,
        config: RuntimeConfig = RuntimeConfig()
    ): Result<SessionInfo> {
        val runtime = getRuntime(game.consoleType) ?: return Result.failure(
            IllegalArgumentException("No runtime found for console platform: ${game.consoleType}")
        )

        // Validate hardware compatibility
        val compat = runtime.isDeviceCompatible(context)
        if (!compat.isCompatible) {
            return Result.failure(IllegalStateException(compat.message))
        }

        // Initialize and launch
        runtime.initialize(context)
        val result = runtime.launchGame(context, game, config)
        if (result.isSuccess) {
            activeRuntime = runtime
            activeSession = result.getOrNull()
        }
        return result
    }

    fun stopActiveSession() {
        activeRuntime?.stopGame()
        activeRuntime?.shutdown()
        activeRuntime = null
        activeSession = null
    }

    fun pauseActiveSession() {
        activeRuntime?.pauseGame()
    }

    fun resumeActiveSession() {
        activeRuntime?.resumeGame()
    }

    fun saveState(slot: Int): Boolean {
        return activeRuntime?.saveState(slot) ?: false
    }

    fun loadState(slot: Int): Boolean {
        return activeRuntime?.loadState(slot) ?: false
    }

    fun getPerformanceInfo(): PerformanceInfo {
        return activeRuntime?.getPerformanceInformation()
            ?: PerformanceInfo(0f, 0f, 0f, 0L, "Vulkan")
    }
}
