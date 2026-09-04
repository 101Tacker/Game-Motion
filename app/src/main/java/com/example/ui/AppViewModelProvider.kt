package com.example.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.GameMotionApplication

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            GameViewModel(gameMotionApplication().repository)
        }
    }
}

fun CreationExtras.gameMotionApplication(): GameMotionApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GameMotionApplication)
