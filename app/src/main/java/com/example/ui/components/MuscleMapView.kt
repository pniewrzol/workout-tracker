package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BodyMeasurement
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSetLog
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import kotlin.math.abs

enum class MuscleGroup(val displayName: String, val isFront: Boolean) {
    CHEST("Klatka piersiowa", true),
    SHOULDERS("Barki (Naramienne)", true),
    BICEPS("Bicepsy", true),
    ABS("Brzuch / Core", true),
    QUADS("Czworogłowe (Uda)", true),
    FOREARMS("Przedramiona", true),
    BACK("Plecy (Najszerszy / Czworoboczny)", false),
    TRICEPS("Tricepsy", false),
    GLUTES("Pośladki", false),
    HAMSTRINGS("Dwugłowe (Tył ud)", false),
    CALVES("Łydki", false)
}

enum class MusclePeriod(val label: String, val days: Int) {
    WEEK("7 dni", 7),
    MONTH("30 dni", 30),
    QUARTER("90 dni", 90),
    ALL("Wszystko", 3650)
}

data class MuscleActivity(
    val muscle: MuscleGroup,
    val setsCount: Int,
    val totalTonnage: Float,
    val exerciseNames: List<String>
) {
    val level: Int = when {
        setsCount >= 16 -> 4 // Przetrenowana / Maksymalne obciążenie
        setsCount >= 10 -> 3 // Optymalny bodziec hipertroficzny
        setsCount >= 5  -> 2 // Umiarkowane obciążenie / Utrzymanie
        setsCount >= 1  -> 1 // Wstępna stymulacja / Lekka
        else -> 0 // Wypoczęta / Brak stymulacji
    }

    val isOvertrained: Boolean get() = setsCount >= 16

    val statusLabel: String get() = when {
        setsCount >= 20 -> "Przetrenowana ⚠️"
        setsCount >= 16 -> "Przeciążona ⚠️"
        setsCount >= 10 -> "Optymalna ✓"
        setsCount >= 5  -> "Umiarkowana"
        setsCount >= 1  -> "Lekka"
        else -> "Wypoczęta"
    }

    val statusColor: Color get() = when {
        setsCount >= 16 -> Color(0xFFEF4444) // Czerwony - Przetrenowane
        setsCount >= 10 -> Color(0xFF10B981) // Zielony - Optymalne
        setsCount >= 5  -> Color(0xFF06B6D4) // Błękit - Umiarkowane
        setsCount >= 1  -> Color(0xFF38BDF8) // Jasnoniebieski - Lekka stymulacja
        else -> Color(0xFF334155) // Ciemny grafit - Wypoczęta
    }

    val recoveryHoursRemaining: Int get() = when {
        setsCount >= 20 -> 72
        setsCount >= 16 -> 48
        setsCount >= 10 -> 36
        setsCount >= 5  -> 24
        setsCount >= 1  -> 12
        else -> 0
    }
}

