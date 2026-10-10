@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.util.WorkoutMetricsCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutHistoryScreen(
    sessions: List<WorkoutSession>,
    allCompletedSets: List<WorkoutSetLog>,
    allExercises: List<Exercise>,
    onDeleteSession: ((WorkoutSession) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val exerciseMap = remember(allExercises) { allExercises.associateBy { it.id } }

    var selectedFilterIndex by remember { mutableStateOf(1) } // 1: Treningi główne (domyślny), 0: Wszystkie, 2: Rozgrzewka & Mobilizacja
    var searchQuery by remember { mutableStateOf("") }
    var sessionToDelete by remember { mutableStateOf<WorkoutSession?>(null) }

    val mainSessions = remember(sessions) { sessions.filter { it.isMainWorkout } }
    val warmupSessions = remember(sessions) { sessions.filter { !it.isMainWorkout } }

    val baseSessions = when (selectedFilterIndex) {
        1 -> mainSessions
        2 -> warmupSessions
        else -> sessions
    }

    val filteredSessions = remember(baseSessions, searchQuery, allCompletedSets, exerciseMap) {
        if (searchQuery.isBlank()) {
            baseSessions
        } else {
            val q = searchQuery.trim().lowercase()
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("pl", "PL"))
            baseSessions.filter { session ->
                session.workoutName.lowercase().contains(q) ||
                session.notes.lowercase().contains(q) ||
                dateFormat.format(Date(session.startTime)).lowercase().contains(q) ||
                allCompletedSets.any { setLog ->
                    setLog.sessionId == session.id && (exerciseMap[setLog.exerciseId]?.name?.lowercase()?.contains(q) == true)
                }
            }
        }
    }

    // Obliczenia statystyk dla widoku podsumowania
    val totalWorkoutsCount = filteredSessions.size
    val totalDurationMinutes = remember(filteredSessions) {
        filteredSessions.sumOf { it.durationSeconds } / 60
    }
    val totalVolumeKg = remember(filteredSessions, allCompletedSets, exerciseMap) {
        val filteredSessionIds = filteredSessions.map { it.id }.toSet()
        allCompletedSets
            .filter { filteredSessionIds.contains(it.sessionId) }
            .filter { setLog ->
                val ex = exerciseMap[setLog.exerciseId]
                ex == null || !ex.isWarmupOrMobility
            }
            .sumOf { it.weightKg.toDouble() * it.reps }
    }
    val totalCaloriesBurned = remember(filteredSessions, allCompletedSets, exerciseMap) {
        filteredSessions.sumOf { session ->
            val sessionSets = allCompletedSets.filter { it.sessionId == session.id }
            val mainSets = sessionSets.filter { setLog ->
                val ex = exerciseMap[setLog.exerciseId]
                ex == null || !ex.isWarmupOrMobility
            }
            val vol = mainSets.sumOf { it.weightKg.toDouble() * it.reps }.toFloat()
            WorkoutMetricsCalculator.calculateCalories(
                durationSeconds = session.durationSeconds,
                totalTonnageKg = vol,
                completedSetsCount = if (session.isMainWorkout) mainSets.size else sessionSets.size
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Nagłówek ekranu
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

        // Karta sumaryczna osiągnięć (jeśli są ukończone treningi)
        if (sessions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HistorySummaryBadge(
                        value = "$totalWorkoutsCount",
                        label = "Sesje",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HistorySummaryBadge(
                        value = if (totalDurationMinutes >= 60) "${totalDurationMinutes / 60}h ${totalDurationMinutes % 60}m" else "${totalDurationMinutes}m",
                        label = "Łączny czas",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HistorySummaryBadge(
                        value = if (totalVolumeKg >= 1000.0) String.format(Locale.US, "%.1ft", totalVolumeKg / 1000.0) else if (totalVolumeKg % 1.0 == 0.0) "${totalVolumeKg.toLong()}kg" else String.format(Locale.US, "%.1fkg", totalVolumeKg),
                        label = "Tonaż",
                        color = ElectricCyan
                    )
                    HistorySummaryBadge(
                        value = "$totalCaloriesBurned",
                        label = "Kcal",
                        color = AthleticOrange
                    )
                }
            }
        }

        // Pole wyszukiwania z zaokrąglonymi rogami
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Szukaj treningu, ćwiczenia, daty...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Szukaj",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Wyczyść wyszukiwanie",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        // Filtry sesji
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
                        text = if (searchQuery.isNotBlank()) "Brak wyników wyszukiwania"
                               else if (selectedFilterIndex == 1) "Brak ukończonych treningów głównych"
                               else if (selectedFilterIndex == 2) "Brak ukończonych rozgrzewek / mobilizacji"
                               else "Brak ukończonych sesji",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Spróbuj innego hasła wyszukiwania"
                               else "Rozpocznij Trening A, B lub C z ekranu głównego!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    val sessionSets = allCompletedSets.filter { it.sessionId == session.id }
                    HistorySessionCard(
                        session = session,
                        sessionSets = sessionSets,
                        exerciseMap = exerciseMap,
                        onDelete = { sessionToDelete = session }
                    )
                }
            }
        }
    }

    // Dialog potwierdzenia usunięcia sesji treningowej
    sessionToDelete?.let { session ->
        val dateText = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("pl", "PL"))
            .format(Date(session.startTime))
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = {
                Text(
                    text = "Usunąć trening z historii?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Czy na pewno chcesz bezpowrotnie usunąć sesję „${session.workoutName}” z dnia $dateText? Zapisane serie zostaną również usunięte z bazy danych.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSession?.invoke(session)
                        sessionToDelete = null
                    }
                ) {
                    Text(
                        text = "Usuń",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
private fun HistorySummaryBadge(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
fun HistorySessionCard(
    session: WorkoutSession,
    sessionSets: List<WorkoutSetLog>,
    exerciseMap: Map<Long, Exercise>,
    onDelete: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    val dateStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("pl", "PL"))
        .format(Date(session.startTime))
    val durationMin = session.durationSeconds / 60

    val isWarmupOrMobilitySession = !session.isMainWorkout
    val mainSets = remember(sessionSets, exerciseMap) {
        sessionSets.filter { setLog ->
            val ex = exerciseMap[setLog.exerciseId]
            ex == null || !ex.isWarmupOrMobility
        }
    }
    val effectiveSets = if (isWarmupOrMobilitySession) sessionSets else mainSets
    val totalVolume = mainSets.sumOf { it.weightKg.toDouble() * it.reps }
    val totalVolumeStr = if (totalVolume % 1.0 == 0.0) totalVolume.toLong().toString() else String.format(Locale.US, "%.1f", totalVolume)
    val setsCount = effectiveSets.size

    val calories = remember(session.durationSeconds, totalVolume, setsCount) {
        WorkoutMetricsCalculator.calculateCalories(
            durationSeconds = session.durationSeconds,
            totalTonnageKg = totalVolume.toFloat(),
            completedSetsCount = setsCount
        )
    }

    val (intensityLabel, intensityColor) = remember(session.durationSeconds, totalVolume, setsCount) {
        WorkoutMetricsCalculator.calculateIntensity(
            durationSeconds = session.durationSeconds,
            totalTonnageKg = totalVolume.toFloat(),
            completedSetsCount = setsCount
        )
    }

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
        Column(modifier = Modifier.padding(14.dp)) {
            // Główny wiersz nagłówka: Ikona + Tytuł/Data + Przyciski akcji
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ikona sesji (okrągła)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (session.isMainWorkout) AthleticOrange.copy(alpha = 0.15f) else ElectricCyan.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (session.isMainWorkout) Icons.Default.FitnessCenter else Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (session.isMainWorkout) AthleticOrange else ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Kolumna z nazwą treningu, etykietą i datą
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.workoutName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (session.isMainWorkout) AthleticOrange.copy(alpha = 0.15f) else ElectricCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (session.isMainWorkout) "Trening główny" else "Rozgrzewka / Mobility",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (session.isMainWorkout) AthleticOrange else ElectricCyan,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Przyciski akcji z prawej strony
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Usuń trening z historii",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dedykowany, uporządkowany kontener metryk sesji (rozwiązuje problem rozjeżdżania się)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    // Wiersz równomiernie rozmieszczonych metryk
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Czas trwania
                        HistoryMetricItem(
                            icon = Icons.Default.Timer,
                            value = "${durationMin} min",
                            label = "Czas",
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )

                        // 2. Liczba serii
                        HistoryMetricItem(
                            icon = Icons.Default.FitnessCenter,
                            value = "$setsCount",
                            label = "Serie",
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )

                        if (session.isMainWorkout) {
                            // 3. Objętość / Tonaż
                            HistoryMetricItem(
                                icon = null,
                                value = "${totalVolumeStr} kg",
                                label = "Tonaż",
                                valueColor = ElectricCyan
                            )

                            // 4. Szacowane kalorie
                            HistoryMetricItem(
                                icon = Icons.Default.Whatshot,
                                value = "~$calories kcal",
                                label = "Kalorie",
                                valueColor = AthleticOrange
                            )
                        } else {
                            // Dla sesji rozgrzewkowej: liczba ćwiczeń
                            HistoryMetricItem(
                                icon = Icons.Default.Speed,
                                value = "${groupedByExercise.size}",
                                label = "Ćwiczenia",
                                valueColor = ElectricCyan
                            )
                        }
                    }

                    // Wskaźnik intensywności treningu (dla sesji głównych)
                    if (session.isMainWorkout) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = intensityColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Szacowana intensywność sesji:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = intensityColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = intensityLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = intensityColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Notatki do sesji (jeśli zostały zapisane)
            if (session.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = session.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Rozwinięte szczegóły: lista wykonanych ćwiczeń i serii
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Wykonane ćwiczenia i serie:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    groupedByExercise.forEach { (exerciseId, sets) ->
                        val ex = exerciseMap[exerciseId]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ex?.let { "${it.code} ${it.name}" } ?: "Ćwiczenie #$exerciseId",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    ex?.let {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        ) {
                                            Text(
                                                text = it.bodyPart,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Wiersz serii z zawijaniem FlowRow
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    sets.forEach { s ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            val setText = when {
                                                ex?.isWeightAndDistanceBased() == true || (s.distanceMeters != null && s.weightKg > 0f) -> {
                                                    val wStr = if (s.weightKg % 1f == 0f) s.weightKg.toInt().toString() else s.weightKg.toString()
                                                    val dist = s.distanceMeters?.toInt() ?: 0
                                                    "s${s.setNumber}: ${wStr}kg × ${dist}m"
                                                }
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
}

@Composable
private fun HistoryMetricItem(
    icon: ImageVector?,
    value: String,
    label: String,
    valueColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}
