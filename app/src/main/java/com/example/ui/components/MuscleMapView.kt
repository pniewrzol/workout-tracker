package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BodyMeasurement
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSetLog
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr

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
        setsCount >= 16 -> 4 // Bardzo wysoka
        setsCount >= 10 -> 3 // Wysoka
        setsCount >= 5  -> 2 // Umiarkowana
        setsCount >= 1  -> 1 // Wstępna
        else -> 0 // Brak
    }
}

fun calculateMuscleActivities(
    exercises: List<Exercise>,
    completedSets: List<WorkoutSetLog>,
    periodDays: Int
): Map<MuscleGroup, MuscleActivity> {
    val now = System.currentTimeMillis()
    val cutoff = if (periodDays >= 3000) 0L else now - (periodDays.toLong() * 24 * 60 * 60 * 1000L)

    val validSets = completedSets.filter { it.isCompleted && it.timestamp >= cutoff }
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
        4 -> AthleticOrange
        3 -> Color(0xFFFFB703)
        2 -> ElectricCyan
        1 -> Color(0xFF38BDF8)
        else -> Color(0xFF283446)
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
    var selectedPeriod by remember { mutableStateOf(MusclePeriod.MONTH) }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(initialSelectedMuscle) }
    // 0: Oba (Przód i Tył obok siebie - domyślny jak na wzorcu), 1: Przód, 2: Tył
    var viewMode by remember { mutableIntStateOf(0) }
    var applyMeasurementScale by remember { mutableStateOf(latestMeasurement != null) }

    val activities = remember(exercises, completedSets, selectedPeriod) {
        calculateMuscleActivities(exercises, completedSets, selectedPeriod.days)
    }

    val totalActiveSets = remember(activities) {
        activities.values.sumOf { it.setsCount }
    }

    // Dynamic silhouette scaling from real measurements
    val chestScale = remember(latestMeasurement, applyMeasurementScale) {
        if (!applyMeasurementScale || latestMeasurement?.chestCm == null) 1.0f
        else (latestMeasurement.chestCm / 105f).coerceIn(0.85f, 1.25f)
    }
    val waistScale = remember(latestMeasurement, applyMeasurementScale) {
        if (!applyMeasurementScale || latestMeasurement?.waistCm == null) 1.0f
        else (latestMeasurement.waistCm / 82f).coerceIn(0.82f, 1.25f)
    }
    val armScale = remember(latestMeasurement, applyMeasurementScale) {
        if (!applyMeasurementScale || latestMeasurement?.bicepsCm == null) 1.0f
        else (latestMeasurement.bicepsCm / 38f).coerceIn(0.85f, 1.30f)
    }
    val legScale = remember(latestMeasurement, applyMeasurementScale) {
        if (!applyMeasurementScale || latestMeasurement?.thighsCm == null) 1.0f
        else (latestMeasurement.thighsCm / 60f).coerceIn(0.85f, 1.25f)
    }
    val calfScale = remember(latestMeasurement, applyMeasurementScale) {
        if (!applyMeasurementScale || latestMeasurement?.calvesCm == null) 1.0f
        else (latestMeasurement.calvesCm / 38f).coerceIn(0.85f, 1.25f)
    }

    val vTaperRatio = if (latestMeasurement?.chestCm != null && latestMeasurement.waistCm != null && latestMeasurement.waistCm > 0) {
        latestMeasurement.chestCm / latestMeasurement.waistCm
    } else null

    Column(modifier = modifier.fillMaxWidth()) {
        // Period Filter Chips
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
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Geometric Faceted Muscular Silhouette Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1522)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF233044)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Model Sylwetki i Mięśni",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "$totalActiveSets ukończonych serii w okresie",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // View Mode Switcher: Full width, 3 equal segments so text NEVER overflows or wraps on any phone!
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
                        val viewOptions = listOf("Oba widoki", "Przód", "Tył")
                        viewOptions.forEachIndexed { index, label ->
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
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else Color(0xFFCBD5E1),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Measurement proportions toggle bar
                if (latestMeasurement != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF182333),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Straighten,
                                    contentDescription = null,
                                    tint = GoldPr,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Skaluj proporcje wg Twoich pomiarów",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (vTaperRatio != null) {
                                        Text(
                                            text = "V-Taper: ${String.format("%.2f", vTaperRatio)} (Klatka: ${latestMeasurement.chestCm}cm / Talia: ${latestMeasurement.waistCm}cm)",
                                            fontSize = 10.sp,
                                            color = GoldPr
                                        )
                                    }
                                }
                            }

                            Switch(
                                checked = applyMeasurementScale,
                                onCheckedChange = { applyMeasurementScale = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GoldPr,
                                    checkedTrackColor = GoldPr.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.size(36.dp, 20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Faceted Muscular Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                        .background(Color(0xFF0C1019), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF1C273A), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                        when (viewMode) {
                            0 -> {
                                // Side-by-Side: Front on Left, Rear on Right (exact match with user's reference image)
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Text(
                                                "PRZÓD",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8),
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            FacetedMuscleCanvas(
                                                isFront = true,
                                                activities = activities,
                                                selectedMuscle = selectedMuscle,
                                                chestScale = chestScale,
                                                waistScale = waistScale,
                                                armScale = armScale,
                                                legScale = legScale,
                                                calfScale = calfScale,
                                                onSelectMuscle = { selectedMuscle = if (selectedMuscle == it) null else it }
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(280.dp)
                                            .background(Color(0xFF1F2B3E))
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Text(
                                                "TYŁ",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8),
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            FacetedMuscleCanvas(
                                                isFront = false,
                                                activities = activities,
                                                selectedMuscle = selectedMuscle,
                                                chestScale = chestScale,
                                                waistScale = waistScale,
                                                armScale = armScale,
                                                legScale = legScale,
                                                calfScale = calfScale,
                                                onSelectMuscle = { selectedMuscle = if (selectedMuscle == it) null else it }
                                            )
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Front View Only (Enlarged)
                                FacetedMuscleCanvas(
                                    isFront = true,
                                    activities = activities,
                                    selectedMuscle = selectedMuscle,
                                    chestScale = chestScale,
                                    waistScale = waistScale,
                                    armScale = armScale,
                                    legScale = legScale,
                                    calfScale = calfScale,
                                    onSelectMuscle = { selectedMuscle = if (selectedMuscle == it) null else it }
                                )
                            }
                            else -> {
                                // Rear View Only (Enlarged)
                                FacetedMuscleCanvas(
                                    isFront = false,
                                    activities = activities,
                                    selectedMuscle = selectedMuscle,
                                    chestScale = chestScale,
                                    waistScale = waistScale,
                                    armScale = armScale,
                                    legScale = legScale,
                                    calfScale = calfScale,
                                    onSelectMuscle = { selectedMuscle = if (selectedMuscle == it) null else it }
                                )
                            }
                        }
                    }

                Spacer(modifier = Modifier.height(10.dp))

                // Heatmap Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Intensywność stymulacji:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LegendChip(Color(0xFF283446), "0")
                        LegendChip(Color(0xFF38BDF8), "1-4")
                        LegendChip(ElectricCyan, "5-9")
                        LegendChip(Color(0xFFFFB703), "10-15")
                        LegendChip(AthleticOrange, "16+")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Muscle Card
        selectedMuscle?.let { muscle ->
            val act = activities[muscle]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPr.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = getMuscleColor(act?.level ?: 0, false),
                                modifier = Modifier.size(14.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = muscle.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${act?.setsCount ?: 0} serii",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val tonnage = act?.totalTonnage?.toLong() ?: 0L
                    if (tonnage > 0) {
                        Text(
                            text = "Tonaż łączny w okresie: ${tonnage}kg",
                            fontSize = 12.sp,
                            color = ElectricCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!act?.exerciseNames.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ćwiczenia: ${act.exerciseNames.distinct().joinToString(", ")}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Quick Horizontal Muscle Selector Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(MuscleGroup.entries) { muscle ->
                val act = activities[muscle]
                val isSelected = selectedMuscle == muscle
                val color = getMuscleColor(act?.level ?: 0, isSelected)

                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .clickable { selectedMuscle = if (selectedMuscle == muscle) null else muscle },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = color,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = muscle.displayName.substringBefore(" ("),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${act?.setsCount ?: 0} serii",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Geometric, faceted low-poly muscular athletic body canvas matching reference design.
 * Sharp polygonal facet plates with dark gaps, dynamic scaling, and muscle heatmaps.
 */
@Composable
fun FacetedMuscleCanvas(
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
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val baseFacetColor = Color(0xFF283446)
        val gapStrokeColor = Color(0xFF0C1019)

        if (isFront) {
            // ================= FRONT VIEW FACETS =================

            // 1. HEAD (Faceted skull, jaw, chin)
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.035f, h * 0.035f),
                Offset(cx + w * 0.035f, h * 0.035f),
                Offset(cx + w * 0.055f, h * 0.065f),
                Offset(cx - w * 0.055f, h * 0.065f)
            )
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.055f, h * 0.067f),
                Offset(cx + w * 0.055f, h * 0.067f),
                Offset(cx + w * 0.035f, h * 0.105f),
                Offset(cx - w * 0.035f, h * 0.105f)
            )
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.035f, h * 0.107f),
                Offset(cx + w * 0.035f, h * 0.107f),
                Offset(cx, h * 0.120f)
            )

            // 2. NECK (SCM Left & Right + Throat)
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.028f, h * 0.112f),
                Offset(cx - w * 0.005f, h * 0.118f),
                Offset(cx - w * 0.012f, h * 0.155f),
                Offset(cx - w * 0.038f, h * 0.145f)
            )
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx + w * 0.005f, h * 0.118f),
                Offset(cx + w * 0.028f, h * 0.112f),
                Offset(cx + w * 0.038f, h * 0.145f),
                Offset(cx + w * 0.012f, h * 0.155f)
            )
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.005f, h * 0.118f),
                Offset(cx + w * 0.005f, h * 0.118f),
                Offset(cx + w * 0.012f, h * 0.155f),
                Offset(cx - w * 0.012f, h * 0.155f)
            )

            // 3. TRAPS (Upper shoulder slopes)
            val isBackSelected = selectedMuscle == MuscleGroup.BACK
            val trapsColor = getMuscleColor(activities[MuscleGroup.BACK]?.level ?: 0, isBackSelected)
            drawFacetPolygon(trapsColor, isBackSelected, gapStrokeColor,
                Offset(cx - w * 0.038f, h * 0.142f),
                Offset(cx - w * 0.012f, h * 0.155f),
                Offset(cx - w * 0.065f, h * 0.165f),
                Offset(cx - w * 0.095f, h * 0.155f)
            )
            drawFacetPolygon(trapsColor, isBackSelected, gapStrokeColor,
                Offset(cx + w * 0.012f, h * 0.155f),
                Offset(cx + w * 0.038f, h * 0.142f),
                Offset(cx + w * 0.095f, h * 0.155f),
                Offset(cx + w * 0.065f, h * 0.165f)
            )

            // 4. SHOULDERS / DELTOIDS
            val isShouldersSelected = selectedMuscle == MuscleGroup.SHOULDERS
            val deltColor = getMuscleColor(activities[MuscleGroup.SHOULDERS]?.level ?: 0, isShouldersSelected)
            // Left Deltoid Anterior
            drawFacetPolygon(deltColor, isShouldersSelected, gapStrokeColor,
                Offset(cx - w * 0.095f, h * 0.155f),
                Offset(cx - w * 0.065f, h * 0.165f),
                Offset(cx - w * 0.10f * chestScale, h * 0.210f),
                Offset(cx - w * 0.145f * armScale, h * 0.190f)
            )
            // Left Deltoid Lateral
            drawFacetPolygon(deltColor, isShouldersSelected, gapStrokeColor,
                Offset(cx - w * 0.145f * armScale, h * 0.190f),
                Offset(cx - w * 0.18f * armScale, h * 0.205f),
                Offset(cx - w * 0.155f * armScale, h * 0.245f),
                Offset(cx - w * 0.115f * armScale, h * 0.240f),
                Offset(cx - w * 0.10f * chestScale, h * 0.210f)
            )
            // Right Deltoid Anterior
            drawFacetPolygon(deltColor, isShouldersSelected, gapStrokeColor,
                Offset(cx + w * 0.065f, h * 0.165f),
                Offset(cx + w * 0.095f, h * 0.155f),
                Offset(cx + w * 0.145f * armScale, h * 0.190f),
                Offset(cx + w * 0.10f * chestScale, h * 0.210f)
            )
            // Right Deltoid Lateral
            drawFacetPolygon(deltColor, isShouldersSelected, gapStrokeColor,
                Offset(cx + w * 0.145f * armScale, h * 0.190f),
                Offset(cx + w * 0.18f * armScale, h * 0.205f),
                Offset(cx + w * 0.155f * armScale, h * 0.245f),
                Offset(cx + w * 0.115f * armScale, h * 0.240f),
                Offset(cx + w * 0.10f * chestScale, h * 0.210f)
            )

            // 5. PECTORALS / CHEST
            val isChestSelected = selectedMuscle == MuscleGroup.CHEST
            val chestColor = getMuscleColor(activities[MuscleGroup.CHEST]?.level ?: 0, isChestSelected)
            // Left Upper Pec
            drawFacetPolygon(chestColor, isChestSelected, gapStrokeColor,
                Offset(cx - w * 0.008f, h * 0.165f),
                Offset(cx - w * 0.062f, h * 0.165f),
                Offset(cx - w * 0.095f * chestScale, h * 0.208f),
                Offset(cx - w * 0.008f, h * 0.208f)
            )
            // Left Lower Pec
            drawFacetPolygon(chestColor, isChestSelected, gapStrokeColor,
                Offset(cx - w * 0.008f, h * 0.211f),
                Offset(cx - w * 0.095f * chestScale, h * 0.211f),
                Offset(cx - w * 0.075f * chestScale, h * 0.252f),
                Offset(cx - w * 0.008f, h * 0.252f)
            )
            // Right Upper Pec
            drawFacetPolygon(chestColor, isChestSelected, gapStrokeColor,
                Offset(cx + w * 0.008f, h * 0.165f),
                Offset(cx + w * 0.062f, h * 0.165f),
                Offset(cx + w * 0.095f * chestScale, h * 0.208f),
                Offset(cx + w * 0.008f, h * 0.208f)
            )
            // Right Lower Pec
            drawFacetPolygon(chestColor, isChestSelected, gapStrokeColor,
                Offset(cx + w * 0.008f, h * 0.211f),
                Offset(cx + w * 0.095f * chestScale, h * 0.211f),
                Offset(cx + w * 0.075f * chestScale, h * 0.252f),
                Offset(cx + w * 0.008f, h * 0.252f)
            )

            // 6. BICEPS
            val isBicepsSelected = selectedMuscle == MuscleGroup.BICEPS
            val bicepColor = getMuscleColor(activities[MuscleGroup.BICEPS]?.level ?: 0, isBicepsSelected)
            // Left Bicep
            drawFacetPolygon(bicepColor, isBicepsSelected, gapStrokeColor,
                Offset(cx - w * 0.155f * armScale, h * 0.248f),
                Offset(cx - w * 0.115f * armScale, h * 0.245f),
                Offset(cx - w * 0.125f * armScale, h * 0.340f),
                Offset(cx - w * 0.175f * armScale, h * 0.330f)
            )
            // Right Bicep
            drawFacetPolygon(bicepColor, isBicepsSelected, gapStrokeColor,
                Offset(cx + w * 0.115f * armScale, h * 0.245f),
                Offset(cx + w * 0.155f * armScale, h * 0.248f),
                Offset(cx + w * 0.175f * armScale, h * 0.330f),
                Offset(cx + w * 0.125f * armScale, h * 0.340f)
            )

            // 7. FOREARMS & HANDS
            val isForearmsSelected = selectedMuscle == MuscleGroup.FOREARMS
            val forearmColor = getMuscleColor(activities[MuscleGroup.FOREARMS]?.level ?: 0, isForearmsSelected)
            // Left Forearm
            drawFacetPolygon(forearmColor, isForearmsSelected, gapStrokeColor,
                Offset(cx - w * 0.175f * armScale, h * 0.335f),
                Offset(cx - w * 0.125f * armScale, h * 0.345f),
                Offset(cx - w * 0.155f * armScale, h * 0.450f),
                Offset(cx - w * 0.210f * armScale, h * 0.435f)
            )
            // Left Hand
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.210f * armScale, h * 0.440f),
                Offset(cx - w * 0.155f * armScale, h * 0.455f),
                Offset(cx - w * 0.180f * armScale, h * 0.510f),
                Offset(cx - w * 0.235f * armScale, h * 0.490f)
            )
            // Right Forearm
            drawFacetPolygon(forearmColor, isForearmsSelected, gapStrokeColor,
                Offset(cx + w * 0.125f * armScale, h * 0.345f),
                Offset(cx + w * 0.175f * armScale, h * 0.335f),
                Offset(cx + w * 0.210f * armScale, h * 0.435f),
                Offset(cx + w * 0.155f * armScale, h * 0.450f)
            )
            // Right Hand
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx + w * 0.155f * armScale, h * 0.455f),
                Offset(cx + w * 0.210f * armScale, h * 0.440f),
                Offset(cx + w * 0.235f * armScale, h * 0.490f),
                Offset(cx + w * 0.180f * armScale, h * 0.510f)
            )

            // 8. ABDOMINALS & SERRATUS
            val isAbsSelected = selectedMuscle == MuscleGroup.ABS
            val absColor = getMuscleColor(activities[MuscleGroup.ABS]?.level ?: 0, isAbsSelected)
            // 6-pack abs
            // Upper
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx - w * 0.045f * waistScale, h * 0.256f),
                Offset(cx - w * 0.005f, h * 0.256f),
                Offset(cx - w * 0.005f, h * 0.292f),
                Offset(cx - w * 0.042f * waistScale, h * 0.292f)
            )
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx + w * 0.005f, h * 0.256f),
                Offset(cx + w * 0.045f * waistScale, h * 0.256f),
                Offset(cx + w * 0.042f * waistScale, h * 0.292f),
                Offset(cx + w * 0.005f, h * 0.292f)
            )
            // Mid
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx - w * 0.042f * waistScale, h * 0.296f),
                Offset(cx - w * 0.005f, h * 0.296f),
                Offset(cx - w * 0.005f, h * 0.334f),
                Offset(cx - w * 0.038f * waistScale, h * 0.334f)
            )
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx + w * 0.005f, h * 0.296f),
                Offset(cx + w * 0.042f * waistScale, h * 0.296f),
                Offset(cx + w * 0.038f * waistScale, h * 0.334f),
                Offset(cx + w * 0.005f, h * 0.334f)
            )
            // Lower
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx - w * 0.038f * waistScale, h * 0.338f),
                Offset(cx - w * 0.005f, h * 0.338f),
                Offset(cx - w * 0.005f, h * 0.378f),
                Offset(cx - w * 0.032f * waistScale, h * 0.374f)
            )
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx + w * 0.005f, h * 0.338f),
                Offset(cx + w * 0.038f * waistScale, h * 0.338f),
                Offset(cx + w * 0.032f * waistScale, h * 0.374f),
                Offset(cx + w * 0.005f, h * 0.378f)
            )
            // Serratus / Obliques Ribs
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx - w * 0.075f * waistScale, h * 0.256f),
                Offset(cx - w * 0.048f * waistScale, h * 0.256f),
                Offset(cx - w * 0.045f * waistScale, h * 0.292f),
                Offset(cx - w * 0.078f * waistScale, h * 0.295f)
            )
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx - w * 0.078f * waistScale, h * 0.298f),
                Offset(cx - w * 0.045f * waistScale, h * 0.296f),
                Offset(cx - w * 0.042f * waistScale, h * 0.334f),
                Offset(cx - w * 0.070f * waistScale, h * 0.338f)
            )
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx - w * 0.070f * waistScale, h * 0.342f),
                Offset(cx - w * 0.042f * waistScale, h * 0.338f),
                Offset(cx - w * 0.036f * waistScale, h * 0.375f),
                Offset(cx - w * 0.060f * waistScale, h * 0.380f)
            )
            // Right Ribs
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx + w * 0.048f * waistScale, h * 0.256f),
                Offset(cx + w * 0.075f * waistScale, h * 0.256f),
                Offset(cx + w * 0.078f * waistScale, h * 0.295f),
                Offset(cx + w * 0.045f * waistScale, h * 0.292f)
            )
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx + w * 0.045f * waistScale, h * 0.296f),
                Offset(cx + w * 0.078f * waistScale, h * 0.298f),
                Offset(cx + w * 0.070f * waistScale, h * 0.338f),
                Offset(cx + w * 0.042f * waistScale, h * 0.334f)
            )
            drawFacetPolygon(absColor, isAbsSelected, gapStrokeColor,
                Offset(cx + w * 0.042f * waistScale, h * 0.338f),
                Offset(cx + w * 0.070f * waistScale, h * 0.342f),
                Offset(cx + w * 0.060f * waistScale, h * 0.380f),
                Offset(cx + w * 0.036f * waistScale, h * 0.375f)
            )

            // 9. PELVIS / INGUINAL
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx, h * 0.380f),
                Offset(cx + w * 0.035f * waistScale, h * 0.380f),
                Offset(cx + w * 0.015f, h * 0.435f),
                Offset(cx - w * 0.015f, h * 0.435f),
                Offset(cx - w * 0.035f * waistScale, h * 0.380f)
            )

            // 10. QUADRICEPS
            val isQuadsSelected = selectedMuscle == MuscleGroup.QUADS
            val quadColor = getMuscleColor(activities[MuscleGroup.QUADS]?.level ?: 0, isQuadsSelected)
            // Left Rectus Femoris (central diamond)
            drawFacetPolygon(quadColor, isQuadsSelected, gapStrokeColor,
                Offset(cx - w * 0.055f, h * 0.438f),
                Offset(cx - w * 0.028f, h * 0.530f),
                Offset(cx - w * 0.052f, h * 0.640f),
                Offset(cx - w * 0.082f * legScale, h * 0.530f)
            )
            // Left Vastus Lateralis (outer teardrop)
            drawFacetPolygon(quadColor, isQuadsSelected, gapStrokeColor,
                Offset(cx - w * 0.055f, h * 0.438f),
                Offset(cx - w * 0.125f * legScale, h * 0.480f),
                Offset(cx - w * 0.115f * legScale, h * 0.620f),
                Offset(cx - w * 0.060f, h * 0.645f),
                Offset(cx - w * 0.082f * legScale, h * 0.530f)
            )
            // Left Vastus Medialis (inner teardrop)
            drawFacetPolygon(quadColor, isQuadsSelected, gapStrokeColor,
                Offset(cx - w * 0.055f, h * 0.438f),
                Offset(cx - w * 0.012f, h * 0.490f),
                Offset(cx - w * 0.015f, h * 0.620f),
                Offset(cx - w * 0.045f, h * 0.655f),
                Offset(cx - w * 0.028f, h * 0.530f)
            )
            // Right Quads
            drawFacetPolygon(quadColor, isQuadsSelected, gapStrokeColor,
                Offset(cx + w * 0.055f, h * 0.438f),
                Offset(cx + w * 0.082f * legScale, h * 0.530f),
                Offset(cx + w * 0.052f, h * 0.640f),
                Offset(cx + w * 0.028f, h * 0.530f)
            )
            drawFacetPolygon(quadColor, isQuadsSelected, gapStrokeColor,
                Offset(cx + w * 0.055f, h * 0.438f),
                Offset(cx + w * 0.082f * legScale, h * 0.530f),
                Offset(cx + w * 0.060f, h * 0.645f),
                Offset(cx + w * 0.115f * legScale, h * 0.620f),
                Offset(cx + w * 0.125f * legScale, h * 0.480f)
            )
            drawFacetPolygon(quadColor, isQuadsSelected, gapStrokeColor,
                Offset(cx + w * 0.055f, h * 0.438f),
                Offset(cx + w * 0.028f, h * 0.530f),
                Offset(cx + w * 0.045f, h * 0.655f),
                Offset(cx + w * 0.015f, h * 0.620f),
                Offset(cx + w * 0.012f, h * 0.490f)
            )

            // 11. KNEES (Patella diamonds)
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.055f, h * 0.655f),
                Offset(cx - w * 0.038f, h * 0.672f),
                Offset(cx - w * 0.055f, h * 0.688f),
                Offset(cx - w * 0.072f, h * 0.672f)
            )
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx + w * 0.055f, h * 0.655f),
                Offset(cx + w * 0.072f, h * 0.672f),
                Offset(cx + w * 0.055f, h * 0.688f),
                Offset(cx + w * 0.038f, h * 0.672f)
            )

            // 12. CALVES & SHINS
            val isCalvesSelected = selectedMuscle == MuscleGroup.CALVES
            val calfColor = getMuscleColor(activities[MuscleGroup.CALVES]?.level ?: 0, isCalvesSelected)
            // Left Tibialis & Gastrocnemius
            drawFacetPolygon(calfColor, isCalvesSelected, gapStrokeColor,
                Offset(cx - w * 0.055f, h * 0.695f),
                Offset(cx - w * 0.040f, h * 0.770f),
                Offset(cx - w * 0.045f, h * 0.880f),
                Offset(cx - w * 0.065f, h * 0.770f)
            )
            drawFacetPolygon(calfColor, isCalvesSelected, gapStrokeColor,
                Offset(cx - w * 0.060f, h * 0.695f),
                Offset(cx - w * 0.100f * calfScale, h * 0.750f),
                Offset(cx - w * 0.068f, h * 0.820f),
                Offset(cx - w * 0.060f, h * 0.760f)
            )
            drawFacetPolygon(calfColor, isCalvesSelected, gapStrokeColor,
                Offset(cx - w * 0.038f, h * 0.760f),
                Offset(cx - w * 0.018f, h * 0.800f),
                Offset(cx - w * 0.025f, h * 0.880f),
                Offset(cx - w * 0.042f, h * 0.840f)
            )
            // Right Calves
            drawFacetPolygon(calfColor, isCalvesSelected, gapStrokeColor,
                Offset(cx + w * 0.055f, h * 0.695f),
                Offset(cx + w * 0.065f, h * 0.770f),
                Offset(cx + w * 0.045f, h * 0.880f),
                Offset(cx + w * 0.040f, h * 0.770f)
            )
            drawFacetPolygon(calfColor, isCalvesSelected, gapStrokeColor,
                Offset(cx + w * 0.060f, h * 0.695f),
                Offset(cx + w * 0.060f, h * 0.760f),
                Offset(cx + w * 0.068f, h * 0.820f),
                Offset(cx + w * 0.100f * calfScale, h * 0.750f)
            )
            drawFacetPolygon(calfColor, isCalvesSelected, gapStrokeColor,
                Offset(cx + w * 0.038f, h * 0.760f),
                Offset(cx + w * 0.042f, h * 0.840f),
                Offset(cx + w * 0.025f, h * 0.880f),
                Offset(cx + w * 0.018f, h * 0.800f)
            )

            // 13. FEET
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.065f, h * 0.885f),
                Offset(cx - w * 0.025f, h * 0.885f),
                Offset(cx - w * 0.020f, h * 0.940f),
                Offset(cx - w * 0.075f, h * 0.940f)
            )
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx + w * 0.025f, h * 0.885f),
                Offset(cx + w * 0.065f, h * 0.885f),
                Offset(cx + w * 0.075f, h * 0.940f),
                Offset(cx + w * 0.020f, h * 0.940f)
            )

        } else {
            // ================= REAR VIEW FACETS =================

            // 1. POSTERIOR HEAD & NECK
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.035f, h * 0.035f),
                Offset(cx + w * 0.035f, h * 0.035f),
                Offset(cx + w * 0.055f, h * 0.065f),
                Offset(cx - w * 0.055f, h * 0.065f)
            )
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.055f, h * 0.067f),
                Offset(cx + w * 0.055f, h * 0.067f),
                Offset(cx + w * 0.032f, h * 0.115f),
                Offset(cx - w * 0.032f, h * 0.115f)
            )

            // 2. TRAPEZIUS (Diamond along spine)
            val isBackSelected = selectedMuscle == MuscleGroup.BACK
            val backColor = getMuscleColor(activities[MuscleGroup.BACK]?.level ?: 0, isBackSelected)
            // Upper Traps
            drawFacetPolygon(backColor, isBackSelected, gapStrokeColor,
                Offset(cx - w * 0.032f, h * 0.115f),
                Offset(cx + w * 0.032f, h * 0.115f),
                Offset(cx + w * 0.090f, h * 0.155f),
                Offset(cx, h * 0.160f),
                Offset(cx - w * 0.090f, h * 0.155f)
            )
            // Mid Traps (Diamond)
            drawFacetPolygon(backColor, isBackSelected, gapStrokeColor,
                Offset(cx, h * 0.162f),
                Offset(cx + w * 0.050f, h * 0.220f),
                Offset(cx, h * 0.280f),
                Offset(cx - w * 0.050f, h * 0.220f)
            )

            // 3. POSTERIOR DELTOIDS
            val isShouldersSelected = selectedMuscle == MuscleGroup.SHOULDERS
            val deltColor = getMuscleColor(activities[MuscleGroup.SHOULDERS]?.level ?: 0, isShouldersSelected)
            drawFacetPolygon(deltColor, isShouldersSelected, gapStrokeColor,
                Offset(cx - w * 0.090f, h * 0.155f),
                Offset(cx - w * 0.145f * armScale, h * 0.175f),
                Offset(cx - w * 0.175f * armScale, h * 0.215f),
                Offset(cx - w * 0.115f * armScale, h * 0.235f),
                Offset(cx - w * 0.085f, h * 0.205f)
            )
            drawFacetPolygon(deltColor, isShouldersSelected, gapStrokeColor,
                Offset(cx + w * 0.090f, h * 0.155f),
                Offset(cx + w * 0.085f, h * 0.205f),
                Offset(cx + w * 0.115f * armScale, h * 0.235f),
                Offset(cx + w * 0.175f * armScale, h * 0.215f),
                Offset(cx + w * 0.145f * armScale, h * 0.175f)
            )

            // 4. LATISSIMUS DORSI & SCAPULA (Wide V-Taper Wings)
            // Left Infraspinatus / Scapula
            drawFacetPolygon(backColor, isBackSelected, gapStrokeColor,
                Offset(cx - w * 0.085f, h * 0.205f),
                Offset(cx - w * 0.050f, h * 0.220f),
                Offset(cx - w * 0.045f, h * 0.275f),
                Offset(cx - w * 0.100f * chestScale, h * 0.255f)
            )
            // Right Infraspinatus / Scapula
            drawFacetPolygon(backColor, isBackSelected, gapStrokeColor,
                Offset(cx + w * 0.050f, h * 0.220f),
                Offset(cx + w * 0.085f, h * 0.205f),
                Offset(cx + w * 0.100f * chestScale, h * 0.255f),
                Offset(cx + w * 0.045f, h * 0.275f)
            )
            // Left Lat Wing (V-Taper)
            drawFacetPolygon(backColor, isBackSelected, gapStrokeColor,
                Offset(cx - w * 0.100f * chestScale, h * 0.255f),
                Offset(cx - w * 0.045f, h * 0.275f),
                Offset(cx - w * 0.010f, h * 0.380f),
                Offset(cx - w * 0.065f * waistScale, h * 0.370f)
            )
            // Right Lat Wing (V-Taper)
            drawFacetPolygon(backColor, isBackSelected, gapStrokeColor,
                Offset(cx + w * 0.045f, h * 0.275f),
                Offset(cx + w * 0.100f * chestScale, h * 0.255f),
                Offset(cx + w * 0.065f * waistScale, h * 0.370f),
                Offset(cx + w * 0.010f, h * 0.380f)
            )
            // Lower Back (Erector Spinae)
            drawFacetPolygon(backColor, isBackSelected, gapStrokeColor,
                Offset(cx - w * 0.025f, h * 0.285f),
                Offset(cx + w * 0.025f, h * 0.285f),
                Offset(cx + w * 0.020f, h * 0.405f),
                Offset(cx - w * 0.020f, h * 0.405f)
            )

            // 5. TRICEPS
            val isTricepsSelected = selectedMuscle == MuscleGroup.TRICEPS
            val tricepsColor = getMuscleColor(activities[MuscleGroup.TRICEPS]?.level ?: 0, isTricepsSelected)
            // Left Triceps
            drawFacetPolygon(tricepsColor, isTricepsSelected, gapStrokeColor,
                Offset(cx - w * 0.175f * armScale, h * 0.220f),
                Offset(cx - w * 0.115f * armScale, h * 0.238f),
                Offset(cx - w * 0.125f * armScale, h * 0.340f),
                Offset(cx - w * 0.180f * armScale, h * 0.325f)
            )
            // Right Triceps
            drawFacetPolygon(tricepsColor, isTricepsSelected, gapStrokeColor,
                Offset(cx + w * 0.115f * armScale, h * 0.238f),
                Offset(cx + w * 0.175f * armScale, h * 0.220f),
                Offset(cx + w * 0.180f * armScale, h * 0.325f),
                Offset(cx + w * 0.125f * armScale, h * 0.340f)
            )

            // 6. POSTERIOR FOREARMS & HANDS
            val isForearmsSelected = selectedMuscle == MuscleGroup.FOREARMS
            val forearmColor = getMuscleColor(activities[MuscleGroup.FOREARMS]?.level ?: 0, isForearmsSelected)
            drawFacetPolygon(forearmColor, isForearmsSelected, gapStrokeColor,
                Offset(cx - w * 0.180f * armScale, h * 0.330f),
                Offset(cx - w * 0.125f * armScale, h * 0.345f),
                Offset(cx - w * 0.155f * armScale, h * 0.450f),
                Offset(cx - w * 0.210f * armScale, h * 0.435f)
            )
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.210f * armScale, h * 0.440f),
                Offset(cx - w * 0.155f * armScale, h * 0.455f),
                Offset(cx - w * 0.180f * armScale, h * 0.510f),
                Offset(cx - w * 0.235f * armScale, h * 0.490f)
            )
            drawFacetPolygon(forearmColor, isForearmsSelected, gapStrokeColor,
                Offset(cx + w * 0.125f * armScale, h * 0.345f),
                Offset(cx + w * 0.180f * armScale, h * 0.330f),
                Offset(cx + w * 0.210f * armScale, h * 0.435f),
                Offset(cx + w * 0.155f * armScale, h * 0.450f)
            )
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx + w * 0.155f * armScale, h * 0.455f),
                Offset(cx + w * 0.210f * armScale, h * 0.440f),
                Offset(cx + w * 0.235f * armScale, h * 0.490f),
                Offset(cx + w * 0.180f * armScale, h * 0.510f)
            )

            // 7. GLUTES
            val isGlutesSelected = selectedMuscle == MuscleGroup.GLUTES
            val gluteColor = getMuscleColor(activities[MuscleGroup.GLUTES]?.level ?: 0, isGlutesSelected)
            // Left Glute
            drawFacetPolygon(gluteColor, isGlutesSelected, gapStrokeColor,
                Offset(cx - w * 0.010f, h * 0.408f),
                Offset(cx - w * 0.080f * waistScale, h * 0.405f),
                Offset(cx - w * 0.095f * waistScale, h * 0.485f),
                Offset(cx - w * 0.060f, h * 0.525f),
                Offset(cx - w * 0.010f, h * 0.520f)
            )
            // Right Glute
            drawFacetPolygon(gluteColor, isGlutesSelected, gapStrokeColor,
                Offset(cx + w * 0.010f, h * 0.408f),
                Offset(cx + w * 0.080f * waistScale, h * 0.405f),
                Offset(cx + w * 0.095f * waistScale, h * 0.485f),
                Offset(cx + w * 0.060f, h * 0.525f),
                Offset(cx + w * 0.010f, h * 0.520f)
            )

            // 8. HAMSTRINGS
            val isHamstringsSelected = selectedMuscle == MuscleGroup.HAMSTRINGS
            val hamColor = getMuscleColor(activities[MuscleGroup.HAMSTRINGS]?.level ?: 0, isHamstringsSelected)
            // Left Biceps Femoris (outer)
            drawFacetPolygon(hamColor, isHamstringsSelected, gapStrokeColor,
                Offset(cx - w * 0.060f, h * 0.528f),
                Offset(cx - w * 0.105f * legScale, h * 0.535f),
                Offset(cx - w * 0.095f * legScale, h * 0.655f),
                Offset(cx - w * 0.060f, h * 0.655f)
            )
            // Left Semitendinosus (inner)
            drawFacetPolygon(hamColor, isHamstringsSelected, gapStrokeColor,
                Offset(cx - w * 0.010f, h * 0.525f),
                Offset(cx - w * 0.055f, h * 0.528f),
                Offset(cx - w * 0.055f, h * 0.655f),
                Offset(cx - w * 0.012f, h * 0.650f)
            )
            // Right Hamstrings
            drawFacetPolygon(hamColor, isHamstringsSelected, gapStrokeColor,
                Offset(cx + w * 0.060f, h * 0.528f),
                Offset(cx + w * 0.105f * legScale, h * 0.535f),
                Offset(cx + w * 0.095f * legScale, h * 0.655f),
                Offset(cx + w * 0.060f, h * 0.655f)
            )
            drawFacetPolygon(hamColor, isHamstringsSelected, gapStrokeColor,
                Offset(cx + w * 0.010f, h * 0.525f),
                Offset(cx + w * 0.055f, h * 0.528f),
                Offset(cx + w * 0.055f, h * 0.655f),
                Offset(cx + w * 0.012f, h * 0.650f)
            )

            // 9. POPLITEAL FOSSA / KNEE BACK
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.055f, h * 0.660f),
                Offset(cx - w * 0.030f, h * 0.675f),
                Offset(cx - w * 0.055f, h * 0.690f),
                Offset(cx - w * 0.080f, h * 0.675f)
            )
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx + w * 0.055f, h * 0.660f),
                Offset(cx + w * 0.080f, h * 0.675f),
                Offset(cx + w * 0.055f, h * 0.690f),
                Offset(cx + w * 0.030f, h * 0.675f)
            )

            // 10. CALVES (Diamond gastrocnemius heads)
            val isCalvesSelected = selectedMuscle == MuscleGroup.CALVES
            val calfColor = getMuscleColor(activities[MuscleGroup.CALVES]?.level ?: 0, isCalvesSelected)
            // Left Gastrocnemius Lateral Head
            drawFacetPolygon(calfColor, isCalvesSelected, gapStrokeColor,
                Offset(cx - w * 0.055f, h * 0.695f),
                Offset(cx - w * 0.105f * calfScale, h * 0.745f),
                Offset(cx - w * 0.065f, h * 0.815f),
                Offset(cx - w * 0.055f, h * 0.760f)
            )
            // Left Gastrocnemius Medial Head
            drawFacetPolygon(calfColor, isCalvesSelected, gapStrokeColor,
                Offset(cx - w * 0.055f, h * 0.695f),
                Offset(cx - w * 0.055f, h * 0.760f),
                Offset(cx - w * 0.045f, h * 0.825f),
                Offset(cx - w * 0.015f, h * 0.760f)
            )
            // Left Achilles Tendon
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.055f, h * 0.820f),
                Offset(cx - w * 0.035f, h * 0.820f),
                Offset(cx - w * 0.032f, h * 0.890f),
                Offset(cx - w * 0.058f, h * 0.890f)
            )

            // Right Gastrocnemius Lateral Head
            drawFacetPolygon(calfColor, isCalvesSelected, gapStrokeColor,
                Offset(cx + w * 0.055f, h * 0.695f),
                Offset(cx + w * 0.105f * calfScale, h * 0.745f),
                Offset(cx + w * 0.065f, h * 0.815f),
                Offset(cx + w * 0.055f, h * 0.760f)
            )
            // Right Gastrocnemius Medial Head
            drawFacetPolygon(calfColor, isCalvesSelected, gapStrokeColor,
                Offset(cx + w * 0.055f, h * 0.695f),
                Offset(cx + w * 0.055f, h * 0.760f),
                Offset(cx + w * 0.045f, h * 0.825f),
                Offset(cx + w * 0.015f, h * 0.760f)
            )
            // Right Achilles Tendon
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx + w * 0.035f, h * 0.820f),
                Offset(cx + w * 0.055f, h * 0.820f),
                Offset(cx + w * 0.058f, h * 0.890f),
                Offset(cx + w * 0.032f, h * 0.890f)
            )

            // 11. HEELS & FEET
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx - w * 0.060f, h * 0.895f),
                Offset(cx - w * 0.030f, h * 0.895f),
                Offset(cx - w * 0.032f, h * 0.945f),
                Offset(cx - w * 0.068f, h * 0.945f)
            )
            drawFacetPolygon(baseFacetColor, false, gapStrokeColor,
                Offset(cx + w * 0.030f, h * 0.895f),
                Offset(cx + w * 0.060f, h * 0.895f),
                Offset(cx + w * 0.068f, h * 0.945f),
                Offset(cx + w * 0.032f, h * 0.945f)
            )
        }
    }
}