fun calculateMuscleActivities(
    exercises: List<Exercise>,
    completedSets: List<WorkoutSetLog>,
    periodDays: Int
): Map<MuscleGroup, MuscleActivity> {
    val now = System.currentTimeMillis()
    val cutoff = if (periodDays >= 3000) 0L else now - (periodDays.toLong() * 24 * 60 * 60 * 1000L)

    val validSets = completedSets.filter { it.isCompleted && (cutoff == 0L || it.timestamp >= cutoff) }
    val exerciseMap = exercises.associateBy { it.id }

    val setCounts = mutableMapOf<MuscleGroup, Int>()
    val tonnages = mutableMapOf<MuscleGroup, Float>()
    val exerciseNames = mutableMapOf<MuscleGroup, MutableSet<String>>()

    MuscleGroup.entries.forEach {
        setCounts[it] = 0
        tonnages[it] = 0f
        exerciseNames[it] = mutableSetOf()
    }

    for (set in validSets) {
        val exercise = exerciseMap[set.exerciseId] ?: continue
        val tonnage = set.weightKg * set.reps.toFloat()
        val textToMatch = "${exercise.name} ${exercise.bodyPart} ${exercise.primaryMuscles} ${exercise.secondaryMuscles} ${exercise.section}".lowercase()

        val hitMuscles = mutableSetOf<MuscleGroup>()

        if (textToMatch.contains("klatka") || textToMatch.contains("piersiow") || textToMatch.contains("chest") || textToMatch.contains("wyciskanie") || textToMatch.contains("dips")) {
            hitMuscles.add(MuscleGroup.CHEST)
        }
        if (textToMatch.contains("bark") || textToMatch.contains("naramienn") || textToMatch.contains("żołnierskie") || textToMatch.contains("overhead") || textToMatch.contains("ohp")) {
            hitMuscles.add(MuscleGroup.SHOULDERS)
        }
        if (textToMatch.contains("biceps") || textToMatch.contains("ramienn") || textToMatch.contains("uginanie ramion") || textToMatch.contains("chin-up")) {
            hitMuscles.add(MuscleGroup.BICEPS)
        }
        if (textToMatch.contains("triceps") || textToMatch.contains("francuskie") || textToMatch.contains("trójgłow") || textToMatch.contains("wyciskanie wąsko") || textToMatch.contains("dips")) {
            hitMuscles.add(MuscleGroup.TRICEPS)
        }
        if (textToMatch.contains("brzuch") || textToMatch.contains("core") || textToMatch.contains("plank") || textToMatch.contains("skośne") || textToMatch.contains("pallof") || textToMatch.contains("kółko")) {
            hitMuscles.add(MuscleGroup.ABS)
        }
        if (textToMatch.contains("plecy") || textToMatch.contains("najszersz") || textToMatch.contains("wiosłow") || textToMatch.contains("podciąganie") || textToMatch.contains("drążk") || textToMatch.contains("chin-up") || textToMatch.contains("czworoboczn")) {
            hitMuscles.add(MuscleGroup.BACK)
        }
        if (textToMatch.contains("czworo") || textToMatch.contains("przysiad") || textToMatch.contains("wykroki") || textToMatch.contains("quad") || textToMatch.contains("schody") || textToMatch.contains("row erg") || textToMatch.contains("air bike")) {
            hitMuscles.add(MuscleGroup.QUADS)
        }
        if (textToMatch.contains("dwugłow") || textToMatch.contains("kulszow") || textToMatch.contains("martwy ciąg") || textToMatch.contains("rdl") || textToMatch.contains("hamstring")) {
            hitMuscles.add(MuscleGroup.HAMSTRINGS)
        }
        if (textToMatch.contains("poślad") || textToMatch.contains("glute") || textToMatch.contains("hip thrust") || textToMatch.contains("spacer farmera")) {
            hitMuscles.add(MuscleGroup.GLUTES)
        }
        if (textToMatch.contains("łydk") || textToMatch.contains("brzuchat") || textToMatch.contains("wspięcia") || textToMatch.contains("schody")) {
            hitMuscles.add(MuscleGroup.CALVES)
        }
        if (textToMatch.contains("przedramion") || textToMatch.contains("chwyt") || textToMatch.contains("farmer") || textToMatch.contains("hold")) {
            hitMuscles.add(MuscleGroup.FOREARMS)
        }

        for (m in hitMuscles) {
            setCounts[m] = (setCounts[m] ?: 0) + 1
            tonnages[m] = (tonnages[m] ?: 0f) + tonnage
            exerciseNames[m]?.add(exercise.name)
        }
    }

    return MuscleGroup.entries.associateWith { m ->
        MuscleActivity(
            muscle = m,
            setsCount = setCounts[m] ?: 0,
            totalTonnage = tonnages[m] ?: 0f,
            exerciseNames = exerciseNames[m]?.toList() ?: emptyList()
        )
    }
}

fun getMuscleColor(level: Int, isSelected: Boolean): Color {
    if (isSelected) return GoldPr
    return when (level) {
        4 -> Color(0xFFEF4444) // Czerwony - Przetrenowane (16+ serii)
        3 -> Color(0xFF10B981) // Zielony - Optymalne (10-15 serii)
        2 -> Color(0xFF06B6D4) // Błękit - Umiarkowane (5-9 serii)
        1 -> Color(0xFF38BDF8) // Jasnoniebieski - Lekka stymulacja (1-4 serie)
        else -> Color(0xFF243044) // Ciemny grafit - Wypoczęta (0 serii)
    }
}

