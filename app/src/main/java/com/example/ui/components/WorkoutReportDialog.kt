package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BodyMeasurement
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportPeriod(val label: String, val days: Int) {
    DAYS_7("7 dni", 7),
    DAYS_30("30 dni", 30),
    DAYS_90("90 dni", 90),
    ALL("Wszystko", 3650)
}

data class GeneratedReportData(
    val periodLabel: String,
    val dateRangeStr: String,
    val completedWorkoutsCount: Int,
    val warmupSessionsCount: Int = 0,
    val totalTrainingMinutes: Long,
    val totalTonnageKg: Long,
    val totalSetsCount: Int,
    val totalRepsCount: Int,
    val weightDiffKg: Float?,
    val initialWeightKg: Float?,
    val finalWeightKg: Float?,
    val bodyFatDiff: Float?,
    val chestDiffCm: Float?,
    val waistDiffCm: Float?,
    val bicepsDiffCm: Float?,
    val thighsDiffCm: Float?,
    val sessionsList: List<WorkoutSessionReportItem>,
    val topExercisesList: List<TopExerciseReportItem>
)

data class WorkoutSessionReportItem(
    val id: Long,
    val dateStr: String,
    val name: String,
    val durationMin: Long,
    val setsCount: Int,
    val tonnageKg: Long,
    val isMainWorkout: Boolean = true
)

data class TopExerciseReportItem(
    val exerciseName: String,
    val maxWeightKg: Float,
    val totalSets: Int,
    val totalTonnageKg: Long
)

fun buildReportData(
    period: ReportPeriod,
    sessions: List<WorkoutSession>,
    allSets: List<WorkoutSetLog>,
    allExercises: List<Exercise>,
    allMeasurements: List<BodyMeasurement>
): GeneratedReportData {
    val now = System.currentTimeMillis()
    val cutoff = if (period.days >= 3000) 0L else now - (period.days.toLong() * 24 * 60 * 60 * 1000L)

    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val dateRangeStr = if (period.days >= 3000) {
        "Pełna historia"
    } else {
        "${dateFormat.format(Date(cutoff))} - ${dateFormat.format(Date(now))}"
    }

    val validSessions = sessions.filter { it.isCompleted && it.startTime >= cutoff }.sortedByDescending { it.startTime }
    val validSessionIds = validSessions.map { it.id }.toSet()

    val validSets = allSets.filter { it.isCompleted && it.sessionId in validSessionIds }
    val exerciseMap = allExercises.associateBy { it.id }

    val totalTrainingMinutes = validSessions.sumOf { session ->
        val end = session.endTime ?: session.startTime
        ((end - session.startTime) / 60000).coerceAtLeast(1)
    }

    val totalTonnageDouble = validSets.sumOf { it.weightKg.toDouble() * it.reps }
    val totalTonnageKg = Math.round(totalTonnageDouble)
    val totalRepsCount = validSets.sumOf { it.reps }
    val exerciseStatsMap = mutableMapOf<Long, MutableList<WorkoutSetLog>>()

    for (set in validSets) {
        exerciseStatsMap.getOrPut(set.exerciseId) { mutableListOf() }.add(set)
    }

    val mainSessions = validSessions.filter { it.isMainWorkout }
    val warmupSessions = validSessions.filter { !it.isMainWorkout }

    // Sessions breakdown
    val sessionItems = validSessions.map { session ->
        val sSets = validSets.filter { it.sessionId == session.id }
        val sTonnage = Math.round(sSets.sumOf { it.weightKg.toDouble() * it.reps })
        val durationMin = (( (session.endTime ?: session.startTime) - session.startTime) / 60000).coerceAtLeast(1)
        WorkoutSessionReportItem(
            id = session.id,
            dateStr = dateFormat.format(Date(session.startTime)),
            name = session.workoutName,
            durationMin = durationMin,
            setsCount = sSets.size,
            tonnageKg = sTonnage,
            isMainWorkout = session.isMainWorkout
        )
    }

    // Top exercises breakdown
    val topExercises = exerciseStatsMap.mapNotNull { (exId, setsList) ->
        val ex = exerciseMap[exId] ?: return@mapNotNull null
        val maxW = setsList.maxOfOrNull { it.weightKg } ?: 0f
        val exTonnage = Math.round(setsList.sumOf { it.weightKg.toDouble() * it.reps })
        TopExerciseReportItem(
            exerciseName = ex.name,
            maxWeightKg = maxW,
            totalSets = setsList.size,
            totalTonnageKg = exTonnage
        )
    }.sortedByDescending { it.totalTonnageKg }

    // Measurements in period
    val periodMeasurements = allMeasurements.filter { it.timestamp >= cutoff }.sortedBy { it.timestamp }
    val firstM = periodMeasurements.firstOrNull()
    val lastM = periodMeasurements.lastOrNull()

    val weightDiff = if (firstM?.weightKg != null && lastM?.weightKg != null && periodMeasurements.size > 1) {
        lastM.weightKg - firstM.weightKg
    } else null

    val bodyFatDiff = if (firstM?.bodyFatPercentage != null && lastM?.bodyFatPercentage != null && periodMeasurements.size > 1) {
        lastM.bodyFatPercentage - firstM.bodyFatPercentage
    } else null

    val chestDiff = if (firstM?.chestCm != null && lastM?.chestCm != null && periodMeasurements.size > 1) {
        lastM.chestCm - firstM.chestCm
    } else null

    val waistDiff = if (firstM?.waistCm != null && lastM?.waistCm != null && periodMeasurements.size > 1) {
        lastM.waistCm - firstM.waistCm
    } else null

    val bicepsDiff = if (firstM?.bicepsCm != null && lastM?.bicepsCm != null && periodMeasurements.size > 1) {
        lastM.bicepsCm - firstM.bicepsCm
    } else null

    val thighsDiff = if (firstM?.thighsCm != null && lastM?.thighsCm != null && periodMeasurements.size > 1) {
        lastM.thighsCm - firstM.thighsCm
    } else null

    return GeneratedReportData(
        periodLabel = period.label,
        dateRangeStr = dateRangeStr,
        completedWorkoutsCount = mainSessions.size,
        warmupSessionsCount = warmupSessions.size,
        totalTrainingMinutes = totalTrainingMinutes,
        totalTonnageKg = totalTonnageKg,
        totalSetsCount = validSets.size,
        totalRepsCount = totalRepsCount,
        weightDiffKg = weightDiff,
        initialWeightKg = firstM?.weightKg,
        finalWeightKg = lastM?.weightKg,
        bodyFatDiff = bodyFatDiff,
        chestDiffCm = chestDiff,
        waistDiffCm = waistDiff,
        bicepsDiffCm = bicepsDiff,
        thighsDiffCm = thighsDiff,
        sessionsList = sessionItems,
        topExercisesList = topExercises
    )
}

