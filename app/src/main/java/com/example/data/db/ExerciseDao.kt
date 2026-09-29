package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY id ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises ORDER BY id ASC")
    suspend fun getAllExercisesList(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE section = :section ORDER BY id ASC")
    fun getExercisesBySection(section: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    fun getExerciseById(id: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getExerciseByIdDirect(id: Long): Exercise?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT * FROM exercises WHERE code = :code LIMIT 1")
    suspend fun getExerciseByCode(code: String): Exercise?

    @Query("UPDATE exercises SET name = :name, targetReps = :targetReps, measurementType = :measurementType, cues = :cues, instructions = :instructions, equipment = :equipment WHERE code = :code")
    suspend fun updateExerciseDetailsByCode(
        code: String,
        name: String,
        targetReps: String,
        measurementType: String,
        cues: String,
        instructions: String,
        equipment: String
    )

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Long)
}
