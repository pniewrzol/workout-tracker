package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.AppDatabase
import com.example.data.repository.WorkoutRepository
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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedTab by remember { mutableStateOf<Screen>(Screen.Home) }

    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val backupPassword by viewModel.backupPassword.collectAsStateWithLifecycle()
    val allExercises by viewModel.allExercises.collectAsStateWithLifecycle()
    val completedSessions by viewModel.completedSessions.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val allCompletedSets by viewModel.allCompletedSets.collectAsStateWithLifecycle()
    val activeSessionSets by viewModel.activeSessionSets.collectAsStateWithLifecycle()
    val allMeasurements by viewModel.allMeasurements.collectAsStateWithLifecycle()
    val allMeasurementsAsc by viewModel.allMeasurementsAsc.collectAsStateWithLifecycle()

    val restTimerSeconds by viewModel.restTimerSeconds.collectAsStateWithLifecycle()
    val isRestTimerActive by viewModel.isRestTimerActive.collectAsStateWithLifecycle()

    val selectedExercise by viewModel.selectedExercise.collectAsStateWithLifecycle()
    val selectedExerciseMedia by viewModel.selectedExerciseMedia.collectAsStateWithLifecycle()
    val selectedExerciseSets by viewModel.selectedExerciseSets.collectAsStateWithLifecycle()

    val navItems = listOf(
        NavigationItem("Pulpit", Icons.Default.Home, Screen.Home, "nav_home"),
        NavigationItem("Ćwiczenia", Icons.Default.FitnessCenter, Screen.Exercises, "nav_exercises"),
        NavigationItem("Wykresy", Icons.Default.ShowChart, Screen.Charts, "nav_charts"),
        NavigationItem("Historia", Icons.Default.History, Screen.History, "nav_history"),
        NavigationItem("Poradnik", Icons.Default.MenuBook, Screen.Guide, "nav_guide")
    )

    val isFullscreenSubScreen = currentScreen == Screen.ActiveWorkout ||
            currentScreen is Screen.ExerciseDetail ||
            currentScreen == Screen.BodyMeasurements ||
            currentScreen == Screen.Settings

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isFullscreenSubScreen) {
                Column {
                    // Ongoing workout mini resume bar if active
                    if (activeSession != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable { currentScreen = Screen.ActiveWorkout },
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        color = Color.White
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
                            val isSelected = currentScreen == item.screen

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    selectedTab = item.screen
                                    currentScreen = item.screen
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
                        onStartWorkout = { workoutName, exercises ->
                            viewModel.startWorkout(workoutName, exercises)
                            currentScreen = Screen.ActiveWorkout
                        },
                        onResumeWorkout = { currentScreen = Screen.ActiveWorkout },
                        onNavigateToExercises = {
                            selectedTab = Screen.Exercises
                            currentScreen = Screen.Exercises
                        },
                        onNavigateToCharts = {
                            selectedTab = Screen.Charts
                            currentScreen = Screen.Charts
                        },
                        onNavigateToHistory = {
                            selectedTab = Screen.History
                            currentScreen = Screen.History
                        },
                        onNavigateToGuide = {
                            selectedTab = Screen.Guide
                            currentScreen = Screen.Guide
                        },
                        onNavigateToMeasurements = {
                            currentScreen = Screen.BodyMeasurements
                        },
                        onNavigateToSettings = {
                            currentScreen = Screen.Settings
                        },
                        onExerciseClick = { exId ->
                            viewModel.selectExercise(exId)
                            currentScreen = Screen.ExerciseDetail(exId)
                        }
                    )
                }

                Screen.Exercises -> {
                    ExerciseListScreen(
                        exercises = allExercises,
                        onExerciseClick = { exId ->
                            viewModel.selectExercise(exId)
                            currentScreen = Screen.ExerciseDetail(exId)
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
                        onDeleteMeasurement = { m -> viewModel.deleteBodyMeasurement(m) },
                        onBack = { currentScreen = Screen.Home }
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
                        onBack = { currentScreen = Screen.Home }
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
                            isRestTimerActive = isRestTimerActive,
                            onToggleSetCompleted = { setLog, restSec ->
                                viewModel.toggleSetCompleted(setLog, restSec)
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
                                currentScreen = Screen.Home
                            },
                            onDiscardWorkout = {
                                viewModel.discardActiveWorkout()
                                currentScreen = Screen.Home
                            },
                            onDismissTimer = { viewModel.dismissRestTimer() },
                            onStartTimer = { seconds -> viewModel.startRestTimer(seconds) },
                            onExerciseDetailsClick = { exId ->
                                viewModel.selectExercise(exId)
                                currentScreen = Screen.ExerciseDetail(exId)
                            }
                        )
                    } else {
                        currentScreen = Screen.Home
                    }
                }

                is Screen.ExerciseDetail -> {
                    val exercise = selectedExercise
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
                            onBack = {
                                if (activeSession != null && selectedTab != Screen.Exercises && selectedTab != Screen.Home) {
                                    currentScreen = Screen.ActiveWorkout
                                } else {
                                    currentScreen = selectedTab
                                }
                            }
                        )
                    } else {
                        currentScreen = selectedTab
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen,
    val testTag: String
)
