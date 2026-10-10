package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsDirect(): List<WorkoutSession>

    @Query("SELECT * FROM workout_set_logs ORDER BY id ASC")
    suspend fun getAllSetLogsDirect(): List<WorkoutSetLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<WorkoutSession>)

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE endTime > 0 ORDER BY startTime DESC")
    fun getCompletedSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE endTime = 0 ORDER BY startTime DESC LIMIT 1")
    fun getActiveSession(): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionById(sessionId: Long): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionByIdDirect(sessionId: Long): WorkoutSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Delete
    suspend fun deleteSession(session: WorkoutSession)

    // Set logs
    @Query("SELECT * FROM workout_set_logs WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSetLog>>

    @Query("SELECT * FROM workout_set_logs WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getSetsForSessionDirect(sessionId: Long): List<WorkoutSetLog>

    @Query("SELECT * FROM workout_set_logs WHERE exerciseId = :exerciseId AND isCompleted = 1 ORDER BY timestamp ASC")
    fun getCompletedSetsForExercise(exerciseId: Long): Flow<List<WorkoutSetLog>>

    @Query("SELECT COUNT(*) FROM workout_set_logs WHERE exerciseId = :exerciseId")
    suspend fun getSetCountForExerciseDirect(exerciseId: Long): Int

    @Query("SELECT * FROM workout_set_logs WHERE isCompleted = 1 ORDER BY timestamp ASC")
    fun getAllCompletedSets(): Flow<List<WorkoutSetLog>>

    @Query("""
        SELECT * FROM workout_set_logs 
        WHERE exerciseId = :exerciseId AND isCompleted = 1 
        AND sessionId != :currentSessionId
        ORDER BY timestamp DESC
    """)
    suspend fun getPreviousCompletedSetsForExercise(exerciseId: Long, currentSessionId: Long): List<WorkoutSetLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(setLog: WorkoutSetLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLogs(setLogs: List<WorkoutSetLog>)

    @Update
    suspend fun updateSetLog(setLog: WorkoutSetLog)

    @Delete
    suspend fun deleteSetLog(setLog: WorkoutSetLog)

    @Query("DELETE FROM workout_set_logs WHERE id = :id")
    suspend fun deleteSetLogById(id: Long)

    @Query("UPDATE workout_set_logs SET exerciseId = :targetExerciseId WHERE exerciseId = :sourceExerciseId")
    suspend fun repointSetLogsExerciseId(sourceExerciseId: Long, targetExerciseId: Long)
}
