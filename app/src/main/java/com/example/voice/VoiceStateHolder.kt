package com.example.voice

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VoiceStateHolder {
    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    // Listener called whenever mic state changes from anywhere (in-app or floating overlay)
    var onMicStateChangedListener: ((Boolean) -> Unit)? = null

    fun toggleMic(context: Context): Boolean {
        val newMuted = !_isMicMuted.value
        setMicMuted(context, newMuted)
        return newMuted
    }

    fun setMicMuted(context: Context, muted: Boolean) {
        _isMicMuted.value = muted
        try {
            val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isMicrophoneMute = muted
        } catch (_: Exception) {}

        // Notify WebView to mute WebRTC audio tracks
        try {
            onMicStateChangedListener?.invoke(muted)
        } catch (_: Exception) {}
    }
}
