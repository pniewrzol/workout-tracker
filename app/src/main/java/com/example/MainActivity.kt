package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.AppDatabase
import com.example.data.repository.WorkoutRepository
import com.example.ui.components.RestTimerPill
import com.example.ui.screens.BodyMeasurementsScreen
import com.example.ui.screens.ExerciseDetailScreen
import com.example.ui.screens.ExerciseListScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProgressChartsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkoutActiveScreen
import com.example.ui.screens.WorkoutGuideScreen
import com.example.ui.screens.WorkoutHistoryScreen
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChartMetric
import com.example.ui.viewmodel.WorkoutViewModel
import com.example.ui.viewmodel.WorkoutViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

sealed class Screen {
    data object Home : Screen()
    data object Exercises : Screen()
    data object Charts : Screen()
    data object History : Screen()
    data object Guide : Screen()
    data object ActiveWorkout : Screen()
    data object BodyMeasurements : Screen()
    data object Settings : Screen()
    data class ExerciseDetail(val exerciseId: Long) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: WorkoutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext, CoroutineScope(Dispatchers.IO))
        val repository = WorkoutRepository(
            context = applicationContext,
            database = database,
            exerciseDao = database.exerciseDao(),
            mediaDao = database.mediaDao(),
            workoutDao = database.workoutDao(),
            bodyMeasurementDao = database.bodyMeasurementDao()
        )
        val factory = WorkoutViewModelFactory(repository, applicationContext)
        viewModel = ViewModelProvider(this, factory)[WorkoutViewModel::class.java]

        setContent {
            val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
            MyApplicationTheme(appThemeMode = appThemeMode) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: WorkoutViewModel) {
    var currentScreenRoute by rememberSaveable { mutableStateOf("home") }
    var previousDetailRoute by rememberSaveable { mutableStateOf("home") }
    var selectedTabRoute by rememberSaveable { mutableStateOf("home") }

    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val backupPassword by viewModel.backupPassword.collectAsStateWithLifecycle()
    val allExercises by viewModel.allExercises.collectAsStateWithLifecycle()
    val completedSessions by viewModel.completedSessions.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val allCompletedSets by viewModel.allCompletedSets.collectAsStateWithLifecycle()
    val activeSessionSets by viewModel.activeSessionSets.collectAsStateWithLifecycle()
    val allMeasurements by viewModel.allMeasurements.collectAsStateWithLifecycle()
    val allMeasurementsAsc by viewModel.allMeasurementsAsc.collectAsStateWithLifecycle()
    val allPlans by viewModel.allPlans.collectAsStateWithLifecycle()
    val activePlan by viewModel.activePlan.collectAsStateWithLifecycle()

    val restTimerSeconds by viewModel.restTimerSeconds.collectAsStateWithLifecycle()
    val restTimerRemainingSeconds by viewModel.restTimerRemainingSeconds.collectAsStateWithLifecycle()
    val isRestTimerActive by viewModel.isRestTimerActive.collectAsStateWithLifecycle()
    val isRestTimerPaused by viewModel.isRestTimerPaused.collectAsStateWithLifecycle()
    val restTimerLabel by viewModel.restTimerLabel.collectAsStateWithLifecycle()

    val timerVibrationEnabled by viewModel.timerVibrationEnabled.collectAsStateWithLifecycle()
    val timerSoundEnabled by viewModel.timerSoundEnabled.collectAsStateWithLifecycle()

    val selectedExercise by viewModel.selectedExercise.collectAsStateWithLifecycle()
    val selectedExerciseMedia by viewModel.selectedExerciseMedia.collectAsStateWithLifecycle()
    val selectedExerciseSets by viewModel.selectedExerciseSets.collectAsStateWithLifecycle()

    // Persistent scroll states hoisted across navigation so opening an exercise detail never resets the list to the top
    val homeListState = rememberLazyListState()
    val exerciseListState = rememberLazyListState()
    val workoutActiveListState = rememberLazyListState()

    var lastViewedExerciseIdInWorkout by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastViewedExerciseIdInList by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastScrolledSessionId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Reset workout active scroll state only when a new workout session starts
    LaunchedEffect(activeSession?.id) {
        val sessId = activeSession?.id
        if (sessId != null && sessId != lastScrolledSessionId) {
            lastScrolledSessionId = sessId
            workoutActiveListState.scrollToItem(0)
        }
    }

    val currentScreen: Screen = remember(currentScreenRoute) {
        when {
            currentScreenRoute == "exercises" -> Screen.Exercises
            currentScreenRoute == "charts" -> Screen.Charts
            currentScreenRoute == "history" -> Screen.History
            currentScreenRoute == "guide" -> Screen.Guide
            currentScreenRoute == "active_workout" -> Screen.ActiveWorkout
            currentScreenRoute == "measurements" -> Screen.BodyMeasurements
            currentScreenRoute == "settings" -> Screen.Settings
            currentScreenRoute.startsWith("detail_") -> {
                val id = currentScreenRoute.removePrefix("detail_").toLongOrNull() ?: 1L
                Screen.ExerciseDetail(id)
            }
            else -> Screen.Home
        }
    }

    // Intercept back button when not on root home screen
    BackHandler(enabled = currentScreenRoute != "home") {
        if (currentScreenRoute.startsWith("detail_")) {
            currentScreenRoute = previousDetailRoute
        } else if (currentScreenRoute == "active_workout") {
            currentScreenRoute = "home"
        } else {
            currentScreenRoute = "home"
            selectedTabRoute = "home"
        }
    }

    val navItems = listOf(
        NavigationItem("Pulpit", Icons.Default.Home, "home", "nav_home"),
        NavigationItem("Ćwiczenia", Icons.Default.FitnessCenter, "exercises", "nav_exercises"),
        NavigationItem("Wykresy", Icons.Default.ShowChart, "charts", "nav_charts"),
        NavigationItem("Historia", Icons.Default.History, "history", "nav_history"),
        NavigationItem("Poradnik", Icons.Default.MenuBook, "guide", "nav_guide")
    )

    val isFullscreenSubScreen = currentScreenRoute == "active_workout" ||
            currentScreenRoute.startsWith("detail_") ||
            currentScreenRoute == "measurements" ||
            currentScreenRoute == "settings"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isFullscreenSubScreen) {
                Column {
                    // Floating Rest / Exercise Timer when active across any screen
                    if (isRestTimerActive && activeSession != null) {
                        RestTimerPill(
                            totalSeconds = restTimerSeconds,
                            remainingSeconds = restTimerRemainingSeconds,
                            isVisible = isRestTimerActive,
                            isPaused = isRestTimerPaused,
                            label = restTimerLabel,
                            onPauseResume = { viewModel.pauseResumeRestTimer() },
                            onAddSeconds = { viewModel.addRestSeconds(it) },
                            onReduceSeconds = { viewModel.reduceRestSeconds(it) },
                            onFinishedOrDismissed = { viewModel.dismissRestTimer() }
                        )
                    }

                    // Ongoing workout mini resume bar if active
                    if (activeSession != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable { currentScreenRoute = "active_workout" },
                            shape = RoundedCornerShape(12.dp),
                            color = AthleticOrange,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "W toku: ${activeSession?.workoutName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Otwórz",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentScreenRoute == item.route

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    selectedTabRoute = item.route
                                    currentScreenRoute = item.route
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AthleticOrange,
                                    selectedTextColor = AthleticOrange,
                                    indicatorColor = AthleticOrange.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                Screen.Home -> {
                    HomeScreen(
                        activeSession = activeSession,
                        completedSessions = completedSessions,
                        allExercises = allExercises,
                        allCompletedSets = allCompletedSets,
                        allPlans = allPlans,
                        activePlan = activePlan,
                        allMeasurements = allMeasurements,
                        onStartWorkout = { workoutName, exercises ->
                            viewModel.startWorkout(workoutName, exercises)
                            currentScreenRoute = "active_workout"
                        },
                        onResumeWorkout = { currentScreenRoute = "active_workout" },
                        onNavigateToExercises = {
                            selectedTabRoute = "exercises"
                            currentScreenRoute = "exercises"
                        },
                        onNavigateToCharts = {
                            selectedTabRoute = "charts"
                            currentScreenRoute = "charts"
                        },
                        onNavigateToHistory = {
                            selectedTabRoute = "history"
                            currentScreenRoute = "history"
                        },
                        onNavigateToGuide = {
                            selectedTabRoute = "guide"
                            currentScreenRoute = "guide"
                        },
                        onNavigateToMeasurements = {
                            currentScreenRoute = "measurements"
                        },
                        onNavigateToSettings = {
                            currentScreenRoute = "settings"
                        },
                        onExerciseClick = { exId ->
                            previousDetailRoute = "home"
                            viewModel.selectExercise(exId)
                            currentScreenRoute = "detail_$exId"
                        },
                        onCreatePlan = { name, desc, includeWarmup, workouts ->
                            viewModel.createPlan(name, desc, includeWarmup, workouts)
                        },
                        onSetActivePlan = { planId ->
                            viewModel.setActivePlan(planId)
                        },
                        onDeletePlan = { plan ->
                            viewModel.deletePlan(plan)
                        },
                        onDuplicatePlan = { plan ->
                            viewModel.duplicatePlan(plan)
                        },
                        onCreateCustomExercise = { ex ->
                            viewModel.createCustomExercise(ex)
                        },
                        onDeleteExercise = { ex ->
                            viewModel.deleteExercise(ex)
                        },
                        onDeleteWorkoutCategory = { plan, cat ->
                            viewModel.deleteWorkoutFromPlan(plan, cat)
                        },
                        listState = homeListState
                    )
                }

                Screen.Exercises -> {
                    ExerciseListScreen(
                        exercises = allExercises,
                        listState = exerciseListState,
                        lastViewedExerciseId = lastViewedExerciseIdInList,
                        onExerciseClick = { exId ->
                            previousDetailRoute = "exercises"
                            lastViewedExerciseIdInList = exId
                            viewModel.selectExercise(exId)
                            currentScreenRoute = "detail_$exId"
                        },
                        onCreateExercise = { newEx ->
                            viewModel.createCustomExercise(newEx)
                        },
                        onDeleteExercise = { ex ->
                            viewModel.deleteExercise(ex)
                        },
                        onDeleteExercises = { list ->
                            viewModel.deleteExercises(list)
                        },
                        onDeleteCategory = { cat ->
                            viewModel.deleteCategory(cat)
                        }
                    )
                }

                Screen.Charts -> {
                    ProgressChartsScreen(
                        exercises = allExercises,
                        allCompletedSets = allCompletedSets,
                        getChartPoints = { sets, metric ->
                            viewModel.getChartPointsForExercise(sets, metric)
                        }
                    )
                }

                Screen.History -> {
                    WorkoutHistoryScreen(
                        sessions = completedSessions,
                        allCompletedSets = allCompletedSets,
                        allExercises = allExercises
                    )
                }

                Screen.Guide -> {
                    WorkoutGuideScreen()
                }

                Screen.BodyMeasurements -> {
                    BodyMeasurementsScreen(
                        measurements = allMeasurements,
                        measurementsAsc = allMeasurementsAsc,
                        getChartPoints = { list, metric ->
                            viewModel.getChartPointsForMeasurement(list, metric)
                        },
                        onAddMeasurement = { m -> viewModel.addBodyMeasurement(m) },
                        onUpdateMeasurement = { m -> viewModel.updateBodyMeasurement(m) },
                        onDeleteMeasurement = { m -> viewModel.deleteBodyMeasurement(m) },
                        onBack = { currentScreenRoute = "home" }
                    )
                }

                Screen.Settings -> {
                    SettingsScreen(
                        currentThemeMode = appThemeMode,
                        onThemeModeChange = { mode -> viewModel.setThemeMode(mode) },
                        savedBackupPassword = backupPassword,
                        onSaveBackupPassword = { pass -> viewModel.setBackupPassword(pass) },
                        onExportBackup = { uri, password, callback -> viewModel.exportBackup(uri, password, callback) },
                        onRestoreBackup = { uri, password, callback -> viewModel.restoreBackup(uri, password, callback) },
                        timerVibrationEnabled = timerVibrationEnabled,
                        onTimerVibrationChange = { viewModel.setTimerVibrationEnabled(it) },
                        timerSoundEnabled = timerSoundEnabled,
                        onTimerSoundChange = { viewModel.setTimerSoundEnabled(it) },
                        onBack = { currentScreenRoute = "home" }
                    )
                }

                Screen.ActiveWorkout -> {
                    if (activeSession != null) {
                        WorkoutActiveScreen(
                            session = activeSession!!,
                            sets = activeSessionSets,
                            allExercises = allExercises,
                            allCompletedSets = allCompletedSets,
                            restTimerSeconds = restTimerSeconds,
                            restTimerRemainingSeconds = restTimerRemainingSeconds,
                            isRestTimerActive = isRestTimerActive,
                            isRestTimerPaused = isRestTimerPaused,
                            restTimerLabel = restTimerLabel,
                            onToggleSetCompleted = { setLog, restSec, nextSupersetName, isFirstInSuperset ->
                                viewModel.toggleSetCompleted(
                                    setLog = setLog,
                                    exerciseRestSeconds = restSec,
                                    nextSupersetExerciseName = nextSupersetName,
                                    isFirstInSuperset = isFirstInSuperset
                                )
                            },
                            onUpdateSet = { setLog ->
                                viewModel.updateSetLog(setLog)
                            },
                            onAddSet = { exerciseId, nextSetNumber, defaultReps, defaultWeight, defaultTimeSeconds, defaultDistanceMeters ->
                                viewModel.addSetToExercise(
                                    exerciseId = exerciseId,
                                    nextSetNumber = nextSetNumber,
                                    defaultReps = defaultReps,
                                    defaultWeight = defaultWeight,
                                    defaultTimeSeconds = defaultTimeSeconds,
                                    defaultDistanceMeters = defaultDistanceMeters
                                )
                            },
                            onRemoveSet = { setLog ->
                                viewModel.removeSetLog(setLog)
                            },
                            onFinishWorkout = { notes ->
                                viewModel.finishActiveWorkout(notes)
                                currentScreenRoute = "home"
                            },
                            onDiscardWorkout = {
                                viewModel.discardActiveWorkout()
                                currentScreenRoute = "home"
                            },
                            onDismissTimer = { viewModel.dismissRestTimer() },
                            onStartTimer = { seconds, label -> viewModel.startRestTimer(seconds, label) },
                            onPauseResumeTimer = { viewModel.pauseResumeRestTimer() },
                            onAddTimerSeconds = { viewModel.addRestSeconds(it) },
                            onReduceTimerSeconds = { viewModel.reduceRestSeconds(it) },
                            onExerciseDetailsClick = { exId ->
                                previousDetailRoute = "active_workout"
                                lastViewedExerciseIdInWorkout = exId
                                viewModel.selectExercise(exId)
                                currentScreenRoute = "detail_$exId"
                            },
                            onMinimize = {
                                currentScreenRoute = "home"
                            },
                            onNavigateToExercises = {
                                selectedTabRoute = "exercises"
                                currentScreenRoute = "exercises"
                            },
                            onNavigateToHistory = {
                                selectedTabRoute = "history"
                                currentScreenRoute = "history"
                            },
                            onNavigateToMeasurements = {
                                currentScreenRoute = "measurements"
                            },
                            listState = workoutActiveListState,
                            lastViewedExerciseId = lastViewedExerciseIdInWorkout
                        )
                    } else {
                        // While the workout session is being created in database, show loading state rather than bouncing to Home
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = AthleticOrange)
                                Text(
                                    text = "Przygotowywanie sesji treningowej...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                TextButton(onClick = { currentScreenRoute = "home" }) {
                                    Text("Wróć do pulpitu", color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                }

                is Screen.ExerciseDetail -> {
                    val targetId = currentScreen.exerciseId
                    LaunchedEffect(targetId) {
                        viewModel.selectExercise(targetId)
                    }
                    val exercise = allExercises.firstOrNull { it.id == targetId } ?: selectedExercise
                    if (exercise != null) {
                        val chartPoints = remember(exercise, selectedExerciseSets) {
                            val metric = when {
                                exercise.isDistanceBased() -> ChartMetric.DISTANCE
                                exercise.isTimeBased() -> ChartMetric.DURATION
                                exercise.isBodyweightBased() -> ChartMetric.REPS
                                else -> ChartMetric.MAX_WEIGHT
                            }
                            viewModel.getChartPointsForExercise(exercise, selectedExerciseSets, metric)
                        }

                        ExerciseDetailScreen(
                            exercise = exercise,
                            mediaList = selectedExerciseMedia,
                            completedSets = selectedExerciseSets,
                            chartPoints = chartPoints,
                            onAddMedia = { uri, isVideo, caption ->
                                viewModel.addMedia(uri, isVideo, exercise.id, caption)
                            },
                            onAddMultipleMedia = { uris, isVideo ->
                                viewModel.addMultipleMedia(uris, isVideo, exercise.id)
                            },
                            onDeleteMedia = { media ->
                                viewModel.deleteMedia(media)
                            },
                            onDeleteExercise = { ex ->
                                viewModel.deleteExercise(ex)
                                currentScreenRoute = previousDetailRoute
                            },
                            onBack = {
                                currentScreenRoute = previousDetailRoute
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val testTag: String
)
