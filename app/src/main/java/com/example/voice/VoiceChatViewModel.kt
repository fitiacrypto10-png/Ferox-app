package com.example.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoiceChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VoiceChatState.defaultState())
    val uiState: StateFlow<VoiceChatState> = _uiState.asStateFlow()

    // Floating bubble position offset in dp or px
    val bubbleOffsetX = MutableStateFlow(24f)
    val bubbleOffsetY = MutableStateFlow(160f)

    init {
        // Sync with global mic state
        viewModelScope.launch {
            VoiceStateHolder.isMicMuted.collect { muted ->
                _uiState.update { current ->
                    current.copy(
                        isMicMuted = muted,
                        activeSpeaker = current.activeSpeaker.copy(
                            isSpeaking = !muted
                        )
                    )
                }
            }
        }

        // Subtle animation for speaker
        viewModelScope.launch {
            while (true) {
                delay(300)
                _uiState.update { current ->
                    if (!current.isMicMuted) {
                        current.copy(
                            activeSpeaker = current.activeSpeaker.copy(
                                isSpeaking = true
                            )
                        )
                    } else {
                        current.copy(
                            activeSpeaker = current.activeSpeaker.copy(
                                isSpeaking = false
                            )
                        )
                    }
                }
            }
        }
    }

    fun toggleMic() {
        VoiceStateHolder.toggleMic(getApplication())
    }

    fun setMicMuted(muted: Boolean) {
        VoiceStateHolder.setMicMuted(getApplication(), muted)
    }

    fun updateBubblePosition(deltaX: Float, deltaY: Float) {
        bubbleOffsetX.value = (bubbleOffsetX.value + deltaX).coerceIn(-20f, 650f)
        bubbleOffsetY.value = (bubbleOffsetY.value + deltaY).coerceIn(40f, 1800f)
    }
}
