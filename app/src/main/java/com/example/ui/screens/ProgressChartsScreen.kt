package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSetLog
import com.example.ui.components.ChartPoint
import com.example.ui.components.LineChartComposable
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.ChartMetric
import kotlin.math.roundToInt

@Composable
fun ProgressChartsScreen(
    exercises: List<Exercise>,
    allCompletedSets: List<WorkoutSetLog>,
    getChartPoints: (List<WorkoutSetLog>, ChartMetric) -> List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    // Default to an exercise that likely has weights, e.g. Military press or Sumo deadlift
    val weightedExercises = remember(exercises) {
        exercises.filter { it.section.startsWith("Trening") }
    }

    var selectedExercise by remember(weightedExercises) {
        mutableStateOf(weightedExercises.firstOrNull() ?: exercises.firstOrNull())
    }

    var isDropdownExpanded by remember { mutableStateOf(false) }
    var selectedMetricIndex by remember { mutableIntStateOf(0) }

    val isDist = selectedExercise?.isDistanceBased() == true
    val isTime = selectedExercise?.isTimeBased() == true
    val isBw = selectedExercise?.isBodyweightBased() == true

    val metrics = remember(isDist, isTime, isBw) {
        when {
            isDist -> listOf(
                "Dystans (m)" to ChartMetric.DISTANCE,
                "Suma dystansu" to ChartMetric.TOTAL_VOLUME
            )
            isTime -> listOf(
                "Czas serii (s)" to ChartMetric.DURATION,
                "Suma czasu" to ChartMetric.TOTAL_VOLUME
            )
            isBw -> listOf(
                "Maks. powtórzenia" to ChartMetric.REPS,
                "Objętość powtórzeń" to ChartMetric.TOTAL_VOLUME
            )
            else -> listOf(
                "Maks. Ciężar" to ChartMetric.MAX_WEIGHT,
                "Objętość serii" to ChartMetric.TOTAL_VOLUME,
                "Szacowane 1RM" to ChartMetric.ESTIMATED_1RM
            )
        }
    }

    val safeMetricIndex = selectedMetricIndex.coerceIn(0, metrics.size - 1)
    val currentMetric = metrics[safeMetricIndex].second

    val exerciseSets = remember(selectedExercise, allCompletedSets) {
        if (selectedExercise == null) emptyList() else {
            allCompletedSets.filter { it.exerciseId == selectedExercise!!.id && it.isCompleted }
        }
    }

    val chartPoints = remember(exerciseSets, currentMetric) {
        getChartPoints(exerciseSets, currentMetric)
    }

    // Exercise specific stats
    val prWeight = remember(exerciseSets) {
        exerciseSets.maxOfOrNull { it.weightKg } ?: 0f
    }
    val totalTonnage = remember(exerciseSets) {
        exerciseSets.sumOf { (it.weightKg * it.reps).toDouble() }.toLong()
    }
    val setsCount = exerciseSets.size
    val est1RmMax = remember(exerciseSets) {
        val maxVal = exerciseSets.maxOfOrNull { it.weightKg * (1f + it.reps / 30f) } ?: 0f
        (maxVal * 10).roundToInt() / 10f
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Wykresy & Postępy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Śledź progresję siłową, tonaż oraz rekordy",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Exercise Selector Dropdown / Card
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDropdownExpanded = true },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = AthleticOrange
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Wybrane ćwiczenie",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedExercise?.let { "${it.code} ${it.name}" } ?: "Wybierz ćwiczenie",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Rozwiń listę"
                        )
                    }
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    exercises.forEach { ex ->
                        DropdownMenuItem(
                            text = {
                                Text("${ex.code} ${ex.name} (${ex.section})")
                            },
                            onClick = {
                                selectedExercise = ex
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Metric Selector Tabs
        item {
            TabRow(
                selectedTabIndex = safeMetricIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
            ) {
                metrics.forEachIndexed { index, (label, _) ->
                    Tab(
                        selected = safeMetricIndex == index,
                        onClick = { selectedMetricIndex = index },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (safeMetricIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }

        // Main Chart Component
        item {
            val unit = when {
                isDist -> "m"
                isTime -> "s"
                isBw -> "powt."
                currentMetric == ChartMetric.MAX_WEIGHT -> "kg"
                currentMetric == ChartMetric.TOTAL_VOLUME -> "kg"
                currentMetric == ChartMetric.ESTIMATED_1RM -> "kg"
                else -> ""
            }
            val chartColor = when {
                isDist -> AthleticOrange
                isTime -> ElectricCyan
                currentMetric == ChartMetric.MAX_WEIGHT -> AthleticOrange
                currentMetric == ChartMetric.TOTAL_VOLUME -> ElectricCyan
                currentMetric == ChartMetric.ESTIMATED_1RM -> GoldPr
                else -> AthleticOrange
            }

            LineChartComposable(
                points = chartPoints,
                unit = unit,
                lineColor = chartColor,
                accentColor = SuccessGreen
            )
        }

        // Key stats grid for the selected exercise
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isDist) {
                    val maxDist = exerciseSets.mapNotNull { it.distanceMeters }.maxOrNull() ?: 0f
                    val totalDist = exerciseSets.mapNotNull { it.distanceMeters }.sum()
                    StatCard(
                        title = "Maks. Dystans",
                        value = "${maxDist.toInt()} m",
                        subtitle = "Najdłuższa seria",
                        icon = Icons.Default.EmojiEvents,
                        accent = GoldPr,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Suma dystansu",
                        value = "${totalDist.toInt()} m",
                        subtitle = "Łącznie pokonane",
                        icon = Icons.Default.ShowChart,
                        accent = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                } else if (isTime) {
                    val maxSec = exerciseSets.mapNotNull { it.timeSeconds }.maxOrNull() ?: 0
                    val totalSec = exerciseSets.mapNotNull { it.timeSeconds }.sum()
                    val maxStr = if (maxSec >= 60) "${maxSec / 60}m ${maxSec % 60}s" else "${maxSec}s"
                    val totalStr = if (totalSec >= 60) "${totalSec / 60} min" else "${totalSec} s"
                    StatCard(
                        title = "Maks. Czas",
                        value = maxStr,
                        subtitle = "Najdłuższa seria",
                        icon = Icons.Default.EmojiEvents,
                        accent = GoldPr,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Łączny Czas",
                        value = totalStr,
                        subtitle = "Suma czasu trwania",
                        icon = Icons.Default.ShowChart,
                        accent = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                } else if (isBw) {
                    val maxReps = exerciseSets.maxOfOrNull { it.reps } ?: 0
                    val totalReps = exerciseSets.sumOf { it.reps }
                    StatCard(
                        title = "Maks. Powtórzenia",
                        value = "$maxReps",
                        subtitle = "Najlepsza seria",
                        icon = Icons.Default.EmojiEvents,
                        accent = GoldPr,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Łącznie powtórzeń",
                        value = "$totalReps",
                        subtitle = "Suma powtórzeń",
                        icon = Icons.Default.ShowChart,
                        accent = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    StatCard(
                        title = "Rekord (PR)",
                        value = "${prWeight.roundToInt()} kg",
                        subtitle = "Maks. seria",
                        icon = Icons.Default.EmojiEvents,
                        accent = GoldPr,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Szacowane 1RM",
                        value = "${est1RmMax} kg",
                        subtitle = "Wzór Epleya",
                        icon = Icons.Default.TrendingUp,
                        accent = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isDist || isTime) {
                    StatCard(
                        title = "Średnia na serię",
                        value = if (isDist) {
                            val avg = if (setsCount > 0) (exerciseSets.mapNotNull { it.distanceMeters }.sum() / setsCount).toInt() else 0
                            "$avg m"
                        } else {
                            val avg = if (setsCount > 0) exerciseSets.mapNotNull { it.timeSeconds }.sum() / setsCount else 0
                            if (avg >= 60) "${avg / 60}m ${avg % 60}s" else "${avg}s"
                        },
                        subtitle = "Średnia z serii",
                        icon = Icons.Default.ShowChart,
                        accent = AthleticOrange,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    StatCard(
                        title = "Całkowity tonaż",
                        value = if (totalTonnage >= 1000) "${totalTonnage / 1000} t" else "${totalTonnage} kg",
                        subtitle = "Suma obciążenia",
                        icon = Icons.Default.ShowChart,
                        accent = AthleticOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
                StatCard(
                    title = "Zapisane serie",
                    value = "$setsCount",
                    subtitle = "Liczba serii",
                    icon = Icons.Default.Repeat,
                    accent = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick tip for progressive overload
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Wskazówka progresji (Progressive Overload)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Staraj się w każdej kolejnej jednostce treningowej dołożyć chociaż 1 powtórzenie lub 1-2.5 kg przy zachowaniu nienagannej techniki. Przy 4 treningach bez postępu zastosuj zasadę -10% obciążenia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
