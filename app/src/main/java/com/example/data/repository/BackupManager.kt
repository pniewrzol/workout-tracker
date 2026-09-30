package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
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

class PasswordRequiredException : Exception("Wymagane hasło do odszyfrowania kopii zapasowej.")
class InvalidPasswordException(msg: String) : Exception(msg)

class BackupManager(
    private val context: Context,
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
    private val bodyMeasurementDao: BodyMeasurementDao
) {

    companion object {
        private const val PBKDF2_ITERATIONS = 10000
        private const val KEY_LENGTH_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val IV_LENGTH_BYTES = 12
        private const val SALT_LENGTH_BYTES = 16
        // Standard encryption key used when no custom password is supplied by the user,
        // ensuring the backup file is always encrypted on disk/drive rather than plaintext JSON.
        private const val DEFAULT_ENCRYPTION_SECRET = "WorkoutTracker_SafeVault_2026!EncryptedBackupKey"

        fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            val secretKey = factory.generateSecret(spec)
            return SecretKeySpec(secretKey.encoded, "AES")
        }

        fun encryptPayload(plainText: String, customPassword: String?): String {
            val hasCustomPassword = !customPassword.isNullOrBlank()
            val passwordToUse = if (hasCustomPassword) customPassword!! else DEFAULT_ENCRYPTION_SECRET

            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH_BYTES)
            random.nextBytes(salt)
            val iv = ByteArray(IV_LENGTH_BYTES)
            random.nextBytes(iv)

            val key = deriveKey(passwordToUse, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val envelope = JSONObject().apply {
                put("encrypted", true)
                put("version", 1)
                put("cipher", "AES-256-GCM")
                put("hasCustomPassword", hasCustomPassword)
                put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                put("payload", Base64.encodeToString(cipherBytes, Base64.NO_WRAP))
            }
            return envelope.toString(2)
        }

        fun decryptPayload(raw: String, customPassword: String?): String {
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

            val hasCustomPassword = root.optBoolean("hasCustomPassword", false)
            val saltBase64 = root.optString("salt", "")
            val ivBase64 = root.optString("iv", "")
            val payloadBase64 = root.optString("payload", "")

            if (saltBase64.isBlank() || ivBase64.isBlank() || payloadBase64.isBlank()) {
                throw InvalidPasswordException("Uszkodzony plik zaszyfrowanej kopii.")
            }

            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(payloadBase64, Base64.NO_WRAP)

            val passwordToUse = if (hasCustomPassword) {
                if (customPassword.isNullOrBlank()) {
                    throw PasswordRequiredException()
                }
                customPassword
            } else {
                if (!customPassword.isNullOrBlank()) customPassword else DEFAULT_ENCRYPTION_SECRET
            }

            val key = deriveKey(passwordToUse, salt)
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
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        // 1. Sessions
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

        // 2. Set logs
        val sets = workoutDao.getAllSetLogsDirect()
        val setsArray = JSONArray()
        for (st in sets) {
            val stObj = JSONObject().apply {
                put("id", st.id)
                put("sessionId", st.sessionId)
                put("exerciseId", st.exerciseId)
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

        // 3. Body measurements
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

    suspend fun exportBackupToUri(uri: Uri, password: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val plainJson = createBackupJson()
            // Always encrypt the backup with AES-256-GCM
            val encryptedJson = encryptPayload(plainJson, password)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(encryptedJson)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackupFromUri(uri: Uri, password: String? = null): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        sb.append(line)
                        line = reader.readLine()
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
            e.printStackTrace()
            RestoreResult(isSuccess = false, message = "Błąd odczytu pliku: ${e.localizedMessage}")
        }
    }

    suspend fun restoreBackupFromJson(jsonString: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Restore Sessions
            val sessionsList = mutableListOf<WorkoutSession>()
            val sessionsArray = root.optJSONArray("sessions")
            if (sessionsArray != null) {
                for (i in 0 until sessionsArray.length()) {
                    val sObj = sessionsArray.getJSONObject(i)
                    sessionsList.add(
                        WorkoutSession(
                            id = sObj.optLong("id", 0),
                            workoutName = sObj.optString("workoutName", "Trening"),
                            startTime = sObj.optLong("startTime", System.currentTimeMillis()),
                            endTime = sObj.optLong("endTime", 0),
                            notes = sObj.optString("notes", ""),
                            durationSeconds = sObj.optLong("durationSeconds", 0)
                        )
                    )
                }
                if (sessionsList.isNotEmpty()) {
                    workoutDao.insertSessions(sessionsList)
                }
            }

            // Restore Set Logs
            val setsList = mutableListOf<WorkoutSetLog>()
            val setsArray = root.optJSONArray("setLogs")
            if (setsArray != null) {
                for (i in 0 until setsArray.length()) {
                    val stObj = setsArray.getJSONObject(i)
                    val rirVal = if (stObj.has("rir")) stObj.getDouble("rir").toFloat() else null
                    val timeSecVal = if (stObj.has("timeSeconds")) stObj.getInt("timeSeconds") else null
                    val distVal = if (stObj.has("distanceMeters")) stObj.getDouble("distanceMeters").toFloat() else null
                    setsList.add(
                        WorkoutSetLog(
                            id = stObj.optLong("id", 0),
                            sessionId = stObj.optLong("sessionId", 0),
                            exerciseId = stObj.optLong("exerciseId", 1),
                            setNumber = stObj.optInt("setNumber", 1),
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
                if (setsList.isNotEmpty()) {
                    workoutDao.insertSetLogs(setsList)
                }
            }

            // Restore Body Measurements
            val measurementsList = mutableListOf<BodyMeasurement>()
            val measurementsArray = root.optJSONArray("measurements")
            if (measurementsArray != null) {
                for (i in 0 until measurementsArray.length()) {
                    val mObj = measurementsArray.getJSONObject(i)
                    measurementsList.add(
                        BodyMeasurement(
                            id = mObj.optLong("id", 0),
                            timestamp = mObj.optLong("timestamp", System.currentTimeMillis()),
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
            e.printStackTrace()
            RestoreResult(isSuccess = false, message = "Nieprawidłowy format pliku JSON: ${e.localizedMessage}")
        }
    }
}