@Composable
fun MuscleMapView(
    exercises: List<Exercise>,
    completedSets: List<WorkoutSetLog>,
    modifier: Modifier = Modifier,
    latestMeasurement: BodyMeasurement? = null,
    initialSelectedMuscle: MuscleGroup? = null
) {
    // Domyślnie 30 dni, aby użytkownik od razu widział swoje serie
    var selectedPeriod by remember { mutableStateOf(MusclePeriod.MONTH) }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(initialSelectedMuscle) }
    // 0: Sylwetka Człowieka (Przód i Tył), 1: Karty Przetrenowania
    var viewMode by remember { mutableIntStateOf(0) }

    val activities = remember(exercises, completedSets, selectedPeriod) {
        calculateMuscleActivities(exercises, completedSets, selectedPeriod.days)
    }

    val totalActiveSets = remember(activities) {
        activities.values.sumOf { it.setsCount }
    }

    val totalTonnage = remember(activities) {
        activities.values.sumOf { it.totalTonnage.toDouble() }.toFloat()
    }

    val overtrainedCount = remember(activities) {
        activities.values.count { it.isOvertrained }
    }
    val optimalCount = remember(activities) {
        activities.values.count { it.level == 3 }
    }
    val moderateCount = remember(activities) {
        activities.values.count { it.level in 1..2 }
    }
    val restedCount = remember(activities) {
        activities.values.count { it.level == 0 }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Okres filtrowania
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MusclePeriod.entries.forEach { period ->
                val isSelected = selectedPeriod == period
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedPeriod = period },
                    label = { Text(period.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AthleticOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Baner Stanu Przetrenowania i Obciążenia
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (overtrainedCount > 0) Color(0xFF450A0A) else Color(0xFF0F172A)
            ),
            border = BorderStroke(
                1.dp,
                if (overtrainedCount > 0) Color(0xFFEF4444).copy(alpha = 0.6f) else Color(0xFF334155)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (overtrainedCount > 0) Icons.Default.Warning else Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (overtrainedCount > 0) Color(0xFFEF4444) else ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (overtrainedCount > 0) "Wykryto Przetrenowane Partie!" else "Stan Obciążenia Mięśniowego",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "$totalActiveSets serii • ${String.format("%.0f", totalTonnage)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (overtrainedCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Masz $overtrainedCount partie z bardzo wysoką objętością (16+ serii w wybranym okresie). Zadbaj o 48–72h odpoczynku, by uniknąć przeciążenia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Podsumowanie statusów
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusSummaryPill(
                        label = "Przetrenowane",
                        count = overtrainedCount,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    StatusSummaryPill(
                        label = "Optymalne",
                        count = optimalCount,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    StatusSummaryPill(
                        label = "Umiarkowane",
                        count = moderateCount,
                        color = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f)
                    )
                    StatusSummaryPill(
                        label = "Wypoczęte",
                        count = restedCount,
                        color = Color(0xFF64748B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Przełącznik widoków: Sylwetka vs Karty
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("Wizualizacja Sylwetki", "Karty Przetrenowania")
                tabs.forEachIndexed { index, label ->
                    val active = viewMode == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (active) AthleticOrange else Color.Transparent)
                            .clickable { viewMode = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.AccessibilityNew else Icons.Default.ViewAgenda,
                                contentDescription = null,
                                tint = if (active) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (viewMode == 0) {
            // Czysty, nowoczesny model sylwetki człowieka (PRZÓD i TYŁ) z kolorami stref obciążenia
            HumanBodyHeatmapView(
                activities = activities,
                selectedMuscle = selectedMuscle,
                onSelectMuscle = { selectedMuscle = if (selectedMuscle == it) null else it }
            )
        } else {
            // Matryca Przetrenowania (Karty posortowane malejąco z paskami postępu)
            OvertrainingCardsGrid(
                activities = activities,
                selectedMuscle = selectedMuscle,
                onSelectMuscle = { selectedMuscle = if (selectedMuscle == it) null else it }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Karta Szczegółów Wybranej Partii Mięśniowej
        selectedMuscle?.let { muscle ->
            val act = activities[muscle]
            MuscleDetailCard(
                muscle = muscle,
                activity = act,
                onDismiss = { selectedMuscle = null }
            )
        }
    }
}

@Composable
fun StatusSummaryPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Nowoczesna wizualizacja sylwetki człowieka z podświetlanymi strefami mięśniowymi.
 * Posiada stałe proporcje (brak rozciągania) i responsywne rozmieszczenie elementów.
 */
@Composable
fun HumanBodyHeatmapView(
    activities: Map<MuscleGroup, MuscleActivity>,
    selectedMuscle: MuscleGroup?,
    onSelectMuscle: (MuscleGroup) -> Unit
) {
    // 0: Obie sylwetki (Przód i Tył), 1: Tylko Przód, 2: Tylko Tył
    var silhouetteViewMode by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Nagłówek i przełącznik widoku
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Model Obciążenia Mięśni",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Dotknij partię na sylwetce, by sprawdzić",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Mini przełącznik: Obie | Przód | Tył
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        val modes = listOf("Obie", "Przód", "Tył")
                        modes.forEachIndexed { idx, title ->
                            val isSelected = silhouetteViewMode == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) AthleticOrange else Color.Transparent)
                                    .clickable { silhouetteViewMode = idx }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Estetyczna 2-rzędowa legenda, która nigdy się nie rozjeżdża na wąskich ekranach
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LegendBadge(
                    color = Color(0xFFEF4444),
                    label = "Przetrenowana",
                    range = "16+ s.",
                    modifier = Modifier.weight(1f)
                )
                LegendBadge(
                    color = Color(0xFF10B981),
                    label = "Optymalna",
                    range = "10–15 s.",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LegendBadge(
                    color = Color(0xFF06B6D4),
                    label = "Umiarkowana",
                    range = "5–9 s.",
                    modifier = Modifier.weight(1f)
                )
                LegendBadge(
                    color = Color(0xFF64748B),
                    label = "Wypoczęta",
                    range = "0–4 s.",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Obszar wizualizacji sylwetki z zachowaniem idealnych proporcji
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
                    .background(Color(0xFF070B14), RoundedCornerShape(14.dp))
                    .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(14.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (silhouetteViewMode) {
                    0 -> {
                        // Obie sylwetki człowieka obok siebie (PRZÓD i TYŁ)
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Kolumna PRZÓD
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = "PRZÓD",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ModernBodySilhouetteCanvas(
                                        isFront = true,
                                        activities = activities,
                                        selectedMuscle = selectedMuscle,
                                        onSelectMuscle = onSelectMuscle
                                    )
                                }
                            }

                            // Linia podziału
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(250.dp)
                                    .background(Color(0xFF1E293B))
                            )

                            // Kolumna TYŁ
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = "TYŁ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AthleticOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ModernBodySilhouetteCanvas(
                                        isFront = false,
                                        activities = activities,
                                        selectedMuscle = selectedMuscle,
                                        onSelectMuscle = onSelectMuscle
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Tylko PRZÓD (wycentrowany, powiększony widok)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = "SYLWETKA: PRZÓD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                ModernBodySilhouetteCanvas(
                                    isFront = true,
                                    activities = activities,
                                    selectedMuscle = selectedMuscle,
                                    onSelectMuscle = onSelectMuscle
                                )
                            }
                        }
                    }
                    else -> {
                        // Tylko TYŁ (wycentrowany, powiększony widok)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = "SYLWETKA: TYŁ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AthleticOrange,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                ModernBodySilhouetteCanvas(
                                    isFront = false,
                                    activities = activities,
                                    selectedMuscle = selectedMuscle,
                                    onSelectMuscle = onSelectMuscle
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Szybki pasek wyboru partii z liczbą serii
            Text(
                text = "Wszystkie grupy mięśniowe (kliknij, by podświetlić):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(MuscleGroup.entries) { muscle ->
                    val act = activities[muscle]
                    val isSelected = selectedMuscle == muscle
                    val color = getMuscleColor(act?.level ?: 0, isSelected)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) GoldPr.copy(alpha = 0.25f) else Color(0xFF1E293B),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) GoldPr else if (act?.isOvertrained == true) Color(0xFFEF4444) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .clickable { onSelectMuscle(muscle) }
                            .testTag("chip_muscle_${muscle.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = muscle.displayName.substringBefore(" ("),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${act?.setsCount ?: 0}s",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendBadge(
    color: Color,
    label: String,
    range: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = range,
                fontSize = 9.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Wektorowy Canvas rysujący atletyczną sylwetkę człowieka (Przód / Tył)
 * z zachowaniem idealnych proporcji (aspect ratio lock),
 * podświetlaniem stref obciążenia i obsługą kliknięć.
 */
@Composable
fun ModernBodySilhouetteCanvas(
    isFront: Boolean,
    activities: Map<MuscleGroup, MuscleActivity>,
    selectedMuscle: MuscleGroup? = null,
    onSelectMuscle: (MuscleGroup) -> Unit = {}
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isFront) {
                detectTapGestures { tapOffset ->
                    val refW = 200f
                    val refH = 340f
                    val scale = minOf(size.width / refW, size.height / refH)
                    val drawnW = refW * scale
                    val drawnH = refH * scale
                    val offX = (size.width - drawnW) / 2f
                    val offY = (size.height - drawnH) / 2f

                    val localX = (tapOffset.x - offX) / scale
                    val localY = (tapOffset.y - offY) / scale

                    val hitMuscle = if (isFront) {
                        when {
                            // Barki
                            localY in 62f..100f && (localX in 50f..86f || localX in 114f..150f) -> MuscleGroup.SHOULDERS
                            // Klatka
                            localY in 66f..98f && localX in 74f..126f -> MuscleGroup.CHEST
                            // Bicepsy
                            localY in 98f..136f && (localX in 48f..76f || localX in 124f..152f) -> MuscleGroup.BICEPS
                            // Brzuch / Core
                            localY in 96f..148f && localX in 76f..124f -> MuscleGroup.ABS
                            // Przedramiona
                            localY in 134f..186f && (localX in 42f..74f || localX in 126f..158f) -> MuscleGroup.FOREARMS
                            // Czworogłowe (Uda)
                            localY in 148f..228f && localX in 70f..130f -> MuscleGroup.QUADS
                            // Łydki
                            localY in 228f..304f && localX in 72f..128f -> MuscleGroup.CALVES
                            else -> null
                        }
                    } else {
                        when {
                            // Plecy
                            localY in 64f..136f && localX in 74f..126f -> MuscleGroup.BACK
                            // Barki
                            localY in 62f..100f && (localX in 50f..86f || localX in 114f..150f) -> MuscleGroup.SHOULDERS
                            // Tricepsy
                            localY in 98f..136f && (localX in 48f..76f || localX in 124f..152f) -> MuscleGroup.TRICEPS
                            // Pośladki
                            localY in 134f..170f && localX in 74f..126f -> MuscleGroup.GLUTES
                            // Przedramiona
                            localY in 134f..186f && (localX in 42f..74f || localX in 126f..158f) -> MuscleGroup.FOREARMS
                            // Dwugłowe (Tył ud)
                            localY in 170f..228f && localX in 70f..130f -> MuscleGroup.HAMSTRINGS
                            // Łydki
                            localY in 228f..304f && localX in 72f..128f -> MuscleGroup.CALVES
                            else -> null
                        }
                    }

                    if (hitMuscle != null) {
                        onSelectMuscle(hitMuscle)
                    }
                }
            }
    ) {
        val refW = 200f
        val refH = 340f
        val scale = minOf(size.width / refW, size.height / refH)
        val drawnW = refW * scale
        val drawnH = refH * scale
        val offX = (size.width - drawnW) / 2f
        val offY = (size.height - drawnH) / 2f
        val cx = offX + 100f * scale

        fun toX(x: Float) = offX + x * scale
        fun toY(y: Float) = offY + y * scale
        fun toS(s: Float) = s * scale

        // 1. GŁOWA i SZYJA
        drawCircle(
            color = Color(0xFF1E293B),
            radius = toS(16f),
            center = Offset(cx, toY(32f))
        )
        drawCircle(
            color = Color(0xFF334155),
            radius = toS(16f),
            center = Offset(cx, toY(32f)),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Szyja
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(toX(93f), toY(47f)),
            size = Size(toS(14f), toS(18f)),
            cornerRadius = CornerRadius(toS(4f))
        )

        // Dłonie
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(toX(46f), toY(180f)),
            size = Size(toS(14f), toS(18f)),
            cornerRadius = CornerRadius(toS(5f))
        )
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(toX(140f), toY(180f)),
            size = Size(toS(14f), toS(18f)),
            cornerRadius = CornerRadius(toS(5f))
        )

        // Stopy
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(toX(74f), toY(300f)),
            size = Size(toS(18f), toS(12f)),
            cornerRadius = CornerRadius(toS(5f))
        )
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(toX(108f), toY(300f)),
            size = Size(toS(18f), toS(12f)),
            cornerRadius = CornerRadius(toS(5f))
        )

        // Funkcja pomocnicza do rysowania partii mięśniowej
        fun drawMuscle(
            color: Color,
            isSelected: Boolean,
            isOvertrained: Boolean,
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            rx: Float = 6f
        ) {
            val corner = CornerRadius(toS(rx), toS(rx))
            val topLeft = Offset(toX(x), toY(y))
            val muscleSize = Size(toS(w), toS(h))

            // Wypełnienie kolorem
            drawRoundRect(
                color = color,
                topLeft = topLeft,
                size = muscleSize,
                cornerRadius = corner
            )

            // Obrys partii (Złoty gdy zaznaczona, czerwony gdy przetrenowana)
            val strokeColor = when {
                isSelected -> GoldPr
                isOvertrained -> Color(0xFFEF4444).copy(alpha = 0.9f)
                else -> Color(0xFF0F172A)
            }
            val strokeWidth = when {
                isSelected -> 2.5.dp.toPx()
                isOvertrained -> 1.8.dp.toPx()
                else -> 1.2.dp.toPx()
            }

            drawRoundRect(
                color = strokeColor,
                topLeft = topLeft,
                size = muscleSize,
                cornerRadius = corner,
                style = Stroke(width = strokeWidth)
            )

            // Wyraźny wskaźnik ostrzegawczy dla przetrenowanej partii
            if (isOvertrained) {
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = toS(3.5f),
                    center = Offset(toX(x + w - 4f), toY(y + 4f))
                )
                drawCircle(
                    color = Color.White,
                    radius = toS(1.5f),
                    center = Offset(toX(x + w - 4f), toY(y + 4f))
                )
            }
        }

        if (isFront) {
            // ================= PRZÓD =================

            // 1. BARKI (SHOULDERS) - lewy i prawy
            val actShoulders = activities[MuscleGroup.SHOULDERS]
            val isShouldersSelected = selectedMuscle == MuscleGroup.SHOULDERS
            val shoulderColor = getMuscleColor(actShoulders?.level ?: 0, isShouldersSelected)
            val isOverShoulders = actShoulders?.isOvertrained == true
            drawMuscle(shoulderColor, isShouldersSelected, isOverShoulders, 54f, 66f, 22f, 28f, rx = 8f)
            drawMuscle(shoulderColor, isShouldersSelected, isOverShoulders, 124f, 66f, 22f, 28f, rx = 8f)

            // 2. KLATKA PIERSIOWA (CHEST)
            val actChest = activities[MuscleGroup.CHEST]
            val isChestSelected = selectedMuscle == MuscleGroup.CHEST
            val chestColor = getMuscleColor(actChest?.level ?: 0, isChestSelected)
            val isOverChest = actChest?.isOvertrained == true
            drawMuscle(chestColor, isChestSelected, isOverChest, 77f, 68f, 21f, 26f, rx = 6f)
            drawMuscle(chestColor, isChestSelected, isOverChest, 102f, 68f, 21f, 26f, rx = 6f)

            // 3. BICEPSY (BICEPS)
            val actBiceps = activities[MuscleGroup.BICEPS]
            val isBicepsSelected = selectedMuscle == MuscleGroup.BICEPS
            val bicepsColor = getMuscleColor(actBiceps?.level ?: 0, isBicepsSelected)
            val isOverBiceps = actBiceps?.isOvertrained == true
            drawMuscle(bicepsColor, isBicepsSelected, isOverBiceps, 52f, 98f, 18f, 32f, rx = 7f)
            drawMuscle(bicepsColor, isBicepsSelected, isOverBiceps, 130f, 98f, 18f, 32f, rx = 7f)

            // 4. BRZUCH / CORE (ABS)
            val actAbs = activities[MuscleGroup.ABS]
            val isAbsSelected = selectedMuscle == MuscleGroup.ABS
            val absColor = getMuscleColor(actAbs?.level ?: 0, isAbsSelected)
            val isOverAbs = actAbs?.isOvertrained == true
            drawMuscle(absColor, isAbsSelected, isOverAbs, 80f, 97f, 40f, 44f, rx = 7f)
            // Linie sześciopaka
            drawLine(
                color = Color(0xFF0F172A).copy(alpha = 0.5f),
                start = Offset(toX(100f), toY(99f)),
                end = Offset(toX(100f), toY(139f)),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color(0xFF0F172A).copy(alpha = 0.5f),
                start = Offset(toX(82f), toY(112f)),
                end = Offset(toX(118f), toY(112f)),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                color = Color(0xFF0F172A).copy(alpha = 0.5f),
                start = Offset(toX(82f), toY(126f)),
                end = Offset(toX(118f), toY(126f)),
                strokeWidth = 1.2.dp.toPx()
            )

            // 5. PRZEDRAMIONA (FOREARMS)
            val actForearms = activities[MuscleGroup.FOREARMS]
            val isForearmsSelected = selectedMuscle == MuscleGroup.FOREARMS
            val forearmsColor = getMuscleColor(actForearms?.level ?: 0, isForearmsSelected)
            val isOverForearms = actForearms?.isOvertrained == true
            drawMuscle(forearmsColor, isForearmsSelected, isOverForearms, 48f, 134f, 17f, 42f, rx = 6f)
            drawMuscle(forearmsColor, isForearmsSelected, isOverForearms, 135f, 134f, 17f, 42f, rx = 6f)

            // 6. CZWOROGŁOWE / UDA (QUADS)
            val actQuads = activities[MuscleGroup.QUADS]
            val isQuadsSelected = selectedMuscle == MuscleGroup.QUADS
            val quadsColor = getMuscleColor(actQuads?.level ?: 0, isQuadsSelected)
            val isOverQuads = actQuads?.isOvertrained == true
            drawMuscle(quadsColor, isQuadsSelected, isOverQuads, 75f, 149f, 22f, 72f, rx = 9f)
            drawMuscle(quadsColor, isQuadsSelected, isOverQuads, 103f, 149f, 22f, 72f, rx = 9f)

            // 7. ŁYDKI (CALVES)
            val actCalves = activities[MuscleGroup.CALVES]
            val isCalvesSelected = selectedMuscle == MuscleGroup.CALVES
            val calvesColor = getMuscleColor(actCalves?.level ?: 0, isCalvesSelected)
            val isOverCalves = actCalves?.isOvertrained == true
            drawMuscle(calvesColor, isCalvesSelected, isOverCalves, 76f, 229f, 19f, 68f, rx = 8f)
            drawMuscle(calvesColor, isCalvesSelected, isOverCalves, 105f, 229f, 19f, 68f, rx = 8f)

        } else {
            // ================= TYŁ =================

            // 1. BARKI Z TYŁU (SHOULDERS)
            val actShoulders = activities[MuscleGroup.SHOULDERS]
            val isShouldersSelected = selectedMuscle == MuscleGroup.SHOULDERS
            val shoulderColor = getMuscleColor(actShoulders?.level ?: 0, isShouldersSelected)
            val isOverShoulders = actShoulders?.isOvertrained == true
            drawMuscle(shoulderColor, isShouldersSelected, isOverShoulders, 54f, 66f, 22f, 28f, rx = 8f)
            drawMuscle(shoulderColor, isShouldersSelected, isOverShoulders, 124f, 66f, 22f, 28f, rx = 8f)

            // 2. PLECY (BACK - Czworoboczny + Najszerszy grzbietu)
            val actBack = activities[MuscleGroup.BACK]
            val isBackSelected = selectedMuscle == MuscleGroup.BACK
            val backColor = getMuscleColor(actBack?.level ?: 0, isBackSelected)
            val isOverBack = actBack?.isOvertrained == true
            drawMuscle(backColor, isBackSelected, isOverBack, 76f, 68f, 48f, 64f, rx = 9f)
            // Linia kręgosłupa
            drawLine(
                color = Color(0xFF0F172A).copy(alpha = 0.5f),
                start = Offset(toX(100f), toY(72f)),
                end = Offset(toX(100f), toY(128f)),
                strokeWidth = 1.5.dp.toPx()
            )

            // 3. TRICEPSY (TRICEPS)
            val actTriceps = activities[MuscleGroup.TRICEPS]
            val isTricepsSelected = selectedMuscle == MuscleGroup.TRICEPS
            val tricepsColor = getMuscleColor(actTriceps?.level ?: 0, isTricepsSelected)
            val isOverTriceps = actTriceps?.isOvertrained == true
            drawMuscle(tricepsColor, isTricepsSelected, isOverTriceps, 52f, 98f, 18f, 32f, rx = 7f)
            drawMuscle(tricepsColor, isTricepsSelected, isOverTriceps, 130f, 98f, 18f, 32f, rx = 7f)

            // 4. PRZEDRAMIONA Z TYŁU (FOREARMS)
            val actForearms = activities[MuscleGroup.FOREARMS]
            val isForearmsSelected = selectedMuscle == MuscleGroup.FOREARMS
            val forearmsColor = getMuscleColor(actForearms?.level ?: 0, isForearmsSelected)
            val isOverForearms = actForearms?.isOvertrained == true
            drawMuscle(forearmsColor, isForearmsSelected, isOverForearms, 48f, 134f, 17f, 42f, rx = 6f)
            drawMuscle(forearmsColor, isForearmsSelected, isOverForearms, 135f, 134f, 17f, 42f, rx = 6f)

            // 5. POŚLADKI (GLUTES)
            val actGlutes = activities[MuscleGroup.GLUTES]
            val isGlutesSelected = selectedMuscle == MuscleGroup.GLUTES
            val glutesColor = getMuscleColor(actGlutes?.level ?: 0, isGlutesSelected)
            val isOverGlutes = actGlutes?.isOvertrained == true
            drawMuscle(glutesColor, isGlutesSelected, isOverGlutes, 76f, 136f, 22f, 30f, rx = 9f)
            drawMuscle(glutesColor, isGlutesSelected, isOverGlutes, 102f, 136f, 22f, 30f, rx = 9f)

            // 6. DWUGŁOWE / TYŁ UD (HAMSTRINGS)
            val actHamstrings = activities[MuscleGroup.HAMSTRINGS]
            val isHamstringsSelected = selectedMuscle == MuscleGroup.HAMSTRINGS
            val hamstringsColor = getMuscleColor(actHamstrings?.level ?: 0, isHamstringsSelected)
            val isOverHamstrings = actHamstrings?.isOvertrained == true
            drawMuscle(hamstringsColor, isHamstringsSelected, isOverHamstrings, 75f, 170f, 22f, 52f, rx = 8f)
            drawMuscle(hamstringsColor, isHamstringsSelected, isOverHamstrings, 103f, 170f, 22f, 52f, rx = 8f)

            // 7. ŁYDKI Z TYŁU (CALVES)
            val actCalves = activities[MuscleGroup.CALVES]
            val isCalvesSelected = selectedMuscle == MuscleGroup.CALVES
            val calvesColor = getMuscleColor(actCalves?.level ?: 0, isCalvesSelected)
            val isOverCalves = actCalves?.isOvertrained == true
            drawMuscle(calvesColor, isCalvesSelected, isOverCalves, 76f, 229f, 19f, 68f, rx = 8f)
            drawMuscle(calvesColor, isCalvesSelected, isOverCalves, 105f, 229f, 19f, 68f, rx = 8f)
        }
    }
}

/**
 * Matryca Przetrenowania (Widok Kart Obciążenia Mięśniowego)
 * Wszystkie partie posortowane od najbardziej przetrenowanych do najmniej.
 */
@Composable
fun OvertrainingCardsGrid(
    activities: Map<MuscleGroup, MuscleActivity>,
    selectedMuscle: MuscleGroup?,
    onSelectMuscle: (MuscleGroup) -> Unit
) {
    val sortedActivities = remember(activities) {
        activities.values.sortedByDescending { it.setsCount }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sortedActivities.forEach { act ->
            val isSelected = selectedMuscle == act.muscle
            val progress = (act.setsCount / 20f).coerceIn(0f, 1f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectMuscle(act.muscle) }
                    .testTag("muscle_card_${act.muscle.name}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) GoldPr.copy(alpha = 0.15f) else Color(0xFF0F172A)
                ),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else if (act.isOvertrained) 1.5.dp else 1.dp,
                    color = if (isSelected) GoldPr else if (act.isOvertrained) Color(0xFFEF4444).copy(alpha = 0.8f) else Color(0xFF1E293B)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = act.statusColor,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = act.muscle.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Badge statusu
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = act.statusColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, act.statusColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = act.statusLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = act.statusColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pasek objętości
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = act.statusColor,
                        trackColor = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Statystyki
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${act.setsCount} serii wykonanych (Optimum: 10-15s)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        if (act.totalTonnage > 0) {
                            Text(
                                text = "Tonaż: ${String.format("%.0f", act.totalTonnage)} kg",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Karta ze szczegółowymi informacjami o wybranej partii mięśniowej.
 */
@Composable
fun MuscleDetailCard(
    muscle: MuscleGroup,
    activity: MuscleActivity?,
    onDismiss: () -> Unit
) {
    val act = activity ?: MuscleActivity(muscle, 0, 0f, emptyList())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_muscle_detail"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.5.dp, GoldPr.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(act.statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = muscle.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = act.statusColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, act.statusColor)
                ) {
                    Text(
                        text = "${act.setsCount} serii • ${act.statusLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = act.statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Szacowany czas regeneracji
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = GoldPr,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (act.recoveryHoursRemaining > 0)
                        "Szacowany czas pełnej regeneracji: ~${act.recoveryHoursRemaining} godzin"
                    else
                        "Partia w pełni wypoczęta i gotowa na trening!",
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.Medium
                )
            }

            if (act.totalTonnage > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = AthleticOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Łączny tonaż partii: ${String.format("%.1f", act.totalTonnage)} kg",
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            if (act.exerciseNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Wykonane ćwiczenia stymulujące (${act.exerciseNames.size}):",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRowWrapper(names = act.exerciseNames)
            }

            // Wskazówka trenerska
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            act.isOvertrained -> "Zmniejsz objętość lub zrób 2-3 dni przerwy dla tej grupy. Zbyt wysoka objętość ogranicza regenerację."
                            act.setsCount >= 10 -> "Idealny zakres objętości (10-15 serii)! Zapewnia optymalną hipertrofię i adaptację siłową."
                            act.setsCount >= 5 -> "Dobra objętość podtrzymująca. Możesz dodać 2-3 serie, jeśli celem jest priorytet tej partii."
                            else -> "Niska stymulacja. Jeśli chcesz rozwijać tę partię, uwzględnij ją w najbliższym treningu."
                        },
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FlowRowWrapper(names: List<String>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(names.distinct()) { name ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Text(
                    text = name,
                    fontSize = 10.sp,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * Kompatybilny zamiennik AnatomicalBodyCanvas dla innych ekranów
 */
@Composable
fun AnatomicalBodyCanvas(
    isFront: Boolean,
    activities: Map<MuscleGroup, MuscleActivity>,
    selectedMuscle: MuscleGroup?,
    chestScale: Float = 1.0f,
    waistScale: Float = 1.0f,
    armScale: Float = 1.0f,
    legScale: Float = 1.0f,
    calfScale: Float = 1.0f,
    onSelectMuscle: (MuscleGroup) -> Unit
) {
    ModernBodySilhouetteCanvas(
        isFront = isFront,
        activities = activities,
        selectedMuscle = selectedMuscle,
        onSelectMuscle = onSelectMuscle
    )
}
