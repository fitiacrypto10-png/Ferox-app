package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.TermsPreferences
import com.example.voice.VoiceChatState
import com.example.voice.VoiceChatViewModel
import com.example.voice.VoiceStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ferox", appName)
    }

    @Test
    fun `test default voice chat state`() {
        val state = VoiceChatState.defaultState()
        assertTrue(state.isConnected)
        assertFalse(state.isMicMuted)
        assertEquals("Vocal En Jeu", state.activeSpeaker.name)
        assertTrue(state.participants.isNotEmpty())
    }

    @Test
    fun `test voice chat view model mic toggle`() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        VoiceStateHolder.setMicMuted(application, false)
        val viewModel = VoiceChatViewModel(application)

        assertFalse(viewModel.uiState.value.isMicMuted)

        // Toggle mic off
        viewModel.toggleMic()
        assertTrue(VoiceStateHolder.isMicMuted.value)

        // Toggle mic back on
        viewModel.toggleMic()
        assertFalse(VoiceStateHolder.isMicMuted.value)
    }

    @Test
    fun `test terms and license acceptance persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        TermsPreferences.setTermsAccepted(context, false)
        assertFalse(TermsPreferences.isTermsAccepted(context))

        TermsPreferences.setTermsAccepted(context, true)
        assertTrue(TermsPreferences.isTermsAccepted(context))
    }

    @Test
    fun `test network monitor connectivity check`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val monitor = com.example.network.NetworkMonitor(context)
        // Verify monitor can be instantiated and checks without crashing
        monitor.checkCurrentConnectivity()
    }
}

