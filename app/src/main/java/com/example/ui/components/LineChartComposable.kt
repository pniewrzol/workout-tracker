package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class ChartPoint(
    val timestamp: Long,
    val value: Float,
    val label: String,
    val detail: String = ""
)

@Composable
fun LineChartComposable(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    unit: String = "kg",
    lineColor: Color = AthleticOrange,
    accentColor: Color = ElectricCyan
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Brak danych do wykresu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Zapisz serie ćwiczenia, aby zobaczyć postęp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxVal = remember(points) { (points.maxOfOrNull { it.value } ?: 100f).coerceAtLeast(10f) }
    val minVal = remember(points) { (points.minOfOrNull { it.value } ?: 0f).coerceAtLeast(0f) }
    val range = remember(maxVal, minVal) { if (maxVal == minVal) 10f else maxVal - minVal }

    val selectedPoint = selectedIndex?.let { if (it in points.indices) points[it] else null }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        // Header with stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ostatni wynik",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${points.last().value.roundToInt()} $unit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = lineColor
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Maksimum (PR)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${maxVal.roundToInt()} $unit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }

        // Tooltip display when tapped
        if (selectedPoint != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedPoint.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${selectedPoint.value} $unit ${if (selectedPoint.detail.isNotEmpty()) "(${selectedPoint.detail})" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = lineColor
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Canvas Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { tapOffset ->
                            if (points.size <= 1) {
                                selectedIndex = 0
                                return@detectTapGestures
                            }
                            val paddingH = 30f
                            val chartWidth = size.width - 2 * paddingH
                            val stepX = chartWidth / (points.size - 1)
                            val tappedIndex = ((tapOffset.x - paddingH + stepX / 2) / stepX)
                                .toInt()
                                .coerceIn(0, points.size - 1)
                            selectedIndex = tappedIndex
                        }
                    }
            ) {
                val paddingH = 30f
                val paddingV = 20f
                val chartWidth = size.width - 2 * paddingH
                val chartHeight = size.height - 2 * paddingV

                // Draw background horizontal grid lines (3 lines)
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = paddingV + (chartHeight / gridLines) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(paddingH, y),
                        end = Offset(size.width - paddingH, y),
                        strokeWidth = 1f
                    )
                }

                if (points.size == 1) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawCircle(
                        color = lineColor,
                        radius = 8f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(cx, cy)
                    )
                    return@Canvas
                }

                val stepX = chartWidth / (points.size - 1)
                val coords = points.mapIndexed { index, point ->
                    val x = paddingH + index * stepX
                    val normY = (point.value - minVal) / range
                    val y = paddingV + chartHeight * (1f - normY)
                    Offset(x, y)
                }

                // Construct path for the line
                val strokePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        val p0 = coords[i - 1]
                        val p1 = coords[i]
                        // Bezier smoothing
                        val cX = (p0.x + p1.x) / 2f
                        cubicTo(cX, p0.y, cX, p1.y, p1.x, p1.y)
                    }
                }

                // Construct path for the gradient fill beneath the line
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(coords.last().x, size.height - paddingV)
                    lineTo(coords.first().x, size.height - paddingV)
                    close()
                }

                // Draw gradient fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.35f),
                            lineColor.copy(alpha = 0.0f)
                        ),
                        startY = paddingV,
                        endY = size.height - paddingV
                    )
                )

                // Draw stroke
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw dots
                coords.forEachIndexed { idx, offset ->
                    val isSelected = selectedIndex == idx
                    val radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
                    drawCircle(
                        color = if (isSelected) accentColor else lineColor,
                        radius = radius,
                        center = offset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = radius / 2f,
                        center = offset
                    )
                }
            }
        }

        // Date timeline labels (first and last)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.first().label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
            if (points.size > 1) {
                Text(
                    text = points.last().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
