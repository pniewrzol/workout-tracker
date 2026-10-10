package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.BodyMeasurement
import com.example.ui.components.ChartPoint
import com.example.ui.components.LineChartComposable
import com.example.ui.components.ProgressPhotoCompareDialog
import com.example.ui.components.ProgressPhotoViewerDialog
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.MeasurementMetric
import java.io.File
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
    onUpdateMeasurement: (BodyMeasurement) -> Unit = {},
    onDeleteMeasurement: (BodyMeasurement) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingMeasurement by remember { mutableStateOf<BodyMeasurement?>(null) }
    var showCompareDialog by remember { mutableStateOf(false) }
    var viewingPhotoUri by remember { mutableStateOf<Pair<String, String>?>(null) } // uri to title
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

    val measurementsWithPhotos = remember(measurements) {
        measurements.filter { !it.frontPhotoUri.isNullOrBlank() || !it.sidePhotoUri.isNullOrBlank() || !it.backPhotoUri.isNullOrBlank() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pomiary Ciała & Sylwetka",
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
                actions = {
                    if (measurementsWithPhotos.size >= 2) {
                        IconButton(onClick = { showCompareDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Porównaj zdjęcia",
                                tint = GoldPr
                            )
                        }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aktualny Stan Sylwetki",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (measurementsWithPhotos.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GoldPr.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable { showCompareDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Compare, contentDescription = null, tint = GoldPr, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Porównaj foto (${measurementsWithPhotos.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldPr)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Ostatnia waga",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = latestMeasurement?.weightKg?.let { "$it kg" } ?: "-",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AthleticOrange
                                )
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
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ElectricCyan
                                )
                            }

                            Column {
                                Text(
                                    text = "Zmiana wagi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (weightDiff != null) {
                                        val isDown = weightDiff < 0
                                        Icon(
                                            imageVector = if (isDown) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = if (isDown) SuccessGreen else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${String.format("%.1f", weightDiff)} kg",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDown) SuccessGreen else MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Text(
                                            text = "-",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Metric selector chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Wykres pomiaru",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MeasurementMetric.entries) { metric ->
                            val isSelected = selectedMetric == metric
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMetric = metric },
                                label = { Text(metric.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Interactive Progress Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedMetric.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${chartPoints.size} wpisów",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        LineChartComposable(
                            points = chartPoints,
                            unit = selectedMetric.unit,
                            accentColor = AthleticOrange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                }
            }

            // History header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historia Pomiarów & Zdjęcia (${measurements.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (measurements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Brak zapisanych pomiarów",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kliknij + aby dodać wagę, obwody i zdjęcia sylwetki",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(measurements, key = { it.id }) { measurement ->
                    MeasurementLogCard(
                        measurement = measurement,
                        onEdit = { editingMeasurement = measurement },
                        onDelete = { onDeleteMeasurement(measurement) },
                        onPhotoClick = { uri, title -> viewingPhotoUri = uri to title }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddOrEditMeasurementDialog(
            initialMeasurement = null,
            onDismiss = { showAddDialog = false },
            onSave = { measurement ->
                onAddMeasurement(measurement)
                showAddDialog = false
            }
        )
    }

    editingMeasurement?.let { toEdit ->
        AddOrEditMeasurementDialog(
            initialMeasurement = toEdit,
            onDismiss = { editingMeasurement = null },
            onSave = { updated ->
                onUpdateMeasurement(updated)
                editingMeasurement = null
            }
        )
    }

    if (showCompareDialog) {
        ProgressPhotoCompareDialog(
            measurementsWithPhotos = measurementsWithPhotos,
            onDismiss = { showCompareDialog = false }
        )
    }

    viewingPhotoUri?.let { (uri, title) ->
        ProgressPhotoViewerDialog(
            photoUriOrPath = uri,
            title = title,
            onDismiss = { viewingPhotoUri = null }
        )
    }
}

@Composable
fun MeasurementLogCard(
    measurement: BodyMeasurement,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPhotoClick: (String, String) -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("pl", "PL"))
        .format(Date(measurement.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("edit_measurement_${measurement.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edytuj pomiar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_measurement_${measurement.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Usuń pomiar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

            // Photos Row (Front, Side, Back)
            val hasPhotos = !measurement.frontPhotoUri.isNullOrBlank() ||
                            !measurement.sidePhotoUri.isNullOrBlank() ||
                            !measurement.backPhotoUri.isNullOrBlank()

            if (hasPhotos) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    measurement.frontPhotoUri?.let { uri ->
                        PhotoThumbnailBox(
                            photoUri = uri,
                            label = "Przód",
                            onClick = { onPhotoClick(uri, "Zdjęcie z przodu ($dateStr)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    measurement.sidePhotoUri?.let { uri ->
                        PhotoThumbnailBox(
                            photoUri = uri,
                            label = "Bok",
                            onClick = { onPhotoClick(uri, "Zdjęcie z boku ($dateStr)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    measurement.backPhotoUri?.let { uri ->
                        PhotoThumbnailBox(
                            photoUri = uri,
                            label = "Tył",
                            onClick = { onPhotoClick(uri, "Zdjęcie z tyłu ($dateStr)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
fun PhotoThumbnailBox(
    photoUri: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val model = remember(photoUri) {
        val file = File(photoUri)
        if (file.exists()) file else Uri.parse(photoUri)
    }

    Box(
        modifier = modifier
            .height(85.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .clickable { onClick() }
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            shape = RoundedCornerShape(topEnd = 6.dp),
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
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

fun saveUriToPrivateMeasurementFile(context: Context, uri: Uri, prefix: String): String? {
    return try {
        val dir = File(context.filesDir, "measurement_photos")
        if (!dir.exists()) dir.mkdirs()
        val destFile = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (_: Exception) {
        null
    }
}

@Composable
fun AddOrEditMeasurementDialog(
    initialMeasurement: BodyMeasurement? = null,
    onDismiss: () -> Unit,
    onSave: (BodyMeasurement) -> Unit
) {
    val isEditing = initialMeasurement != null
    val context = LocalContext.current

    val formatFloat: (Float?) -> String = { num ->
        if (num == null) "" else if (num % 1f == 0f) num.toInt().toString() else num.toString()
    }

    var weightText by remember { mutableStateOf(formatFloat(initialMeasurement?.weightKg)) }
    var bodyFatText by remember { mutableStateOf(formatFloat(initialMeasurement?.bodyFatPercentage)) }
    var chestText by remember { mutableStateOf(formatFloat(initialMeasurement?.chestCm)) }
    var waistText by remember { mutableStateOf(formatFloat(initialMeasurement?.waistCm)) }
    var bicepsText by remember { mutableStateOf(formatFloat(initialMeasurement?.bicepsCm)) }
    var hipsText by remember { mutableStateOf(formatFloat(initialMeasurement?.hipsCm)) }
    var thighsText by remember { mutableStateOf(formatFloat(initialMeasurement?.thighsCm)) }
    var calvesText by remember { mutableStateOf(formatFloat(initialMeasurement?.calvesCm)) }
    var shouldersText by remember { mutableStateOf(formatFloat(initialMeasurement?.shouldersCm)) }
    var notesText by remember { mutableStateOf(initialMeasurement?.notes ?: "") }
    var measurementTimestamp by remember { mutableStateOf(initialMeasurement?.timestamp ?: System.currentTimeMillis()) }

    var frontPhotoPath by remember { mutableStateOf(initialMeasurement?.frontPhotoUri) }
    var sidePhotoPath by remember { mutableStateOf(initialMeasurement?.sidePhotoUri) }
    var backPhotoPath by remember { mutableStateOf(initialMeasurement?.backPhotoUri) }

    val frontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { frontPhotoPath = saveUriToPrivateMeasurementFile(context, it, "front") }
    }
    val sideLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { sidePhotoPath = saveUriToPrivateMeasurementFile(context, it, "side") }
    }
    val backLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { backPhotoPath = saveUriToPrivateMeasurementFile(context, it, "back") }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = null,
                    tint = AthleticOrange,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Edytuj Pomiar Sylwetki" else "Dodaj Pomiary Sylwetki",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val dateFormatted = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("pl", "PL"))
                        .format(Date(measurementTimestamp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Data: $dateFormatted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isEditing) {
                                TextButton(
                                    onClick = { measurementTimestamp = System.currentTimeMillis() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text("Zmień na teraz", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Weight & Body Fat
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

                // Waist & Chest
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

                // Biceps & Hips
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

                // Thighs & Calves
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

                // Shoulders
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

                // Photos Section: Front, Side, Back
                item {
                    Text(
                        text = "Zdjęcia sylwetki (Przód, Bok, Tył):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhotoPickerSlot(
                            label = "Przód",
                            photoPath = frontPhotoPath,
                            onPick = { frontLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onClear = { frontPhotoPath = null },
                            modifier = Modifier.weight(1f)
                        )
                        PhotoPickerSlot(
                            label = "Bok",
                            photoPath = sidePhotoPath,
                            onPick = { sideLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onClear = { sidePhotoPath = null },
                            modifier = Modifier.weight(1f)
                        )
                        PhotoPickerSlot(
                            label = "Tył",
                            photoPath = backPhotoPath,
                            onPick = { backLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onClear = { backPhotoPath = null },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                    val measurement = (initialMeasurement ?: BodyMeasurement()).copy(
                        timestamp = measurementTimestamp,
                        weightKg = weightText.toFloatOrNull(),
                        bodyFatPercentage = bodyFatText.toFloatOrNull(),
                        chestCm = chestText.toFloatOrNull(),
                        waistCm = waistText.toFloatOrNull(),
                        bicepsCm = bicepsText.toFloatOrNull(),
                        hipsCm = hipsText.toFloatOrNull(),
                        thighsCm = thighsText.toFloatOrNull(),
                        calvesCm = calvesText.toFloatOrNull(),
                        shouldersCm = shouldersText.toFloatOrNull(),
                        notes = notesText,
                        frontPhotoUri = frontPhotoPath,
                        sidePhotoUri = sidePhotoPath,
                        backPhotoUri = backPhotoPath
                    )
                    onSave(measurement)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                modifier = Modifier.testTag("save_measurement_button")
            ) {
                Text(
                    text = if (isEditing) "Zapisz zmiany" else "Zapisz pomiar",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun AddMeasurementDialog(
    onDismiss: () -> Unit,
    onSave: (BodyMeasurement) -> Unit
) {
    AddOrEditMeasurementDialog(
        initialMeasurement = null,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@Composable
fun PhotoPickerSlot(
    label: String,
    photoPath: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .height(95.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, if (photoPath != null) AthleticOrange else Color(0xFF334155), RoundedCornerShape(10.dp))
            .clickable { if (photoPath == null) onPick() },
        contentAlignment = Alignment.Center
    ) {
        if (photoPath != null) {
            val model = remember(photoPath) {
                val f = File(photoPath)
                if (f.exists()) f else Uri.parse(photoPath)
            }

            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Remove button overlay
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Usuń", tint = Color.White, modifier = Modifier.size(14.dp))
            }

            Surface(
                shape = RoundedCornerShape(topEnd = 6.dp),
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
