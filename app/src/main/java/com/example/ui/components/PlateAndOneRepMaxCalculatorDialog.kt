package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import kotlin.math.roundToInt

data class PlateSpec(
    val weightKg: Float,
    val countPerSide: Int,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateAndOneRepMaxCalculatorDialog(
    initialTargetWeightKg: Float? = null,
    initialReps: Int? = null,
    onApplyWeight: ((Float) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Talerze, 1: 1RM

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .widthIn(max = 560.dp)
                    .testTag("dialog_plate_calculator"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Nagłówek
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = AthleticOrange.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = AthleticOrange,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = when (selectedTab) {
                                        0 -> "Kalkulator Talerzy"
                                        1 -> "Kalkulator 1RM"
                                        else -> "Kalkulator Rozgrzewki"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (selectedTab) {
                                        0 -> "Rozkład obciążenia na gryf"
                                        1 -> "Szacowanie rekordu na 1 powtórzenie"
                                        else -> "Rampa rozgrzewkowa pod serie robocze"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("btn_close_calculator")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Zamknij")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Navigation
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Talerze", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("1RM", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Rozgrzewka", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (selectedTab) {
                        0 -> {
                            PlateCalculatorTab(
                                initialWeight = initialTargetWeightKg ?: 60f,
                                onApplyWeight = onApplyWeight,
                                onDismiss = onDismiss
                            )
                        }
                        1 -> {
                            OneRepMaxTab(
                                initialWeight = initialTargetWeightKg ?: 80f,
                                initialReps = initialReps ?: 8,
                                onApplyWeight = onApplyWeight,
                                onDismiss = onDismiss
                            )
                        }
                        else -> {
                            WarmupCalculatorTab(
                                initialWorkingWeight = initialTargetWeightKg ?: 80f,
                                onApplyWeight = onApplyWeight,
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlateCalculatorTab(
    initialWeight: Float,
    onApplyWeight: ((Float) -> Unit)?,
    onDismiss: () -> Unit
) {
    var targetWeightText by remember { mutableStateOf(if (initialWeight % 1f == 0f) initialWeight.toInt().toString() else initialWeight.toString()) }
    var barWeight by remember { mutableStateOf(20f) } // 20kg olimpijski, 15kg damski, 10kg łamany, 0kg maszyna

    val targetWeight = targetWeightText.toFloatOrNull() ?: 0f
    val weightForPlates = (targetWeight - barWeight).coerceAtLeast(0f)
    val weightPerSide = weightForPlates / 2f

    // Wyliczenie talerzy na stronę
    val standardPlates = listOf(
        25f to Color(0xFFEF4444), // Czerwony
        20f to Color(0xFF3B82F6), // Niebieski
        15f to Color(0xFFEAB308), // Żółty
        10f to Color(0xFF22C55E), // Zielony
        5f to Color(0xFFE2E8F0),  // Biały
        2.5f to Color(0xFF475569), // Czarny
        1.25f to Color(0xFF94A3B8) // Srebrny
    )

    val platesPerSide = remember(weightPerSide) {
        val result = mutableListOf<PlateSpec>()
        var rem = weightPerSide
        for ((pWeight, pColor) in standardPlates) {
            val count = (rem / pWeight).toInt()
            if (count > 0) {
                result.add(PlateSpec(pWeight, count, pColor))
                rem -= count * pWeight
                rem = (rem * 100).roundToInt() / 100f // Zaokrąglenie float
            }
        }
        result
    }

    val remainder = remember(weightPerSide, platesPerSide) {
        val accounted = platesPerSide.sumOf { (it.weightKg * it.countPerSide).toDouble() }.toFloat()
        (weightPerSide - accounted).coerceAtLeast(0f)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Pole docelowego ciężaru
            OutlinedTextField(
                value = targetWeightText,
                onValueChange = { targetWeightText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Docelowy ciężar całkowity (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_plate_target_weight")
            )
        }

        item {
            // Szybkie przyciski korekty +/-
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(-5f, -2.5f, -1.25f, 1.25f, 2.5f, 5f).forEach { delta ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val current = targetWeightText.toFloatOrNull() ?: 0f
                                val updated = (current + delta).coerceAtLeast(0f)
                                targetWeightText = if (updated % 1f == 0f) updated.toInt().toString() else updated.toString()
                            }
                    ) {
                        Text(
                            text = if (delta > 0) "+$delta" else "$delta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            // Wybór gryfu
            Text(
                text = "Waga gryfu:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    20f to "20 kg (Olimpijski)",
                    15f to "15 kg (Techniczny)",
                    10f to "10 kg (Łamany EZ)",
                    0f to "0 kg (Maszyna)"
                ).forEach { (w, lbl) ->
                    FilterChip(
                        selected = barWeight == w,
                        onClick = { barWeight = w },
                        label = { Text(lbl, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AthleticOrange,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            // Podsumowanie na każdą stronę
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AthleticOrange.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Na każdą stronę:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AthleticOrange.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${weightPerSide} kg",
                                fontWeight = FontWeight.ExtraBold,
                                color = AthleticOrange,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Wizualny gryf z talerzami
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Oś gryfu
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color(0xFF94A3B8), RoundedCornerShape(3.dp))
                        )

                        // Talerze nałożone na tuleję
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            // Kołnierz gryfu
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(38.dp)
                                    .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                            )

                            platesPerSide.forEach { spec ->
                                repeat(spec.countPerSide) {
                                    val plateHeight = when (spec.weightKg) {
                                        25f -> 48.dp
                                        20f -> 44.dp
                                        15f -> 40.dp
                                        10f -> 36.dp
                                        5f -> 30.dp
                                        2.5f -> 26.dp
                                        else -> 22.dp
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(10.dp)
                                            .height(plateHeight)
                                            .background(spec.color, RoundedCornerShape(2.dp))
                                            .border(0.5.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lista talerzy
                    if (platesPerSide.isEmpty()) {
                        Text(
                            text = if (targetWeight <= barWeight) "Sam gryf bez talerzy" else "Brak pasujących talerzy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            platesPerSide.forEach { spec ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = spec.color.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, spec.color.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "${spec.countPerSide}× ${spec.weightKg} kg",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (spec.weightKg == 5f) MaterialTheme.colorScheme.onSurface else spec.color,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (remainder > 0.05f) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pozostało poza standardowymi talerzami: ${(remainder * 2).roundToInt()} kg łącznie",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPr
                        )
                    }
                }
            }
        }

        if (onApplyWeight != null) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        onApplyWeight(targetWeight)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_apply_calculated_weight"),
                    colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ustaw ${targetWeight} kg w serii", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OneRepMaxTab(
    initialWeight: Float,
    initialReps: Int,
    onApplyWeight: ((Float) -> Unit)?,
    onDismiss: () -> Unit
) {
    var weightText by remember { mutableStateOf(if (initialWeight % 1f == 0f) initialWeight.toInt().toString() else initialWeight.toString()) }
    var repsText by remember { mutableStateOf(initialReps.toString()) }

    val weight = weightText.toFloatOrNull() ?: 0f
    val reps = repsText.toIntOrNull() ?: 1

    // Obliczenia 1RM
    val oneRepMaxEpley = if (reps > 1) weight * (1f + reps / 30f) else weight
    val oneRepMaxBrzycki = if (reps > 1 && reps < 37) weight * (36f / (37f - reps)) else weight
    val estimated1RM = ((oneRepMaxEpley + oneRepMaxBrzycki) / 2f).coerceAtLeast(weight)

    val percentages = listOf(
        100 to 1,
        95 to 2,
        90 to 4,
        85 to 6,
        80 to 8,
        75 to 10,
        70 to 12,
        65 to 15
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Podniesiony ciężar (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Powtórzenia") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.7f)
                )
            }
        }

        item {
            // Wynik 1RM
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ElectricCyan.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SZACUNKOWY REKORD (1RM)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${((estimated1RM * 10).roundToInt() / 10f)} kg",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Epley: ${((oneRepMaxEpley * 10).roundToInt() / 10f)} kg • Brzycki: ${((oneRepMaxBrzycki * 10).roundToInt() / 10f)} kg",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "Tabela obciążeń treningowych (% z 1RM):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(percentages) { (percent, approxReps) ->
            val calcWeight = ((estimated1RM * (percent / 100f) * 4).roundToInt() / 4f) // Do 0.25 kg
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onApplyWeight?.invoke(calcWeight)
                        if (onApplyWeight != null) onDismiss()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AthleticOrange.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "$percent%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = AthleticOrange,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ok. $approxReps powt.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$calcWeight kg",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (onApplyWeight != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Zastosuj",
                                tint = AthleticOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class WarmupStep(
    val stepName: String,
    val weightKg: Float,
    val repsInfo: String,
    val purpose: String,
    val percentageOfWorking: Int,
    val isWorkingSet: Boolean = false
)

@Composable
private fun WarmupCalculatorTab(
    initialWorkingWeight: Float,
    onApplyWeight: ((Float) -> Unit)?,
    onDismiss: () -> Unit
) {
    var workingWeightText by remember {
        mutableStateOf(if (initialWorkingWeight % 1f == 0f) initialWorkingWeight.toInt().toString() else initialWorkingWeight.toString())
    }
    var barWeight by remember { mutableStateOf(20f) }

    val targetWeight = workingWeightText.toFloatOrNull() ?: 0f

    val warmupSteps = remember(targetWeight, barWeight) {
        if (targetWeight <= 0f) emptyList()
        else {
            val list = mutableListOf<WarmupStep>()
            val step0Weight = if (targetWeight > barWeight) barWeight else (targetWeight * 0.4f)
            val roundedStep0 = (step0Weight * 2f).roundToInt() / 2f
            list.add(
                WarmupStep(
                    stepName = "Dogrzanie wzorca",
                    weightKg = roundedStep0,
                    repsInfo = "10–12 powt.",
                    purpose = "Mobilizacja stawów i tor ruchu",
                    percentageOfWorking = if (targetWeight > 0f) ((roundedStep0 / targetWeight) * 100).toInt() else 0
                )
            )

            val w1 = maxOf(barWeight, ((targetWeight * 0.50f) / 2.5f).roundToInt() * 2.5f)
            if (w1 > roundedStep0 && w1 < targetWeight) {
                list.add(
                    WarmupStep(
                        stepName = "Rampa 1 (50%)",
                        weightKg = w1,
                        repsInfo = "5–6 powt.",
                        purpose = "Aktywacja układu mięśniowego",
                        percentageOfWorking = ((w1 / targetWeight) * 100).toInt()
                    )
                )
            }

            val w2 = maxOf(w1 + 2.5f, ((targetWeight * 0.70f) / 2.5f).roundToInt() * 2.5f)
            if (w2 > w1 && w2 < targetWeight) {
                list.add(
                    WarmupStep(
                        stepName = "Rampa 2 (70%)",
                        weightKg = w2,
                        repsInfo = "3–4 powt.",
                        purpose = "Pobudzenie układu nerwowego (OUN)",
                        percentageOfWorking = ((w2 / targetWeight) * 100).toInt()
                    )
                )
            }

            val w3 = maxOf(w2 + 2.5f, ((targetWeight * 0.85f) / 2.5f).roundToInt() * 2.5f)
            if (w3 > w2 && w3 < targetWeight) {
                list.add(
                    WarmupStep(
                        stepName = "Rampa 3 (85%)",
                        weightKg = w3,
                        repsInfo = "2 powt.",
                        purpose = "Adaptacja do ciężaru roboczego",
                        percentageOfWorking = ((w3 / targetWeight) * 100).toInt()
                    )
                )
            }

            if (targetWeight >= 75f) {
                val w4 = maxOf(w3 + 2.5f, ((targetWeight * 0.93f) / 2.5f).roundToInt() * 2.5f)
                if (w4 > w3 && w4 < targetWeight) {
                    list.add(
                        WarmupStep(
                            stepName = "Rampa 4 (93%)",
                            weightKg = w4,
                            repsInfo = "1 powt.",
                            purpose = "Potencjacja (PAP) bez zmęczenia kwasem",
                            percentageOfWorking = ((w4 / targetWeight) * 100).toInt()
                        )
                    )
                }
            }

            list.add(
                WarmupStep(
                    stepName = "Seria Robocza",
                    weightKg = targetWeight,
                    repsInfo = "Docelowe powtórzenia",
                    purpose = "100% obciążenia planowanego",
                    percentageOfWorking = 100,
                    isWorkingSet = true
                )
            )
            list
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = workingWeightText,
                onValueChange = { workingWeightText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Docelowy ciężar serii roboczych (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_warmup_target_weight")
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(-10f, -5f, -2.5f, 2.5f, 5f, 10f).forEach { delta ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val current = workingWeightText.toFloatOrNull() ?: 0f
                                val updated = (current + delta).coerceAtLeast(0f)
                                workingWeightText = if (updated % 1f == 0f) updated.toInt().toString() else updated.toString()
                            }
                    ) {
                        Text(
                            text = if (delta > 0) "+$delta" else "$delta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Waga gryfu:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    20f to "20 kg (Olimpijski)",
                    15f to "15 kg (Kobiety)",
                    10f to "10 kg (EZ)",
                    0f to "0 kg (Hantle)"
                ).forEach { (w, lbl) ->
                    FilterChip(
                        selected = barWeight == w,
                        onClick = { barWeight = w },
                        label = { Text(lbl, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AthleticOrange,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = AthleticOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Rampa rozgrzewkowa przygotowuje układ nerwowy i stawy bez kumulacji zmęczenia. Kliknij na krok, aby zastosować ciężar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        items(warmupSteps) { step ->
            val perSide = if (step.weightKg > barWeight) (step.weightKg - barWeight) / 2f else 0f
            val perSideText = if (step.weightKg <= barWeight) "Pusty gryf ($barWeight kg)"
                              else "+${if (perSide % 1f == 0f) perSide.toInt() else perSide} kg / stronę"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onApplyWeight?.invoke(step.weightKg)
                        if (onApplyWeight != null) onDismiss()
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (step.isWorkingSet) AthleticOrange.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (step.isWorkingSet) 1.5.dp else 1.dp,
                    color = if (step.isWorkingSet) AthleticOrange else Color.Transparent
                )
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
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (step.isWorkingSet) AthleticOrange else ElectricCyan.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${step.percentageOfWorking}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (step.isWorkingSet) Color.White else ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step.stepName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${step.repsInfo} • ${step.purpose}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = perSideText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (step.isWorkingSet) AthleticOrange else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (step.weightKg % 1f == 0f) step.weightKg.toInt() else step.weightKg} kg",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (step.isWorkingSet) AthleticOrange else MaterialTheme.colorScheme.onSurface
                        )
                        if (onApplyWeight != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Zastosuj",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Wstaw",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
