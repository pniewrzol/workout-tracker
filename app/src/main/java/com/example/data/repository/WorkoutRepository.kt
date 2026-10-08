package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.BodyMeasurementDao
import com.example.data.db.ExerciseDao
import com.example.data.db.MediaDao
import com.example.data.db.WorkoutDao
import com.example.data.db.WorkoutPlanDao
import com.example.data.model.BodyMeasurement
import com.example.data.model.Exercise
import com.example.data.model.ExerciseMedia
import com.example.data.model.InitialWorkoutData
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class WorkoutRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val exerciseDao: ExerciseDao = database.exerciseDao(),
    private val mediaDao: MediaDao = database.mediaDao(),
    private val workoutDao: WorkoutDao = database.workoutDao(),
    private val bodyMeasurementDao: BodyMeasurementDao = database.bodyMeasurementDao(),
    private val workoutPlanDao: WorkoutPlanDao = database.workoutPlanDao()
) {

    constructor(
        context: Context,
        exerciseDao: ExerciseDao,
        mediaDao: MediaDao,
        workoutDao: WorkoutDao,
        bodyMeasurementDao: BodyMeasurementDao
    ) : this(
        context = context,
        database = AppDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(Dispatchers.IO)),
        exerciseDao = exerciseDao,
        mediaDao = mediaDao,
        workoutDao = workoutDao,
        bodyMeasurementDao = bodyMeasurementDao
    )

    val allExercises: Flow<List<Exercise>> = exerciseDao.getAllExercises()
    val allCompletedSessions: Flow<List<WorkoutSession>> = workoutDao.getCompletedSessions()
    val activeSession: Flow<WorkoutSession?> = workoutDao.getActiveSession()
    val allCompletedSets: Flow<List<WorkoutSetLog>> = workoutDao.getAllCompletedSets()
    val allMeasurements: Flow<List<BodyMeasurement>> = bodyMeasurementDao.getAllMeasurements()
    val allMeasurementsAsc: Flow<List<BodyMeasurement>> = bodyMeasurementDao.getAllMeasurementsAsc()
    val allPlans: Flow<List<WorkoutPlan>> = workoutPlanDao.getAllPlansFlow()
    val activePlan: Flow<WorkoutPlan?> = workoutPlanDao.getActivePlanFlow()

    val backupManager = BackupManager(context, database)

    suspend fun checkAndSeedExercises() = withContext(Dispatchers.IO) {
        // 1. Migrate any existing old codes (e.g. "1.1", "2.8", "3.5a") to new codes ("R.1", "M.8", "A.5a")
        val oldToNewCodeMap = mapOf(
            "1.1" to "R.1", "1.2" to "R.2",
            "2.1" to "M.1", "2.2" to "M.2", "2.3" to "M.3", "2.4" to "M.4", "2.5" to "M.5",
            "2.6" to "M.6", "2.7" to "M.7", "2.8" to "M.8", "2.9" to "M.9", "2.10" to "M.10",
            "3.1" to "A.1", "3.2" to "A.2", "3.3" to "A.3", "3.4" to "A.4", "3.5a" to "A.5a", "3.5b" to "A.5b", "3.6a" to "A.6a", "3.6b" to "A.6b",
            "4.1" to "B.1", "4.2" to "B.2", "4.3" to "B.3", "4.4" to "B.4", "4.5a" to "B.5a", "4.5b" to "B.5b", "4.6a" to "B.6a", "4.6b" to "B.6b", "4.7" to "B.7",
            "5.1a" to "C.1a", "5.1b" to "C.1b", "5.2" to "C.2", "5.3" to "C.3", "5.4a" to "C.4a", "5.4b" to "C.4b", "5.5a" to "C.5a", "5.5b" to "C.5b", "5.6" to "C.6"
        )
        val initialExisting = exerciseDao.getAllExercisesList()
        for (ex in initialExisting) {
            val mappedCode = oldToNewCodeMap[ex.code]
            val isCossack = ex.name.contains("cossack", ignoreCase = true) || ex.code == "2.8" || ex.code == "M.8"
            if (mappedCode != null || isCossack) {
                val newCode = mappedCode ?: "M.8"
                val updatedEx = if (isCossack) {
                    ex.copy(
                        code = "M.8",
                        name = "Cossack squat (knee dominant)",
                        equipment = "Bodyweight",
                        measurementType = "BODYWEIGHT_REPS"
                    )
                } else {
                    ex.copy(code = newCode)
                }
                exerciseDao.updateExercise(updatedEx)
            }
        }

        // 2. Deduplicate any existing exercises in the database
        val existingExercises = exerciseDao.getAllExercisesList()
        val grouped = existingExercises.groupBy { if (it.code.isNotBlank()) it.code else it.name }
        for ((_, group) in grouped) {
            if (group.size > 1) {
                val primary = group.first()
                for (duplicate in group.drop(1)) {
                    workoutDao.repointSetLogsExerciseId(duplicate.id, primary.id)
                    mediaDao.repointMediaExerciseId(duplicate.id, primary.id)
                    exerciseDao.deleteExerciseById(duplicate.id)
                }
            }
        }

        // 3. Fetch fresh list after deduplication
        val currentExercises = exerciseDao.getAllExercisesList()
        val currentByCode = currentExercises.associateBy { it.code }

        val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
        val hasSeeded = prefs.getBoolean("has_initial_seeded_exercises", false)

        // Auto-migrate any Farmer's Walk / Spacer farmera to WEIGHT_AND_DISTANCE
        for (ex in currentExercises) {
            val nm = ex.name.lowercase()
            if ((nm.contains("farmer") || nm.contains("spacer") || nm.contains("carry") || ex.code == "C.1a") && ex.measurementType != "WEIGHT_AND_DISTANCE") {
                exerciseDao.updateExercise(ex.copy(measurementType = "WEIGHT_AND_DISTANCE"))
            }
        }

        // 4. For each default exercise: insert if missing (ONLY during initial setup), update details if present
        for (defEx in InitialWorkoutData.defaultExercises) {
            val existing = currentByCode[defEx.code]
            if (existing == null) {
                if (!hasSeeded) {
                    exerciseDao.insertExercise(defEx)
                }
            } else {
                exerciseDao.updateExercise(
                    existing.copy(
                        name = defEx.name,
                        targetReps = defEx.targetReps,
                        targetSets = defEx.targetSets,
                        measurementType = defEx.measurementType,
                        cues = defEx.cues,
                        instructions = defEx.instructions,
                        equipment = defEx.equipment,
                        section = defEx.section,
                        bodyPart = defEx.bodyPart,
                        primaryMuscles = defEx.primaryMuscles,
                        secondaryMuscles = defEx.secondaryMuscles,
                        restDisplay = defEx.restDisplay,
                        restSeconds = defEx.restSeconds,
                        tempo = defEx.tempo,
                        rir = defEx.rir
                    )
                )
            }
        }
        if (!hasSeeded) {
            prefs.edit().putBoolean("has_initial_seeded_exercises", true).apply()
        }
    }

    fun getExercisesBySection(section: String): Flow<List<Exercise>> {
        return exerciseDao.getExercisesBySection(section)
    }

    fun getExerciseById(id: Long): Flow<Exercise?> {
        return exerciseDao.getExerciseById(id)
    }

    suspend fun getExerciseByIdDirect(id: Long): Exercise? = withContext(Dispatchers.IO) {
        exerciseDao.getExerciseByIdDirect(id)
    }

    suspend fun insertCustomExercise(exercise: Exercise): Long = withContext(Dispatchers.IO) {
        exerciseDao.insertExercise(exercise)
    }

    suspend fun updateExercise(exercise: Exercise) = withContext(Dispatchers.IO) {
        exerciseDao.updateExercise(exercise)
    }

    // Media Handling
    fun getMediaForExercise(exerciseId: Long): Flow<List<ExerciseMedia>> {
        return mediaDao.getMediaForExercise(exerciseId)
    }

    suspend fun saveMediaFile(uri: Uri, isVideo: Boolean, exerciseId: Long, caption: String = ""): ExerciseMedia? = withContext(Dispatchers.IO) {
        try {
            val extension = if (isVideo) ".mp4" else ".jpg"
            val fileName = "exercise_${exerciseId}_${UUID.randomUUID()}$extension"
            val targetDir = File(context.filesDir, "exercise_media")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val destinationFile = File(targetDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val media = ExerciseMedia(
                exerciseId = exerciseId,
                mediaType = if (isVideo) "VIDEO" else "IMAGE",
                uriString = destinationFile.absolutePath,
                caption = caption,
                timestamp = System.currentTimeMillis()
            )
            val newId = mediaDao.insertMedia(media)
            media.copy(id = newId)
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Error saving media file", e)
            null
        }
    }

    suspend fun deleteMedia(media: ExerciseMedia) = withContext(Dispatchers.IO) {
        try {
            val file = File(media.uriString)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Error deleting media file", e)
        }
        mediaDao.deleteMedia(media)
    }

    // Body Measurements
    suspend fun insertMeasurement(measurement: BodyMeasurement): Long = withContext(Dispatchers.IO) {
        bodyMeasurementDao.insert(measurement)
    }

    suspend fun deleteMeasurement(measurement: BodyMeasurement) = withContext(Dispatchers.IO) {
        bodyMeasurementDao.delete(measurement)
    }

    // Backup & Restore
    suspend fun exportBackup(uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        backupManager.exportBackupToUri(uri, password)
    }

    suspend fun restoreBackup(uri: Uri, password: String? = null): RestoreResult = withContext(Dispatchers.IO) {
        backupManager.restoreBackupFromUri(uri, password)
    }

    // Workout Sessions
    suspend fun startWorkoutSession(workoutName: String, initialExercises: List<Exercise>): Long = withContext(Dispatchers.IO) {
        val session = WorkoutSession(
            workoutName = workoutName,
            startTime = System.currentTimeMillis(),
            endTime = 0
        )
        val sessionId = workoutDao.insertSession(session)

        val setLogsToInsert = mutableListOf<WorkoutSetLog>()
        for (exercise in initialExercises) {
            val numSets = if (exercise.targetSets > 0) exercise.targetSets else 1
            // Look for sets completed in previous workout sessions to preserve progressive overload
            val prevCompletedSets = workoutDao.getPreviousCompletedSetsForExercise(exercise.id, sessionId)
            val mostRecentSessionId = prevCompletedSets.firstOrNull()?.sessionId
            val lastSessionSets = if (mostRecentSessionId != null) {
                prevCompletedSets.filter { it.sessionId == mostRecentSessionId }.sortedBy { it.setNumber }
            } else {
                emptyList()
            }

            for (setIndex in 1..numSets) {
                val isDist = exercise.isDistanceBased()
                val isTime = exercise.isTimeBased()
                val isBw = exercise.isBodyweightBased()

                val defDist = if (isDist) parseDistanceMeters(exercise.targetReps, setIndex) else null
                val defTime = if (isTime) parseTimeSeconds(exercise.targetReps, setIndex) else null
                val defReps = if (!isDist && !isTime) parseDefaultReps(exercise.targetReps, setIndex) else 0

                val matchingPrev = lastSessionSets.firstOrNull { it.setNumber == setIndex }
                    ?: lastSessionSets.lastOrNull()

                val initialWeight = if (matchingPrev != null && !isBw) {
                    matchingPrev.weightKg
                } else 0f

                val initialReps = if (matchingPrev != null && matchingPrev.reps > 0) {
                    matchingPrev.reps
                } else defReps

                val initialDist = if (isDist && matchingPrev != null && matchingPrev.distanceMeters != null) {
                    matchingPrev.distanceMeters
                } else defDist

                val initialTime = if (isTime && matchingPrev != null && matchingPrev.timeSeconds != null) {
                    matchingPrev.timeSeconds
                } else defTime

                setLogsToInsert.add(
                    WorkoutSetLog(
                        sessionId = sessionId,
                        exerciseId = exercise.id,
                        setNumber = setIndex,
                        weightKg = initialWeight,
                        reps = initialReps,
                        timeSeconds = initialTime,
                        distanceMeters = initialDist,
                        rir = parseDefaultRir(exercise.rir, setIndex),
                        isCompleted = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        if (setLogsToInsert.isNotEmpty()) {
            workoutDao.insertSetLogs(setLogsToInsert)
        }

        sessionId
    }

    fun parseDistanceMeters(repsString: String, setIndex: Int = 1): Float {
        val tokens = repsString.split(",").map { it.trim() }
        if (tokens.isNotEmpty()) {
            val tokenIndex = (setIndex - 1).coerceIn(0, tokens.size - 1)
            val token = tokens[tokenIndex]
            val digits = Regex("""\b\d+(\.\d+)?\b""").find(token)?.value?.toFloatOrNull()
            return digits ?: if (repsString.contains("500")) 500f else 40f
        }
        val digits = Regex("""\b\d+(\.\d+)?\b""").find(repsString)?.value?.toFloatOrNull()
        return digits ?: if (repsString.contains("500")) 500f else 40f
    }

    fun parseTimeSeconds(repsString: String, setIndex: Int): Int {
        if (repsString.contains("minut", ignoreCase = true) || repsString.contains("min", ignoreCase = true)) {
            val mins = Regex("""\b\d+\b""").find(repsString)?.value?.toIntOrNull() ?: 20
            return mins * 60
        }
        val tokens = repsString.split(",").map { it.trim() }
        if (tokens.isNotEmpty()) {
            val tokenIndex = (setIndex - 1).coerceIn(0, tokens.size - 1)
            val token = tokens[tokenIndex]
            val secs = Regex("""\b\d+\b""").find(token)?.value?.toIntOrNull() ?: 30
            return secs
        }
        return 30
    }

    fun parseDefaultReps(repsString: String, setIndex: Int): Int {
        val tokens = repsString.split(",").map { it.trim() }
        if (tokens.isNotEmpty()) {
            val tokenIndex = (setIndex - 1).coerceIn(0, tokens.size - 1)
            val token = tokens[tokenIndex]
            val firstInt = Regex("""\b\d+\b""").find(token)?.value?.toIntOrNull()
            return firstInt ?: 10
        }
        return 10
    }

    private fun parseDefaultRir(rirString: String, setIndex: Int): Float? {
        if (rirString.contains("3-2-2-1")) {
            return when (setIndex) {
                1 -> 3f
                2 -> 2f
                3 -> 2f
                else -> 1f
            }
        }
        if (rirString.contains("3-2-1")) {
            return when (setIndex) {
                1 -> 3f
                2 -> 2f
                else -> 1f
            }
        }
        if (rirString.contains("3-2-1-1")) {
            return when (setIndex) {
                1 -> 3f
                2 -> 2f
                3 -> 1f
                else -> 1f
            }
        }
        return null
    }

    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSetLog>> {
        return workoutDao.getSetsForSession(sessionId)
    }

    suspend fun updateSetLog(setLog: WorkoutSetLog) = withContext(Dispatchers.IO) {
        workoutDao.updateSetLog(setLog)
    }

    suspend fun insertSetLog(setLog: WorkoutSetLog): Long = withContext(Dispatchers.IO) {
        workoutDao.insertSetLog(setLog)
    }

    suspend fun deleteSetLog(setLog: WorkoutSetLog) = withContext(Dispatchers.IO) {
        workoutDao.deleteSetLog(setLog)
    }

    suspend fun finishWorkoutSession(sessionId: Long, notes: String = "") = withContext(Dispatchers.IO) {
        val session = workoutDao.getSessionByIdDirect(sessionId)
        if (session != null) {
            val now = System.currentTimeMillis()
            val durationSec = (now - session.startTime) / 1000
            val updated = session.copy(
                endTime = now,
                durationSeconds = durationSec,
                notes = notes
            )
            workoutDao.updateSession(updated)

            // If the user didn't toggle individual checkmarks but saved the workout,
            // mark all sets with valid data as completed so their progressive overload
            // is safely preserved and carried over to future workouts.
            val sets = workoutDao.getSetsForSessionDirect(sessionId)
            val hasAnyCompleted = sets.any { it.isCompleted }
            if (!hasAnyCompleted && sets.isNotEmpty()) {
                val autoCompleted = sets.map { it.copy(isCompleted = true, timestamp = now) }
                workoutDao.insertSetLogs(autoCompleted)
            }
        }
    }

    suspend fun discardWorkoutSession(sessionId: Long) = withContext(Dispatchers.IO) {
        val session = workoutDao.getSessionByIdDirect(sessionId)
        if (session != null) {
            workoutDao.deleteSession(session)
        }
    }

    suspend fun getPreviousCompletedSets(exerciseId: Long, currentSessionId: Long): List<WorkoutSetLog> = withContext(Dispatchers.IO) {
        workoutDao.getPreviousCompletedSetsForExercise(exerciseId, currentSessionId)
    }

    fun getCompletedSetsForExercise(exerciseId: Long): Flow<List<WorkoutSetLog>> {
        return workoutDao.getCompletedSetsForExercise(exerciseId)
    }

    suspend fun createPlan(
        name: String,
        description: String = "",
        includeWarmupAndMobility: Boolean = false,
        initialWorkouts: List<Pair<String, String>> = emptyList()
    ): Long = withContext(Dispatchers.IO) {
        val validWorkouts = initialWorkouts.map { it.second.trim() }.filter { it.isNotBlank() }
        val workoutsRawStr = validWorkouts.joinToString(",")
        val plan = WorkoutPlan(
            name = name,
            description = description,
            createdAt = System.currentTimeMillis(),
            isActive = true,
            workoutsRaw = workoutsRawStr
        )
        val id = workoutPlanDao.insertPlan(plan)
        workoutPlanDao.setActivePlan(id)

        // As requested by user: No exercises are created automatically. The new plan starts completely empty.

        id
    }

    suspend fun setActivePlan(planId: Long) = withContext(Dispatchers.IO) {
        workoutPlanDao.setActivePlan(planId)
    }

    suspend fun deletePlan(plan: WorkoutPlan) = withContext(Dispatchers.IO) {
        workoutPlanDao.deletePlan(plan)
        exerciseDao.deleteExercisesByPlanId(plan.id)
        val remaining = workoutPlanDao.getAllPlansList()
        if (remaining.isNotEmpty()) {
            workoutPlanDao.setActivePlan(remaining.first().id)
        } else {
            val freshDefault = WorkoutPlan(
                name = "Nowy Plan Treningowy",
                description = "Czysty plan treningowy",
                createdAt = System.currentTimeMillis(),
                isActive = true,
                workoutsRaw = "Trening A,Trening B,Trening C"
            )
            val newId = workoutPlanDao.insertPlan(freshDefault)
            workoutPlanDao.setActivePlan(newId)
        }
    }

    suspend fun deleteWorkoutFromPlan(plan: WorkoutPlan, workoutSection: String) = withContext(Dispatchers.IO) {
        val currentList = plan.workoutsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val updatedList = currentList.filterNot { it.equals(workoutSection, ignoreCase = true) }
        val updatedPlan = plan.copy(workoutsRaw = updatedList.joinToString(","))
        workoutPlanDao.updatePlan(updatedPlan)
        exerciseDao.deleteExercisesBySectionAndPlanId(workoutSection, plan.id)
    }

    suspend fun updatePlan(plan: WorkoutPlan) = withContext(Dispatchers.IO) {
        workoutPlanDao.updatePlan(plan)
    }

    suspend fun deleteCategory(section: String) = withContext(Dispatchers.IO) {
        exerciseDao.deleteExercisesBySection(section)
    }

    suspend fun createCustomExercise(exercise: Exercise): Long = withContext(Dispatchers.IO) {
        exerciseDao.insertExercise(exercise)
    }

    suspend fun deleteExercise(exercise: Exercise) = withContext(Dispatchers.IO) {
        exerciseDao.deleteExercise(exercise)
    }

    suspend fun deleteExercises(exercises: List<Exercise>) = withContext(Dispatchers.IO) {
        exerciseDao.deleteExercises(exercises)
    }

    suspend fun deleteExerciseById(id: Long) = withContext(Dispatchers.IO) {
        exerciseDao.deleteExerciseById(id)
    }

    suspend fun insertBodyMeasurement(measurement: BodyMeasurement): Long = withContext(Dispatchers.IO) {
        bodyMeasurementDao.insert(measurement)
    }

    suspend fun updateBodyMeasurement(measurement: BodyMeasurement) = withContext(Dispatchers.IO) {
        bodyMeasurementDao.update(measurement)
    }

    suspend fun deleteBodyMeasurement(measurement: BodyMeasurement) = withContext(Dispatchers.IO) {
        bodyMeasurementDao.delete(measurement)
    }
}
