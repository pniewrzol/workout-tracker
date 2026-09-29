package com.example.data.repository

import android.content.Context
import android.net.Uri
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

data class RestoreResult(
    val sessionsCount: Int = 0,
    val setsCount: Int = 0,
    val measurementsCount: Int = 0,
    val isSuccess: Boolean = true,
    val message: String = ""
)

class BackupManager(
    private val context: Context,
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
    private val bodyMeasurementDao: BodyMeasurementDao
) {

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

    suspend fun exportBackupToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackupFromUri(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        sb.append(line)
                        line = reader.readLine()
                    }
                }
            }
            val jsonString = sb.toString()
            if (jsonString.isBlank()) {
                return@withContext RestoreResult(isSuccess = false, message = "Plik kopii jest pusty")
            }
            restoreBackupFromJson(jsonString)
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
                message = "Przywrócono: ${sessionsList.size} treningów, ${setsList.size} serii, ${measurementsList.size} pomiarów."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult(isSuccess = false, message = "Nieprawidłowy format pliku JSON: ${e.localizedMessage}")
        }
    }
}
