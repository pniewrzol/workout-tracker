package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BodyMeasurement
import com.example.ui.components.ChartPoint
import com.example.ui.components.LineChartComposable
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.MeasurementMetric
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMeasurementsScreen(
    measurements: List<BodyMeasurement>,
    measurementsAsc: List<BodyMeasurement>,
    getChartPoints: (List<BodyMeasurement>, MeasurementMetric) -> List<ChartPoint>,
    onAddMeasurement: (BodyMeasurement) -> Unit,
    onDeleteMeasurement: (BodyMeasurement) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMetric by remember { mutableStateOf(MeasurementMetric.WEIGHT) }

    val chartPoints = remember(measurementsAsc, selectedMetric) {
        getChartPoints(measurementsAsc, selectedMetric)
    }

    // Key statistics calculations
    val latestMeasurement = measurements.firstOrNull()
    val oldestMeasurement = measurements.lastOrNull()

    val weightDiff = remember(latestMeasurement, oldestMeasurement) {
        if (latestMeasurement?.weightKg != null && oldestMeasurement?.weightKg != null && measurements.size > 1) {
            latestMeasurement.weightKg - oldestMeasurement.weightKg
        } else null
    }

    val bodyFatDiff = remember(latestMeasurement, oldestMeasurement) {
        if (latestMeasurement?.bodyFatPercentage != null && oldestMeasurement?.bodyFatPercentage != null && measurements.size > 1) {
            latestMeasurement.bodyFatPercentage - oldestMeasurement.bodyFatPercentage
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pomiary Ciała & Waga",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wstecz"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AthleticOrange,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_measurement_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Dodaj pomiar")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Aktualny Stan Sylwetki",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Waga",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = latestMeasurement?.weightKg?.let { "$it kg" } ?: "-",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AthleticOrange
                                )
                                if (weightDiff != null) {
                                    val diffText = if (weightDiff > 0) "+${(weightDiff * 10).roundToInt() / 10f} kg" else "${(weightDiff * 10).roundToInt() / 10f} kg"
                                    Text(
                                        text = "Zmiana: $diffText",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (weightDiff <= 0) SuccessGreen else AthleticOrange
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Tkanka tłuszczowa",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = latestMeasurement?.bodyFatPercentage?.let { "$it %" } ?: "-",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                                if (bodyFatDiff != null) {
                                    val diffText = if (bodyFatDiff > 0) "+${(bodyFatDiff * 10).roundToInt() / 10f}%" else "${(bodyFatDiff * 10).roundToInt() / 10f}%"
                                    Text(
                                        text = "Zmiana: $diffText",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (bodyFatDiff <= 0) SuccessGreen else AthleticOrange
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Pas / Talia",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = latestMeasurement?.waistCm?.let { "$it cm" } ?: "-",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPr
                                )
                                Text(
                                    text = "Wpisy: ${measurements.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Metric Selector for Chart
            item {
                Column {
                    Text(
                        text = "Wykres Zmian w Czasie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(MeasurementMetric.entries) { metric ->
                            val isSelected = selectedMetric == metric
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMetric = metric },
                                label = { Text("${metric.label} (${metric.unit})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AthleticOrange,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    LineChartComposable(
                        points = chartPoints,
                        unit = selectedMetric.unit,
                        lineColor = AthleticOrange,
                        accentColor = ElectricCyan
                    )
                }
            }

            // Measurement History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historia Pomiarów",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${measurements.size} wpisów",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (measurements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Brak zarejestrowanych pomiarów",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Dotknij '+', aby dodać pierwszy pomiar wagi i obwodów",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(measurements, key = { it.id }) { item ->
                    MeasurementLogCard(
                        measurement = item,
                        onDelete = { onDeleteMeasurement(item) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddMeasurementDialog(
            onDismiss = { showAddDialog = false },
            onSave = { measurement ->
                onAddMeasurement(measurement)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MeasurementLogCard(
    measurement: BodyMeasurement,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("pl", "PL"))
        .format(Date(measurement.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Usuń pomiar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                measurement.weightKg?.let {
                    item { MeasurementBadge(label = "Waga", value = "$it kg", color = AthleticOrange) }
                }
                measurement.bodyFatPercentage?.let {
                    item { MeasurementBadge(label = "BF", value = "$it %", color = ElectricCyan) }
                }
                measurement.waistCm?.let {
                    item { MeasurementBadge(label = "Pas", value = "$it cm", color = GoldPr) }
                }
                measurement.chestCm?.let {
                    item { MeasurementBadge(label = "Klatka", value = "$it cm", color = SuccessGreen) }
                }
                measurement.bicepsCm?.let {
                    item { MeasurementBadge(label = "Biceps", value = "$it cm", color = MaterialTheme.colorScheme.primary) }
                }
                measurement.hipsCm?.let {
                    item { MeasurementBadge(label = "Biodra", value = "$it cm", color = MaterialTheme.colorScheme.secondary) }
                }
                measurement.thighsCm?.let {
                    item { MeasurementBadge(label = "Udo", value = "$it cm", color = MaterialTheme.colorScheme.tertiary) }
                }
                measurement.calvesCm?.let {
                    item { MeasurementBadge(label = "Łydka", value = "$it cm", color = MaterialTheme.colorScheme.outline) }
                }
                measurement.shouldersCm?.let {
                    item { MeasurementBadge(label = "Barki", value = "$it cm", color = AthleticOrange) }
                }
            }

            if (measurement.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = measurement.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MeasurementBadge(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun AddMeasurementDialog(
    onDismiss: () -> Unit,
    onSave: (BodyMeasurement) -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    var bodyFatText by remember { mutableStateOf("") }
    var chestText by remember { mutableStateOf("") }
    var waistText by remember { mutableStateOf("") }
    var bicepsText by remember { mutableStateOf("") }
    var hipsText by remember { mutableStateOf("") }
    var thighsText by remember { mutableStateOf("") }
    var calvesText by remember { mutableStateOf("") }
    var shouldersText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Dodaj Pomiary Sylwetki", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Waga (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("measurement_weight_input")
                        )
                        OutlinedTextField(
                            value = bodyFatText,
                            onValueChange = { bodyFatText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Tkanka (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = waistText,
                            onValueChange = { waistText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Pas / Talia (cm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = chestText,
                            onValueChange = { chestText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Klatka (cm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bicepsText,
                            onValueChange = { bicepsText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Biceps (cm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = hipsText,
                            onValueChange = { hipsText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Biodra (cm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = thighsText,
                            onValueChange = { thighsText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Udo (cm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = calvesText,
                            onValueChange = { calvesText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Łydka (cm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = shouldersText,
                        onValueChange = { shouldersText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Barki (cm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notatka (opcjonalnie)") },
                        placeholder = { Text("np. rano na czczo") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val measurement = BodyMeasurement(
                        timestamp = System.currentTimeMillis(),
                        weightKg = weightText.toFloatOrNull(),
                        bodyFatPercentage = bodyFatText.toFloatOrNull(),
                        chestCm = chestText.toFloatOrNull(),
                        waistCm = waistText.toFloatOrNull(),
                        bicepsCm = bicepsText.toFloatOrNull(),
                        hipsCm = hipsText.toFloatOrNull(),
                        thighsCm = thighsText.toFloatOrNull(),
                        calvesCm = calvesText.toFloatOrNull(),
                        shouldersCm = shouldersText.toFloatOrNull(),
                        notes = notesText
                    )
                    onSave(measurement)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                modifier = Modifier.testTag("save_measurement_button")
            ) {
                Text("Zapisz pomiar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