fun DrawScope.drawFacetPolygon(
    fillColor: Color,
    isSelected: Boolean = false,
    strokeColor: Color = Color(0xFF0C1019),
    p1: Offset,
    p2: Offset,
    p3: Offset,
    p4: Offset? = null,
    p5: Offset? = null,
    p6: Offset? = null
) {
    val points = if (p6 != null) listOf(p1, p2, p3, p4!!, p5!!, p6)
    else if (p5 != null) listOf(p1, p2, p3, p4!!, p5)
    else if (p4 != null) listOf(p1, p2, p3, p4)
    else listOf(p1, p2, p3)

    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
        close()
    }
    drawPath(path, color = fillColor, style = Fill)
    drawPath(path, color = strokeColor, style = Stroke(width = 2.2f))
    if (isSelected) {
        drawPath(path, color = Color.White, style = Stroke(width = 3.5f))
    }
}

@Composable
fun LegendChip(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = text, fontSize = 10.sp, color = Color.White)
    }
}

@Composable
fun SculptedAnatomyCanvas(
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
    FacetedMuscleCanvas(
        isFront = isFront,
        activities = activities,
        selectedMuscle = selectedMuscle,
        chestScale = chestScale,
        waistScale = waistScale,
        armScale = armScale,
        legScale = legScale,
        calfScale = calfScale,
        onSelectMuscle = onSelectMuscle
    )
}

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
    FacetedMuscleCanvas(
        isFront = isFront,
        activities = activities,
        selectedMuscle = selectedMuscle,
        chestScale = chestScale,
        waistScale = waistScale,
        armScale = armScale,
        legScale = legScale,
        calfScale = calfScale,
        onSelectMuscle = onSelectMuscle
    )
}
