package com.example

import android.app.Application
import com.example.data.GameDatabase
import com.example.data.GameRepository

class GameMotionApplication : Application() {
    val database by lazy { GameDatabase.getDatabase(this) }
    val repository by lazy { GameRepository(database.gameDao(), database.osFileDao()) }
}
