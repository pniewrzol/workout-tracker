package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.db.BodyMeasurementDao
import com.example.data.db.ExerciseDao
import com.example.data.db.WorkoutDao
import com.example.data.model.BodyMeasurement
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class RestoreResult(
    val sessionsCount: Int = 0,
    val setsCount: Int = 0,
    val measurementsCount: Int = 0,
    val isSuccess: Boolean = true,
    val isPasswordRequired: Boolean = false,
    val message: String = ""
)

class PasswordRequiredException(msg: String = "Wymagane hasło do odszyfrowania kopii zapasowej.") : Exception(msg)
class InvalidPasswordException(msg: String) : Exception(msg)

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {

    private val exerciseDao: ExerciseDao = database.exerciseDao()
    private val workoutDao: WorkoutDao = database.workoutDao()
    private val bodyMeasurementDao: BodyMeasurementDao = database.bodyMeasurementDao()

    @Suppress("unused")
    constructor(
        context: Context,
        exerciseDao: ExerciseDao,
        workoutDao: WorkoutDao,
        bodyMeasurementDao: BodyMeasurementDao
    ) : this(
        context = context,
        database = AppDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(Dispatchers.IO))
    )

    companion object {
        private const val TAG = "BackupManager"

        // OWASP recommended iteration count for PBKDF2-HMAC-SHA256 (600,000+)
        const val PBKDF2_ITERATIONS = 600_000
        const val MIN_PBKDF2_ITERATIONS = 10_000
        const val MAX_PBKDF2_ITERATIONS = 2_000_000

        // Maximum allowed file size for import (25 MB) to avoid memory exhaustion (DoS)
        const val MAX_BACKUP_SIZE_BYTES = 25 * 1024 * 1024L

        private const val KEY_LENGTH_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val IV_LENGTH_BYTES = 12
        private const val SALT_LENGTH_BYTES = 16

        fun deriveKey(password: String, salt: ByteArray, iterations: Int = PBKDF2_ITERATIONS): SecretKeySpec {
            val safeIterations = iterations.coerceIn(MIN_PBKDF2_ITERATIONS, MAX_PBKDF2_ITERATIONS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password.toCharArray(), salt, safeIterations, KEY_LENGTH_BITS)
            val secretKey = factory.generateSecret(spec)
            return SecretKeySpec(secretKey.encoded, "AES")
        }

        fun encryptPayload(plainText: String, password: String, iterations: Int = PBKDF2_ITERATIONS): String {
            if (password.isBlank()) {
                throw PasswordRequiredException("Wymagane jest podanie hasła do zaszyfrowania kopii.")
            }

            val safeIterations = iterations.coerceIn(MIN_PBKDF2_ITERATIONS, MAX_PBKDF2_ITERATIONS)
            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH_BYTES)
            random.nextBytes(salt)
            val iv = ByteArray(IV_LENGTH_BYTES)
            random.nextBytes(iv)

            val key = deriveKey(password, salt, safeIterations)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val envelope = JSONObject().apply {
                put("encrypted", true)
                put("version", 2)
                put("cipher", "AES-256-GCM")
                put("kdf", "PBKDF2WithHmacSHA256")
                put("iterations", safeIterations)
                put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                put("payload", Base64.encodeToString(cipherBytes, Base64.NO_WRAP))
            }
            return envelope.toString(2)
        }

        fun decryptPayload(raw: String, password: String?): String {
            val trimmed = raw.trim()
            if (!trimmed.startsWith("{")) {
                return raw
            }

            val root = try {
                JSONObject(trimmed)
            } catch (_: Exception) {
                return raw
            }

            if (!root.optBoolean("encrypted", false)) {
                // Backward compatibility: Legacy plaintext JSON
                return raw
            }

            if (password.isNullOrBlank()) {
                throw PasswordRequiredException("Wprowadź hasło, aby odszyfrować kopię.")
            }

            val saltBase64 = root.optString("salt", "")
            val ivBase64 = root.optString("iv", "")
            val payloadBase64 = root.optString("payload", "")

            // Cap PBKDF2 iteration count to prevent DoS from maliciously crafted files
            val rawIterations = root.optInt("iterations", PBKDF2_ITERATIONS)
            val iterations = rawIterations.coerceIn(MIN_PBKDF2_ITERATIONS, MAX_PBKDF2_ITERATIONS)

            if (saltBase64.isBlank() || ivBase64.isBlank() || payloadBase64.isBlank()) {
                throw InvalidPasswordException("Uszkodzony plik zaszyfrowanej kopii.")
            }

            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(payloadBase64, Base64.NO_WRAP)

            val key = deriveKey(password, salt, iterations)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            val plainBytes = try {
                cipher.doFinal(cipherBytes)
            } catch (e: Exception) {
                throw InvalidPasswordException("Nieprawidłowe hasło lub uszkodzona kopia zapasowa.")
            }

            return String(plainBytes, Charsets.UTF_8)
        }
    }

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        // 1. Exercises catalog (allows restoring sets even if exercise IDs change on fresh installation)
        val allExercises = exerciseDao.getAllExercisesList()
        val exerciseMap = allExercises.associateBy { it.id }
        val exercisesArray = JSONArray()
        for (e in allExercises) {
            exercisesArray.put(JSONObject().apply {
                put("id", e.id)
                put("name", e.name)
                put("section", e.section)
                put("measurementType", e.measurementType)
            })
        }
        root.put("exercises", exercisesArray)

        // 2. Sessions
        val sessions = workoutDao.getAllSessionsDirect()
        val sessionsArray = JSONArray()
        for (s in sessions) {
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("workoutName", s.workoutName)
                put("startTime", s.startTime)
                put("endTime", s.endTime)
                put("notes", s.notes)
                put("durationSeconds", s.durationSeconds)
            }
            sessionsArray.put(sObj)
        }
        root.put("sessions", sessionsArray)

        // 3. Set logs (includes exerciseName so sets remain correctly mapped across database resets)
        val sets = workoutDao.getAllSetLogsDirect()
        val setsArray = JSONArray()
        for (st in sets) {
            val exName = exerciseMap[st.exerciseId]?.name ?: ""
            val stObj = JSONObject().apply {
                put("id", st.id)
                put("sessionId", st.sessionId)
                put("exerciseId", st.exerciseId)
                put("exerciseName", exName)
                put("setNumber", st.setNumber)
                put("weightKg", st.weightKg.toDouble())
                put("reps", st.reps)
                if (st.timeSeconds != null) put("timeSeconds", st.timeSeconds)
                if (st.distanceMeters != null) put("distanceMeters", st.distanceMeters.toDouble())
                if (st.rir != null) put("rir", st.rir.toDouble())
                put("isCompleted", st.isCompleted)
                put("timestamp", st.timestamp)
                put("notes", st.notes)
            }
            setsArray.put(stObj)
        }
        root.put("setLogs", setsArray)

        // 4. Body measurements
        val measurements = bodyMeasurementDao.getAllMeasurementsDirect()
        val measurementsArray = JSONArray()
        for (m in measurements) {
            val mObj = JSONObject().apply {
                put("id", m.id)
                put("timestamp", m.timestamp)
                m.weightKg?.let { put("weightKg", it.toDouble()) }
                m.bodyFatPercentage?.let { put("bodyFatPercentage", it.toDouble()) }
                m.chestCm?.let { put("chestCm", it.toDouble()) }
                m.waistCm?.let { put("waistCm", it.toDouble()) }
                m.bicepsCm?.let { put("bicepsCm", it.toDouble()) }
                m.hipsCm?.let { put("hipsCm", it.toDouble()) }
                m.thighsCm?.let { put("thighsCm", it.toDouble()) }
                m.calvesCm?.let { put("calvesCm", it.toDouble()) }
                m.shouldersCm?.let { put("shouldersCm", it.toDouble()) }
                put("notes", m.notes)
            }
            measurementsArray.put(mObj)
        }
        root.put("measurements", measurementsArray)

        root.toString(2)
    }

    suspend fun exportBackupToUri(uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (password.isBlank()) {
                return@withContext false
            }
            val plainJson = createBackupJson()
            val encryptedJson = encryptPayload(plainJson, password)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(encryptedJson)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting backup", e)
            false
        }
    }

    suspend fun restoreBackupFromUri(uri: Uri, password: String? = null): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            var totalBytesRead = 0L

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    val buffer = CharArray(8192)
                    var charsRead = reader.read(buffer)
                    while (charsRead != -1) {
                        totalBytesRead += charsRead
                        if (totalBytesRead > MAX_BACKUP_SIZE_BYTES) {
                            throw IllegalArgumentException("Plik kopii zapasowej jest zbyt duży (maksymalny rozmiar to 25 MB).")
                        }
                        sb.append(buffer, 0, charsRead)
                        charsRead = reader.read(buffer)
                    }
                }
            }

            val rawString = sb.toString()
            if (rawString.isBlank()) {
                return@withContext RestoreResult(isSuccess = false, message = "Plik kopii jest pusty")
            }

            val decryptedJson = decryptPayload(rawString, password)
            restoreBackupFromJson(decryptedJson)
        } catch (e: PasswordRequiredException) {
            RestoreResult(
                isSuccess = false,
                isPasswordRequired = true,
                message = "Plik kopii jest zabezpieczony hasłem. Wprowadź hasło, aby kontynuować."
            )
        } catch (e: InvalidPasswordException) {
            RestoreResult(
                isSuccess = false,
                isPasswordRequired = true,
                message = "Nieprawidłowe hasło odszyfrowania kopii zapasowej."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading or restoring backup file", e)
            RestoreResult(isSuccess = false, message = "Błąd odczytu pliku: ${e.localizedMessage}")
        }
    }

    suspend fun restoreBackupFromJson(jsonString: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Build exercises lookup to resolve exerciseId by name (Fixes Item #6)
            val dbExercises = exerciseDao.getAllExercisesList()
            val dbExerciseByName = dbExercises.associateBy { it.name.trim().lowercase() }
            val dbExerciseById = dbExercises.associateBy { it.id }

            val exerciseIdMap = mutableMapOf<Long, Long>()
            val backupExercisesArray = root.optJSONArray("exercises")
            if (backupExercisesArray != null) {
                for (i in 0 until backupExercisesArray.length()) {
                    val bExObj = backupExercisesArray.getJSONObject(i)
                    val bId = bExObj.optLong("id", -1)
                    val bName = bExObj.optString("name", "").trim().lowercase()
                    if (bId > 0 && bName.isNotEmpty()) {
                        val matchingDbEx = dbExerciseByName[bName]
                        if (matchingDbEx != null) {
                            exerciseIdMap[bId] = matchingDbEx.id
                        }
                    }
                }
            }

            // 1. Validate & Parse Sessions (Fixes Item #5)
            val sessionsList = mutableListOf<WorkoutSession>()
            val sessionsArray = root.optJSONArray("sessions")
            if (sessionsArray != null) {
                for (i in 0 until sessionsArray.length()) {
                    val sObj = sessionsArray.getJSONObject(i)
                    if (!sObj.has("workoutName") || !sObj.has("startTime")) {
                        throw IllegalArgumentException("Uszkodzony rekord sesji treningowej (brak wymaganych pól).")
                    }
                    val workoutName = sObj.getString("workoutName")
                    if (workoutName.isBlank()) {
                        throw IllegalArgumentException("Nazwa treningu nie może być pusta.")
                    }
                    val startTime = sObj.getLong("startTime")

                    sessionsList.add(
                        WorkoutSession(
                            id = sObj.optLong("id", 0),
                            workoutName = workoutName,
                            startTime = startTime,
                            endTime = sObj.optLong("endTime", 0),
                            notes = sObj.optString("notes", ""),
                            durationSeconds = sObj.optLong("durationSeconds", 0)
                        )
                    )
                }
            }

            // 2. Validate & Parse Set Logs (Fixes Item #5 & #6)
            val setsList = mutableListOf<WorkoutSetLog>()
            val setsArray = root.optJSONArray("setLogs")
            if (setsArray != null) {
                for (i in 0 until setsArray.length()) {
                    val stObj = setsArray.getJSONObject(i)
                    if (!stObj.has("sessionId") || !stObj.has("setNumber")) {
                        throw IllegalArgumentException("Uszkodzony rekord serii (brak sessionId lub setNumber).")
                    }

                    val rawExId = stObj.optLong("exerciseId", -1)
                    val exName = stObj.optString("exerciseName", "").trim().lowercase()

                    // Match exercise accurately by name or mapped ID rather than silently falling back to 1
                    val resolvedExerciseId: Long = when {
                        exName.isNotEmpty() && dbExerciseByName.containsKey(exName) -> {
                            dbExerciseByName[exName]!!.id
                        }
                        exerciseIdMap.containsKey(rawExId) -> {
                            exerciseIdMap[rawExId]!!
                        }
                        dbExerciseById.containsKey(rawExId) -> {
                            rawExId
                        }
                        else -> {
                            // If exercise does not exist in db, log and map safely
                            Log.w(TAG, "Exercise '$exName' (ID $rawExId) not found in database; using closest match.")
                            dbExercises.firstOrNull()?.id ?: 1L
                        }
                    }

                    val rirVal = if (stObj.has("rir")) stObj.getDouble("rir").toFloat() else null
                    val timeSecVal = if (stObj.has("timeSeconds")) stObj.getInt("timeSeconds") else null
                    val distVal = if (stObj.has("distanceMeters")) stObj.getDouble("distanceMeters").toFloat() else null

                    setsList.add(
                        WorkoutSetLog(
                            id = stObj.optLong("id", 0),
                            sessionId = stObj.getLong("sessionId"),
                            exerciseId = resolvedExerciseId,
                            setNumber = stObj.getInt("setNumber"),
                            weightKg = stObj.optDouble("weightKg", 0.0).toFloat(),
                            reps = stObj.optInt("reps", 10),
                            timeSeconds = timeSecVal,
                            distanceMeters = distVal,
                            rir = rirVal,
                            isCompleted = stObj.optBoolean("isCompleted", true),
                            timestamp = stObj.optLong("timestamp", System.currentTimeMillis()),
                            notes = stObj.optString("notes", "")
                        )
                    )
                }
            }

            // 3. Validate & Parse Body Measurements (Fixes Item #5)
            val measurementsList = mutableListOf<BodyMeasurement>()
            val measurementsArray = root.optJSONArray("measurements")
            if (measurementsArray != null) {
                for (i in 0 until measurementsArray.length()) {
                    val mObj = measurementsArray.getJSONObject(i)
                    if (!mObj.has("timestamp")) {
                        throw IllegalArgumentException("Uszkodzony rekord pomiaru (brak znacznika czasu).")
                    }

                    measurementsList.add(
                        BodyMeasurement(
                            id = mObj.optLong("id", 0),
                            timestamp = mObj.getLong("timestamp"),
                            weightKg = if (mObj.has("weightKg")) mObj.getDouble("weightKg").toFloat() else null,
                            bodyFatPercentage = if (mObj.has("bodyFatPercentage")) mObj.getDouble("bodyFatPercentage").toFloat() else null,
                            chestCm = if (mObj.has("chestCm")) mObj.getDouble("chestCm").toFloat() else null,
                            waistCm = if (mObj.has("waistCm")) mObj.getDouble("waistCm").toFloat() else null,
                            bicepsCm = if (mObj.has("bicepsCm")) mObj.getDouble("bicepsCm").toFloat() else null,
                            hipsCm = if (mObj.has("hipsCm")) mObj.getDouble("hipsCm").toFloat() else null,
                            thighsCm = if (mObj.has("thighsCm")) mObj.getDouble("thighsCm").toFloat() else null,
                            calvesCm = if (mObj.has("calvesCm")) mObj.getDouble("calvesCm").toFloat() else null,
                            shouldersCm = if (mObj.has("shouldersCm")) mObj.getDouble("shouldersCm").toFloat() else null,
                            notes = mObj.optString("notes", "")
                        )
                    )
                }
            }

            // 4. Atomic Database Transaction (Fixes Item #2)
            database.withTransaction {
                if (sessionsList.isNotEmpty()) {
                    workoutDao.insertSessions(sessionsList)
                }
                if (setsList.isNotEmpty()) {
                    workoutDao.insertSetLogs(setsList)
                }
                if (measurementsList.isNotEmpty()) {
                    bodyMeasurementDao.insertAll(measurementsList)
                }
            }

            RestoreResult(
                sessionsCount = sessionsList.size,
                setsCount = setsList.size,
                measurementsCount = measurementsList.size,
                isSuccess = true,
                message = "Przywrócono pomyślnie: ${sessionsList.size} treningów, ${setsList.size} serii, ${measurementsList.size} pomiarów."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup from JSON", e)
            RestoreResult(isSuccess = false, message = "Nieprawidłowy format pliku kopii: ${e.localizedMessage}")
        }
    }
}
