package com.example.voice

import androidx.annotation.DrawableRes
import com.example.R

data class VoiceParticipant(
    val id: String,
    val name: String,
    val role: String,
    @DrawableRes val avatarResId: Int,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val audioLevel: Float = 0f
)

data class VoiceChatState(
    val isConnected: Boolean = true,
    val isMicMuted: Boolean = false,
    val isSpeakerMuted: Boolean = false,
    val isBubbleExpanded: Boolean = false,
    val activeSpeaker: VoiceParticipant,
    val participants: List<VoiceParticipant> = emptyList(),
    val roomName: String = "Ferox Community"
) {
    companion object {
        fun defaultState(): VoiceChatState {
            val mainSpeaker = VoiceParticipant(
                id = "gaming_leader",
                name = "Vocal En Jeu",
                role = "Orateur",
                avatarResId = R.drawable.ic_bubble_logo,
                isSpeaking = true,
                isMuted = false,
                audioLevel = 0.85f
            )

            val participantsList = listOf(
                mainSpeaker,
                VoiceParticipant(
                    id = "member_1",
                    name = "Membre 1",
                    role = "Participant",
                    avatarResId = R.drawable.ic_bubble_logo,
                    isSpeaking = false,
                    isMuted = false,
                    audioLevel = 0.0f
                ),
                VoiceParticipant(
                    id = "me",
                    name = "Moi",
                    role = "Pilote",
                    avatarResId = R.drawable.ic_bubble_logo,
                    isSpeaking = false,
                    isMuted = false,
                    audioLevel = 0.1f
                )
            )

            return VoiceChatState(
                isConnected = true,
                isMicMuted = false,
                isSpeakerMuted = false,
                isBubbleExpanded = false,
                activeSpeaker = mainSpeaker,
                participants = participantsList,
                roomName = "Ferox Voice"
            )
        }
    }
}
