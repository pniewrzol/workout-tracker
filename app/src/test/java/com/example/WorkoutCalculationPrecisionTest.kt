package com.example

import com.example.data.model.Exercise
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import com.example.ui.components.ReportPeriod
import com.example.ui.components.buildReportData
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutCalculationPrecisionTest {

    @Test
    fun `tonnage calculation preserves decimal weight fractions without premature truncation`() {
        // Example: 20 sets of 20.5 kg with 5 reps each
        // 20.5 * 5 = 102.5 kg per set.
        // If truncated to Long per set: 102 * 20 = 2040 kg (loss of 10 kg).
        // Correct calculation with Double precision: 102.5 * 20 = 2050 kg!

        val exercise = Exercise(
            id = 1L,
            section = "Trening A",
            code = "A.1",
            name = "Wyciskanie hantli skos dodatni",
            bodyPart = "Klatka piersiowa",
            equipment = "Hantle",
            primaryMuscles = "Góra klatki",
            secondaryMuscles = "Barki",
            cues = "Płynnie",
            instructions = "Wyciskaj w górę"
        )

        val session = WorkoutSession(
            id = 10L,
            workoutName = "Trening Klatki",
            startTime = 1700000000000L,
            endTime = 1700003600000L,
            durationSeconds = 3600L
        )

        val sets = (1..20).map { setNum ->
            WorkoutSetLog(
                id = setNum.toLong(),
                sessionId = 10L,
                exerciseId = 1L,
                setNumber = setNum,
                weightKg = 20.5f,
                reps = 5,
                isCompleted = true,
                timestamp = 1700000000000L + setNum * 60000L
            )
        }

        val reportData = buildReportData(
            period = ReportPeriod.ALL,
            sessions = listOf(session),
            allSets = sets,
            allExercises = listOf(exercise),
            allMeasurements = emptyList()
        )

        // Expected tonnage: 20 * (20.5 * 5) = 2050 kg
        assertEquals(2050L, reportData.totalTonnageKg)
        assertEquals(20, reportData.totalSetsCount)
        assertEquals(100, reportData.totalRepsCount)

        val sessionReport = reportData.sessionsList.first()
        assertEquals(2050L, sessionReport.tonnageKg)

        val topExercise = reportData.topExercisesList.first()
        assertEquals(2050L, topExercise.totalTonnageKg)
        assertEquals(20.5f, topExercise.maxWeightKg, 0.001f)
    }

    @Test
    fun `tonnage calculation handles fractional micro-plates like 1,25 kg and 2,5 kg accurately`() {
        val exercise = Exercise(
            id = 2L,
            section = "Trening B",
            code = "B.1",
            name = "Uginanie przedramion ze sztangą",
            bodyPart = "Ramiona",
            equipment = "Gryf łamany",
            primaryMuscles = "Biceps",
            secondaryMuscles = "Przedramiona",
            cues = "Łokcie nieruchomo",
            instructions = "Ugnij przedramiona"
        )

        val session = WorkoutSession(
            id = 20L,
            workoutName = "Trening Biceps",
            startTime = 1700000000000L,
            endTime = 1700002000000L,
            durationSeconds = 2000L
        )

        // Set 1: 32.5 kg * 8 = 260.0 kg
        // Set 2: 35.0 kg * 6 = 210.0 kg
        // Set 3: 37.5 kg * 4 = 150.0 kg
        // Total = 620.0 kg
        val sets = listOf(
            WorkoutSetLog(id = 1L, sessionId = 20L, exerciseId = 2L, setNumber = 1, weightKg = 32.5f, reps = 8, isCompleted = true),
            WorkoutSetLog(id = 2L, sessionId = 20L, exerciseId = 2L, setNumber = 2, weightKg = 35.0f, reps = 6, isCompleted = true),
            WorkoutSetLog(id = 3L, sessionId = 20L, exerciseId = 2L, setNumber = 3, weightKg = 37.5f, reps = 4, isCompleted = true)
        )

        val reportData = buildReportData(
            period = ReportPeriod.ALL,
            sessions = listOf(session),
            allSets = sets,
            allExercises = listOf(exercise),
            allMeasurements = emptyList()
        )

        assertEquals(620L, reportData.totalTonnageKg)
        assertEquals(3, reportData.totalSetsCount)
        assertEquals(18, reportData.totalRepsCount)
    }

    @Test
    fun `empty history and boundary values calculate safely without crashes`() {
        // Completely empty history
        val emptyReport = buildReportData(
            period = ReportPeriod.ALL,
            sessions = emptyList(),
            allSets = emptyList(),
            allExercises = emptyList(),
            allMeasurements = emptyList()
        )

        assertEquals(0L, emptyReport.totalTonnageKg)
        assertEquals(0, emptyReport.totalSetsCount)
        assertEquals(0, emptyReport.totalRepsCount)
        assertEquals(0, emptyReport.completedWorkoutsCount)
        assertEquals(emptyList<Any>(), emptyReport.sessionsList)
        assertEquals(emptyList<Any>(), emptyReport.topExercisesList)

        // Boundary value: 0 reps or bodyweight (0.0 kg)
        val ex = Exercise(id = 3L, section = "C", code = "C.1", name = "Podciąganie", bodyPart = "Plecy", equipment = "Drążek", primaryMuscles = "Najszerszy", secondaryMuscles = "Biceps", cues = "", instructions = "")
        val s = WorkoutSession(id = 30L, workoutName = "Drążek", startTime = 1700000000000L, endTime = 1700001000000L)
        val zeroWeightSets = listOf(
            WorkoutSetLog(id = 100L, sessionId = 30L, exerciseId = 3L, setNumber = 1, weightKg = 0.0f, reps = 12, isCompleted = true),
            WorkoutSetLog(id = 101L, sessionId = 30L, exerciseId = 3L, setNumber = 2, weightKg = 0.0f, reps = 10, isCompleted = true)
        )

        val bwReport = buildReportData(
            period = ReportPeriod.ALL,
            sessions = listOf(s),
            allSets = zeroWeightSets,
            allExercises = listOf(ex),
            allMeasurements = emptyList()
        )

        assertEquals(0L, bwReport.totalTonnageKg)
        assertEquals(2, bwReport.totalSetsCount)
        assertEquals(22, bwReport.totalRepsCount)
    }
}
