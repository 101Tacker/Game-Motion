package com.example.controller

data class GameMotionInput(
    // Analog Sticks (-1.0f to 1.0f)
    var leftStickX: Float = 0f,
    var leftStickY: Float = 0f,
    var rightStickX: Float = 0f,
    var rightStickY: Float = 0f,

    // Face Buttons (Cross/A, Circle/B, Square/X, Triangle/Y)
    var buttonA: Boolean = false, // Cross / A
    var buttonB: Boolean = false, // Circle / B
    var buttonX: Boolean = false, // Square / X
    var buttonY: Boolean = false, // Triangle / Y

    // Directional Pad
    var dpadUp: Boolean = false,
    var dpadDown: Boolean = false,
    var dpadLeft: Boolean = false,
    var dpadRight: Boolean = false,

    // Shoulder & Triggers
    var leftShoulder: Boolean = false,  // L1
    var rightShoulder: Boolean = false, // R1
    var leftTrigger: Float = 0f,        // L2 (0.0f to 1.0f)
    var rightTrigger: Float = 0f,       // R2 (0.0f to 1.0f)

    // Thumbstick Clicks
    var thumbLeft: Boolean = false,     // L3
    var thumbRight: Boolean = false,    // R3

    // System Buttons
    var start: Boolean = false,
    var select: Boolean = false,
    var home: Boolean = false
) {
    fun isAnyButtonPressed(): Boolean {
        return buttonA || buttonB || buttonX || buttonY ||
                dpadUp || dpadDown || dpadLeft || dpadRight ||
                leftShoulder || rightShoulder || leftTrigger > 0.3f || rightTrigger > 0.3f ||
                thumbLeft || thumbRight || start || select || home
    }
}

data class ControllerProfile(
    val platform: String, // "GLOBAL", "PSP", "PS2", "PS3", "GODOT"
    val buttonAMap: String = "Cross / A",
    val buttonBMap: String = "Circle / B",
    val buttonXMap: String = "Square / X",
    val buttonYMap: String = "Triangle / Y",
    val deadzone: Float = 0.15f
)
