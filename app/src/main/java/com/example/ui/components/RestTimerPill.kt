package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import kotlinx.coroutines.delay

@Composable
fun RestTimerPill(
    totalSeconds: Int,
    remainingSeconds: Int,
    isVisible: Boolean,
    isPaused: Boolean = false,
    label: String = "Czas na przerwę",
    onPauseResume: () -> Unit = {},
    onAddSeconds: (Int) -> Unit = {},
    onReduceSeconds: (Int) -> Unit = {},
    onFinishedOrDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isExerciseTimer = remember(label) {
        label.contains("ćwiczen", ignoreCase = true) || label.contains("seri", ignoreCase = true)
    }
    val timerAccent = if (isExerciseTimer) ElectricCyan else AthleticOrange

    AnimatedVisibility(
        visible = isVisible && remainingSeconds > 0,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
                        CircularProgressIndicator(
                            progress = { progress },
                            color = timerAccent,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.size(38.dp),
                            strokeWidth = 3.5.dp
                        )
                        IconButton(
                            onClick = onPauseResume,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Wznów timer" else "Zatrzymaj timer",
                                tint = timerAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (isPaused) "$label (Wstrzymano)" else label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPaused) timerAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        val mins = remainingSeconds / 60
                        val secs = remainingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", mins, secs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaused) MaterialTheme.colorScheme.onSurfaceVariant else timerAccent,
                            fontSize = 18.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // -15s
                    IconButton(
                        onClick = { onReduceSeconds(15) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "-15",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // +30s
                    IconButton(
                        onClick = { onAddSeconds(30) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "+30",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Close/Skip
                    IconButton(
                        onClick = onFinishedOrDismissed,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Pomiń przerwę",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Przeciążenie zachowujące kompatybilność wsteczną.
 */
@Composable
fun RestTimerPill(
    totalSeconds: Int,
    isVisible: Boolean,
    onFinishedOrDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    RestTimerPill(
        totalSeconds = totalSeconds,
        remainingSeconds = totalSeconds,
        isVisible = isVisible,
        isPaused = false,
        label = "Czas na przerwę",
        onPauseResume = {},
        onAddSeconds = {},
        onReduceSeconds = {},
        onFinishedOrDismissed = onFinishedOrDismissed,
        modifier = modifier
    )
}
