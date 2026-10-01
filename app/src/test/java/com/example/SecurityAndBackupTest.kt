package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Exercise
import com.example.data.repository.BackupManager
import com.example.data.repository.InvalidPasswordException
import com.example.data.repository.PasswordRequiredException
import com.example.data.security.SecurePasswordStorage
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityAndBackupTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backupManager = BackupManager(context, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `SecurePasswordStorage saves, retrieves and clears password without plaintext disk leaks`() {
        // Pre-populate legacy plaintext SharedPreferences to test automatic purging
        val legacyPrefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
        legacyPrefs.edit().putString("user_backup_password", "PlaintextLeak123!").apply()
        assertTrue(legacyPrefs.contains("user_backup_password"))

        val secureStorage = SecurePasswordStorage(context)

        // 1. Verify legacy plaintext key was wiped from disk
        assertFalse(
            "Legacy plaintext password must be purged from SharedPreferences",
            legacyPrefs.contains("user_backup_password")
        )

        // 2. Set and retrieve secure password
        secureStorage.setBackupPassword("StrongMasterPass#2026")
        assertEquals("StrongMasterPass#2026", secureStorage.getBackupPassword())

        // 3. Ensure no plaintext fallback file exists on disk
        val fallbackPrefs = context.getSharedPreferences("secure_vault_encrypted_prefs_fallback", Context.MODE_PRIVATE)
        assertTrue(
            "Fallback plaintext file must be empty or purged",
            fallbackPrefs.all.isEmpty()
        )

        // 4. Clearing password
        secureStorage.setBackupPassword("")
        assertEquals("", secureStorage.getBackupPassword())
    }

    @Test
    fun `BackupManager encrypts and decrypts valid payload successfully`() {
        val originalJson = """{"test":"training_data_123","val":42}"""
        val password = "SuperSecretPassword99@"

        val encrypted = BackupManager.encryptPayload(originalJson, password, iterations = 100_000)
        assertTrue(encrypted.contains(""""encrypted": true"""))
        assertTrue(encrypted.contains(""""cipher": "AES-256-GCM""""))
        assertTrue(encrypted.contains(""""kdf": "PBKDF2WithHmacSHA256""""))

        val decrypted = BackupManager.decryptPayload(encrypted, password)
        assertEquals(originalJson, decrypted)
    }

    @Test
    fun `BackupManager decrypt with invalid password throws InvalidPasswordException`() {
        val originalJson = """{"secret":"workout_history"}"""
        val encrypted = BackupManager.encryptPayload(originalJson, "CorrectPass123", iterations = 100_000)

        try {
            BackupManager.decryptPayload(encrypted, "WrongPassword!")
            fail("Expected InvalidPasswordException was not thrown")
        } catch (_: InvalidPasswordException) {
            // Expected
        }
    }

    @Test
    fun `BackupManager decrypt without password throws PasswordRequiredException`() {
        val originalJson = """{"data":"my_sets"}"""
        val encrypted = BackupManager.encryptPayload(originalJson, "ValidPassword!", iterations = 100_000)

        try {
            BackupManager.decryptPayload(encrypted, "")
            fail("Expected PasswordRequiredException was not thrown")
        } catch (_: PasswordRequiredException) {
            // Expected
        }
    }

    @Test
    fun `BackupManager rejects out-of-bounds PBKDF2 iterations to prevent DoS`() {
        val originalJson = """{"data":"dos_prevention_test"}"""
        val password = "TestPassword123"

        // 1. Manually craft envelope with iteration count > 2,000,000 (DoS attack attempt)
        val maliciousEnvelope = JSONObject().apply {
            put("encrypted", true)
            put("version", 2)
            put("cipher", "AES-256-GCM")
            put("kdf", "PBKDF2WithHmacSHA256")
            put("iterations", 50_000_000) // Excessively high
            put("salt", "dGVzdHNhbHQ=")
            put("iv", "dGVzdGl2MTIzNA==")
            put("payload", "dGVzdHBheWxvYWQ=")
        }.toString()

        try {
            BackupManager.decryptPayload(maliciousEnvelope, password)
            fail("Expected InvalidPasswordException for iteration count > 2,000,000")
        } catch (e: InvalidPasswordException) {
            assertTrue(e.message?.contains("niebezpieczna liczba iteracji") == true)
        }

        // 2. Manually craft envelope with iteration count < 10,000 (dangerously weak)
        val weakEnvelope = JSONObject().apply {
            put("encrypted", true)
            put("version", 2)
            put("cipher", "AES-256-GCM")
            put("kdf", "PBKDF2WithHmacSHA256")
            put("iterations", 500) // Way too weak
            put("salt", "dGVzdHNhbHQ=")
            put("iv", "dGVzdGl2MTIzNA==")
            put("payload", "dGVzdHBheWxvYWQ=")
        }.toString()

        try {
            BackupManager.decryptPayload(weakEnvelope, password)
            fail("Expected InvalidPasswordException for iteration count < 10,000")
        } catch (e: InvalidPasswordException) {
            assertTrue(e.message?.contains("niebezpieczna liczba iteracji") == true)
        }
    }

    @Test
    fun `Restore rolls back atomically and aborts when encountering an unknown exercise`() = runBlocking {
        // Seed 1 known exercise: "Wyciskanie sztangi leżąc"
        val knownExId = database.exerciseDao().insertExercise(
            Exercise(
                section = "Trening A",
                code = "A.1",
                name = "Wyciskanie sztangi leżąc",
                bodyPart = "Klatka piersiowa",
                equipment = "Sztanga",
                primaryMuscles = "Mięśnie piersiowe",
                secondaryMuscles = "Triceps",
                cues = "Płynny ruch",
                instructions = "Opuść sztangę i wyciśnij w górę"
            )
        )

        // Craft a backup JSON containing:
        // - A valid session
        // - A set log referencing an unknown exercise that does not exist in db
        val backupJson = JSONObject().apply {
            put("version", 2)
            put("timestamp", System.currentTimeMillis())

            val sessions = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 101L)
                    put("workoutName", "Trening A - Klata")
                    put("startTime", 1700000000000L)
                    put("endTime", 1700003600000L)
                    put("durationSeconds", 3600L)
                    put("notes", "")
                })
            }
            put("sessions", sessions)

            val sets = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 1L)
                    put("sessionId", 101L)
                    put("exerciseId", 99999L) // Non-existent ID
                    put("exerciseName", "Ćwiczenie Nieistniejące Z Kosmosu") // Non-existent name
                    put("setNumber", 1)
                    put("weightKg", 100.0)
                    put("reps", 10)
                    put("isCompleted", true)
                    put("timestamp", 1700000500000L)
                    put("notes", "")
                })
            }
            put("setLogs", sets)
        }.toString()

        // Perform restore
        val result = backupManager.restoreBackupFromJson(backupJson)

        // 1. Restore must fail and report failure
        assertFalse("Restore must fail when unknown exercise cannot be mapped", result.isSuccess)
        assertTrue(result.message.contains("nie istnieje w bazie danych"))

        // 2. Atomic rollback check: Database MUST NOT contain the session that was parsed before the failed set!
        val sessionsInDb = database.workoutDao().getAllSessionsDirect()
        val setsInDb = database.workoutDao().getAllSetLogsDirect()
        assertEquals(
            "Transaction must rollback sessions on failure (no orphaned data)",
            0,
            sessionsInDb.size
        )
        assertEquals(
            "Transaction must rollback sets on failure",
            0,
            setsInDb.size
        )
    }

    @Test
    fun `Restore succeeds when exercises match by name across different IDs`() = runBlocking {
        // Seed exercise in database with ID 42
        val targetExId = database.exerciseDao().insertExercise(
            Exercise(
                section = "Trening A",
                code = "A.1",
                name = "Przysiad ze sztangą",
                bodyPart = "Nogi",
                equipment = "Sztanga",
                primaryMuscles = "Czworogłowe",
                secondaryMuscles = "Pośladki",
                cues = "Kolana na zewnątrz",
                instructions = "Przysiądź głęboko"
            )
        )

        // In the backup file, the exercise had an old ID 999, but its name matches "Przysiad ze sztangą"
        val backupJson = JSONObject().apply {
            put("version", 2)
            put("timestamp", System.currentTimeMillis())

            val sessions = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 200L)
                    put("workoutName", "Trening Nóg")
                    put("startTime", 1700000000000L)
                })
            }
            put("sessions", sessions)

            val sets = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 10L)
                    put("sessionId", 200L)
                    put("exerciseId", 999L) // Old file ID
                    put("exerciseName", "Przysiad ze sztangą") // Exact matching name
                    put("setNumber", 1)
                    put("weightKg", 120.0)
                    put("reps", 8)
                    put("isCompleted", true)
                })
            }
            put("setLogs", sets)
        }.toString()

        val result = backupManager.restoreBackupFromJson(backupJson)
        assertTrue("Restore should succeed when exercise matches by name", result.isSuccess)
        assertEquals(1, result.sessionsCount)
        assertEquals(1, result.setsCount)

        // Verify the set in database resolved to the real database exercise ID
        val insertedSets = database.workoutDao().getAllSetLogsDirect()
        assertEquals(1, insertedSets.size)
        assertEquals(
            "Set must be mapped to target database exercise ID",
            targetExId,
            insertedSets.first().exerciseId
        )
    }
}
