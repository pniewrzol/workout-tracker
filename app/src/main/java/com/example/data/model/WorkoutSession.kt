package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutName: String, // "Trening A", "Trening B", "Trening C", "Rozgrzewka i Mobilizacja", etc.
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0, // 0 means currently ongoing
    val notes: String = "",
    val durationSeconds: Long = 0
) {
    val isMainWorkout: Boolean
        get() = !workoutName.contains("Rozgrzewka", ignoreCase = true) &&
                !workoutName.contains("Mobilizacja", ignoreCase = true)
}
