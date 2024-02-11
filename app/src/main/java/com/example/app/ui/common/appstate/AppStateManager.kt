package com.example.app.ui.common.appstate

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppStateManager : DefaultLifecycleObserver {
    private val _state = MutableStateFlow(AppState.UNKNOWN)
    val state: StateFlow<AppState> get() = _state

    private val _onAppForegrounded = MutableSharedFlow<Unit>()
    val onAppForegrounded: SharedFlow<Unit> get() = _onAppForegrounded
    private val _onAppBackgrounded = MutableSharedFlow<Unit>()
    val onAppBackgrounded: SharedFlow<Unit> get() = _onAppBackgrounded

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        owner.lifecycleScope.launch {
            state.collectLatest {
                when (it) {
                    AppState.FOREGROUND -> _onAppForegrounded.emit(Unit)
                    AppState.BACKGROUND -> _onAppBackgrounded.emit(Unit)
                    AppState.UNKNOWN -> Unit
                }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            onAppForegrounded()
        }
    }

    private suspend fun onAppForegrounded() {
        _state.emit(AppState.FOREGROUND)
    }

    override fun onStop(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            onAppBackgrounded()
        }
    }

    private suspend fun onAppBackgrounded() {
        _state.emit(AppState.BACKGROUND)
    }
}