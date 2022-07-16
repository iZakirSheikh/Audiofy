package com.prime.player.audio.console


import android.app.Application
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


private const val TAG = "HomeViewModel"

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val context: Application
) : ViewModel()