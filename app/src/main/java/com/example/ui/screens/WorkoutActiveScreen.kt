@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Exercise
import com.example.data.model.ExerciseType
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import com.example.ui.components.RestTimerPill
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SuccessGreen
import com.example.util.SupersetHelper
import com.example.util.SupersetInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WorkoutActiveScreen(
    session: WorkoutSession,
    sets: List<WorkoutSetLog>,
    allExercises: List<Exercise>,
    allCompletedSets: List<WorkoutSetLog> = emptyList(),
    restTimerSeconds: Int,
    isRestTimerActive: Boolean,
    restTimerRemainingSeconds: Int = restTimerSeconds,
    isRestTimerPaused: Boolean = false,
    restTimerLabel: String = "Czas na przerwę",
    onToggleSetCompleted: (setLog: WorkoutSetLog, restSeconds: Int, nextSupersetExerciseName: String?, isFirstInSuperset: Boolean) -> Unit,
    onUpdateSet: (WorkoutSetLog) -> Unit,
    onAddSet: (exerciseId: Long, nextSetNumber: Int, defaultReps: Int, defaultWeight: Float, defaultTimeSeconds: Int?, defaultDistanceMeters: Float?) -> Unit,
    onRemoveSet: (WorkoutSetLog) -> Unit,
    onFinishWorkout: (notes: String) -> Unit,
    onDiscardWorkout: () -> Unit,
    onDismissTimer: () -> Unit,
    onStartTimer: (seconds: Int, label: String) -> Unit = { _, _ -> },
    onPauseResumeTimer: () -> Unit = {},
    onAddTimerSeconds: (Int) -> Unit = {},
    onReduceTimerSeconds: (Int) -> Unit = {},
    onExerciseDetailsClick: (Long) -> Unit,
    onMinimize: () -> Unit = {},
    onNavigateToExercises: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToMeasurements: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    lastViewedExerciseId: Long? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var workoutNotes by remember { mutableStateOf(session.notes) }
    var elapsedSeconds by remember {
        mutableLongStateOf((System.currentTimeMillis() - session.startTime) / 1000)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSeconds = (System.currentTimeMillis() - session.startTime) / 1000
        }
    }

    // Intercept back button to minimize workout rather than exit or discard
    BackHandler {
        onMinimize()
    }

    val timerString = remember(elapsedSeconds) {
        if (elapsedSeconds >= 3600) {
            val hours = elapsedSeconds / 3600
            val mins = (elapsedSeconds % 3600) / 60
            val secs = elapsedSeconds % 60
            String.format("%02d:%02d:%02d", hours, mins, secs)
        } else {
            val mins = elapsedSeconds / 60
            val secs = elapsedSeconds % 60
            String.format("%02d:%02d", mins, secs)
        }
    }

    val exerciseMap = remember(allExercises) {
        allExercises.associateBy { it.id }
    }

    // Group sets by exercise in the order they were inserted
    val groupedSets = remember(sets) {
        val map = linkedMapOf<Long, MutableList<WorkoutSetLog>>()
        for (setLog in sets) {
            map.getOrPut(setLog.exerciseId) { mutableListOf() }.add(setLog)
        }
        map
    }

    val activeExercises = remember(groupedSets.keys, exerciseMap) {
        groupedSets.keys.mapNotNull { exerciseMap[it] }
    }

    val supersetsMap = remember(activeExercises) {
        SupersetHelper.detectSupersets(activeExercises)
    }

    LaunchedEffect(lastViewedExerciseId, groupedSets.keys) {
        if (lastViewedExerciseId != null && groupedSets.isNotEmpty()) {
            val exerciseKeys = groupedSets.keys.toList()
            val index = exerciseKeys.indexOf(lastViewedExerciseId)
            if (index >= 0) {
                // In the LazyColumn: item 0 is summary, item 1 is chips, exercises start at index 2
                listState.scrollToItem(index + 2)
            }
        }
    }

    val isWarmupOrMobilitySession = remember(session.workoutName) {
        session.workoutName.contains("Rozgrzewka", ignoreCase = true) ||
        session.workoutName.contains("Mobilizacja", ignoreCase = true)
    }

    // Warmup and mobility sets are excluded from strength series and volume calculations
    val statsSets = remember(sets, exerciseMap, isWarmupOrMobilitySession) {
        if (isWarmupOrMobilitySession) {
            sets
        } else {
            sets.filter { setLog ->
                val ex = exerciseMap[setLog.exerciseId]
                ex == null || !ex.isWarmupOrMobility
            }
        }
    }

    val completedCount = remember(statsSets) { statsSets.count { it.isCompleted } }
    val totalCount = statsSets.size
    val totalVolume = remember(statsSets) {
        statsSets.filter { it.isCompleted }.sumOf { (it.weightKg * it.reps).toDouble() }.toLong()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier.testTag("btn_minimize_workout")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zminimalizuj trening do paska"
                        )
                    }
                },
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = session.workoutName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Czas trwania: $timerString",
                            style = MaterialTheme.typography.labelSmall,
                            color = AthleticOrange,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showDiscardDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Anuluj", fontSize = 13.sp)
                    }
                    Button(
                        onClick = { showFinishDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zakończ", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Floating Rest / Exercise Timer
            RestTimerPill(
                totalSeconds = restTimerSeconds,
                remainingSeconds = restTimerRemainingSeconds,
                isVisible = isRestTimerActive,
                isPaused = isRestTimerPaused,
                label = restTimerLabel,
                onPauseResume = onPauseResumeTimer,
                onAddSeconds = onAddTimerSeconds,
                onReduceSeconds = onReduceTimerSeconds,
                onFinishedOrDismissed = onDismissTimer
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary progress strip
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Serie: $completedCount / $totalCount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (totalVolume > 0) {
                            Text(
                                text = "Objętość siłowa: ${totalVolume}kg",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Ćwiczenia: ${groupedSets.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Quick navigation chips to browse app without interrupting workout & quick timers
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AssistChip(
                            onClick = onMinimize,
                            label = { Text("Pulpit", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Home, null, Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onStartTimer(60, "Czas na przerwę (60s)") },
                            label = { Text("Przerwa 60s", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.Timer, null, Modifier.size(14.dp), tint = AthleticOrange) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = AthleticOrange.copy(alpha = 0.15f)
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onStartTimer(90, "Czas na przerwę (90s)") },
                            label = { Text("Przerwa 90s", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.Timer, null, Modifier.size(14.dp), tint = AthleticOrange) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = AthleticOrange.copy(alpha = 0.15f)
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onStartTimer(120, "Czas na przerwę (120s)") },
                            label = { Text("Przerwa 120s", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.Timer, null, Modifier.size(14.dp), tint = AthleticOrange) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = AthleticOrange.copy(alpha = 0.15f)
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onNavigateToExercises,
                            label = { Text("Ćwiczenia", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.FitnessCenter, null, Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onNavigateToHistory,
                            label = { Text("Historia", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.History, null, Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onNavigateToMeasurements,
                            label = { Text("Pomiary", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Straighten, null, Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }

            // Exercise Cards
            groupedSets.forEach { (exerciseId, exerciseSets) ->
                val exercise = exerciseMap[exerciseId]
                if (exercise != null) {
                    item(key = exerciseId) {
                        val prevExerciseSets = remember(allCompletedSets, exerciseId, session.id) {
                            allCompletedSets.filter { it.exerciseId == exerciseId && it.sessionId != session.id && it.isCompleted }
                        }
                        val prevSessionId = prevExerciseSets.maxByOrNull { it.timestamp }?.sessionId
                        val lastCompletedSets = remember(prevExerciseSets, prevSessionId) {
                            if (prevSessionId != null) {
                                prevExerciseSets.filter { it.sessionId == prevSessionId }.sortedBy { it.setNumber }
                            } else emptyList()
                        }
                        val previousSummary = remember(lastCompletedSets, exercise) {
                            if (lastCompletedSets.isEmpty()) null
                            else {
                                lastCompletedSets.joinToString(", ") { s ->
                                    when (exercise.getEffectiveMeasurementType()) {
                                        ExerciseType.BODYWEIGHT_REPS -> "${s.reps} powt."
                                        ExerciseType.DISTANCE -> "${s.distanceMeters?.toInt() ?: 0}m"
                                        ExerciseType.WEIGHT_AND_DISTANCE -> {
                                            val wStr = if (s.weightKg % 1f == 0f) s.weightKg.toInt().toString() else s.weightKg.toString()
                                            val dStr = if ((s.distanceMeters ?: 0f) % 1f == 0f) (s.distanceMeters ?: 0f).toInt().toString() else (s.distanceMeters ?: 0f).toString()
                                            "${wStr}kg × ${dStr}m"
                                        }
                                        ExerciseType.TIME -> {
                                            val sec = s.timeSeconds ?: 0
                                            if (sec >= 60 && sec % 60 == 0) "${sec / 60}m" else "${sec}s"
                                        }
                                        ExerciseType.WEIGHT_AND_REPS -> {
                                            val wStr = if (s.weightKg % 1f == 0f) s.weightKg.toInt().toString() else s.weightKg.toString()
                                            "${wStr}kg × ${s.reps}"
                                        }
                                    }
                                }
                            }
                        }

                        val supersetInfo = supersetsMap[exercise.id]
                        val isFirstInSuperset = supersetInfo != null && !supersetInfo.isLastInGroup
                        val nextSupersetName = if (isFirstInSuperset) {
                            val partner = supersetInfo?.partnerExercises?.firstOrNull()
                            partner?.name ?: supersetInfo?.groupKey
                        } else if (supersetInfo != null) {
                            supersetInfo.groupKey
                        } else {
                            null
                        }

                        ExerciseWorkoutCard(
                            exercise = exercise,
                            sets = exerciseSets,
                            previousSummary = previousSummary,
                            supersetInfo = supersetInfo,
                            onNavigateToPartner = { partnerId ->
                                val exKeys = groupedSets.keys.toList()
                                val pIndex = exKeys.indexOf(partnerId)
                                if (pIndex >= 0) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(pIndex + 2)
                                    }
                                }
                            },
                            onToggleCompleted = { setLog ->
                                onToggleSetCompleted(
                                    setLog,
                                    exercise.restSeconds,
                                    nextSupersetName,
                                    isFirstInSuperset
                                )
                            },
                            onUpdateSet = onUpdateSet,
                            onAddSet = {
                                val nextSetNum = (exerciseSets.maxOfOrNull { it.setNumber } ?: 0) + 1
                                val lastSet = exerciseSets.lastOrNull()
                                val effType = exercise.getEffectiveMeasurementType()
                                val isDistOnly = effType == ExerciseType.DISTANCE
                                val isWeightDist = effType == ExerciseType.WEIGHT_AND_DISTANCE
                                val isTime = effType == ExerciseType.TIME
                                val isBw = effType == ExerciseType.BODYWEIGHT_REPS

                                val defReps = if (!isDistOnly && !isWeightDist && !isTime) (lastSet?.reps ?: 10) else 0
                                val defWeight = if (isWeightDist || (!isDistOnly && !isTime && !isBw)) (lastSet?.weightKg ?: 0f) else 0f
                                val defTime = if (isTime) (lastSet?.timeSeconds ?: 30) else null
                                val defDist = if (isDistOnly || isWeightDist) (lastSet?.distanceMeters ?: 40f) else null

                                onAddSet(
                                    exercise.id,
                                    nextSetNum,
                                    defReps,
                                    defWeight,
                                    defTime,
                                    defDist
                                )
                            },
                            onRemoveSet = onRemoveSet,
                            onStartTimerForSet = { sec, lbl ->
                                onStartTimer(sec, lbl)
                            },
                            onDetailsClick = { onExerciseDetailsClick(exercise.id) }
                        )
                    }
                }
            }
        }
    }

    // Finish Workout Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = {
                Text(
                    text = "Zakończyć trening?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    val summaryMsg = if (totalVolume > 0) {
                        "Świetna robota! Wykonano $completedCount z $totalCount serii o łącznej objętości $totalVolume kg."
                    } else {
                        "Świetna robota! Ukończono $completedCount z $totalCount serii w tym treningu."
                    }
                    Text(
                        text = summaryMsg,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = workoutNotes,
                        onValueChange = { workoutNotes = it },
                        label = { Text("Notatki z treningu (opcjonalnie)") },
                        placeholder = { Text("np. dobra forma, świetne tempo, mocny core") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        onFinishWorkout(workoutNotes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Zapisz trening", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Kontynuuj trening")
                }
            }
        )
    }

    // Discard Workout Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text("Anulować trening?")
            },
            text = {
                Text("Czy na pewno chcesz przerwać ten trening? Wszystkie dotychczasowe serie z tej sesji zostaną utracone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onDiscardWorkout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Tak, anuluj")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Wróć do treningu")
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ExerciseWorkoutCard(
    exercise: Exercise,
    sets: List<WorkoutSetLog>,
    previousSummary: String? = null,
    supersetInfo: SupersetInfo? = null,
    onNavigateToPartner: ((Long) -> Unit)? = null,
    onToggleCompleted: (WorkoutSetLog) -> Unit,
    onUpdateSet: (WorkoutSetLog) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (WorkoutSetLog) -> Unit,
    onStartTimerForSet: (seconds: Int, label: String) -> Unit,
    onDetailsClick: () -> Unit
) {
    val measurementType = exercise.getEffectiveMeasurementType()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (supersetInfo != null) Color(0xFF1E1B4B).copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = if (supersetInfo != null) BorderStroke(1.5.dp, Color(0xFF8B5CF6).copy(alpha = 0.55f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Superset banner if exercise is part of a superset (np. C.5a, C.5b)
            if (supersetInfo != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SERIA ŁĄCZONA ${supersetInfo.groupKey.uppercase()} (${supersetInfo.letter.uppercase()})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFFE9D5FF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (supersetInfo.partnerExercises.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val partner = supersetInfo.partnerExercises.first()
                            TextButton(
                                onClick = { onNavigateToPartner?.invoke(partner.id) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Do: ${partner.code.ifBlank { partner.name.take(8) }} →",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA78BFA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Exercise Title and Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AthleticOrange.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = exercise.code,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AthleticOrange,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${exercise.equipment} • ${exercise.primaryMuscles}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Previous workout results banner
                    if (!previousSummary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Poprzednio: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = previousSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }
                }

                // Info / Media button
                IconButton(onClick = onDetailsClick) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Szczegóły ćwiczenia i multimedia",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Specs badges (Target reps / distance / time, Rest, RIR, Tempo)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val specLabel = when (measurementType) {
                    ExerciseType.DISTANCE, ExerciseType.WEIGHT_AND_DISTANCE -> "Dystans"
                    ExerciseType.TIME -> "Czas"
                    ExerciseType.BODYWEIGHT_REPS, ExerciseType.WEIGHT_AND_REPS -> "Powt."
                }
                SpecBadge(label = specLabel, value = exercise.targetReps)
                SpecBadge(label = "Przerwa", value = exercise.restDisplay)
                if (exercise.rir != "-") {
                    SpecBadge(label = "RIR", value = exercise.rir)
                }
                if (exercise.tempo != "-") {
                    SpecBadge(label = "Tempo", value = exercise.tempo)
                }
            }

            // Sets Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SERIA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (measurementType) {
                    ExerciseType.DISTANCE -> {
                        Text(
                            text = "DYSTANS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ExerciseType.WEIGHT_AND_DISTANCE -> {
                        Text(
                            text = "CIĘŻAR (KG)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DYSTANS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ExerciseType.TIME -> {
                        Text(
                            text = "CZAS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ExerciseType.BODYWEIGHT_REPS -> {
                        Text(
                            text = "POWTÓRZENIA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ExerciseType.WEIGHT_AND_REPS -> {
                        Text(
                            text = "CIĘŻAR (KG)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "POWT.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Delete button alignment spacer in table header
                Spacer(modifier = Modifier.width(28.dp))

                Text(
                    text = "STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(38.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Set Rows
            sets.forEach { setLog ->
                when (measurementType) {
                    ExerciseType.WEIGHT_AND_DISTANCE -> {
                        SetLogWeightDistanceRow(
                            setLog = setLog,
                            canDelete = sets.size > 1,
                            onToggleCompleted = { onToggleCompleted(setLog) },
                            onUpdateSet = onUpdateSet,
                            onDeleteSet = { onRemoveSet(setLog) }
                        )
                    }
                    ExerciseType.DISTANCE -> {
                        SetLogDistanceRow(
                            setLog = setLog,
                            canDelete = sets.size > 1,
                            onToggleCompleted = { onToggleCompleted(setLog) },
                            onUpdateSet = onUpdateSet,
                            onDeleteSet = { onRemoveSet(setLog) }
                        )
                    }
                    ExerciseType.TIME -> {
                        SetLogTimeRow(
                            setLog = setLog,
                            canDelete = sets.size > 1,
                            onToggleCompleted = { onToggleCompleted(setLog) },
                            onUpdateSet = onUpdateSet,
                            onDeleteSet = { onRemoveSet(setLog) },
                            onStartTimer = { sec ->
                                onStartTimerForSet(sec, "Czas serii: ${exercise.name} (seria ${setLog.setNumber})")
                            }
                        )
                    }
                    ExerciseType.BODYWEIGHT_REPS -> {
                        SetLogRepsOnlyRow(
                            setLog = setLog,
                            canDelete = sets.size > 1,
                            onToggleCompleted = { onToggleCompleted(setLog) },
                            onUpdateSet = onUpdateSet,
                            onDeleteSet = { onRemoveSet(setLog) }
                        )
                    }
                    ExerciseType.WEIGHT_AND_REPS -> {
                        SetLogWeightRepsRow(
                            setLog = setLog,
                            canDelete = sets.size > 1,
                            onToggleCompleted = { onToggleCompleted(setLog) },
                            onUpdateSet = onUpdateSet,
                            onDeleteSet = { onRemoveSet(setLog) }
                        )
                    }
                }
            }

            // Add Set button
            TextButton(
                onClick = onAddSet,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dodaj kolejną serię", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Custom numeric input box for workout set rows.
 * Replaces Material 3 OutlinedTextField (which has huge internal padding and fixed min-height)
 * with a clean, perfectly centered, non-clipped BasicTextField inside a styled container.
 */
@Composable
fun SetNumberInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "0",
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isCompleted) SuccessGreen.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) AthleticOrange
                else if (isCompleted) SuccessGreen.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (value.length >= 5) 12.sp else if (value.length >= 4) 13.sp else 14.sp,
                    color = if (isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(AthleticOrange),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .onFocusChanged { isFocused = it.isFocused }
                    .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (!suffix.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

/**
 * Row for distance-based exercises (e.g. Row erg 500m, Air bike 500m, Farmers walk 40m).
 * ONLY displays distance in meters with +/- stepper, NO weight and NO reps.
 */
@Composable
fun SetLogDistanceRow(
    setLog: WorkoutSetLog,
    canDelete: Boolean,
    onToggleCompleted: () -> Unit,
    onUpdateSet: (WorkoutSetLog) -> Unit,
    onDeleteSet: () -> Unit
) {
    val currentMeters = setLog.distanceMeters ?: 40f
    var distText by remember(setLog.distanceMeters) {
        val formatted = if (currentMeters % 1f == 0f) {
            currentMeters.toInt().toString()
        } else {
            currentMeters.toString()
        }
        mutableStateOf(formatted)
    }

    val isDone = setLog.isCompleted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isDone) SuccessGreen.copy(alpha = 0.12f) else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Text(
            text = "${setLog.setNumber}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            color = if (isDone) SuccessGreen else MaterialTheme.colorScheme.onSurface
        )

        // Distance Stepper & Input
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val step = if (currentMeters >= 100f) 50f else 5f

            IconButton(
                onClick = {
                    val newDist = (currentMeters - step).coerceAtLeast(0f)
                    onUpdateSet(setLog.copy(distanceMeters = newDist, weightKg = 0f, reps = 0))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(4.dp))

            SetNumberInputBox(
                value = distText,
                onValueChange = { newVal ->
                    val filtered = newVal.filter { it.isDigit() || it == '.' }
                    distText = filtered
                    val d = filtered.toFloatOrNull() ?: 0f
                    onUpdateSet(setLog.copy(distanceMeters = d, weightKg = 0f, reps = 0))
                },
                placeholder = "0",
                suffix = "m",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.width(82.dp),
                isCompleted = isDone
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    val newDist = currentMeters + step
                    onUpdateSet(setLog.copy(distanceMeters = newDist, weightKg = 0f, reps = 0))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AthleticOrange)
            }
        }

        // Delete button if > 1 set
        if (canDelete) {
            IconButton(
                onClick = onDeleteSet,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Usuń serię",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }

        // Complete Checkbox Button
        IconButton(
            onClick = onToggleCompleted,
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isDone) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
                .testTag("set_checkbox_${setLog.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isDone) "Ukończona" else "Zaznacz jako ukończona",
                tint = if (isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Row for loaded distance exercises (e.g. Spacer farmera / Farmers walk with dumbbells or trap bar).
 * Displays BOTH weight (kg) and distance (meters) with steppers and direct input.
 */
@Composable
fun SetLogWeightDistanceRow(
    setLog: WorkoutSetLog,
    canDelete: Boolean,
    onToggleCompleted: () -> Unit,
    onUpdateSet: (WorkoutSetLog) -> Unit,
    onDeleteSet: () -> Unit
) {
    var weightText by remember(setLog.weightKg) {
        mutableStateOf(if (setLog.weightKg == 0f) "" else setLog.weightKg.toString().removeSuffix(".0"))
    }
    val currentMeters = setLog.distanceMeters ?: 40f
    var distText by remember(setLog.distanceMeters) {
        val formatted = if (currentMeters % 1f == 0f) {
            currentMeters.toInt().toString()
        } else {
            currentMeters.toString()
        }
        mutableStateOf(formatted)
    }

    val isDone = setLog.isCompleted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isDone) SuccessGreen.copy(alpha = 0.12f) else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Text(
            text = "${setLog.setNumber}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            color = if (isDone) SuccessGreen else MaterialTheme.colorScheme.onSurface
        )

        // Weight Input (KG)
        Row(
            modifier = Modifier.weight(1.15f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val current = setLog.weightKg
                    val newWeight = (current - 2.5f).coerceAtLeast(0f)
                    onUpdateSet(setLog.copy(weightKg = newWeight))
                },
                modifier = Modifier.size(26.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(2.dp))

            SetNumberInputBox(
                value = weightText,
                onValueChange = { newVal ->
                    val filtered = newVal.filter { it.isDigit() || it == '.' }
                    weightText = filtered
                    val w = filtered.toFloatOrNull() ?: 0f
                    onUpdateSet(setLog.copy(weightKg = w))
                },
                placeholder = "0",
                suffix = "kg",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.widthIn(min = 40.dp, max = 54.dp),
                isCompleted = isDone
            )

            Spacer(modifier = Modifier.width(2.dp))

            IconButton(
                onClick = {
                    val current = setLog.weightKg
                    val newWeight = current + 2.5f
                    onUpdateSet(setLog.copy(weightKg = newWeight))
                },
                modifier = Modifier.size(26.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AthleticOrange)
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Distance Input (M)
        Row(
            modifier = Modifier.weight(1.15f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val step = if (currentMeters >= 100f) 50f else 5f

            IconButton(
                onClick = {
                    val newDist = (currentMeters - step).coerceAtLeast(0f)
                    onUpdateSet(setLog.copy(distanceMeters = newDist))
                },
                modifier = Modifier.size(26.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(2.dp))

            SetNumberInputBox(
                value = distText,
                onValueChange = { newVal ->
                    val filtered = newVal.filter { it.isDigit() || it == '.' }
                    distText = filtered
                    val d = filtered.toFloatOrNull() ?: 0f
                    onUpdateSet(setLog.copy(distanceMeters = d))
                },
                placeholder = "0",
                suffix = "m",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.widthIn(min = 40.dp, max = 54.dp),
                isCompleted = isDone
            )

            Spacer(modifier = Modifier.width(2.dp))

            IconButton(
                onClick = {
                    val newDist = currentMeters + step
                    onUpdateSet(setLog.copy(distanceMeters = newDist))
                },
                modifier = Modifier.size(26.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AthleticOrange)
            }
        }

        // Delete button if > 1 set
        if (canDelete) {
            IconButton(
                onClick = onDeleteSet,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Usuń serię",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }

        // Complete Checkbox Button
        IconButton(
            onClick = onToggleCompleted,
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isDone) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
                .testTag("set_checkbox_${setLog.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isDone) "Ukończona" else "Zaznacz jako ukończona",
                tint = if (isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Row for time-based exercises (e.g. Forearm side plank 35s, Isometric chin up hold 35s, Stairs 20 min).
 * ONLY displays time in seconds/minutes with +/- stepper, NO weight and NO reps.
 */
@Composable
fun SetLogTimeRow(
    setLog: WorkoutSetLog,
    canDelete: Boolean,
    onToggleCompleted: () -> Unit,
    onUpdateSet: (WorkoutSetLog) -> Unit,
    onDeleteSet: () -> Unit,
    onStartTimer: (Int) -> Unit
) {
    val currentSec = setLog.timeSeconds ?: 30
    var timeInputText by remember(setLog.timeSeconds) {
        val text = if (currentSec >= 60 && currentSec % 60 == 0) {
            "${currentSec / 60}m"
        } else {
            "${currentSec}s"
        }
        mutableStateOf(text)
    }

    val isDone = setLog.isCompleted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isDone) SuccessGreen.copy(alpha = 0.12f) else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Text(
            text = "${setLog.setNumber}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            color = if (isDone) SuccessGreen else MaterialTheme.colorScheme.onSurface
        )

        // Time Stepper & Input
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val step = if (currentSec >= 120) 60 else 5

            IconButton(
                onClick = {
                    val newSec = (currentSec - step).coerceAtLeast(5)
                    onUpdateSet(setLog.copy(timeSeconds = newSec, weightKg = 0f, reps = 0))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(4.dp))

            SetNumberInputBox(
                value = timeInputText,
                onValueChange = { newVal ->
                    timeInputText = newVal
                    val isMin = newVal.contains("m", ignoreCase = true)
                    val digits = newVal.filter { it.isDigit() }
                    val num = digits.toIntOrNull() ?: 0
                    val totalSec = if (isMin) num * 60 else num
                    if (totalSec > 0) {
                        onUpdateSet(setLog.copy(timeSeconds = totalSec, weightKg = 0f, reps = 0))
                    }
                },
                placeholder = "0s",
                keyboardType = KeyboardType.Text,
                modifier = Modifier.width(76.dp),
                isCompleted = isDone
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    val newSec = currentSec + step
                    onUpdateSet(setLog.copy(timeSeconds = newSec, weightKg = 0f, reps = 0))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AthleticOrange)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Quick trigger timer button for this set duration
            IconButton(
                onClick = { onStartTimer(currentSec) },
                modifier = Modifier
                    .size(28.dp)
                    .background(ElectricCyan.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Uruchom stoper na $currentSec sek.",
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Delete button if > 1 set
        if (canDelete) {
            IconButton(
                onClick = onDeleteSet,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Usuń serię",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }

        // Complete Checkbox Button
        IconButton(
            onClick = onToggleCompleted,
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isDone) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
                .testTag("set_checkbox_${setLog.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isDone) "Ukończona" else "Zaznacz jako ukończona",
                tint = if (isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Row for bodyweight exercises (reps only, no kg input).
 */
@Composable
fun SetLogRepsOnlyRow(
    setLog: WorkoutSetLog,
    canDelete: Boolean,
    onToggleCompleted: () -> Unit,
    onUpdateSet: (WorkoutSetLog) -> Unit,
    onDeleteSet: () -> Unit
) {
    var repsText by remember(setLog.reps) {
        mutableStateOf(if (setLog.reps <= 0) "10" else setLog.reps.toString())
    }

    val isDone = setLog.isCompleted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isDone) SuccessGreen.copy(alpha = 0.12f) else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Text(
            text = "${setLog.setNumber}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            color = if (isDone) SuccessGreen else MaterialTheme.colorScheme.onSurface
        )

        // Reps Input in the center (stepper with minus, input field, and plus)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val current = setLog.reps
                    val newReps = (current - 1).coerceAtLeast(1)
                    onUpdateSet(setLog.copy(reps = newReps))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(4.dp))

            SetNumberInputBox(
                value = repsText,
                onValueChange = { newVal ->
                    val filtered = newVal.filter { it.isDigit() }
                    repsText = filtered
                    val r = filtered.toIntOrNull() ?: 0
                    onUpdateSet(setLog.copy(reps = r))
                },
                placeholder = "0",
                suffix = "powt.",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.width(90.dp),
                isCompleted = isDone
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    val current = setLog.reps
                    val newReps = current + 1
                    onUpdateSet(setLog.copy(reps = newReps))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold, color = AthleticOrange, fontSize = 16.sp)
            }
        }

        // Delete button if > 1 set
        if (canDelete) {
            IconButton(
                onClick = onDeleteSet,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Usuń serię",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }

        // Completed checkbox
        IconButton(
            onClick = onToggleCompleted,
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isDone) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
                .testTag("set_checkbox_${setLog.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isDone) "Ukończona" else "Zaznacz jako ukończona",
                tint = if (isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Row for standard weight and reps exercises.
 */
@Composable
fun SetLogWeightRepsRow(
    setLog: WorkoutSetLog,
    canDelete: Boolean,
    onToggleCompleted: () -> Unit,
    onUpdateSet: (WorkoutSetLog) -> Unit,
    onDeleteSet: () -> Unit
) {
    var weightText by remember(setLog.weightKg) {
        mutableStateOf(if (setLog.weightKg == 0f) "" else setLog.weightKg.toString().removeSuffix(".0"))
    }
    var repsText by remember(setLog.reps) {
        mutableStateOf(if (setLog.reps <= 0) "10" else setLog.reps.toString())
    }

    val isDone = setLog.isCompleted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isDone) SuccessGreen.copy(alpha = 0.12f) else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Text(
            text = "${setLog.setNumber}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            color = if (isDone) SuccessGreen else MaterialTheme.colorScheme.onSurface
        )

        // Weight Input (with quick +/- stepper buttons)
        Row(
            modifier = Modifier.weight(1.15f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val current = setLog.weightKg
                    val newWeight = (current - 2.5f).coerceAtLeast(0f)
                    onUpdateSet(setLog.copy(weightKg = newWeight))
                },
                modifier = Modifier.size(26.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(2.dp))

            SetNumberInputBox(
                value = weightText,
                onValueChange = { newVal ->
                    val filtered = newVal.filter { it.isDigit() || it == '.' }
                    weightText = filtered
                    val w = filtered.toFloatOrNull() ?: 0f
                    onUpdateSet(setLog.copy(weightKg = w))
                },
                placeholder = "0",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.widthIn(min = 40.dp, max = 54.dp),
                isCompleted = isDone
            )

            Spacer(modifier = Modifier.width(2.dp))

            IconButton(
                onClick = {
                    val current = setLog.weightKg
                    val newWeight = current + 2.5f
                    onUpdateSet(setLog.copy(weightKg = newWeight))
                },
                modifier = Modifier.size(26.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AthleticOrange)
            }
        }

        // Reps Input
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val current = setLog.reps
                    val newReps = (current - 1).coerceAtLeast(1)
                    onUpdateSet(setLog.copy(reps = newReps))
                },
                modifier = Modifier.size(26.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(2.dp))

            SetNumberInputBox(
                value = repsText,
                onValueChange = { newVal ->
                    val filtered = newVal.filter { it.isDigit() }
                    repsText = filtered
                    val r = filtered.toIntOrNull() ?: 0
                    onUpdateSet(setLog.copy(reps = r))
                },
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.widthIn(min = 38.dp, max = 48.dp),
                isCompleted = isDone
            )

            Spacer(modifier = Modifier.width(2.dp))

            IconButton(
                onClick = {
                    val current = setLog.reps
                    val newReps = current + 1
                    onUpdateSet(setLog.copy(reps = newReps))
                },
                modifier = Modifier.size(26.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AthleticOrange)
            }
        }

        // Delete button if > 1 set
        if (canDelete) {
            IconButton(
                onClick = onDeleteSet,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Usuń serię",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }

        // Complete Checkbox Button
        IconButton(
            onClick = onToggleCompleted,
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isDone) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
                .testTag("set_checkbox_${setLog.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isDone) "Ukończona" else "Zaznacz jako ukończona",
                tint = if (isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SpecBadge(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