fun formatReportToText(report: GeneratedReportData): String {
    val sb = StringBuilder()
    sb.appendLine("📊 RAPORT TRENINGOWY (${report.periodLabel})")
    sb.appendLine("📅 Okres: ${report.dateRangeStr}")
    sb.appendLine("════════════════════════════════════")
    sb.appendLine("🏋️ PODSUMOWANIE AKTYWNOŚCI:")
    sb.appendLine("• Liczba ukończonych treningów: ${report.completedWorkoutsCount}")
    if (report.warmupSessionsCount > 0) {
        sb.appendLine("• Sesje rozgrzewki / mobility: ${report.warmupSessionsCount} (nie wliczane do głównych treningów)")
    }
    val hours = report.totalTrainingMinutes / 60
    val mins = report.totalTrainingMinutes % 60
    sb.appendLine("• Czas spędzony na sali: ${hours}h ${mins}min")
    val tonFormatted = if (report.totalTonnageKg >= 1000) "${report.totalTonnageKg / 1000f} t" else "${report.totalTonnageKg} kg"
    sb.appendLine("• Całkowity przerzucony ciężar (tonaż): $tonFormatted")
    sb.appendLine("• Łącznie ukończonych serii: ${report.totalSetsCount}")
    sb.appendLine("• Łącznie wykonanych powtórzeń: ${report.totalRepsCount}")
    sb.appendLine()

    if (report.weightDiffKg != null || report.chestDiffCm != null || report.waistDiffCm != null) {
        sb.appendLine("📏 PROGRES POMIARÓW CIAŁA:")
        if (report.initialWeightKg != null && report.finalWeightKg != null && report.weightDiffKg != null) {
            val sign = if (report.weightDiffKg > 0) "+" else ""
            sb.appendLine("• Waga: ${report.initialWeightKg} kg ➔ ${report.finalWeightKg} kg ($sign${String.format("%.1f", report.weightDiffKg)} kg)")
        }
        if (report.bodyFatDiff != null) {
            val sign = if (report.bodyFatDiff > 0) "+" else ""
            sb.appendLine("• Tkanka tłuszczowa: $sign${String.format("%.1f", report.bodyFatDiff)} %")
        }
        if (report.waistDiffCm != null) {
            val sign = if (report.waistDiffCm > 0) "+" else ""
            sb.appendLine("• Talia: $sign${String.format("%.1f", report.waistDiffCm)} cm")
        }
        if (report.chestDiffCm != null) {
            val sign = if (report.chestDiffCm > 0) "+" else ""
            sb.appendLine("• Klatka piersiowa: $sign${String.format("%.1f", report.chestDiffCm)} cm")
        }
        if (report.bicepsDiffCm != null) {
            val sign = if (report.bicepsDiffCm > 0) "+" else ""
            sb.appendLine("• Biceps: $sign${String.format("%.1f", report.bicepsDiffCm)} cm")
        }
        if (report.thighsDiffCm != null) {
            val sign = if (report.thighsDiffCm > 0) "+" else ""
            sb.appendLine("• Udo: $sign${String.format("%.1f", report.thighsDiffCm)} cm")
        }
        sb.appendLine()
    }

    if (report.topExercisesList.isNotEmpty()) {
        sb.appendLine("🏆 NAJWIĘKSZA OBJĘTOŚĆ I REKORDY:")
        report.topExercisesList.take(6).forEachIndexed { i, ex ->
            val exTon = if (ex.totalTonnageKg >= 1000) "${ex.totalTonnageKg / 1000f} t" else "${ex.totalTonnageKg} kg"
            sb.appendLine("${i + 1}. ${ex.exerciseName}: maks. ${ex.maxWeightKg} kg | ${ex.totalSets} serii | tonaż: $exTon")
        }
        sb.appendLine()
    }

    if (report.sessionsList.isNotEmpty()) {
        sb.appendLine("📋 WYKAZ TRENINGÓW:")
        report.sessionsList.forEach { s ->
            sb.appendLine("• ${s.dateStr} - ${s.name} (${s.durationMin} min, ${s.setsCount} serii, ${s.tonnageKg} kg)")
        }
    }

    sb.appendLine("════════════════════════════════════")
    sb.appendLine("Wygenerowano w aplikacji Trening Tracker")
    return sb.toString()
}

