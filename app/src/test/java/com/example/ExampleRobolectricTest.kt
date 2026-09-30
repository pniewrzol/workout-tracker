package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.repository.WorkoutRepository
import androidx.room.Room
import com.example.data.model.BodyMeasurement
import com.example.data.model.WorkoutSetLog
import com.example.data.model.InitialWorkoutData
import com.example.data.repository.RestoreResult
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Trening Tracker", appName)
  }

  @Test
  fun `initial exercises count is complete`() {
    val exercises = InitialWorkoutData.defaultExercises
    assertTrue(exercises.isNotEmpty())
    assertEquals(38, exercises.size)

    val warmupExercises = exercises.filter { it.section == "Rozgrzewka" }
    assertEquals(2, warmupExercises.size)

    val mobilityExercises = exercises.filter { it.section == "Mobilizacja" }
    assertEquals(10, mobilityExercises.size)

    val trainingAExercises = exercises.filter { it.section == "Trening A" }
    assertEquals(8, trainingAExercises.size)

    val trainingBExercises = exercises.filter { it.section == "Trening B" }
    assertEquals(9, trainingBExercises.size)

    val trainingCExercises = exercises.filter { it.section == "Trening C" }
    assertEquals(9, trainingCExercises.size)
  }

  @Test
  fun `body measurement model stores values correctly`() {
    val measurement = BodyMeasurement(
      id = 1L,
      timestamp = System.currentTimeMillis(),
      weightKg = 82.5f,
      bodyFatPercentage = 14.5f,
      chestCm = 104f,
      waistCm = 84f,
      bicepsCm = 38.5f,
      notes = "Rano na czczo"
    )

    assertEquals(82.5f, measurement.weightKg)
    assertEquals(14.5f, measurement.bodyFatPercentage)
    assertEquals(104f, measurement.chestCm)
    assertEquals(84f, measurement.waistCm)
    assertEquals(38.5f, measurement.bicepsCm)
    assertEquals("Rano na czczo", measurement.notes)
  }

  @Test
  fun `distance and time exercises are categorized correctly`() {
    val exercises = InitialWorkoutData.defaultExercises

    val rowErg = exercises.first { it.code == "R.1" }
    assertTrue(rowErg.isDistanceBased())
    assertEquals(com.example.data.model.ExerciseType.DISTANCE, rowErg.getEffectiveMeasurementType())

    val airBike = exercises.first { it.code == "R.2" }
    assertTrue(airBike.isDistanceBased())
    assertEquals(com.example.data.model.ExerciseType.DISTANCE, airBike.getEffectiveMeasurementType())

    val sidePlank = exercises.first { it.code == "A.6b" }
    assertTrue(sidePlank.isTimeBased())
    assertEquals(com.example.data.model.ExerciseType.TIME, sidePlank.getEffectiveMeasurementType())

    val stairs = exercises.first { it.code == "B.7" }
    assertTrue(stairs.isTimeBased())
    assertEquals(com.example.data.model.ExerciseType.TIME, stairs.getEffectiveMeasurementType())

    val farmersWalk = exercises.first { it.code == "C.1a" }
    assertTrue(farmersWalk.isDistanceBased())
    assertEquals(com.example.data.model.ExerciseType.DISTANCE, farmersWalk.getEffectiveMeasurementType())

    val chinUpHold = exercises.first { it.code == "C.3" }
    assertTrue(chinUpHold.isTimeBased())
    assertEquals(com.example.data.model.ExerciseType.TIME, chinUpHold.getEffectiveMeasurementType())

    val standardEx = exercises.first { it.code == "A.4" }
    assertEquals(com.example.data.model.ExerciseType.WEIGHT_AND_REPS, standardEx.getEffectiveMeasurementType())

    // Crucial check: verify that ONLY the 3 distance exercises are distance-based
    val distanceExercises = exercises.filter { it.isDistanceBased() }
    assertEquals(3, distanceExercises.size)
    assertEquals(setOf("R.1", "R.2", "C.1a"), distanceExercises.map { it.code }.toSet())

    // Crucial check: verify that ONLY the 3 time exercises are time-based
    val timeExercises = exercises.filter { it.isTimeBased() }
    assertEquals(3, timeExercises.size)
    assertEquals(setOf("A.6b", "B.7", "C.3"), timeExercises.map { it.code }.toSet())

    // Exercises with bodyweight are categorized as BODYWEIGHT_REPS (reps only)
    val worldsGreatest = exercises.first { it.code == "M.2" }
    assertEquals(com.example.data.model.ExerciseType.BODYWEIGHT_REPS, worldsGreatest.getEffectiveMeasurementType())
    assertTrue(worldsGreatest.isBodyweightBased())
    org.junit.Assert.assertFalse(worldsGreatest.isDistanceBased())

    val couchStretch = exercises.first { it.code == "M.5" }
    assertEquals(com.example.data.model.ExerciseType.BODYWEIGHT_REPS, couchStretch.getEffectiveMeasurementType())
    assertTrue(couchStretch.isBodyweightBased())
    org.junit.Assert.assertFalse(couchStretch.isDistanceBased())

    // 2.8 / M.8 Cossack squat is strictly bodyweight reps only
    val cossackSquat = exercises.first { it.code == "M.8" }
    assertEquals(com.example.data.model.ExerciseType.BODYWEIGHT_REPS, cossackSquat.getEffectiveMeasurementType())
    assertTrue(cossackSquat.isBodyweightBased())
    org.junit.Assert.assertFalse(cossackSquat.isDistanceBased())
    org.junit.Assert.assertFalse(cossackSquat.isTimeBased())

    val plateGoodMorning = exercises.first { it.code == "M.7" }
    assertEquals(com.example.data.model.ExerciseType.WEIGHT_AND_REPS, plateGoodMorning.getEffectiveMeasurementType())
    org.junit.Assert.assertFalse(plateGoodMorning.isTimeBased())
    org.junit.Assert.assertFalse(plateGoodMorning.isBodyweightBased())
  }

  @Test
  fun `warmup and mobility do not count as main workouts`() {
    val mainA = com.example.data.model.WorkoutSession(id = 1, workoutName = "Trening A")
    val mainB = com.example.data.model.WorkoutSession(id = 2, workoutName = "Trening B")
    val mainC = com.example.data.model.WorkoutSession(id = 3, workoutName = "Trening C")
    val warmupMobility = com.example.data.model.WorkoutSession(id = 4, workoutName = "Rozgrzewka i Mobilizacja")

    assertTrue(mainA.isMainWorkout)
    assertTrue(mainB.isMainWorkout)
    assertTrue(mainC.isMainWorkout)
    org.junit.Assert.assertFalse(warmupMobility.isMainWorkout)

    // Test volume and sets filtering
    val completedSessions = listOf(mainA, warmupMobility)
    val mainSessionIds = completedSessions.filter { it.isMainWorkout }.map { it.id }.toSet()

    val warmupExercise = InitialWorkoutData.defaultExercises.first { it.code == "R.1" }.copy(id = 101L)
    val mainExercise = InitialWorkoutData.defaultExercises.first { it.code == "A.4" }.copy(id = 202L)

    val setFromMain = WorkoutSetLog(sessionId = 1, exerciseId = mainExercise.id, setNumber = 1, weightKg = 50f, reps = 10, isCompleted = true)
    val setFromWarmup = WorkoutSetLog(sessionId = 4, exerciseId = warmupExercise.id, setNumber = 1, weightKg = 0f, distanceMeters = 500f, reps = 0, isCompleted = true)

    val allCompletedSets = listOf(setFromMain, setFromWarmup)
    val warmupOrMobilityExerciseIds = setOf(warmupExercise.id)

    val mainWorkoutCompletedSets = allCompletedSets.filter { 
        it.sessionId in mainSessionIds && it.exerciseId !in warmupOrMobilityExerciseIds 
    }

    assertEquals(1, mainWorkoutCompletedSets.size)
    val totalVolumeKg = mainWorkoutCompletedSets.sumOf { (it.weightKg * it.reps).toDouble() }.toLong()
    assertEquals(500L, totalVolumeKg)
  }

  @Test
  fun `backup json format is valid`() {
    val json = JSONObject().apply {
      put("version", 1)
      put("timestamp", System.currentTimeMillis())
      put("sessions", JSONArray())
      put("setLogs", JSONArray())
      put("measurements", JSONArray())
    }
    assertEquals(1, json.getInt("version"))
    assertNotNull(json.getJSONArray("measurements"))
  }

  @Test
  fun `checkAndSeedExercises deduplicates doubled exercises back to 38`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = WorkoutRepository(
        context = context,
        exerciseDao = db.exerciseDao(),
        mediaDao = db.mediaDao(),
        workoutDao = db.workoutDao(),
        bodyMeasurementDao = db.bodyMeasurementDao()
    )

    // Simulate duplicated initial data (e.g. 76 exercises)
    db.exerciseDao().insertAll(InitialWorkoutData.defaultExercises)
    db.exerciseDao().insertAll(InitialWorkoutData.defaultExercises)
    assertEquals(76, db.exerciseDao().getCount())

    // Run checkAndSeedExercises
    repo.checkAndSeedExercises()

    // Verify deduplicated to exactly 38 unique exercises
    assertEquals(38, db.exerciseDao().getCount())
    val allEx = db.exerciseDao().getAllExercisesList()
    val uniqueCodes = allEx.map { it.code }.toSet()
    assertEquals(38, uniqueCodes.size)

    db.close()
  }

  @Test
  fun `startWorkoutSession carries forward weight and reps from previous workout`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = WorkoutRepository(
        context = context,
        exerciseDao = db.exerciseDao(),
        mediaDao = db.mediaDao(),
        workoutDao = db.workoutDao(),
        bodyMeasurementDao = db.bodyMeasurementDao()
    )

    repo.checkAndSeedExercises()
    val militaryPress = db.exerciseDao().getExerciseByCode("A.4")!!

    // Session 1: User completes military press with 50kg, 52.5kg, 52.5kg, 55kg
    val s1Id = repo.startWorkoutSession("Trening A", listOf(militaryPress))
    val s1Sets = db.workoutDao().getSetsForSessionDirect(s1Id)
    assertEquals(4, s1Sets.size)
    db.workoutDao().updateSetLog(s1Sets[0].copy(weightKg = 50f, reps = 10, isCompleted = true))
    db.workoutDao().updateSetLog(s1Sets[1].copy(weightKg = 52.5f, reps = 9, isCompleted = true))
    db.workoutDao().updateSetLog(s1Sets[2].copy(weightKg = 52.5f, reps = 8, isCompleted = true))
    db.workoutDao().updateSetLog(s1Sets[3].copy(weightKg = 55f, reps = 7, isCompleted = true))
    val s1 = db.workoutDao().getSessionByIdDirect(s1Id)!!
    db.workoutDao().updateSession(s1.copy(endTime = System.currentTimeMillis()))

    // Session 2: User starts new workout -> sets must be prefilled with 50kg, 52.5kg, 52.5kg, 55kg!
    val s2Id = repo.startWorkoutSession("Trening A", listOf(militaryPress))
    val s2Sets = db.workoutDao().getSetsForSessionDirect(s2Id)
    assertEquals(4, s2Sets.size)
    assertEquals(50f, s2Sets[0].weightKg)
    assertEquals(10, s2Sets[0].reps)
    assertEquals(52.5f, s2Sets[1].weightKg)
    assertEquals(9, s2Sets[1].reps)
    assertEquals(52.5f, s2Sets[2].weightKg)
    assertEquals(8, s2Sets[2].reps)
    assertEquals(55f, s2Sets[3].weightKg)
    assertEquals(7, s2Sets[3].reps)

    db.close()
  }
}
