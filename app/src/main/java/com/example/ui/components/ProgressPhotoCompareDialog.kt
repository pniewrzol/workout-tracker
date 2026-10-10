package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.BodyMeasurement
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressPhotoCompareDialog(
    measurementsWithPhotos: List<BodyMeasurement>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // 0: Przód, 1: Bok, 2: Tył
    var selectedAngle by remember { mutableIntStateOf(0) }
    // 0: Obok siebie, 1: Suwak Przed/Po
    var viewMode by remember { mutableIntStateOf(0) }
    var sliderFraction by remember { mutableFloatStateOf(0.5f) }

    val oldest = measurementsWithPhotos.lastOrNull()
    val newest = measurementsWithPhotos.firstOrNull()

    var beforeMeasurement by remember { mutableStateOf<BodyMeasurement?>(oldest) }
    var afterMeasurement by remember { mutableStateOf<BodyMeasurement?>(newest) }

    val beforePhotoUri = when (selectedAngle) {
        0 -> beforeMeasurement?.frontPhotoUri
        1 -> beforeMeasurement?.sidePhotoUri
        else -> beforeMeasurement?.backPhotoUri
    }

    val afterPhotoUri = when (selectedAngle) {
        0 -> afterMeasurement?.frontPhotoUri
        1 -> afterMeasurement?.sidePhotoUri
        else -> afterMeasurement?.backPhotoUri
    }

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
                    .testTag("dialog_photo_compare"),
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
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = GoldPr.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Compare,
                                        contentDescription = null,
                                        tint = GoldPr,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Porównanie Sylwetki",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Zestawienie zmian sylwetkowych",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Zamknij")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Angle Selector (Front / Side / Back) and View Mode Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Przód", "Bok", "Tył").forEachIndexed { index, label ->
                                FilterChip(
                                    selected = selectedAngle == index,
                                    onClick = { selectedAngle = index },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedAngle == index) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPr,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        // Tryb: Obok siebie vs Suwak
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { viewMode = 0 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewColumn,
                                    contentDescription = "Obok siebie",
                                    tint = if (viewMode == 0) GoldPr else MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(
                                onClick = { viewMode = 1 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewStream,
                                    contentDescription = "Suwak Przed/Po",
                                    tint = if (viewMode == 1) GoldPr else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (viewMode == 0) {
                        // Side by Side Comparison Display
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Before Panel
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = "PRZED (${beforeMeasurement?.timestamp?.let { dateFormat.format(Date(it)) } ?: "-"})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                PhotoCompareCard(
                                    photoUri = beforePhotoUri,
                                    label = "Przed",
                                    weight = beforeMeasurement?.weightKg
                                )
                            }

                            // After Panel
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AthleticOrange,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = "PO (${afterMeasurement?.timestamp?.let { dateFormat.format(Date(it)) } ?: "-"})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                PhotoCompareCard(
                                    photoUri = afterPhotoUri,
                                    label = "Po",
                                    weight = afterMeasurement?.weightKg
                                )
                            }
                        }
                    } else {
                        // Interactive Split-Slider View
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "← PRZED: ${beforeMeasurement?.timestamp?.let { dateFormat.format(Date(it)) } ?: "-"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "PO: ${afterMeasurement?.timestamp?.let { dateFormat.format(Date(it)) } ?: "-"} →",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AthleticOrange
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                            ) {
                                // Zdjęcie "PO" (warstwa spodnia)
                                if (!afterPhotoUri.isNullOrBlank()) {
                                    val fAfter = File(afterPhotoUri)
                                    val mAfter = if (fAfter.exists()) fAfter else Uri.parse(afterPhotoUri)
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(context).data(mAfter).crossfade(true).build(),
                                        contentDescription = "Po",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Zdjęcie "PRZED" (warstwa wierzchnia ucięta wg suwaka)
                                if (!beforePhotoUri.isNullOrBlank()) {
                                    val fBefore = File(beforePhotoUri)
                                    val mBefore = if (fBefore.exists()) fBefore else Uri.parse(beforePhotoUri)
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(context).data(mBefore).crossfade(true).build(),
                                        contentDescription = "Przed",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(SplitClipShape(sliderFraction))
                                    )
                                }

                                // Linia podziału suwaka
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(3.dp)
                                        .align(Alignment.CenterStart)
                                        .padding(start = (300.dp * sliderFraction).coerceIn(0.dp, 300.dp))
                                        .background(Color.White)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Slider(
                                value = sliderFraction,
                                onValueChange = { sliderFraction = it },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldPr,
                                    activeTrackColor = ElectricCyan,
                                    inactiveTrackColor = AthleticOrange
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom Shape to clip the "Before" photo to the slider position fraction
class SplitClipShape(private val fraction: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val width = size.width * fraction.coerceIn(0f, 1f)
        return Outline.Rectangle(Rect(0f, 0f, width, size.height))
    }
}

@Composable
fun PhotoCompareCard(photoUri: String?, label: String, weight: Float?) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (photoUri.isNullOrBlank()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Brak zdjęcia $label",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            } else {
                val model = remember(photoUri) {
                    val file = File(photoUri)
                    if (file.exists()) file else Uri.parse(photoUri)
                }

                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(model)
                        .crossfade(true)
                        .build(),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GoldPr, modifier = Modifier.size(24.dp))
                        }
                    },
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            weight?.let {
                Surface(
                    shape = RoundedCornerShape(topStart = 8.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = "$it kg",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
