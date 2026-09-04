package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.controller.ControllerManager
import com.example.ui.GameMotionApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    ControllerManager.initialize(this)

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          GameMotionApp(
            onToggleFullscreen = { isFullscreen ->
              setImmersiveFullscreen(isFullscreen)
            }
          )
        }
      }
    }
  }

  fun setImmersiveFullscreen(enable: Boolean) {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    if (enable) {
      windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    } else {
      windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    val handled = ControllerManager.processKeyEvent(event)
    return if (handled) true else super.dispatchKeyEvent(event)
  }

  override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
    val handled = ControllerManager.processMotionEvent(event)
    return if (handled) true else super.dispatchGenericMotionEvent(event)
  }
}