@Composable
fun WorkoutReportDialog(
    sessions: List<WorkoutSession>,
    allSets: List<WorkoutSetLog>,
    allExercises: List<Exercise>,
    allMeasurements: List<BodyMeasurement>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.DAYS_30) }

    val cutoffTime = remember(selectedPeriod) {
        if (selectedPeriod.days >= 3000) 0L else System.currentTimeMillis() - (selectedPeriod.days.toLong() * 24 * 60 * 60 * 1000L)
    }

    val reportData = remember(selectedPeriod, sessions, allSets, allExercises, allMeasurements) {
        buildReportData(selectedPeriod, sessions, allSets, allExercises, allMeasurements)
    }

    val periodSessions = remember(sessions, cutoffTime) {
        sessions.filter { it.isCompleted && it.startTime >= cutoffTime }.sortedByDescending { it.startTime }
    }
    val periodSessionIds = remember(periodSessions) {
        periodSessions.map { it.id }.toSet()
    }
    val periodSets = remember(allSets, periodSessionIds) {
        allSets.filter { it.isCompleted && it.sessionId in periodSessionIds }
    }
    val periodMeasurements = remember(allMeasurements, cutoffTime) {
        allMeasurements.filter { it.timestamp >= cutoffTime }.sortedByDescending { it.timestamp }
    }

    // Muscle activities
    val muscleActivities = remember(allExercises, periodSets) {
        calculateMuscleActivities(allExercises, periodSets, 3650).values.filter { it.setsCount > 0 }.sortedByDescending { it.setsCount }
    }

    // Best sets map (exercise id -> best set)
    val bestSetsList = remember(periodSets, allExercises) {
        val exMap = allExercises.associateBy { it.id }
        val map = mutableMapOf<Long, WorkoutSetLog>()
        for (set in periodSets) {
            val cur = map[set.exerciseId]
            if (cur == null || (set.weightKg * set.reps > cur.weightKg * cur.reps) || (set.weightKg * set.reps == cur.weightKg * cur.reps && set.weightKg > cur.weightKg)) {
                map[set.exerciseId] = set
            }
        }
        map.mapNotNull { (exId, set) ->
            val ex = exMap[exId] ?: return@mapNotNull null
            Triple(ex, set, (set.weightKg * (1f + set.reps / 30f))) // Estimated 1RM
        }.sortedByDescending { it.second.weightKg }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AthleticOrange.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = AthleticOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Raport z Treningów",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = reportData.dateRangeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Period Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportPeriod.entries.forEach { period ->
                        val isSelected = selectedPeriod == period
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AthleticOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Grid
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Podsumowanie okresu",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ReportStatBox("Treningi", "${reportData.completedWorkoutsCount}", AthleticOrange)
                                    val hours = reportData.totalTrainingMinutes / 60
                                    val mins = reportData.totalTrainingMinutes % 60
                                    ReportStatBox("Czas", "${hours}h ${mins}m", ElectricCyan)
                                    val tonStr = if (reportData.totalTonnageKg >= 1000) "${reportData.totalTonnageKg / 1000}t" else "${reportData.totalTonnageKg}kg"
                                    ReportStatBox("Tonaż", tonStr, SuccessGreen)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ReportStatBox("Serie", "${reportData.totalSetsCount}", GoldPr)
                                    ReportStatBox("Powtórzenia", "${reportData.totalRepsCount}", Color(0xFF38BDF8))
                                    val avgMin = if (reportData.completedWorkoutsCount > 0) reportData.totalTrainingMinutes / reportData.completedWorkoutsCount else 0
                                    ReportStatBox("Śr. trening", "${avgMin} min", Color(0xFFA855F7))
                                }
                            }
                        }
                    }

                    // Body Measurements Delta
                    if (reportData.weightDiffKg != null || reportData.chestDiffCm != null || reportData.waistDiffCm != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.MonitorWeight,
                                            contentDescription = null,
                                            tint = GoldPr,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Zmiana pomiarów ciała",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (reportData.weightDiffKg != null && reportData.initialWeightKg != null && reportData.finalWeightKg != null) {
                                        DeltaRow(
                                            label = "Waga ciała",
                                            startVal = "${reportData.initialWeightKg} kg",
                                            endVal = "${reportData.finalWeightKg} kg",
                                            diff = reportData.weightDiffKg,
                                            unit = "kg"
                                        )
                                    }
                                    reportData.bodyFatDiff?.let {
                                        DeltaRow(label = "Tkanka tłuszczowa", startVal = "-", endVal = "-", diff = it, unit = "%")
                                    }
                                    reportData.waistDiffCm?.let {
                                        DeltaRow(label = "Obwód talii / pasa", startVal = "-", endVal = "-", diff = it, unit = "cm")
                                    }
                                    reportData.chestDiffCm?.let {
                                        DeltaRow(label = "Klatka piersiowa", startVal = "-", endVal = "-", diff = it, unit = "cm")
                                    }
                                    reportData.bicepsDiffCm?.let {
                                        DeltaRow(label = "Ramiona (biceps)", startVal = "-", endVal = "-", diff = it, unit = "cm")
                                    }
                                    reportData.thighsDiffCm?.let {
                                        DeltaRow(label = "Uda", startVal = "-", endVal = "-", diff = it, unit = "cm")
                                    }
                                }
                            }
                        }
                    }

                    // Zaangażowane grupy mięśniowe
                    if (muscleActivities.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Zaangażowane grupy mięśniowe",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    muscleActivities.forEach { act ->
                                        val color = when (act.level) {
                                            4 -> AthleticOrange
                                            3 -> GoldPr
                                            2 -> ElectricCyan
                                            else -> Color(0xFF38BDF8)
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(act.muscle.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("${act.setsCount} serii", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("(${act.totalTonnage.toLong()} kg)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Najlepsze serie (PR) w okresie
                    if (bestSetsList.isNotEmpty()) {
                        item {
                            Text(
                                text = "Najlepsze serie (PR) w okresie (${bestSetsList.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(bestSetsList.take(8)) { (ex, set, est1rm) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ex.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "Szacowane 1RM: ${String.format(Locale.US, "%.1f", est1rm)} kg",
                                            fontSize = 11.sp,
                                            color = GoldPr
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoldPr.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = GoldPr, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (set.weightKg > 0) "${set.weightKg} kg × ${set.reps}" else "${set.reps} powt.",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = GoldPr,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Top exercises in this period
                    if (reportData.topExercisesList.isNotEmpty()) {
                        item {
                            Text(
                                text = "Główne ćwiczenia i rekordy ciężaru",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(reportData.topExercisesList.take(6)) { ex ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = ex.exerciseName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "${ex.totalSets} serii • tonaż: ${if (ex.totalTonnageKg >= 1000) "${ex.totalTonnageKg / 1000f}t" else "${ex.totalTonnageKg}kg"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AthleticOrange.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Maks ${ex.maxWeightKg} kg",
                                            fontWeight = FontWeight.Bold,
                                            color = AthleticOrange,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Wszystkie pomiary z okresu
                    if (periodMeasurements.isNotEmpty()) {
                        item {
                            Text(
                                text = "Wszystkie pomiary z okresu (${periodMeasurements.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(periodMeasurements) { m ->
                            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = dateFormat.format(Date(m.timestamp)),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = ElectricCyan
                                        )
                                        Text(
                                            text = "${m.weightKg ?: "-"} kg",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        m.bodyFatPercentage?.let { Text("BF: $it%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        m.waistCm?.let { Text("Pas: ${it}cm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        m.chestCm?.let { Text("Klatka: ${it}cm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        m.bicepsCm?.let { Text("Biceps: ${it}cm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    }
                                    if (m.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(m.notes, fontSize = 11.sp, color = Color(0xFF94A3B8), maxLines = 2)
                                    }
                                }
                            }
                        }
                    }

                    // Completed Workouts List
                    if (reportData.sessionsList.isNotEmpty()) {
                        item {
                            Text(
                                text = "Wykonane sesje (Główne: ${reportData.completedWorkoutsCount}${if (reportData.warmupSessionsCount > 0) ", Rozgrzewki: ${reportData.warmupSessionsCount}" else ""})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(reportData.sessionsList) { sessionItem ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = sessionItem.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (!sessionItem.isMainWorkout) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = SuccessGreen.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "Rozgrzewka",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SuccessGreen,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${sessionItem.dateStr} • ${sessionItem.durationMin} min",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${sessionItem.tonnageKg} kg",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${sessionItem.setsCount} serii",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Exports: HTML & Excel (CSV)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            HtmlReportExporter.generateAndShareHtmlReport(
                                context = context,
                                reportData = reportData,
                                sessions = sessions,
                                allSets = allSets,
                                allExercises = allExercises,
                                allMeasurements = allMeasurements,
                                cutoffTime = cutoffTime
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Raport HTML", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            ExcelReportExporter.generateAndShareExcelReport(
                                context = context,
                                reportData = reportData,
                                sessions = sessions,
                                allSets = allSets,
                                allExercises = allExercises,
                                allMeasurements = allMeasurements,
                                cutoffTime = cutoffTime
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Raport Excel", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Action Buttons: Copy to Clipboard & Share Plain Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = formatReportToText(reportData)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Raport Treningowy", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Raport skopiowany do schowka!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kopiuj tekst", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val text = formatReportToText(reportData)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Raport Treningowy - ${reportData.periodLabel}")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Udostępnij raport"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Udostępnij", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportStatBox(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = accent)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DeltaRow(label: String, startVal: String, endVal: String, diff: Float, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val sign = if (diff > 0) "+" else ""
            val diffColor = if (label.contains("Talia") || label.contains("tłuszcz")) {
                if (diff <= 0) SuccessGreen else MaterialTheme.colorScheme.error
            } else {
                if (diff >= 0) SuccessGreen else MaterialTheme.colorScheme.error
            }
            Text(
                text = "$sign${String.format("%.1f", diff)} $unit",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = diffColor
            )
        }
    }
}
