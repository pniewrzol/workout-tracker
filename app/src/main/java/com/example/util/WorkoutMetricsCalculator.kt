package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSetLog

data class MuscleVolumeGroup(
    val name: String,
    val setCount: Int,
    val statusText: String,
    val statusColor: Color,
    val progress: Float
)

object WorkoutMetricsCalculator {

    fun calculateCalories(
        durationSeconds: Long,
        totalTonnageKg: Float,
        completedSetsCount: Int,
        userWeightKg: Float? = null
    ): Int {
        val weight = userWeightKg ?: 75f
        val durationHours = durationSeconds.coerceAtLeast(60L) / 3600f
        // Base MET for resistance training: ~5.0 to 6.0
        val baseMet = if (totalTonnageKg > 5000f) 6.0f else 5.2f
        val baseCalories = baseMet * weight * durationHours
        // Work bonus based on volume tonnage lifted: ~4.5 kcal per 1000 kg moved
        val workBonus = (totalTonnageKg / 1000f) * 4.5f
        val total = (baseCalories + workBonus).toInt()
        val minRealistic = (durationSeconds / 60L * 3.5f).toInt().coerceAtLeast(35)
        return total.coerceAtLeast(minRealistic)
    }

    fun calculateIntensity(
        durationSeconds: Long,
        totalTonnageKg: Float,
        completedSetsCount: Int
    ): Pair<String, Color> {
        val durationMinutes = (durationSeconds / 60L).coerceAtLeast(1L)
        val kgPerMinute = totalTonnageKg / durationMinutes.toFloat()
        return when {
            kgPerMinute >= 150f || (completedSetsCount >= 22 && durationMinutes <= 65) -> {
                "Wysoka" to Color(0xFFEF4444)
            }
            kgPerMinute >= 75f || completedSetsCount >= 12 -> {
                "Umiarkowana" to Color(0xFFF59E0B)
            }
            else -> {
                "Lekka" to Color(0xFF10B981)
            }
        }
    }

    fun calculateWeeklyMuscleVolume(
        setsLast7Days: List<WorkoutSetLog>,
        exerciseMap: Map<Long, Exercise>
    ): List<MuscleVolumeGroup> {
        val chestCount = countSetsForCategory(setsLast7Days, exerciseMap) { bp, pm, name ->
            bp.contains("klatka") || pm.contains("klatka") || pm.contains("piersiow") || name.contains("wyciskanie leżąc") || name.contains("rozpiętki") || name.contains("dips")
        }
        val backCount = countSetsForCategory(setsLast7Days, exerciseMap) { bp, pm, name ->
            bp.contains("plecy") || pm.contains("plecy") || pm.contains("najszerszy") || pm.contains("czwórboczny") || name.contains("wiosłowan") || name.contains("podciągan") || name.contains("ściągan") || name.contains("martwy ciąg")
        }
        val legsCount = countSetsForCategory(setsLast7Days, exerciseMap) { bp, pm, name ->
            bp.contains("nogi") || bp.contains("uda") || bp.contains("poślad") || pm.contains("czworogłow") || pm.contains("dwugłow") || pm.contains("poślad") || name.contains("przysiad") || name.contains("suwnic") || name.contains("wykroki") || name.contains("hip thrust")
        }
        val shouldersCount = countSetsForCategory(setsLast7Days, exerciseMap) { bp, pm, name ->
            bp.contains("barki") || pm.contains("naramien") || pm.contains("barki") || name.contains("ohp") || name.contains("żołnierskie") || name.contains("wznosy")
        }
        val armsCount = countSetsForCategory(setsLast7Days, exerciseMap) { bp, pm, name ->
            bp.contains("ramiona") || pm.contains("biceps") || pm.contains("triceps") || name.contains("uginanie") || name.contains("francuskie") || name.contains("prostowanie")
        }
        val coreCount = countSetsForCategory(setsLast7Days, exerciseMap) { bp, pm, name ->
            bp.contains("brzuch") || bp.contains("core") || pm.contains("brzuch") || name.contains("plank") || name.contains("brzuszki") || name.contains("allah")
        }

        val rawGroups = listOf(
            "Klatka piersiowa" to chestCount,
            "Plecy" to backCount,
            "Nogi & Pośladki" to legsCount,
            "Barki" to shouldersCount,
            "Ramiona (Bic/Tric)" to armsCount,
            "Brzuch / Core" to coreCount
        )

        return rawGroups.map { (name, count) ->
            val progress = (count / 20f).coerceIn(0f, 1f)
            val (statusText, statusColor) = when {
                count < 6 -> "Niska objętość (<6)" to Color(0xFF94A3B8)
                count in 6..11 -> "Umiarkowana (6–11)" to Color(0xFF38BDF8)
                count in 12..20 -> "Optymalna (12–20)" to Color(0xFF22C55E)
                else -> "Wysoka (>20)" to Color(0xFFF97316)
            }
            MuscleVolumeGroup(
                name = name,
                setCount = count,
                statusText = statusText,
                statusColor = statusColor,
                progress = progress
            )
        }
    }

    private inline fun countSetsForCategory(
        sets: List<WorkoutSetLog>,
        exerciseMap: Map<Long, Exercise>,
        matcher: (bodyPart: String, primaryMuscles: String, name: String) -> Boolean
    ): Int {
        return sets.count { setLog ->
            val ex = exerciseMap[setLog.exerciseId]
            if (ex == null) false
            else {
                val bp = ex.bodyPart.lowercase()
                val pm = ex.primaryMuscles.lowercase()
                val nm = ex.name.lowercase()
                matcher(bp, pm, nm)
            }
        }
    }
}
