package com.example.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BodyMeasurement
import com.example.data.model.Exercise
import com.example.data.model.ExerciseMedia
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import com.example.data.repository.RestoreResult
import com.example.data.repository.WorkoutRepository
import com.example.data.security.SecurePasswordStorage
import com.example.ui.components.ChartPoint
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ChartMetric {
    MAX_WEIGHT,
    TOTAL_VOLUME,
    ESTIMATED_1RM,
    DISTANCE,
    DURATION,
    REPS
}

enum class MeasurementMetric(val label: String, val unit: String) {
    WEIGHT("Waga", "kg"),
    BODY_FAT("Tkanka tłuszczowa", "%"),
    CHEST("Klatka piersiowa", "cm"),
    WAIST("Talia / Pas", "cm"),
    BICEPS("Biceps", "cm"),
    HIPS("Biodra", "cm"),
    THIGHS("Uda", "cm"),
    CALVES("Łydki", "cm"),
    SHOULDERS("Barki", "cm")
}

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val sharedPreferences: SharedPreferences,
    private val securePasswordStorage: SecurePasswordStorage
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.checkAndSeedExercises()
        }
    }

    // App Theme State
    private val _appThemeMode = MutableStateFlow(loadSavedThemeMode())
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _appThemeMode.value = mode
        sharedPreferences.edit().putString("app_theme_mode", mode.name).apply()
    }

    // User configured backup encryption password (stored securely via EncryptedSharedPreferences / Android KeyStore)
    private val _backupPassword = MutableStateFlow(securePasswordStorage.getBackupPassword())
    val backupPassword: StateFlow<String> = _backupPassword.asStateFlow()

    fun setBackupPassword(password: String) {
        _backupPassword.value = password
        securePasswordStorage.setBackupPassword(password)
    }

    private fun loadSavedThemeMode(): AppThemeMode {
        val saved = sharedPreferences.getString("app_theme_mode", AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    val allExercises: StateFlow<List<Exercise>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedSessions: StateFlow<List<WorkoutSession>> = repository.allCompletedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<WorkoutSession?> = repository.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allCompletedSets: StateFlow<List<WorkoutSetLog>> = repository.allCompletedSets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Body Measurements
    val allMeasurements: StateFlow<List<BodyMeasurement>> = repository.allMeasurements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMeasurementsAsc: StateFlow<List<BodyMeasurement>> = repository.allMeasurementsAsc
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlans: StateFlow<List<WorkoutPlan>> = repository.allPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePlan: StateFlow<WorkoutPlan?> = repository.activePlan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active session sets
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSessionSets: StateFlow<List<WorkoutSetLog>> = activeSession
        .flatMapLatest { session ->
            if (session != null) {
                repository.getSetsForSession(session.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Exercise for detail view
    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    val selectedExerciseId: StateFlow<Long?> = _selectedExerciseId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedExercise: StateFlow<Exercise?> = _selectedExerciseId
        .flatMapLatest { id ->
            if (id != null) repository.getExerciseById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedExerciseMedia: StateFlow<List<ExerciseMedia>> = _selectedExerciseId
        .flatMapLatest { id ->
            if (id != null) repository.getMediaForExercise(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedExerciseSets: StateFlow<List<WorkoutSetLog>> = _selectedExerciseId
        .flatMapLatest { id ->
            if (id != null) repository.getCompletedSetsForExercise(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rest Timer state
    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()

    private val _isRestTimerActive = MutableStateFlow(false)
    val isRestTimerActive: StateFlow<Boolean> = _isRestTimerActive.asStateFlow()

    fun selectExercise(id: Long) {
        _selectedExerciseId.value = id
    }

    fun startRestTimer(seconds: Int) {
        if (seconds > 0) {
            _restTimerSeconds.value = seconds
            _isRestTimerActive.value = true
        }
    }

    fun dismissRestTimer() {
        _isRestTimerActive.value = false
    }

    fun startWorkout(workoutName: String, exercises: List<Exercise>, onStarted: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.startWorkoutSession(workoutName, exercises)
            onStarted?.invoke()
        }
    }

    fun createPlan(
        name: String,
        description: String = "",
        includeWarmupAndMobility: Boolean = true,
        initialWorkouts: List<Pair<String, String>> = emptyList()
    ) {
        viewModelScope.launch {
            repository.createPlan(name, description, includeWarmupAndMobility, initialWorkouts)
        }
    }

    fun setActivePlan(planId: Long) {
        viewModelScope.launch {
            repository.setActivePlan(planId)
        }
    }

    fun deletePlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            repository.deletePlan(plan)
        }
    }

    fun createCustomExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.createCustomExercise(exercise)
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
        }
    }

    fun deleteExerciseById(id: Long) {
        viewModelScope.launch {
            repository.deleteExerciseById(id)
        }
    }

    fun deleteCategory(section: String) {
        viewModelScope.launch {
            repository.deleteCategory(section)
        }
    }

    fun deleteWorkoutFromPlan(plan: com.example.data.model.WorkoutPlan, workoutSection: String) {
        viewModelScope.launch {
            repository.deleteWorkoutFromPlan(plan, workoutSection)
        }
    }

    fun addBodyMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.insertBodyMeasurement(measurement)
        }
    }

    fun updateBodyMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.updateBodyMeasurement(measurement)
        }
    }

    fun deleteBodyMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.deleteBodyMeasurement(measurement)
        }
    }

    fun finishActiveWorkout(notes: String = "") {
        val current = activeSession.value ?: return
        viewModelScope.launch {
            repository.finishWorkoutSession(current.id, notes)
            dismissRestTimer()
        }
    }

    fun discardActiveWorkout() {
        val current = activeSession.value ?: return
        viewModelScope.launch {
            repository.discardWorkoutSession(current.id)
            dismissRestTimer()
        }
    }

    fun updateSetLog(setLog: WorkoutSetLog) {
        viewModelScope.launch {
            repository.updateSetLog(setLog)
        }
    }

    fun toggleSetCompleted(setLog: WorkoutSetLog, exerciseRestSeconds: Int) {
        val newCompleted = !setLog.isCompleted
        val updated = setLog.copy(
            isCompleted = newCompleted,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updateSetLog(updated)
            if (newCompleted && exerciseRestSeconds > 0) {
                startRestTimer(exerciseRestSeconds)
            }
        }
    }

    fun addSetToExercise(
        exerciseId: Long,
        nextSetNumber: Int,
        defaultReps: Int = 10,
        defaultWeight: Float = 0f,
        defaultTimeSeconds: Int? = null,
        defaultDistanceMeters: Float? = null
    ) {
        val currentSession = activeSession.value ?: return
        viewModelScope.launch {
            repository.insertSetLog(
                WorkoutSetLog(
                    sessionId = currentSession.id,
                    exerciseId = exerciseId,
                    setNumber = nextSetNumber,
                    weightKg = defaultWeight,
                    reps = defaultReps,
                    timeSeconds = defaultTimeSeconds,
                    distanceMeters = defaultDistanceMeters,
                    isCompleted = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeSetLog(setLog: WorkoutSetLog) {
        viewModelScope.launch {
            repository.deleteSetLog(setLog)
        }
    }

    fun addMedia(uri: Uri, isVideo: Boolean, exerciseId: Long, caption: String = "") {
        viewModelScope.launch {
            repository.saveMediaFile(uri, isVideo, exerciseId, caption)
        }
    }

    fun addMultipleMedia(uris: List<Uri>, isVideo: Boolean, exerciseId: Long) {
        viewModelScope.launch {
            for (uri in uris) {
                repository.saveMediaFile(uri, isVideo, exerciseId, "")
            }
        }
    }

    fun deleteMedia(media: ExerciseMedia) {
        viewModelScope.launch {
            repository.deleteMedia(media)
        }
    }

    fun getChartPointsForMeasurement(
        measurementsAsc: List<BodyMeasurement>,
        metric: MeasurementMetric
    ): List<ChartPoint> {
        val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
        return measurementsAsc.mapNotNull { m ->
            val value = when (metric) {
                MeasurementMetric.WEIGHT -> m.weightKg
                MeasurementMetric.BODY_FAT -> m.bodyFatPercentage
                MeasurementMetric.CHEST -> m.chestCm
                MeasurementMetric.WAIST -> m.waistCm
                MeasurementMetric.BICEPS -> m.bicepsCm
                MeasurementMetric.HIPS -> m.hipsCm
                MeasurementMetric.THIGHS -> m.thighsCm
                MeasurementMetric.CALVES -> m.calvesCm
                MeasurementMetric.SHOULDERS -> m.shouldersCm
            }
            if (value != null && value > 0f) {
                ChartPoint(
                    timestamp = m.timestamp,
                    value = value,
                    label = dateFormat.format(Date(m.timestamp)),
                    detail = "${metric.label}: $value ${metric.unit}"
                )
            } else null
        }
    }

    // Backup & Restore
    fun exportBackup(uri: Uri, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.exportBackup(uri, password)
            onResult(result)
        }
    }

    fun restoreBackup(uri: Uri, password: String? = null, onResult: (RestoreResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.restoreBackup(uri, password)
            onResult(result)
        }
    }

    // Exercise Chart helpers
    fun getChartPointsForExercise(
        exerciseSets: List<WorkoutSetLog>,
        metric: ChartMetric
    ): List<ChartPoint> {
        val exercise = allExercises.value.firstOrNull { it.id == exerciseSets.firstOrNull()?.exerciseId }
        return getChartPointsForExercise(exercise, exerciseSets, metric)
    }

    fun getChartPointsForExercise(
        exercise: Exercise?,
        exerciseSets: List<WorkoutSetLog>,
        metric: ChartMetric
    ): List<ChartPoint> {
        val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
        val completedOnly = exerciseSets.filter { it.isCompleted }
        if (completedOnly.isEmpty()) return emptyList()

        val groupedByDay = completedOnly.groupBy {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
        }

        val isDist = exercise?.isDistanceBased() == true
        val isTime = exercise?.isTimeBased() == true
        val isBw = exercise?.isBodyweightBased() == true

        return groupedByDay.map { (_, sets) ->
            val timestamp = sets.first().timestamp
            val dateLabel = dateFormat.format(Date(timestamp))

            when {
                isDist || metric == ChartMetric.DISTANCE -> {
                    val maxDist = sets.mapNotNull { it.distanceMeters }.maxOrNull() ?: 0f
                    ChartPoint(
                        timestamp = timestamp,
                        value = maxDist,
                        label = dateLabel,
                        detail = "${maxDist.toInt()} m"
                    )
                }
                isTime || metric == ChartMetric.DURATION -> {
                    val maxSec = sets.mapNotNull { it.timeSeconds }.maxOrNull() ?: 0
                    val minText = if (maxSec >= 60) "${maxSec / 60}m ${maxSec % 60}s" else "${maxSec}s"
                    ChartPoint(
                        timestamp = timestamp,
                        value = maxSec.toFloat(),
                        label = dateLabel,
                        detail = minText
                    )
                }
                isBw || metric == ChartMetric.REPS -> {
                    val maxReps = sets.maxOfOrNull { it.reps } ?: 0
                    ChartPoint(
                        timestamp = timestamp,
                        value = maxReps.toFloat(),
                        label = dateLabel,
                        detail = "$maxReps powt."
                    )
                }
                metric == ChartMetric.MAX_WEIGHT -> {
                    val maxWeight = sets.maxOfOrNull { it.weightKg } ?: 0f
                    val bestSet = sets.firstOrNull { it.weightKg == maxWeight }
                    ChartPoint(
                        timestamp = timestamp,
                        value = maxWeight,
                        label = dateLabel,
                        detail = "${bestSet?.reps ?: 0} powt."
                    )
                }
                metric == ChartMetric.TOTAL_VOLUME -> {
                    when {
                        isDist -> {
                            val totalDist = sets.mapNotNull { it.distanceMeters }.sum()
                            ChartPoint(
                                timestamp = timestamp,
                                value = totalDist,
                                label = dateLabel,
                                detail = "${totalDist.toInt()} m"
                            )
                        }
                        isTime -> {
                            val totalSec = sets.mapNotNull { it.timeSeconds }.sum()
                            val minText = if (totalSec >= 60) "${totalSec / 60}m ${totalSec % 60}s" else "${totalSec}s"
                            ChartPoint(
                                timestamp = timestamp,
                                value = totalSec.toFloat(),
                                label = dateLabel,
                                detail = minText
                            )
                        }
                        else -> {
                            val totalVolume = sets.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()
                            ChartPoint(
                                timestamp = timestamp,
                                value = totalVolume,
                                label = dateLabel,
                                detail = "${sets.size} serii"
                            )
                        }
                    }
                }
                metric == ChartMetric.ESTIMATED_1RM -> {
                    val max1Rm = sets.maxOfOrNull { it.weightKg * (1f + it.reps / 30f) } ?: 0f
                    ChartPoint(
                        timestamp = timestamp,
                        value = (max1Rm * 10).toInt() / 10f,
                        label = dateLabel,
                        detail = "1RM (Epley)"
                    )
                }
                else -> ChartPoint(timestamp, 0f, dateLabel)
            }
        }.sortedBy { it.timestamp }
    }
}

class WorkoutViewModelFactory(
    private val repository: WorkoutRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
            val secureStorage = SecurePasswordStorage(context.applicationContext)
            return WorkoutViewModel(repository, prefs, secureStorage) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
