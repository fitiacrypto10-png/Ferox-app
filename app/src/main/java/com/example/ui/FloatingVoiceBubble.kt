package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voice.VoiceChatState
import kotlin.math.roundToInt

@Composable
fun FloatingVoiceBubble(
    state: VoiceChatState,
    offsetX: Float,
    offsetY: Float,
    onDrag: (Float, Float) -> Unit,
    onToggleMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isWindowOpen by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseWave by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseWave"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Messenger-Style Round Bubble
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clickable { isWindowOpen = !isWindowOpen }
                    .testTag("floating_voice_bubble"),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing audio wave if unmuted & speaking
                if (!state.isMicMuted && state.activeSpeaker.isSpeaking) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .scale(pulseWave)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                    )
                }

                // Round Messenger circle
                Surface(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(10.dp, CircleShape),
                    shape = CircleShape,
                    color = Color(0xFF161426),
                    border = androidx.compose.foundation.BorderStroke(
                        2.5.dp,
                        if (state.isMicMuted) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = state.activeSpeaker.avatarResId),
                            contentDescription = "Bulle ronde Messenger",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )

                        // Small status dot
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (state.isMicMuted) Color(0xFFEF4444) else Color(0xFF10B981))
                                .border(1.5.dp, Color(0xFF161426), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
            }

            // Petite Fenêtre ultra-compacte (très petite comme demandé)
            AnimatedVisibility(
                visible = isWindowOpen,
                enter = fadeIn(tween(140)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(tween(120)) + scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(120)
                )
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(135.dp)
                        .shadow(12.dp, RoundedCornerShape(14.dp))
                        .testTag("bubble_popup_window"),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1C192E),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.isMicMuted) Color(0xFFEF4444).copy(alpha = 0.6f) else Color(0xFF10B981).copy(alpha = 0.6f)
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                onToggleMic()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isMicMuted) Color(0xFF10B981) else Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .testTag("toggle_mic_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (state.isMicMuted) Icons.Filled.Mic else Icons.Filled.MicOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (state.isMicMuted) "Activer micro" else "Couper micro",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
