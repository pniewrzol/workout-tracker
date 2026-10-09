package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Exercise
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import com.example.util.SupersetHelper
import com.example.util.SupersetInfo

/**
 * Dialog podglądu planu treningowego bez konieczności jego rozpoczynania.
 * Prezentuje wszystkie ćwiczenia, ich parametry, kolejność oraz serie łączone (np. C.5a + C.5b).
 * Wyśrodkowany, w pełni responsywny i odporny na rozjeżdżanie się elementów w emulatorze.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutPlanPreviewDialog(
    workoutName: String,
    exercises: List<Exercise>,
    estimatedTime: String,
    onDismiss: () -> Unit,
    onStartWorkout: () -> Unit,
    onExerciseClick: (Long) -> Unit = {}
) {
    val totalSets = remember(exercises) { exercises.sumOf { it.targetSets } }
    val supersetsMap = remember(exercises) { SupersetHelper.detectSupersets(exercises) }
    val supersetGroupsCount = remember(supersetsMap) {
        supersetsMap.values.map { it.groupKey }.distinct().size
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .widthIn(max = 560.dp)
                    .testTag("dialog_workout_plan_preview"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Nagłówek Dialogu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(30.dp)
                                    .background(AthleticOrange, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Podgląd: $workoutName",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${exercises.size} ćwiczeń • $totalSets serii • ok. $estimatedTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_close_plan_preview")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Zamknij podgląd"
                            )
                        }
                    }

                    // Podsumowanie superserii jeśli występują
                    if (supersetGroupsCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Wykryto $supersetGroupsCount serii łączonych (np. z literami a/b). Wykonuj je naprzemiennie przed dłuższą przerwą.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE9D5FF),
                                    lineHeight = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Lista ćwiczeń
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(exercises, key = { _, ex -> ex.id }) { index, exercise ->
                            val supersetInfo = supersetsMap[exercise.id]
                            PreviewExerciseCard(
                                index = index,
                                exercise = exercise,
                                supersetInfo = supersetInfo,
                                onExerciseClick = { onExerciseClick(exercise.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Przyciski akcji na dole (zawsze równa wysokość i brak rozjeżdżania się)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Zamknij",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onStartWorkout()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("btn_start_workout_from_preview"),
                            colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rozpocznij trening",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewExerciseCard(
    index: Int,
    exercise: Exercise,
    supersetInfo: SupersetInfo?,
    onExerciseClick: () -> Unit
) {
    val isSuperset = supersetInfo != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExerciseClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuperset) Color(0xFF1E1B4B).copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = if (isSuperset) 1.5.dp else 1.dp,
            color = if (isSuperset) Color(0xFF8B5CF6).copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Superset banner if part of a superset
            if (isSuperset && supersetInfo != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SERIA ŁĄCZONA ${supersetInfo.groupKey.uppercase()} (${supersetInfo.letter.uppercase()})",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE9D5FF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (supersetInfo.partnerExercises.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val partner = supersetInfo.partnerExercises.first()
                            Text(
                                text = "z: ${partner.code.ifBlank { partner.name.take(12) }}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFA78BFA),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Nagłówek ćwiczenia (Kod + Nazwa)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayCode = if (exercise.code.isNotBlank()) exercise.code else "${index + 1}."
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSuperset) Color(0xFF8B5CF6).copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = displayCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSuperset) Color(0xFFA78BFA) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Parametry: Serie × Powtórzenia, Przerwa, RIR, Tempo
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PreviewSpecBadge(
                    icon = Icons.Default.FitnessCenter,
                    text = "${exercise.targetSets} serie × ${exercise.targetReps}",
                    color = AthleticOrange
                )

                val restText = if (isSuperset && supersetInfo?.isFirstInSuperset == true) {
                    "Przejście: 15s"
                } else if (exercise.restDisplay.isNotBlank()) {
                    "Przerwa: ${exercise.restDisplay}"
                } else {
                    "Przerwa: ${exercise.restSeconds}s"
                }
                PreviewSpecBadge(
                    icon = Icons.Default.Timer,
                    text = restText,
                    color = ElectricCyan
                )

                if (exercise.rir.isNotBlank() && exercise.rir != "-") {
                    PreviewSpecBadge(
                        icon = Icons.Default.Speed,
                        text = "RIR: ${exercise.rir}",
                        color = GoldPr
                    )
                }

                if (exercise.tempo.isNotBlank() && exercise.tempo != "-") {
                    PreviewSpecBadge(
                        icon = Icons.Default.Schedule,
                        text = "Tempo: ${exercise.tempo}",
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            val details = mutableListOf<String>()
            if (exercise.equipment.isNotBlank()) details.add(exercise.equipment)
            if (exercise.primaryMuscles.isNotBlank()) details.add(exercise.primaryMuscles)
            if (details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = details.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PreviewSpecBadge(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
