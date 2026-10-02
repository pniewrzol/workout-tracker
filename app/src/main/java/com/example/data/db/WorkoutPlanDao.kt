package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {

    @Query("SELECT * FROM workout_plans ORDER BY id ASC")
    fun getAllPlansFlow(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_plans ORDER BY id ASC")
    suspend fun getAllPlansList(): List<WorkoutPlan>

    @Query("SELECT * FROM workout_plans WHERE isActive = 1 LIMIT 1")
    fun getActivePlanFlow(): Flow<WorkoutPlan?>

    @Query("SELECT * FROM workout_plans WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlanDirect(): WorkoutPlan?

    @Query("SELECT * FROM workout_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: Long): WorkoutPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlan): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<WorkoutPlan>)

    @Update
    suspend fun updatePlan(plan: WorkoutPlan)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlan)

    @Query("UPDATE workout_plans SET isActive = CASE WHEN id = :activePlanId THEN 1 ELSE 0 END")
    suspend fun setActivePlan(activePlanId: Long)

    @Query("SELECT COUNT(*) FROM workout_plans")
    suspend fun getCount(): Int
}
