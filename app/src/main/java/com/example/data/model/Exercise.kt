package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ExerciseType {
    WEIGHT_AND_REPS,
    BODYWEIGHT_REPS,
    TIME,
    DISTANCE
}

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val section: String, // "Rozgrzewka", "Mobilizacja", "Trening A", "Trening B", "Trening C"
    val code: String, // "1.1", "2.1", "3.1", "3.5a", etc.
    val name: String,
    val targetSets: Int = 4,
    val targetReps: String = "10,9,8,7",
    val restDisplay: String = "120s",
    val restSeconds: Int = 120,
    val tempo: String = "-",
    val rir: String = "malejące (3-2-2-1)",
    val bodyPart: String,
    val equipment: String,
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val cues: String,
    val instructions: String,
    val isCustom: Boolean = false,
    val measurementType: String = "WEIGHT_AND_REPS", // "WEIGHT_AND_REPS", "BODYWEIGHT_REPS", "TIME", "DISTANCE"
    val planId: Long = 1L
) {
    fun isDistanceBased(): Boolean {
        if (measurementType == "DISTANCE") return true
        if (measurementType == "TIME" || measurementType == "WEIGHT_AND_REPS" || measurementType == "BODYWEIGHT_REPS") return false
        val distanceRegex = Regex("""(^|[,\s])\d+(\.\d+)?\s*m($|[,\s])""", RegexOption.IGNORE_CASE)
        return distanceRegex.containsMatchIn(targetReps)
    }

    fun isTimeBased(): Boolean {
        if (measurementType == "TIME") return true
        if (measurementType == "DISTANCE" || measurementType == "WEIGHT_AND_REPS" || measurementType == "BODYWEIGHT_REPS") return false
        val timeRegex = Regex("""(^|[,\s])\d+(\.\d+)?\s*(s|sek|sekund|min|minut)($|[,\s])""", RegexOption.IGNORE_CASE)
        return timeRegex.containsMatchIn(targetReps)
    }

    fun isBodyweightBased(): Boolean {
        if (measurementType == "BODYWEIGHT_REPS") return true
        if (measurementType == "DISTANCE" || measurementType == "TIME") return false
        if (measurementType == "WEIGHT_AND_REPS" && equipment.isNotBlank()) {
            val eq = equipment.lowercase()
            val isExplicitWeight = eq.contains("dumbbell") || eq.contains("barbell") || 
                                   eq.contains("cable") || eq.contains("machine") || 
                                   eq.contains("plate") || eq.contains("ez bar") || eq.contains("trap bar")
            if (isExplicitWeight) return false
            return eq.contains("bodyweight") || eq.contains("stick") || 
                   eq.contains("stability ball") || eq.contains("ab wheel") || 
                   eq.contains("pull-up bar") || eq.contains("mata") || eq.contains("band")
        }
        return false
    }

    fun getEffectiveMeasurementType(): ExerciseType {
        return when {
            isDistanceBased() -> ExerciseType.DISTANCE
            isTimeBased() -> ExerciseType.TIME
            isBodyweightBased() -> ExerciseType.BODYWEIGHT_REPS
            else -> ExerciseType.WEIGHT_AND_REPS
        }
    }

    val isDistance: Boolean get() = getEffectiveMeasurementType() == ExerciseType.DISTANCE
    val isTime: Boolean get() = getEffectiveMeasurementType() == ExerciseType.TIME
    val isBodyweight: Boolean get() = getEffectiveMeasurementType() == ExerciseType.BODYWEIGHT_REPS
    val isWeightAndReps: Boolean get() = getEffectiveMeasurementType() == ExerciseType.WEIGHT_AND_REPS

    val isWarmupOrMobility: Boolean
        get() = section.contains("Rozgrzewka", ignoreCase = true) ||
                section.contains("Mobilizacja", ignoreCase = true) ||
                section.contains("Warmup", ignoreCase = true) ||
                section.contains("Mobility", ignoreCase = true) ||
                code.startsWith("R.", ignoreCase = true) ||
                code.startsWith("M.", ignoreCase = true) ||
                code.startsWith("1.", ignoreCase = true) ||
                code.startsWith("2.", ignoreCase = true)
}
