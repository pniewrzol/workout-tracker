package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BodyMeasurement
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelReportExporter {

    private fun escapeCsv(value: Any?): String {
        if (value == null) return ""
        val str = value.toString().replace("\"", "\"\"")
        return if (str.contains(";") || str.contains("\n") || str.contains("\"")) {
            "\"$str\""
        } else {
            str
        }
    }

    fun generateAndShareExcelReport(
        context: Context,
        reportData: GeneratedReportData,
        sessions: List<WorkoutSession>,
        allSets: List<WorkoutSetLog>,
        allExercises: List<Exercise>,
        allMeasurements: List<BodyMeasurement>,
        cutoffTime: Long
    ) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val generatedDateStr = dateFormat.format(Date())

        val exerciseMap = allExercises.associateBy { it.id }

        // Filter sessions and measurements in period
        val periodSessions = sessions.filter { it.isCompleted && it.startTime >= cutoffTime }.sortedByDescending { it.startTime }
        val periodSessionIds = periodSessions.map { it.id }.toSet()
        val periodSets = allSets.filter { it.isCompleted && it.sessionId in periodSessionIds }
        val periodMeasurements = allMeasurements.filter { it.timestamp >= cutoffTime }.sortedBy { it.timestamp }

        // Best set per exercise
        val bestSetsMap = mutableMapOf<Long, WorkoutSetLog>()
        for (set in periodSets) {
            val currentBest = bestSetsMap[set.exerciseId]
            if (currentBest == null) {
                bestSetsMap[set.exerciseId] = set
            } else {
                val currentScore = currentBest.weightKg * currentBest.reps
                val setScore = set.weightKg * set.reps
                if (setScore > currentScore || (setScore == currentScore && set.weightKg > currentBest.weightKg)) {
                    bestSetsMap[set.exerciseId] = set
                }
            }
        }

        val csvBuilder = StringBuilder()
        // UTF-8 BOM so Microsoft Excel immediately opens with UTF-8 encoding
        csvBuilder.append('\uFEFF')

        // 1. HEADER & SUMMARY
        csvBuilder.append("RAPORT TRENINGOWY - DANE LICZBOWE\n")
        csvBuilder.append("Zakres:;${escapeCsv(reportData.periodLabel)} (${escapeCsv(reportData.dateRangeStr)})\n")
        csvBuilder.append("Data wygenerowania:;${escapeCsv(generatedDateStr)}\n")
        csvBuilder.append("\n")

        val mainPeriodSessions = periodSessions.filter { it.isMainWorkout }
        val warmupPeriodSessions = periodSessions.filter { !it.isMainWorkout }

        csvBuilder.append("PODSUMOWANIE OGÓLNE\n")
        csvBuilder.append("Metryka;Wartość;Jednostka\n")
        csvBuilder.append("Ukończone treningi główne;${mainPeriodSessions.size};sesji\n")
        if (warmupPeriodSessions.isNotEmpty()) {
            csvBuilder.append("Sesje pomocnicze (rozgrzewka/mobility);${warmupPeriodSessions.size};sesji\n")
        }
        csvBuilder.append("Łączny tonaż siłowy;${reportData.totalTonnageKg};kg\n")
        csvBuilder.append("Łączny czas treningów;${reportData.totalTrainingMinutes};min\n")
        csvBuilder.append("Łączna liczba serii;${reportData.totalSetsCount};serii\n")
        csvBuilder.append("Łączna liczba powtórzeń;${reportData.totalRepsCount};powtórzeń\n")
        csvBuilder.append("\n")

        // 2. PODZIAŁ NA DANE TRENINGI (Summary per Workout Plan / Session Type)
        csvBuilder.append("ZESTAWIENIE WG TRENINGÓW\n")
        csvBuilder.append("Nazwa Treningu;Liczba sesji;Łączny tonaż (kg);Łączna liczba serii;Średni czas sesji (min)\n")
        val groupedByWorkoutName = periodSessions.groupBy { it.workoutName }
        for ((name, sessList) in groupedByWorkoutName) {
            val sIds = sessList.map { it.id }.toSet()
            val wSets = periodSets.filter { it.sessionId in sIds }
            val wTonnage = wSets.sumOf { (it.weightKg * it.reps).toLong() }
            val totalMin = sessList.sumOf { s ->
                val end = if (s.endTime > s.startTime) s.endTime else s.startTime
                ((end - s.startTime) / 60000).coerceAtLeast(1)
            }
            val avgMin = if (sessList.isNotEmpty()) totalMin / sessList.size else 0
            csvBuilder.append("${escapeCsv(name)};${sessList.size};$wTonnage;${wSets.size};$avgMin\n")
        }
        csvBuilder.append("\n")

        // 3. WSZYSTKIE SERIE, POWTÓRZENIA I OBCIĄŻENIA (Detailed Training Log)
        csvBuilder.append("SZCZEGÓŁOWA HISTORIA: TRENINGI, SERIE, POWTÓRZENIA I OBCIĄŻENIA\n")
        csvBuilder.append("Data;Godzina;Trening;Ćwiczenie;Kod;Nr Serii;Ciężar (kg);Powtórzenia;Tonaż serii (kg);Czas (s);Dystans (m);Najlepsza seria (PR);Notatki sesji\n")

        for (session in periodSessions) {
            val sSets = periodSets.filter { it.sessionId == session.id }.sortedWith(
                compareBy<WorkoutSetLog> { it.exerciseId }.thenBy { it.setNumber }
            )
            for (set in sSets) {
                val ex = exerciseMap[set.exerciseId]
                val exName = ex?.name ?: "Ćwiczenie"
                val exCode = ex?.code ?: ""
                val isBest = set.id == bestSetsMap[set.exerciseId]?.id
                val bestMarker = if (isBest) "TAK (PR)" else "Nie"
                val setTonnage = (set.weightKg * set.reps).toLong()

                csvBuilder.append("${dateFormat.format(Date(session.startTime))};")
                csvBuilder.append("${timeFormat.format(Date(session.startTime))};")
                csvBuilder.append("${escapeCsv(session.workoutName)};")
                csvBuilder.append("${escapeCsv(exName)};")
                csvBuilder.append("${escapeCsv(exCode)};")
                csvBuilder.append("${set.setNumber};")
                csvBuilder.append("${set.weightKg};")
                csvBuilder.append("${set.reps};")
                csvBuilder.append("$setTonnage;")
                csvBuilder.append("${set.timeSeconds ?: ""};")
                csvBuilder.append("${set.distanceMeters ?: ""};")
                csvBuilder.append("$bestMarker;")
                csvBuilder.append("${escapeCsv(session.notes)}\n")
            }
        }
        csvBuilder.append("\n")

        // 4. POMIARY SYLWETKI (Body Measurements)
        csvBuilder.append("POMIARY SYLWETKI W DANYM OKRESIE\n")
        csvBuilder.append("Data;Waga (kg);Tkanka tłuszczowa (%);Klatka (cm);Talia (cm);Biceps (cm);Biodra (cm);Uda (cm);Łydki (cm);Barki (cm);Notatki\n")
        for (m in periodMeasurements) {
            csvBuilder.append("${dateFormat.format(Date(m.timestamp))};")
            csvBuilder.append("${m.weightKg ?: ""};")
            csvBuilder.append("${m.bodyFatPercentage ?: ""};")
            csvBuilder.append("${m.chestCm ?: ""};")
            csvBuilder.append("${m.waistCm ?: ""};")
            csvBuilder.append("${m.bicepsCm ?: ""};")
            csvBuilder.append("${m.hipsCm ?: ""};")
            csvBuilder.append("${m.thighsCm ?: ""};")
            csvBuilder.append("${m.calvesCm ?: ""};")
            csvBuilder.append("${m.shouldersCm ?: ""};")
            csvBuilder.append("${escapeCsv(m.notes)}\n")
        }
        csvBuilder.append("\n")

        // 5. ZAANGAŻOWANE GRUPY MIĘŚNIOWE
        csvBuilder.append("ZAANGAŻOWANE GRUPY MIĘŚNIOWE\n")
        csvBuilder.append("Grupa Mięśniowa;Liczba Serii;Łączny Tonaż (kg);Poziom Aktywności\n")
        val muscleActivities = calculateMuscleActivities(allExercises, periodSets, 3650)
        for ((m, act) in muscleActivities) {
            val levelDesc = when (act.level) {
                4 -> "Bardzo wysoka (16+ serii)"
                3 -> "Wysoka (10-15 serii)"
                2 -> "Umiarkowana (5-9 serii)"
                1 -> "Wstępna (1-4 serii)"
                else -> "Brak"
            }
            csvBuilder.append("${escapeCsv(m.displayName)};${act.setsCount};${act.totalTonnage.toLong()};$levelDesc\n")
        }

        // Save file to cache and share
        try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val fileName = "Raport_Treningi_Excel_${System.currentTimeMillis()}.csv"
            val reportFile = File(reportsDir, fileName)
            FileOutputStream(reportFile).use { fos ->
                fos.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Raport Treningowy Excel - $generatedDateStr")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Otwórz lub Udostępnij Raport w Excel / Arkusze"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
