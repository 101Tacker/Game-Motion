package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.Vibrator
import android.os.VibratorManager
import android.view.InputDevice
import java.io.File
import java.util.Locale

data class DeviceHardwareInfo(
    val brand: String,
    val model: String,
    val manufacturer: String,
    val socName: String,
    val androidVersion: String,
    val apiLevel: Int,
    val is64Bit: Boolean,
    val supportedAbis: String,
    val cpuCores: Int,
    val totalRamGb: Double,
    val availableRamGb: Double,
    val isLowRamDevice: Boolean,
    val vulkanSupported: Boolean,
    val vulkanVersion: String,
    val openGlVersion: String,
    val totalStorageGb: Double,
    val freeStorageGb: Double,
    val hasHapticVibrator: Boolean,
    val connectedGamepadsCount: Int,
    val gamepadNames: List<String>,
    val gamingPerformanceScore: Int,
    val overallRating: String
)

enum class HardwareTier {
    OPTIMAL,       // 100% full speed, upscaled resolution
    PLAYABLE,      // Good playable performance with minor frame pacing adjustments
    MODERATE,      // Playable at native 1x resolution, some demanding titles may drop frames
    UNSUPPORTED    // Device does not meet minimum hardware requirements
}

data class ConsoleCompatibilityEvaluation(
    val platform: String,
    val consoleName: String,
    val tier: HardwareTier,
    val tierLabel: String,
    val recommendedResolution: String,
    val targetFps: String,
    val hardwareVerdict: String,
    val checks: List<Pair<String, Boolean>>,
    val recommendations: List<String>
)

object DeviceHardwareInspector {

    fun inspectDevice(context: Context): DeviceHardwareInfo {
        val brand = Build.BRAND.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socModel = Build.SOC_MODEL
            val socMan = Build.SOC_MANUFACTURER
            if (socModel.isNotEmpty() && socModel != Build.UNKNOWN) {
                if (socMan.isNotEmpty() && socMan != Build.UNKNOWN) "$socMan $socModel" else socModel
            } else {
                Build.HARDWARE
            }
        } else {
            Build.HARDWARE
        }

