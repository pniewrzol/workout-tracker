package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.BodyMeasurementDao
import com.example.data.db.ExerciseDao
import com.example.data.db.MediaDao
import com.example.data.db.WorkoutDao
import com.example.data.model.BodyMeasurement
import com.example.data.model.Exercise
import com.example.data.model.ExerciseMedia
import com.example.data.model.InitialWorkoutData
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
    private val exerciseDao: ExerciseDao,
    private val mediaDao: MediaDao,
    private val workoutDao: WorkoutDao,
    private val bodyMeasurementDao: BodyMeasurementDao
) {

    val allExercises: Flow<List<Exercise>> = exerciseDao.getAllExercises()
    val allCompletedSessions: Flow<List<WorkoutSession>> = workoutDao.getCompletedSessions()
    val activeSession: Flow<WorkoutSession?> = workoutDao.getActiveSession()
    val allCompletedSets: Flow<List<WorkoutSetLog>> = workoutDao.getAllCompletedSets()
    val allMeasurements: Flow<List<BodyMeasurement>> = bodyMeasurementDao.getAllMeasurements()
    val allMeasurementsAsc: Flow<List<BodyMeasurement>> = bodyMeasurementDao.getAllMeasurementsAsc()

    val backupManager = BackupManager(context, exerciseDao, workoutDao, bodyMeasurementDao)

    suspend fun checkAndSeedExercises() = withContext(Dispatchers.IO) {
        // 1. Deduplicate any existing exercises in the database (handles instances where exercises were doubled)
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

        // 2. Fetch fresh list after deduplication
        val currentExercises = exerciseDao.getAllExercisesList()
        val currentByCode = currentExercises.associateBy { it.code }

        // 3. For each default exercise: insert if missing, update details if present
        for (defEx in InitialWorkoutData.defaultExercises) {
            val existing = currentByCode[defEx.code]
            if (existing == null) {
                exerciseDao.insertExercise(defEx)
            } else {
                exerciseDao.updateExerciseDetailsByCode(
                    code = defEx.code,
                    name = defEx.name,
                    targetReps = defEx.targetReps,
                    measurementType = defEx.measurementType,
                    cues = defEx.cues,
                    instructions = defEx.instructions,
                    equipment = defEx.equipment
                )
            }
        }

        // 4. Clean up any historical or active set logs where non-distance/non-time exercises were corrupted with distance or 0 reps
        sanitizeSetLogs()
    }

    private suspend fun sanitizeSetLogs() {
        val exercisesMap = exerciseDao.getAllExercisesList().associateBy { it.id }
        val allSets = workoutDao.getAllSetLogsDirect()
        for (set in allSets) {
            val ex = exercisesMap[set.exerciseId] ?: continue
            if (ex.isWeightAndReps) {
                var needsUpdate = false
                var newDist = set.distanceMeters
                var newTime = set.timeSeconds
                var newReps = set.reps
                if (set.distanceMeters != null) {
                    newDist = null
                    needsUpdate = true
                }
                if (set.timeSeconds != null) {
                    newTime = null
                    needsUpdate = true
                }
                if (set.reps <= 0) {
                    newReps = parseDefaultReps(ex.targetReps, set.setNumber)
                    needsUpdate = true
                }
                if (needsUpdate) {
                    workoutDao.updateSetLog(
                        set.copy(
                            distanceMeters = newDist,
                            timeSeconds = newTime,
                            reps = newReps
                        )
                    )
                }
            }
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
            e.printStackTrace()
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
            e.printStackTrace()
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
    suspend fun exportBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        backupManager.exportBackupToUri(uri)
    }

    suspend fun restoreBackup(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        backupManager.restoreBackupFromUri(uri)
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
}
