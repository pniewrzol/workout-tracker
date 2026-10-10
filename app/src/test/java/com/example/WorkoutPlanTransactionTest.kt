package com.example

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Exercise
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkoutPlanTransactionTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: WorkoutRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(context, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `duplicatePlan creates new plan and exercises with new IDs without modifying original`() = runBlocking {
        // 1. Create a source plan
        val planId = repository.createPlan(
            name = "Plan FBW Zaawansowany",
            description = "Oryginalny plan 3-dniowy",
            initialWorkouts = listOf("A" to "Trening A", "B" to "Trening B")
        )

        // Add 2 exercises to source plan
        val ex1Id = repository.createCustomExercise(
            Exercise(
                planId = planId,
                section = "Trening A",
                code = "A.1",
                name = "Przysiad ze sztangą",
                bodyPart = "Nogi",
                equipment = "Sztanga",
                primaryMuscles = "Czworogłowe",
                secondaryMuscles = "Pośladki",
                cues = "Płynny ruch",
                instructions = "Schodź do kąta prostego"
            )
        )

        val ex2Id = repository.createCustomExercise(
            Exercise(
                planId = planId,
                section = "Trening B",
                code = "B.1",
                name = "Wyciskanie żołnierskie OHP",
                bodyPart = "Barki",
                equipment = "Sztanga",
                primaryMuscles = "Barki",
                secondaryMuscles = "Triceps",
                cues = "Brzuch i pośladki spięte",
                instructions = "Wypchnij sztangę nad głowę"
            )
        )

        val sourcePlan = database.workoutPlanDao().getPlanById(planId)!!

        // 2. Duplicate the plan
        val duplicatedPlanId = repository.duplicatePlan(sourcePlan)

        // 3. Verify duplicated plan has a distinct ID
        assertNotEquals(planId, duplicatedPlanId)
        val duplicatedPlan = database.workoutPlanDao().getPlanById(duplicatedPlanId)
        assertNotNull(duplicatedPlan)
        assertEquals("Plan FBW Zaawansowany (Kopia)", duplicatedPlan?.name)

        // 4. Verify original plan remains completely unchanged
        val originalPlanCheck = database.workoutPlanDao().getPlanById(planId)
        assertEquals("Plan FBW Zaawansowany", originalPlanCheck?.name)

        // 5. Verify duplicated exercises have new IDs and point to duplicatedPlanId
        val duplicatedExercises = database.exerciseDao().getAllExercisesList().filter { it.planId == duplicatedPlanId }
        assertEquals(2, duplicatedExercises.size)
        assertTrue(duplicatedExercises.any { it.name == "Przysiad ze sztangą" && it.id != ex1Id })
        assertTrue(duplicatedExercises.any { it.name == "Wyciskanie żołnierskie OHP" && it.id != ex2Id })

        // 6. Verify original exercises still point to the original plan
        val originalExercises = database.exerciseDao().getAllExercisesList().filter { it.planId == planId }
        assertEquals(2, originalExercises.size)
        assertTrue(originalExercises.any { it.id == ex1Id })
        assertTrue(originalExercises.any { it.id == ex2Id })
    }

    @Test
    fun `withTransaction rolls back all operations atomically when an error occurs`() = runBlocking {
        val initialPlansCount = database.workoutPlanDao().getAllPlansList().size
        val initialExercisesCount = database.exerciseDao().getAllExercisesList().size

        try {
            database.withTransaction {
                // Step 1: Insert plan
                val newPlan = WorkoutPlan(
                    name = "Plan tymczasowy (ma ulec cofnięciu)",
                    description = "Test transakcji",
                    createdAt = System.currentTimeMillis(),
                    isActive = false
                )
                val newPlanId = database.workoutPlanDao().insertPlan(newPlan)

                // Step 2: Insert exercise
                database.exerciseDao().insertExercise(
                    Exercise(
                        planId = newPlanId,
                        section = "Trening A",
                        code = "A.1",
                        name = "Martwy ciąg",
                        bodyPart = "Plecy",
                        equipment = "Sztanga",
                        primaryMuscles = "Grzbiet",
                        secondaryMuscles = "Dwugłowe",
                        cues = "Proste plecy",
                        instructions = "Dociągnij do bioder"
                    )
                )

                // Step 3: Trigger intentional failure to test rollback
                throw IllegalStateException("Symulowany błąd w trakcie tworzenia powiązanych rekordów!")
            }
            fail("Oczekiwano wyjątku IllegalStateException")
        } catch (_: IllegalStateException) {
            // Expected
        }

        // Verify atomic rollback: Neither plan nor exercise exists in database
        val afterPlansCount = database.workoutPlanDao().getAllPlansList().size
        val afterExercisesCount = database.exerciseDao().getAllExercisesList().size

        assertEquals("Transakcja musiała wycofać utworzenie planu", initialPlansCount, afterPlansCount)
        assertEquals("Transakcja musiała wycofać utworzenie ćwiczenia", initialExercisesCount, afterExercisesCount)
    }

    @Test
    fun `deletePlan and deleteExercise protect historical workout logs without data loss`() = runBlocking {
        // 1. Create plan and an exercise
        val planId = repository.createPlan("Plan Redukcyjny", "Do usunięcia")
        val exId = repository.createCustomExercise(
            Exercise(
                planId = planId,
                section = "Trening A",
                code = "A.1",
                name = "Dipsy na poręczach",
                bodyPart = "Klatka piersiowa",
                equipment = "Poręcze",
                primaryMuscles = "Triceps",
                secondaryMuscles = "Klatka",
                cues = "Łokcie blisko ciała",
                instructions = "Opuść tułów i wyciśnij"
            )
        )

        // 2. Record historical session and completed set logs with this exercise
        val sessionId = database.workoutDao().insertSession(
            WorkoutSession(
                workoutName = "Trening A - Dipsy",
                startTime = 1700000000000L,
                endTime = 1700003600000L,
                durationSeconds = 3600L
            )
        )

        val setLogId = database.workoutDao().insertSetLog(
            WorkoutSetLog(
                sessionId = sessionId,
                exerciseId = exId,
                setNumber = 1,
                weightKg = 20.0f,
                reps = 10,
                isCompleted = true,
                timestamp = 1700000500000L
            )
        )

        assertEquals(1, database.workoutDao().getSetCountForExerciseDirect(exId))

        // 3. Delete the plan containing this exercise
        val plan = database.workoutPlanDao().getPlanById(planId)!!
        repository.deletePlan(plan)

        // 4. Verify: Historical workout session and set logs MUST STILL EXIST!
        val sessionCheck = database.workoutDao().getSessionByIdDirect(sessionId)
        assertNotNull("Historia sesji treningowej nie może zostać skasowana!", sessionCheck)

        val setLogsCheck = database.workoutDao().getSetsForSessionDirect(sessionId)
        assertEquals("Log serii treningowej nie może zostać usunięty przy usunięciu planu!", 1, setLogsCheck.size)
        assertEquals(setLogId, setLogsCheck.first().id)
        assertEquals(exId, setLogsCheck.first().exerciseId)

        // 5. Delete individual exercise via repository
        val ex = database.exerciseDao().getExerciseByIdDirect(exId)!!
        repository.deleteExercise(ex)

        // 6. Verify: Exercise is archived (planId = -1) rather than hard-deleted with SQLite CASCADE,
        // so historical workout set logs remain 100% intact!
        val setLogsAfterExDelete = database.workoutDao().getSetsForSessionDirect(sessionId)
        assertEquals("Log serii w historii musi pozostać po usunięciu ćwiczenia!", 1, setLogsAfterExDelete.size)
        assertEquals(20.0f, setLogsAfterExDelete.first().weightKg, 0.001f)
    }
}
