package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ExerciseMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM exercise_media WHERE exerciseId = :exerciseId ORDER BY timestamp DESC")
    fun getMediaForExercise(exerciseId: Long): Flow<List<ExerciseMedia>>

    @Query("SELECT * FROM exercise_media ORDER BY timestamp DESC")
    fun getAllMedia(): Flow<List<ExerciseMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: ExerciseMedia): Long

    @Delete
    suspend fun deleteMedia(media: ExerciseMedia)

    @Query("DELETE FROM exercise_media WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("UPDATE exercise_media SET exerciseId = :targetExerciseId WHERE exerciseId = :sourceExerciseId")
    suspend fun repointMediaExerciseId(sourceExerciseId: Long, targetExerciseId: Long)
}
