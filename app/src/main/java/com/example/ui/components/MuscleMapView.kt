package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
        setsCount >= 10 -> 3 // Optymalna / wysoka
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

        if (textToMatch.contains("klatka") || textToMatch.contains("piersiow") || textToMatch.contains("chest") || textToMatch.contains("wyciskanie")) {
            hitMuscles.add(MuscleGroup.CHEST)
        }
        if (textToMatch.contains("bark") || textToMatch.contains("naramienn") || textToMatch.contains("żołnierskie") || textToMatch.contains("overhead") || textToMatch.contains("ohp")) {
            hitMuscles.add(MuscleGroup.SHOULDERS)
        }
        if (textToMatch.contains("biceps") || textToMatch.contains("ramienn") || textToMatch.contains("uginanie ramion")) {
            hitMuscles.add(MuscleGroup.BICEPS)
        }
        if (textToMatch.contains("triceps") || textToMatch.contains("francuskie") || textToMatch.contains("trójgłow") || textToMatch.contains("dips")) {
            hitMuscles.add(MuscleGroup.TRICEPS)
        }
        if (textToMatch.contains("brzuch") || textToMatch.contains("core") || textToMatch.contains("plank") || textToMatch.contains("skośne") || textToMatch.contains("pallof")) {
            hitMuscles.add(MuscleGroup.ABS)
        }
        if (textToMatch.contains("plecy") || textToMatch.contains("najszersz") || textToMatch.contains("wiosłow") || textToMatch.contains("podciąganie") || textToMatch.contains("chin-up") || textToMatch.contains("czworoboczn")) {
            hitMuscles.add(MuscleGroup.BACK)
        }
        if (textToMatch.contains("czworo") || textToMatch.contains("przysiad") || textToMatch.contains("wykroki") || textToMatch.contains("quad")) {
            hitMuscles.add(MuscleGroup.QUADS)
        }
        if (textToMatch.contains("dwugłow") || textToMatch.contains("kulszow") || textToMatch.contains("martwy ciąg") || textToMatch.contains("rdl") || textToMatch.contains("hamstring")) {
            hitMuscles.add(MuscleGroup.HAMSTRINGS)
        }
        if (textToMatch.contains("poślad") || textToMatch.contains("glute") || textToMatch.contains("hip thrust")) {
            hitMuscles.add(MuscleGroup.GLUTES)
        }
        if (textToMatch.contains("łydk") || textToMatch.contains("brzuchat") || textToMatch.contains("wspięcia")) {
            hitMuscles.add(MuscleGroup.CALVES)
        }
        if (textToMatch.contains("przedramion") || textToMatch.contains("chwyt") || textToMatch.contains("farmer")) {
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
        3 -> Color(0xFFFF9E0B)
        2 -> ElectricCyan
        1 -> Color(0xFF38BDF8)
        else -> Color(0xFF1E293B)
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
    var displayTab by remember { mutableIntStateOf(0) } // 0: Model Wektorowy z Pomiarów (Beta), 1: Atlas Anatomiczny HD
    var selectedPeriod by remember { mutableStateOf(MusclePeriod.MONTH) }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(initialSelectedMuscle) }
    var viewMode by remember { mutableIntStateOf(0) } // 0: Przód, 1: Tył, 2: Oba
    var applyMeasurementScale by remember { mutableStateOf(latestMeasurement != null) }

    val activities = remember(exercises, completedSets, selectedPeriod) {
        calculateMuscleActivities(exercises, completedSets, selectedPeriod.days)
    }

    val totalActiveSets = remember(activities) {
        activities.values.sumOf { it.setsCount }
    }

    // Dynamic silhouette scaling from real measurements (Beta)
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
        // Tab Row: Model Wektorowy vs Atlas Anatomiczny HD
        TabRow(
            selectedTabIndex = displayTab,
            containerColor = Color(0xFF0D1424),
            contentColor = ElectricCyan
        ) {
            Tab(
                selected = displayTab == 0,
                onClick = { displayTab = 0 },
                text = { Text("Model Sylwetki (Pomiary)", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AccessibilityNew, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = displayTab == 1,
                onClick = { displayTab = 1 },
                text = { Text("Atlas Anatomiczny HD", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

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

        if (displayTab == 0) {
            // Interactive Muscular Silhouette Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B111E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header & View Mode Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessibilityNew,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sylwetka i Zaangażowanie",
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

                        // View Mode Switcher: Przód / Tył / Oba
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                listOf("Przód", "Tył", "Oba").forEachIndexed { index, label ->
                                    val active = viewMode == index
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { viewMode = index }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Measurement Proportions Toggle (Beta Feature)
                    if (latestMeasurement != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF162033),
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
                                            text = "Odwzorowanie wymiarów (Beta)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (vTaperRatio != null) {
                                            Text(
                                                text = "Wskaźnik V-Taper: ${String.format("%.2f", vTaperRatio)} (Klatka: ${latestMeasurement.chestCm}cm / Talia: ${latestMeasurement.waistCm}cm)",
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

                    // Muscular Anatomical Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .background(Color(0xFF070B14), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (viewMode) {
                            0 -> {
                                SculptedAnatomyCanvas(
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
                            1 -> {
                                SculptedAnatomyCanvas(
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
                            else -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Przód", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                        Box(modifier = Modifier.weight(1f).aspectRatio(0.52f)) {
                                            SculptedAnatomyCanvas(
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
                                            .height(240.dp)
                                            .background(Color(0xFF1E293B))
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Tył", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                        Box(modifier = Modifier.weight(1f).aspectRatio(0.52f)) {
                                            SculptedAnatomyCanvas(
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
                            LegendChip(Color(0xFF1E293B), "0")
                            LegendChip(Color(0xFF38BDF8), "1-4")
                            LegendChip(ElectricCyan, "5-9")
                            LegendChip(Color(0xFFFF9E0B), "10-15")
                            LegendChip(AthleticOrange, "16+")
                        }
                    }
                }
            }
        } else {
            // High-Definition Medical Fitness Anatomy Atlas
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B111E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Atlas Anatomiczny HD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.muscle_anatomy_model),
                            contentDescription = "Atlas Anatomiczny",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kompletny schemat zaangażowania włókien mięśniowych w treningu siłowym.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
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
                        Text(
                            text = "${act?.setsCount ?: 0} serii",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Tonaż całkowity: ${((act?.totalTonnage ?: 0f) / 1000f).let { String.format("%.1f t", it) }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (act != null && act.exerciseNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ćwiczenia: ${act.exerciseNames.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Brak zarejestrowanych serii w tym okresie. Wykonaj ćwiczenia na tę partię!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Horizontal breakdown cards for all muscles
        Text(
            text = "Objętość według partii",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )

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
                        .width(140.dp)
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
 * Beautifully sculpted athletic human body canvas.
 * Draws organic curves, distinct muscle contours, and dynamically scales proportions to body measurements!
 */
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
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Head (Sculpted oval)
        drawOval(
            color = Color(0xFF334155),
            topLeft = Offset(cx - w * 0.055f, h * 0.035f),
            size = Size(w * 0.11f, h * 0.082f)
        )
        // Neck & Traps Base
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(cx - w * 0.045f, h * 0.11f),
            size = Size(w * 0.09f, h * 0.045f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        val shoulderSpan = (w * 0.22f) * chestScale

        if (isFront) {
            // ================= FRONT ANATOMY =================

            // Shoulders (Deltoids - organic chiseled curves)
            val shoulderColor = getMuscleColor(activities[MuscleGroup.SHOULDERS]?.level ?: 0, selectedMuscle == MuscleGroup.SHOULDERS)
            val deltWidth = (w * 0.115f) * armScale
            val deltHeight = h * 0.095f

            // Left Deltoid
            drawRoundRect(
                color = shoulderColor,
                topLeft = Offset(cx - shoulderSpan - deltWidth * 0.65f, h * 0.155f),
                size = Size(deltWidth, deltHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )
            // Right Deltoid
            drawRoundRect(
                color = shoulderColor,
                topLeft = Offset(cx + shoulderSpan - deltWidth * 0.35f, h * 0.155f),
                size = Size(deltWidth, deltHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Pectorals (Chest Plates)
            val chestColor = getMuscleColor(activities[MuscleGroup.CHEST]?.level ?: 0, selectedMuscle == MuscleGroup.CHEST)
            val pecWidth = (w * 0.15f) * chestScale
            val pecHeight = h * 0.096f

            // Left Pec
            drawRoundRect(
                color = chestColor,
                topLeft = Offset(cx - pecWidth - w * 0.008f, h * 0.165f),
                size = Size(pecWidth, pecHeight),
                cornerRadius = CornerRadius(14f, 14f)
            )
            // Right Pec
            drawRoundRect(
                color = chestColor,
                topLeft = Offset(cx + w * 0.008f, h * 0.165f),
                size = Size(pecWidth, pecHeight),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // Biceps (Arm Curvature)
            val bicepsColor = getMuscleColor(activities[MuscleGroup.BICEPS]?.level ?: 0, selectedMuscle == MuscleGroup.BICEPS)
            val bicepWidth = (w * 0.09f) * armScale
            val bicepHeight = h * 0.12f

            drawRoundRect(
                color = bicepsColor,
                topLeft = Offset(cx - shoulderSpan - bicepWidth * 0.85f, h * 0.25f),
                size = Size(bicepWidth, bicepHeight),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = bicepsColor,
                topLeft = Offset(cx + shoulderSpan - bicepWidth * 0.15f, h * 0.25f),
                size = Size(bicepWidth, bicepHeight),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // Forearms
            val forearmColor = getMuscleColor(activities[MuscleGroup.FOREARMS]?.level ?: 0, selectedMuscle == MuscleGroup.FOREARMS)
            val forearmWidth = (w * 0.078f) * armScale
            drawRoundRect(
                color = forearmColor,
                topLeft = Offset(cx - shoulderSpan - forearmWidth * 1.05f, h * 0.375f),
                size = Size(forearmWidth, h * 0.13f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = forearmColor,
                topLeft = Offset(cx + shoulderSpan + forearmWidth * 0.05f, h * 0.375f),
                size = Size(forearmWidth, h * 0.13f),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // Abs / Core (V-Taper Tapered Abdominal Grid)
            val absColor = getMuscleColor(activities[MuscleGroup.ABS]?.level ?: 0, selectedMuscle == MuscleGroup.ABS)
            val baseAbsWidth = (w * 0.18f) * waistScale

            for (row in 0..2) {
                val rowWidth = baseAbsWidth * (1.0f - row * 0.045f)
                val rowLeft = cx - rowWidth / 2f
                drawRoundRect(
                    color = absColor,
                    topLeft = Offset(rowLeft, h * (0.272f + row * 0.04f)),
                    size = Size(rowWidth, h * 0.034f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }

            // Pelvis / Hips
            val hipWidth = (w * 0.26f) * waistScale
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(cx - hipWidth / 2f, h * 0.405f),
                size = Size(hipWidth, h * 0.065f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Quads (Czworogłowe ud)
            val quadColor = getMuscleColor(activities[MuscleGroup.QUADS]?.level ?: 0, selectedMuscle == MuscleGroup.QUADS)
            val quadWidth = (w * 0.16f) * legScale
            val quadHeight = h * 0.245f

            // Left Quad
            drawRoundRect(
                color = quadColor,
                topLeft = Offset(cx - quadWidth - w * 0.012f, h * 0.478f),
                size = Size(quadWidth, quadHeight),
                cornerRadius = CornerRadius(18f, 18f)
            )
            // Right Quad
            drawRoundRect(
                color = quadColor,
                topLeft = Offset(cx + w * 0.012f, h * 0.478f),
                size = Size(quadWidth, quadHeight),
                cornerRadius = CornerRadius(18f, 18f)
            )

            // Calves (Front Shin / Calves)
            val calfColor = getMuscleColor(activities[MuscleGroup.CALVES]?.level ?: 0, selectedMuscle == MuscleGroup.CALVES)
            val calfWidth = (w * 0.10f) * calfScale
            val calfHeight = h * 0.20f

            drawRoundRect(
                color = calfColor,
                topLeft = Offset(cx - calfWidth - w * 0.022f, h * 0.735f),
                size = Size(calfWidth, calfHeight),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = calfColor,
                topLeft = Offset(cx + w * 0.022f, h * 0.735f),
                size = Size(calfWidth, calfHeight),
                cornerRadius = CornerRadius(14f, 14f)
            )

        } else {
            // ================= REAR ANATOMY =================

            // Trapezius & Back (Najszerszy Grzbietu / Lats)
            val backColor = getMuscleColor(activities[MuscleGroup.BACK]?.level ?: 0, selectedMuscle == MuscleGroup.BACK)
            val latsWidth = (w * 0.28f) * chestScale
            val latsHeight = h * 0.22f

            drawRoundRect(
                color = backColor,
                topLeft = Offset(cx - latsWidth / 2f, h * 0.155f),
                size = Size(latsWidth, latsHeight),
                cornerRadius = CornerRadius(18f, 18f)
            )

            // Rear Shoulders
            val shoulderColor = getMuscleColor(activities[MuscleGroup.SHOULDERS]?.level ?: 0, selectedMuscle == MuscleGroup.SHOULDERS)
            val deltWidth = (w * 0.115f) * armScale
            drawRoundRect(
                color = shoulderColor,
                topLeft = Offset(cx - shoulderSpan - deltWidth * 0.65f, h * 0.155f),
                size = Size(deltWidth, h * 0.095f),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = shoulderColor,
                topLeft = Offset(cx + shoulderSpan - deltWidth * 0.35f, h * 0.155f),
                size = Size(deltWidth, h * 0.095f),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Triceps (Horseshoe)
            val tricepsColor = getMuscleColor(activities[MuscleGroup.TRICEPS]?.level ?: 0, selectedMuscle == MuscleGroup.TRICEPS)
            val tricepWidth = (w * 0.09f) * armScale
            drawRoundRect(
                color = tricepsColor,
                topLeft = Offset(cx - shoulderSpan - tricepWidth * 0.85f, h * 0.25f),
                size = Size(tricepWidth, h * 0.12f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = tricepsColor,
                topLeft = Offset(cx + shoulderSpan - tricepWidth * 0.15f, h * 0.25f),
                size = Size(tricepWidth, h * 0.12f),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // Glutes (Pośladki)
            val gluteColor = getMuscleColor(activities[MuscleGroup.GLUTES]?.level ?: 0, selectedMuscle == MuscleGroup.GLUTES)
            val gluteWidth = (w * 0.145f) * waistScale
            val gluteHeight = h * 0.11f

            drawRoundRect(
                color = gluteColor,
                topLeft = Offset(cx - gluteWidth - w * 0.008f, h * 0.385f),
                size = Size(gluteWidth, gluteHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = gluteColor,
                topLeft = Offset(cx + w * 0.008f, h * 0.385f),
                size = Size(gluteWidth, gluteHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Hamstrings (Dwugłowe ud)
            val hamColor = getMuscleColor(activities[MuscleGroup.HAMSTRINGS]?.level ?: 0, selectedMuscle == MuscleGroup.HAMSTRINGS)
            val hamWidth = (w * 0.15f) * legScale
            val hamHeight = h * 0.225f

            drawRoundRect(
                color = hamColor,
                topLeft = Offset(cx - hamWidth - w * 0.012f, h * 0.505f),
                size = Size(hamWidth, hamHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = hamColor,
                topLeft = Offset(cx + w * 0.012f, h * 0.505f),
                size = Size(hamWidth, hamHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Calves (Tył łydek - Diamond gastrocnemius heads)
            val calfColor = getMuscleColor(activities[MuscleGroup.CALVES]?.level ?: 0, selectedMuscle == MuscleGroup.CALVES)
            val calfWidth = (w * 0.11f) * calfScale
            val calfHeight = h * 0.20f

            drawRoundRect(
                color = calfColor,
                topLeft = Offset(cx - calfWidth - w * 0.02f, h * 0.74f),
                size = Size(calfWidth, calfHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = calfColor,
                topLeft = Offset(cx + w * 0.02f, h * 0.74f),
                size = Size(calfWidth, calfHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )
        }
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
    SculptedAnatomyCanvas(
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

