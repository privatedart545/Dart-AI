package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.StatusError
import com.example.voice.VoiceState

@Composable
fun VoiceModalSheet(
    voiceState: VoiceState,
    isGenerating: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onDismiss()
    }

    val pulseAnim = remember { Animatable(1.0f) }

    val isListening = voiceState is VoiceState.Listening || voiceState is VoiceState.Transcribing
    val isSpeaking = voiceState is VoiceState.Speaking

    LaunchedEffect(isListening, isSpeaking, isGenerating) {
        if (isListening || isSpeaking || isGenerating) {
            pulseAnim.animateTo(
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAnim.snapTo(1.0f)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("voice_modal_overlay"),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DART AI VOICE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Text(
                        text = "Autonomous voice command stream",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                FilledIconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("voice_modal_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Voice Modal",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Central Animated Visualizer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer pulsing halo
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .scale(pulseAnim.value)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (isSpeaking) PurpleGlow else CyanGlow,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.1f)
                                    )
                                )
                            )
                    )

                    // Secondary ring
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .border(
                                2.dp,
                                if (isSpeaking) PurpleAccent else MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )

                    // Core Mic Button
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (isSpeaking) listOf(PurpleAccent, MaterialTheme.colorScheme.primary)
                                    else listOf(MaterialTheme.colorScheme.primary, PurpleAccent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isSpeaking -> Icons.Default.Stop
                                isListening -> Icons.Default.Mic
                                else -> Icons.Default.MicOff
                            },
                            contentDescription = "Voice State Icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Status Label
                Text(
                    text = when {
                        isSpeaking -> "DART AI IS SPEAKING..."
                        isGenerating -> "NEMOTRON 3 ULTRA REASONING..."
                        isListening -> "LISTENING TO YOUR COMMAND..."
                        voiceState is VoiceState.Error -> "VOICE ERROR"
                        else -> "TAP MIC TO SPEAK"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = when {
                            voiceState is VoiceState.Error -> StatusError
                            isSpeaking -> PurpleAccent
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Transcription or Subtitle Box
                val subtitleText = when (voiceState) {
                    is VoiceState.Transcribing -> "\"${voiceState.partialText}\""
                    is VoiceState.SpeechReady -> "\"${voiceState.fullText}\""
                    is VoiceState.Speaking -> "\"${voiceState.text.take(130)}...\""
                    is VoiceState.Error -> voiceState.message
                    else -> "Ask anything or try: 'Turn on flashlight', 'Check battery', 'Open Wi-Fi settings', 'Set timer for 5 minutes'..."
                }

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (voiceState is VoiceState.Error) StatusError else MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // Bottom Action Controls: Single unambiguous voice control + Explicit "Switch to Text" button!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Voice Control Button
                when {
                    isSpeaking -> {
                        Button(
                            onClick = onStopSpeaking,
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(48.dp)
                                .testTag("voice_stop_speaking_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Speaking", fontWeight = FontWeight.Bold)
                        }
                    }
                    isListening -> {
                        Button(
                            onClick = onStopListening,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(48.dp)
                                .testTag("voice_stop_listening_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Done", tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Done Listening", fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onStartListening,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(48.dp)
                                .testTag("voice_tap_to_speak_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Mic", tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tap to Speak", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Explicit button to return to texting mode!
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(44.dp)
                        .testTag("switch_to_text_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Switch to Text",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch to Keyboard Mode", fontSize = 13.sp)
                }
            }
        }
    }
}