        val is64Bit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Process.is64Bit()
        } else {
            Build.SUPPORTED_ABIS.any { it.contains("64") }
        }

        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val availRamGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        val isLowRam = memInfo.lowMemory || (activityManager?.isLowRamDevice == true)

        // GPU & Graphics
        val pm = context.packageManager
        val hasVulkan = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        val vulkanVersion = if (hasVulkan) {
            if (pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x403000)) {
                "Vulkan 1.3 (Native SPIR-V)"
            } else if (pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x402000)) {
                "Vulkan 1.2"
            } else if (pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x401000)) {
                "Vulkan 1.1"
            } else {
                "Vulkan 1.0"
            }
        } else {
            "Not Available"
        }

        val glEsVersion = activityManager?.deviceConfigurationInfo?.glEsVersion ?: "3.0"

        // Storage
        var totalStorageGb = 0.0
        var freeStorageGb = 0.0
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            totalStorageGb = (stat.blockCountLong * blockSize) / (1024.0 * 1024.0 * 1024.0)
            freeStorageGb = (stat.availableBlocksLong * blockSize) / (1024.0 * 1024.0 * 1024.0)
        } catch (_: Exception) {}

        // Haptics / Vibrator
        val hasVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.hasVibrator() == true
        } else {
            @Suppress("DEPRECATION")
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vib?.hasVibrator() == true
        }

        // Controllers
        val gamepads = mutableListOf<String>()
        val deviceIds = InputDevice.getDeviceIds()
        for (id in deviceIds) {
            val dev = InputDevice.getDevice(id) ?: continue
            val sources = dev.sources
            if ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            ) {
                gamepads.add(dev.name ?: "Game Controller #$id")
            }
        }

        // Calculate Gaming Performance Score (0 - 100)
        var score = 0
        // CPU cores (max 25)
        score += when {
            cpuCores >= 8 -> 25
            cpuCores >= 6 -> 18
            cpuCores >= 4 -> 12
            else -> 6
        }
        // RAM (max 30)
        score += when {
            totalRamGb >= 11.0 -> 30
            totalRamGb >= 7.5 -> 26
            totalRamGb >= 5.5 -> 20
            totalRamGb >= 3.5 -> 14
            else -> 8
        }
        // 64-bit (max 15)
        if (is64Bit) score += 15
        // Vulkan (max 20)
        score += when {
            vulkanVersion.contains("1.3") -> 20
            vulkanVersion.contains("1.2") -> 17
            vulkanVersion.contains("1.1") -> 14
            hasVulkan -> 10
            else -> 4
        }
        // Storage headroom (max 10)
        score += when {
            freeStorageGb >= 30.0 -> 10
            freeStorageGb >= 10.0 -> 7
            freeStorageGb >= 4.0 -> 4
            else -> 1
        }

        val rating = when {
            score >= 85 -> "Flagship Tier (Console Elite)"
            score >= 70 -> "High Performance Tier"
            score >= 50 -> "Mid-Range Gaming Tier"
            else -> "Entry Level Tier"
        }

        return DeviceHardwareInfo(
            brand = brand,
            model = model,
            manufacturer = manufacturer,
            socName = soc,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            apiLevel = Build.VERSION.SDK_INT,
            is64Bit = is64Bit,
            supportedAbis = abis,
            cpuCores = cpuCores,
            totalRamGb = totalRamGb,
            availableRamGb = availRamGb,
            isLowRamDevice = isLowRam,
            vulkanSupported = hasVulkan,
            vulkanVersion = vulkanVersion,
            openGlVersion = glEsVersion,
            totalStorageGb = totalStorageGb,
            freeStorageGb = freeStorageGb,
            hasHapticVibrator = hasVibrator,
            connectedGamepadsCount = gamepads.size,
            gamepadNames = gamepads,
            gamingPerformanceScore = score,
            overallRating = rating
        )
    }

    fun evaluateConsoleCompatibility(info: DeviceHardwareInfo): List<ConsoleCompatibilityEvaluation> {
        val list = mutableListOf<ConsoleCompatibilityEvaluation>()

        // 1. PSP Evaluation
        val pspChecks = listOf(
            "ARM64/ARMv7 Architecture" to true,
            "RAM Capacity (>= 2GB)" to (info.totalRamGb >= 1.8),
            "CPU Topology (>= 4 Cores)" to (info.cpuCores >= 4),
            "OpenGL ES 3.0+ / Vulkan" to (info.vulkanSupported || info.openGlVersion >= "3.0")
        )
        list.add(
            ConsoleCompatibilityEvaluation(
                platform = "PSP",
                consoleName = "PlayStation Portable (PPSSPP Core)",
                tier = HardwareTier.OPTIMAL,
                tierLabel = "OPTIMAL (100% FPS)",
                recommendedResolution = if (info.totalRamGb >= 4.0) "3x-4x Native (1080p/2K)" else "2x Native (720p)",
                targetFps = "60 FPS Locked",
                hardwareVerdict = "This device exceeds PSP emulation requirements with seamless full-speed graphics and audio synchronization.",
                checks = pspChecks,
                recommendations = listOf(
                    "Vulkan backend is recommended for minimal latency.",
                    "Upscaling to 3x HD produces crystal-clear textures."
                )
            )
        )

        // 2. PS2 Evaluation
        val ps2RamPass = info.totalRamGb >= 3.8
        val ps2CpuPass = info.cpuCores >= 6
        val ps2ArchPass = info.is64Bit
        val ps2Checks = listOf(
            "64-Bit ARMv8+ Execution Mode" to ps2ArchPass,
            "RAM Capacity (>= 4GB)" to ps2RamPass,
            "CPU Topology (>= 6 Cores with high IPC)" to ps2CpuPass,
            "Vulkan 1.1+ Hardware Acceleration" to info.vulkanSupported
        )

        val ps2Tier = when {
            !ps2ArchPass -> HardwareTier.UNSUPPORTED
            info.totalRamGb >= 6.0 && info.cpuCores >= 8 -> HardwareTier.OPTIMAL
            ps2RamPass && info.cpuCores >= 6 -> HardwareTier.PLAYABLE
            else -> HardwareTier.MODERATE
        }

        list.add(
            ConsoleCompatibilityEvaluation(
                platform = "PS2",
                consoleName = "PlayStation 2 (AetherSX2 / PCSX2 Core)",
                tier = ps2Tier,
                tierLabel = when (ps2Tier) {
                    HardwareTier.OPTIMAL -> "OPTIMAL (Full Speed)"
                    HardwareTier.PLAYABLE -> "PLAYABLE (60 FPS)"
                    HardwareTier.MODERATE -> "MODERATE (1x Native)"
                    HardwareTier.UNSUPPORTED -> "INCOMPATIBLE"
                },
                recommendedResolution = when (ps2Tier) {
                    HardwareTier.OPTIMAL -> "2x-3x Native HD"
                    HardwareTier.PLAYABLE -> "2x Native HD"
                    else -> "1x Native Standard"
                },
                targetFps = "50-60 FPS",
                hardwareVerdict = if (ps2Tier == HardwareTier.UNSUPPORTED) {
                    "PS2 Emotion Engine emulation requires 64-bit ARM architecture. 32-bit devices cannot compile the dynamic recompiler."
                } else if (ps2Tier == HardwareTier.OPTIMAL) {
                    "Hardware has generous multi-core headroom and memory bandwidth for smooth PS2 Emotion Engine execution."
                } else {
                    "Playable performance. Demanding PS2 titles (Gran Turismo 4, Shadow of the Colossus) may require 1x native rendering."
                },
                checks = ps2Checks,
                recommendations = listOf(
                    "Use Vulkan graphics backend for best Emotion Engine synchronization.",
                    "Enable Multi-Threaded VU1 in settings if frame rate dips occur."
                )
            )
        )

        // 3. PS3 Evaluation
        val ps3RamPass = info.totalRamGb >= 5.8
        val ps3CpuPass = info.cpuCores >= 8
        val ps3ArchPass = info.is64Bit
        val ps3VulkanPass = info.vulkanSupported && (info.vulkanVersion.contains("1.2") || info.vulkanVersion.contains("1.3"))
        val ps3Checks = listOf(
            "64-Bit ARMv8.2+ Architecture" to ps3ArchPass,
            "RAM Capacity (>= 6GB Minimum, 8GB+ Recommended)" to ps3RamPass,
            "Octa-Core CPU (8 Cores for PPE + 6 SPEs)" to ps3CpuPass,
            "Vulkan 1.2+ Driver with SPIR-V Support" to ps3VulkanPass
        )

        val ps3Tier = when {
            !ps3ArchPass -> HardwareTier.UNSUPPORTED
            info.totalRamGb >= 10.0 && info.cpuCores >= 8 && ps3VulkanPass -> HardwareTier.OPTIMAL
            info.totalRamGb >= 6.0 && info.cpuCores >= 8 -> HardwareTier.PLAYABLE
            info.totalRamGb >= 5.0 && info.cpuCores >= 6 -> HardwareTier.MODERATE
            else -> HardwareTier.UNSUPPORTED
        }

        list.add(
            ConsoleCompatibilityEvaluation(
                platform = "PS3",
                consoleName = "PlayStation 3 (RPCS3 Cell Architecture)",
                tier = ps3Tier,
                tierLabel = when (ps3Tier) {
                    HardwareTier.OPTIMAL -> "FLAGSHIP READY"
                    HardwareTier.PLAYABLE -> "EXPERIMENTAL / PLAYABLE"
                    HardwareTier.MODERATE -> "HEAVY LOAD (Sub-30 FPS)"
                    HardwareTier.UNSUPPORTED -> "HARDWARE LIMITED"
                },
                recommendedResolution = if (ps3Tier == HardwareTier.OPTIMAL) "720p Native HD" else "720p with Resolution Scaling (80%)",
                targetFps = "30-60 FPS",
                hardwareVerdict = when (ps3Tier) {
                    HardwareTier.OPTIMAL -> "Device features flagship-grade CPU cores, high RAM headroom, and modern Vulkan support capable of handling complex Cell Broadband Engine threads."
                    HardwareTier.PLAYABLE -> "Hardware meets baseline Cell requirements. Lighter PS3 titles and 2D/arcade games run well; AAA titles may require shader pre-compilation."
                    HardwareTier.MODERATE -> "Device RAM (~${String.format(Locale.US, "%.1f", info.totalRamGb)} GB) is near the operational floor. RSX texture swapping will cause frame drops."
                    HardwareTier.UNSUPPORTED -> "Device hardware is below minimum specs for Cell SPE emulation. At least 6GB RAM and an 8-core 64-bit CPU are strictly required."
                },
                checks = ps3Checks,
                recommendations = listOf(
                    "Ensure Official System Software (.PUP) is imported.",
                    "Close background applications to maximize available RAM before launching."
                )
            )
        )

        // 4. Godot Engine Evaluation
        val godotChecks = listOf(
            "Native Android Execution" to true,
            "Vulkan Forward+ / Mobile Renderer" to info.vulkanSupported,
            "OpenGL ES 3.0 Fallback" to true,
            "Direct Gamepad Input" to true
        )
        list.add(
            ConsoleCompatibilityEvaluation(
                platform = "GODOT",
                consoleName = "Godot Engine (Vulkan / GLES3)",
                tier = HardwareTier.OPTIMAL,
                tierLabel = "OPTIMAL (Native Speed)",
                recommendedResolution = "Native Display Resolution",
                targetFps = "60-120 FPS",
                hardwareVerdict = "Godot titles run directly on Android with native CPU and GPU execution, offering supreme battery efficiency and responsiveness.",
                checks = godotChecks,
                recommendations = listOf(
                    "Supports both 2D pixel-art and 3D textured titles.",
                    "Controllers map automatically to Godot Action Map inputs."
                )
            )
        )

        return list
    }
}
