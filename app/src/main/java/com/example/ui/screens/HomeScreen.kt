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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.model.BodyMeasurement
import com.example.data.model.WorkoutPlan
import com.example.ui.components.WorkoutPlanManagerDialog
import com.example.ui.components.WorkoutReportDialog

@Composable
fun HomeScreen(
    activeSession: WorkoutSession?,
    completedSessions: List<WorkoutSession>,
    allExercises: List<Exercise>,
    allCompletedSets: List<WorkoutSetLog>,
    allPlans: List<WorkoutPlan> = emptyList(),
    activePlan: WorkoutPlan? = null,
    allMeasurements: List<BodyMeasurement> = emptyList(),
    onStartWorkout: (workoutName: String, exercises: List<Exercise>) -> Unit,
    onResumeWorkout: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToCharts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToMeasurements: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onCreatePlan: (String, String, Boolean, List<Pair<String, String>>) -> Unit = { _, _, _, _ -> },
    onSetActivePlan: (Long) -> Unit = {},
    onDeletePlan: (WorkoutPlan) -> Unit = {},
    onCreateCustomExercise: (Exercise) -> Unit = {},
    onDeleteExercise: (Exercise) -> Unit = {},
    onDeleteWorkoutCategory: (WorkoutPlan, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var showPlanManagerDialog by remember { mutableStateOf(false) }
    val mainSessionIds = remember(completedSessions) {
        completedSessions.filter { it.isMainWorkout }.map { it.id }.toSet()
    }
    val warmupOrMobilityExerciseIds = remember(allExercises) {
        allExercises.filter { it.isWarmupOrMobility }.map { it.id }.toSet()
    }

    // Exclude warmup and mobility sets and sessions from total series count and total volume calculation
    val mainWorkoutCompletedSets = remember(allCompletedSets, mainSessionIds, warmupOrMobilityExerciseIds) {
        allCompletedSets.filter { 
            it.sessionId in mainSessionIds && it.exerciseId !in warmupOrMobilityExerciseIds 
        }
    }

    val totalVolumeKg = remember(mainWorkoutCompletedSets) {
        mainWorkoutCompletedSets.sumOf { (it.weightKg * it.reps).toDouble() }.toLong()
    }
    val mainWorkouts = remember(completedSessions) { completedSessions.filter { it.isMainWorkout } }
    val totalWorkoutsCount = mainWorkouts.size
    val totalSetsCount = mainWorkoutCompletedSets.size

    val currentPlanId = activePlan?.id ?: 1L
    val activePlanWarmups = remember(allExercises, currentPlanId) {
        if (currentPlanId == 1L) {
            allExercises.filter { (it.section == "Rozgrzewka" || it.section == "Mobilizacja") && (it.planId == 1L || it.planId == 0L) }
        } else {
            allExercises.filter { (it.section == "Rozgrzewka" || it.section == "Mobilizacja") && it.planId == currentPlanId }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Active workout alert banner
        if (activeSession != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResumeWorkout() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AthleticOrange.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AthleticOrange)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(AthleticOrange, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "TRENING W TOKU",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AthleticOrange
                                )
                                Text(
                                    text = activeSession.workoutName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = onResumeWorkout,
                            colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Wróć", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Hero Greeting & Quick Stats Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A)
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Dziennik Treningowy",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Plan treningowy i postępy",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onNavigateToGuide,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF334155), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Zasady & Wskazówki",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = onNavigateToSettings,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF334155), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Ustawienia",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Stats Grid (Workouts, Sets, Total Volume)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMiniBox(
                                label = "Treningi",
                                value = "$totalWorkoutsCount",
                                accent = AthleticOrange,
                                icon = Icons.Default.Whatshot
                            )
                            StatMiniBox(
                                label = "Serie",
                                value = "$totalSetsCount",
                                accent = ElectricCyan,
                                icon = Icons.Default.FitnessCenter
                            )
                            StatMiniBox(
                                label = "Objętość",
                                value = if (totalVolumeKg >= 1000) "${totalVolumeKg / 1000}t" else "${totalVolumeKg}kg",
                                accent = SuccessGreen,
                                icon = Icons.Default.ShowChart
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Shortcuts
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickNavCard(
                        title = "Pomiary Ciała",
                        subtitle = "Waga, obwody, %BF",
                        icon = Icons.Default.MonitorWeight,
                        accentColor = GoldPr,
                        onClick = onNavigateToMeasurements,
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavCard(
                        title = "Wykresy",
                        subtitle = "Postępy siłowe",
                        icon = Icons.Default.ShowChart,
                        accentColor = AthleticOrange,
                        onClick = onNavigateToCharts,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickNavCard(
                        title = "Baza Ćwiczeń",
                        subtitle = "Plan & Multimedia",
                        icon = Icons.Default.FitnessCenter,
                        accentColor = ElectricCyan,
                        onClick = onNavigateToExercises,
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavCard(
                        title = "Historia",
                        subtitle = "Ukończone sesje",
                        icon = Icons.Default.History,
                        accentColor = SuccessGreen,
                        onClick = onNavigateToHistory,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickNavCard(
                        title = "Generuj Raport",
                        subtitle = "Okres, kg, serie, foto",
                        icon = Icons.Default.Assessment,
                        accentColor = Color(0xFFA855F7),
                        onClick = { showReportDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavCard(
                        title = "Plany Treningowe",
                        subtitle = activePlan?.name ?: "Zarządzaj planami",
                        icon = Icons.Default.Folder,
                        accentColor = AthleticOrange,
                        onClick = { showPlanManagerDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Plan Treningowy Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Twój Plan Treningowy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aktywny: ${activePlan?.name ?: "Plan Główny (A/B/C)"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AthleticOrange,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { showPlanManagerDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Zmień plan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // 1. Rozgrzewka i Mobilizacja (pokazuj TYLKO jeśli ten plan zawiera ćwiczenia rozgrzewkowe!)
        if (activePlanWarmups.isNotEmpty()) {
            item {
                WorkoutPlanCard(
                    title = "Rozgrzewka i Mobilizacja",
                    subtitle = "Row erg, Air bike + ćwiczenia mobilizacyjne i aktywacja stawowa (zalecana przed treningiem)",
                    exerciseCount = activePlanWarmups.size,
                    setsTotal = activePlanWarmups.sumOf { it.targetSets }.coerceAtLeast(activePlanWarmups.size),
                    estimatedMin = "15 min",
                    badgeColor = SuccessGreen,
                    onStart = {
                        onStartWorkout("Rozgrzewka & Mobilizacja", activePlanWarmups)
                    }
                )
            }
        }

        // 2. Workouts based on the active plan
        val getWorkoutEstimatedTime: (String, String) -> String = { name, defaultEst ->
            val completed = completedSessions.filter { it.workoutName == name && it.durationSeconds > 600 }
            if (completed.isNotEmpty()) {
                val avg = completed.map { it.durationSeconds / 60 }.average().toInt()
                "$defaultEst (Twój śr. czas: $avg min)"
            } else {
                defaultEst
            }
        }

        if (currentPlanId == 1L) {
            // Default 3-day FBW Plan (A, B, C)
            item {
                WorkoutPlanCard(
                    title = "Trening A",
                    subtitle = "Powerband Chin-ups, Dips, Wykroki, OHP, Ramiona, Brzuch, Plank boczny (czas)",
                    exerciseCount = 8,
                    setsTotal = 30,
                    estimatedMin = getWorkoutEstimatedTime("Trening A", "95 - 110 min"),
                    badgeColor = AthleticOrange,
                    onStart = {
                        val exercises = allExercises.filter { it.section == "Trening A" }
                        onStartWorkout("Trening A", exercises)
                    }
                )
            }

            item {
                WorkoutPlanCard(
                    title = "Trening B",
                    subtitle = "Semi Sumo Martwy Ciąg, Skos Hammer, Wiosło, Bok barku, Ramiona, Brzuch, Schody (czas)",
                    exerciseCount = 9,
                    setsTotal = 31,
                    estimatedMin = getWorkoutEstimatedTime("Trening B", "110 - 125 min"),
                    badgeColor = ElectricCyan,
                    onStart = {
                        val exercises = allExercises.filter { it.section == "Trening B" }
                        onStartWorkout("Trening B", exercises)
                    }
                )
            }

            item {
                WorkoutPlanCard(
                    title = "Trening C",
                    subtitle = "Spacer farmera (ciężar + dystans), Pallof Press, Wyciskanie hantli, Chin-up hold (czas), Tył barku, Piłka, Kółko",
                    exerciseCount = 9,
                    setsTotal = 33,
                    estimatedMin = getWorkoutEstimatedTime("Trening C", "100 - 115 min"),
                    badgeColor = GoldPr,
                    onStart = {
                        val exercises = allExercises.filter { it.section == "Trening C" }
                        onStartWorkout("Trening C", exercises)
                    }
                )
            }
        } else {
            // Custom Plan Workouts
            val customPlanExercises = allExercises.filter { it.planId == currentPlanId }
            val rawWorkouts = activePlan?.workoutsRaw?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val customWorkouts = if (rawWorkouts.isNotEmpty()) rawWorkouts else customPlanExercises.map { it.section }.distinct().filter { it != "Rozgrzewka" && it != "Mobilizacja" }

            if (customWorkouts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Brak treningów w tym planie",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kliknij poniżej, aby dodać swój pierwszy trening lub ćwiczenia",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showPlanManagerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Dodaj trening do planu")
                            }
                        }
                    }
                }
            } else {
                items(customWorkouts) { workoutName ->
                    val workoutExs = customPlanExercises.filter { it.section == workoutName }
                    val totalSets = workoutExs.sumOf { it.targetSets }
                    val estMin = if (workoutExs.isEmpty()) {
                        "0 min"
                    } else {
                        val cardioMin = workoutExs.count { it.isTimeBased() || it.bodyPart.contains("cardio", ignoreCase = true) } * 15
                        "${(totalSets * 2.8 + cardioMin + 15).toInt()} min"
                    }
                    WorkoutPlanCard(
                        title = workoutName,
                        subtitle = if (workoutExs.isNotEmpty()) workoutExs.joinToString(", ") { it.name } else "Brak ćwiczeń w tym treningu (kliknij, aby rozpocząć lub dodać ćwiczenia)",
                        exerciseCount = workoutExs.size,
                        setsTotal = totalSets,
                        estimatedMin = getWorkoutEstimatedTime(workoutName, estMin),
                        badgeColor = AthleticOrange,
                        onStart = {
                            onStartWorkout(workoutName, workoutExs)
                        }
                    )
                }
            }
        }

        // Recent Workouts summary
        if (completedSessions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ostatnie Treningi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zobacz wszystkie",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }
            }

            items(completedSessions.take(3)) { session ->
                val dateStr = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(Date(session.startTime))
                val durationMin = session.durationSeconds / 60
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHistory() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = session.workoutName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$dateStr • ${durationMin} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        WorkoutReportDialog(
            sessions = completedSessions,
            allSets = allCompletedSets,
            allExercises = allExercises,
            allMeasurements = allMeasurements,
            onDismiss = { showReportDialog = false }
        )
    }

    if (showPlanManagerDialog) {
        WorkoutPlanManagerDialog(
            plans = allPlans,
            activePlan = activePlan,
            allExercises = allExercises,
            onSelectActivePlan = { planId ->
                onSetActivePlan(planId)
            },
            onCreatePlan = { name, desc, includeWarmup, workouts ->
                onCreatePlan(name, desc, includeWarmup, workouts)
            },
            onDeletePlan = { plan ->
                onDeletePlan(plan)
            },
            onCreateExercise = { ex ->
                onCreateCustomExercise(ex)
            },
            onDeleteExercise = onDeleteExercise,
            onDeleteWorkoutCategory = onDeleteWorkoutCategory,
            onDismiss = { showPlanManagerDialog = false }
        )
    }
}

@Composable
fun StatMiniBox(
    label: String,
    value: String,
    accent: Color,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .background(Color(0xFF1E293B).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp
        )
    }
}

@Composable
fun QuickNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun WorkoutPlanCard(
    title: String,
    subtitle: String,
    exerciseCount: Int,
    setsTotal: Int,
    estimatedMin: String,
    badgeColor: Color,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp)
                            .background(badgeColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = estimatedMin,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$exerciseCount ćwiczeń • $setsTotal serii",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rozpocznij",
                        fontWeight = FontWeight.Bold,
                        color = if (badgeColor == GoldPr) Color.Black else Color.White
                    )
                }
            }
        }
    }
}
