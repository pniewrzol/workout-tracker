package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutHistoryScreen(
    sessions: List<WorkoutSession>,
    allCompletedSets: List<WorkoutSetLog>,
    allExercises: List<Exercise>,
    modifier: Modifier = Modifier
) {
    val exerciseMap = remember(allExercises) { allExercises.associateBy { it.id } }

    var selectedFilterIndex by remember { mutableStateOf(0) } // 0: Wszystkie, 1: Treningi główne, 2: Rozgrzewka & Mobilizacja

    val mainSessions = remember(sessions) { sessions.filter { it.isMainWorkout } }
    val warmupSessions = remember(sessions) { sessions.filter { !it.isMainWorkout } }

    val filteredSessions = when (selectedFilterIndex) {
        1 -> mainSessions
        2 -> warmupSessions
        else -> sessions
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
            Text(
                text = "Historia Treningów",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Przeglądaj ukończone sesje treningowe i zapisane serie",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilterIndex == 0,
                onClick = { selectedFilterIndex = 0 },
                label = { Text("Wszystkie (${sessions.size})") }
            )
            FilterChip(
                selected = selectedFilterIndex == 1,
                onClick = { selectedFilterIndex = 1 },
                label = { Text("Treningi główne (${mainSessions.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AthleticOrange.copy(alpha = 0.2f),
                    selectedLabelColor = AthleticOrange
                )
            )
            FilterChip(
                selected = selectedFilterIndex == 2,
                onClick = { selectedFilterIndex = 2 },
                label = { Text("Rozgrzewka / Mobility (${warmupSessions.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                    selectedLabelColor = ElectricCyan
                )
            )
        }

        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedFilterIndex == 1) "Brak ukończonych treningów głównych"
                               else if (selectedFilterIndex == 2) "Brak ukończonych rozgrzewek / mobilizacji"
                               else "Brak ukończonych sesji",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rozpocznij Trening A, B lub C z ekranu głównego!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    val sessionSets = allCompletedSets.filter { it.sessionId == session.id }
                    HistorySessionCard(
                        session = session,
                        sessionSets = sessionSets,
                        exerciseMap = exerciseMap
                    )
                }
            }
        }
    }
}

@Composable
fun HistorySessionCard(
    session: WorkoutSession,
    sessionSets: List<WorkoutSetLog>,
    exerciseMap: Map<Long, Exercise>
) {
    var isExpanded by remember { mutableStateOf(false) }

    val dateStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("pl", "PL"))
        .format(Date(session.startTime))
    val durationMin = session.durationSeconds / 60
    val totalVolume = sessionSets.sumOf { (it.weightKg * it.reps).toDouble() }.toLong()
    val setsCount = sessionSets.size

    val groupedByExercise = remember(sessionSets) {
        sessionSets.groupBy { it.exerciseId }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(AthleticOrange.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = AthleticOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = session.workoutName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (session.isMainWorkout) AthleticOrange.copy(alpha = 0.15f) else ElectricCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (session.isMainWorkout) "Trening" else "Rozgrzewka / Mobility",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (session.isMainWorkout) AthleticOrange else ElectricCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${durationMin} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (session.isMainWorkout) {
                    Text(
                        text = "$setsCount serii",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Objętość: ${totalVolume}kg",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                } else {
                    Text(
                        text = "${groupedByExercise.size} ćwiczeń mobilizacyjnych",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (session.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded detail: exercises & sets
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    groupedByExercise.forEach { (exerciseId, sets) ->
                        val ex = exerciseMap[exerciseId]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = ex?.let { "${it.code} ${it.name}" } ?: "Ćwiczenie #$exerciseId",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            // Sets chips row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sets.forEach { s ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        val setText = when {
                                            ex?.isDistanceBased() == true -> {
                                                val dist = s.distanceMeters?.toInt() ?: 0
                                                "s${s.setNumber}: ${dist}m"
                                            }
                                            ex?.isTimeBased() == true -> {
                                                val sec = s.timeSeconds ?: 0
                                                val timeFormatted = if (sec >= 60 && sec % 60 == 0) "${sec / 60}m" else "${sec}s"
                                                "s${s.setNumber}: $timeFormatted"
                                            }
                                            ex?.isBodyweightBased() == true -> {
                                                if (s.weightKg > 0f) {
                                                    val wStr = if (s.weightKg % 1f == 0f) s.weightKg.toInt().toString() else s.weightKg.toString()
                                                    "s${s.setNumber}: +${wStr}kg × ${s.reps}"
                                                } else {
                                                    "s${s.setNumber}: ${s.reps} powt."
                                                }
                                            }
                                            ex != null -> {
                                                val wStr = if (s.weightKg % 1f == 0f) s.weightKg.toInt().toString() else s.weightKg.toString()
                                                "s${s.setNumber}: ${wStr}kg × ${s.reps}"
                                            }
                                            s.distanceMeters != null -> {
                                                val dist = s.distanceMeters.toInt()
                                                "s${s.setNumber}: ${dist}m"
                                            }
                                            s.timeSeconds != null -> {
                                                val sec = s.timeSeconds
                                                val timeFormatted = if (sec >= 60 && sec % 60 == 0) "${sec / 60}m" else "${sec}s"
                                                "s${s.setNumber}: $timeFormatted"
                                            }
                                            else -> {
                                                val wStr = if (s.weightKg % 1f == 0f) s.weightKg.toInt().toString() else s.weightKg.toString()
                                                "s${s.setNumber}: ${wStr}kg × ${s.reps}"
                                            }
                                        }
                                        Text(
                                            text = setText,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
