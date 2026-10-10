package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Exercise
import com.example.data.model.WorkoutPlan
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import com.example.ui.theme.SuccessGreen

@Composable
fun WorkoutPlanManagerDialog(
    plans: List<WorkoutPlan>,
    activePlan: WorkoutPlan?,
    allExercises: List<Exercise>,
    onSelectActivePlan: (Long) -> Unit,
    onCreatePlan: (name: String, description: String, includeWarmup: Boolean, workouts: List<Pair<String, String>>) -> Unit,
    onDeletePlan: (WorkoutPlan) -> Unit,
    onCreateExercise: (Exercise) -> Unit,
    onDeleteExercise: ((Exercise) -> Unit)? = null,
    onDeleteWorkoutCategory: ((WorkoutPlan, String) -> Unit)? = null,
    onDuplicatePlan: ((WorkoutPlan) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showAddWorkoutDialog by remember { mutableStateOf(false) }

    var planToDelete by remember { mutableStateOf<WorkoutPlan?>(null) }
    var exerciseToDeleteInPlan by remember { mutableStateOf<Exercise?>(null) }
    var workoutCategoryToDelete by remember { mutableStateOf<Pair<WorkoutPlan, String>?>(null) }
    var selectedPlanForDetails by remember { mutableStateOf(activePlan ?: plans.firstOrNull()) }

    // Update selectedPlanForDetails when plans change
    val currentSelectedPlan = plans.find { it.id == selectedPlanForDetails?.id } ?: activePlan ?: plans.firstOrNull()

    // Exercises for the inspected plan
    val planExercises = remember(allExercises, currentSelectedPlan) {
        val pId = currentSelectedPlan?.id ?: 1L
        allExercises.filter { it.planId == pId || (pId == 1L && it.planId == 0L) }
    }

    // Warmup & Mobility in this plan
    val planWarmupExercises = remember(planExercises) {
        planExercises.filter { it.section == "Rozgrzewka" || it.section == "Mobilizacja" }
    }

    // Workouts (sections) in this plan - combines workouts defined in plan and those with exercises
    val planWorkouts = remember(planExercises, currentSelectedPlan) {
        val fromRaw = currentSelectedPlan?.workoutsRaw?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val fromEx = planExercises.map { it.section }.distinct().filter { it != "Rozgrzewka" && it != "Mobilizacja" }
        (fromRaw + fromEx).distinct().ifEmpty { listOf("Trening A") }
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
                    .widthIn(max = 560.dp),
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
                            color = ElectricCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Plany Treningowe",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Twórz i przełączaj plany treningowe",
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

                // Action Bar: Create New Plan Button
                Button(
                    onClick = { showCreatePlanDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Utwórz nowy plan treningowy", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable List of Plans & Details
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Twoje plany (${plans.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(plans) { plan ->
                        val isActive = plan.id == activePlan?.id
                        val isSelected = plan.id == currentSelectedPlan?.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlanForDetails = plan },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isActive) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                                color = if (isActive) SuccessGreen else if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        if (isActive) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SuccessGreen,
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                Text(
                                                    text = "AKTYWNY",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = plan.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (!isActive) {
                                        Button(
                                            onClick = { onSelectActivePlan(plan.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Wybierz", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }

                                    if (onDuplicatePlan != null) {
                                        IconButton(
                                            onClick = { onDuplicatePlan(plan) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Duplikuj plan",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { planToDelete = plan },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Usuń plan",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (plan.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = plan.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Inspected Plan Details: Workouts & Exercises
                    currentSelectedPlan?.let { selPlan ->
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Treningi w: ${selPlan.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${planWorkouts.size} treningów • ${planExercises.size} ćwiczeń",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Row {
                                    OutlinedButton(
                                        onClick = { showAddWorkoutDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Trening", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = { showAddExerciseDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Ćwiczenie", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        // 1. Warmup & Mobility in this plan
                        if (planWarmupExercises.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = SuccessGreen) {
                                                    Text(
                                                        text = "ROZGRZEWKA & MOBILIZACJA",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.Black,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${planWarmupExercises.size} ćwiczeń w planie",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        planWarmupExercises.forEach { ex ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (ex.code.isNotBlank()) "${ex.code} " else "• ",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SuccessGreen
                                                    )
                                                    Text(
                                                        text = ex.name,
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = ex.targetReps,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (onDeleteExercise != null) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        IconButton(
                                                            onClick = { exerciseToDeleteInPlan = ex },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Usuń ćwiczenie",
                                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                                modifier = Modifier.size(16.dp)
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

                        // Display workouts in this plan
                        items(planWorkouts) { workoutSection ->
                            val exercisesInWorkout = planExercises.filter { it.section == workoutSection }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = workoutSection,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = AthleticOrange,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${exercisesInWorkout.size} ćwiczeń",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (onDeleteWorkoutCategory != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                IconButton(
                                                    onClick = { workoutCategoryToDelete = Pair(selPlan, workoutSection) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Usuń trening / kategorię",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (exercisesInWorkout.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        exercisesInWorkout.forEachIndexed { idx, ex ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    val displayCode = if (ex.code.isNotBlank()) "${ex.code}. " else "${idx + 1}. "
                                                    Text(
                                                        text = displayCode,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = ex.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${ex.targetSets}x (${ex.targetReps})",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (onDeleteExercise != null) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        IconButton(
                                                            onClick = { exerciseToDeleteInPlan = ex },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Usuń ćwiczenie",
                                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                                modifier = Modifier.size(16.dp)
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
                    }
                }
            }
        }
    }
    }

    // Dialog: Create Plan (Kompleksowe tworzenie planu z własną numeracją i rozgrzewką)
    if (showCreatePlanDialog) {
        var planName by remember { mutableStateOf("") }
        var planDesc by remember { mutableStateOf("") }
        var includeWarmup by remember { mutableStateOf(false) }

        // Customizable list of workouts for the new plan
        var workoutEntries by remember {
            mutableStateOf(
                listOf(
                    Pair("A", "Trening A"),
                    Pair("B", "Trening B"),
                    Pair("C", "Trening C")
                )
            )
        }

        AlertDialog(
            onDismissRequest = { showCreatePlanDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nowy Plan Treningowy", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = planName,
                            onValueChange = { planName = it },
                            label = { Text("Nazwa planu *") },
                            placeholder = { Text("np. Push Pull Legs, Góra/Dół, FBW") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = planDesc,
                            onValueChange = { planDesc = it },
                            label = { Text("Opis / Założenia / Dni treningowe") },
                            placeholder = { Text("np. 3-4 dni w tygodniu, budowa siły i masy") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AthleticOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Plan zostanie utworzony jako czysty szablon bez automatycznych ćwiczeń. Wszystkie ćwiczenia dodasz według własnych założeń.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Struktura treningów w planie:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Wybierz szablon lub nadaj własne oznaczenia i nazwy:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    workoutEntries = listOf(
                                        Pair("A", "Trening A"),
                                        Pair("B", "Trening B"),
                                        Pair("C", "Trening C")
                                    )
                                },
                                label = { Text("A / B / C", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    workoutEntries = listOf(
                                        Pair("1", "Trening 1"),
                                        Pair("2", "Trening 2"),
                                        Pair("3", "Trening 3"),
                                        Pair("4", "Trening 4")
                                    )
                                },
                                label = { Text("1 / 2 / 3 / 4", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    workoutEntries = listOf(
                                        Pair("Push", "Trening Push"),
                                        Pair("Pull", "Trening Pull"),
                                        Pair("Legs", "Trening Legs")
                                    )
                                },
                                label = { Text("P / P / L", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    workoutEntries = listOf(
                                        Pair("Góra", "Trening Góra"),
                                        Pair("Dół", "Trening Dół")
                                    )
                                },
                                label = { Text("Góra/Dół", fontSize = 10.sp) }
                            )
                        }
                    }

                    // Editable list of workouts
                    items(workoutEntries.size) { index ->
                        val entry = workoutEntries[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = entry.first,
                                onValueChange = { newCode ->
                                    val updated = workoutEntries.toMutableList()
                                    updated[index] = Pair(newCode, updated[index].second)
                                    workoutEntries = updated
                                },
                                label = { Text("Kod") },
                                modifier = Modifier.width(80.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = entry.second,
                                onValueChange = { newName ->
                                    val updated = workoutEntries.toMutableList()
                                    updated[index] = Pair(updated[index].first, newName)
                                    workoutEntries = updated
                                },
                                label = { Text("Nazwa treningu") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            if (workoutEntries.size > 1) {
                                IconButton(
                                    onClick = {
                                        val updated = workoutEntries.toMutableList()
                                        updated.removeAt(index)
                                        workoutEntries = updated
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Usuń trening",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                val nextNum = workoutEntries.size + 1
                                workoutEntries = workoutEntries + Pair("$nextNum", "Trening $nextNum")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dodaj kolejny trening do listy", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (planName.isNotBlank()) {
                            onCreatePlan(
                                planName.trim(),
                                planDesc.trim(),
                                includeWarmup,
                                workoutEntries.filter { it.second.isNotBlank() }
                            )
                            showCreatePlanDialog = false
                        }
                    },
                    enabled = planName.isNotBlank()
                ) {
                    Text("Utwórz plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlanDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Dialog: Add Workout to Current Plan
    if (showAddWorkoutDialog && currentSelectedPlan != null) {
        var workoutCode by remember { mutableStateOf("D") }
        var workoutName by remember { mutableStateOf("Trening D") }

        AlertDialog(
            onDismissRequest = { showAddWorkoutDialog = false },
            title = { Text("Dodaj Trening do Planu", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nadaj własne oznaczenie i nazwę nowemu treningowi:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = workoutCode,
                        onValueChange = { workoutCode = it },
                        label = { Text("Kod / Numeracja (np. D, 4, Push, Góra)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = workoutName,
                        onValueChange = { workoutName = it },
                        label = { Text("Pełna nazwa treningu (np. Trening D - Ramiona)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (workoutName.isNotBlank()) {
                            val cleanCode = workoutCode.trim().ifEmpty { workoutName.take(1).uppercase() }
                            // Seed an initial exercise for this workout
                            val defaultEx = Exercise(
                                name = "Pierwsze ćwiczenie ($workoutName)",
                                section = workoutName.trim(),
                                code = "$cleanCode.1",
                                targetSets = 4,
                                targetReps = "10,10,8,8",
                                restSeconds = 90,
                                restDisplay = "90s",
                                bodyPart = "Różne",
                                equipment = "Sztanga / Hantle",
                                primaryMuscles = "Całe ciało",
                                secondaryMuscles = "",
                                cues = "Prawidłowa postawa i kontrola fazy ekscentrycznej",
                                instructions = "Wykonaj serie zgodnie z zaplanowanym zakresem powtórzeń",
                                isCustom = true,
                                planId = currentSelectedPlan.id
                            )
                            onCreateExercise(defaultEx)
                            showAddWorkoutDialog = false
                        }
                    },
                    enabled = workoutName.isNotBlank()
                ) {
                    Text("Dodaj trening")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWorkoutDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Dialog: Add Custom Exercise
    if (showAddExerciseDialog && currentSelectedPlan != null) {
        var exName by remember { mutableStateOf("") }
        var exCode by remember { mutableStateOf("") }
        var exSection by remember { mutableStateOf(planWorkouts.firstOrNull() ?: "Trening 1") }
        var exSets by remember { mutableStateOf("4") }
        var exReps by remember { mutableStateOf("10,10,8,8") }
        var exRest by remember { mutableStateOf("90") }
        var exBodyPart by remember { mutableStateOf("Klatka piersiowa") }
        var exType by remember { mutableStateOf("WEIGHT_AND_REPS") }

        val allAvailableSections = remember(planWorkouts) {
            val list = mutableListOf<String>()
            list.addAll(planWorkouts)
            if (!list.contains("Rozgrzewka")) list.add("Rozgrzewka")
            if (!list.contains("Mobilizacja")) list.add("Mobilizacja")
            list
        }

        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Nowe Ćwiczenie w Planie", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = exName,
                            onValueChange = { exName = it },
                            label = { Text("Nazwa ćwiczenia *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = exCode,
                            onValueChange = { exCode = it },
                            label = { Text("Własna numeracja / Kod (np. A1, 1., B2, R1)") },
                            placeholder = { Text("np. A1, 1., B2") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("Przypisz do sekcji / treningu:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            allAvailableSections.forEach { wName ->
                                FilterChip(
                                    selected = exSection == wName,
                                    onClick = { exSection = wName },
                                    label = { Text(wName, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = exSets,
                                onValueChange = { exSets = it },
                                label = { Text("Serie") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = exReps,
                                onValueChange = { exReps = it },
                                label = { Text("Powtórzenia") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = exRest,
                            onValueChange = { exRest = it },
                            label = { Text("Przerwa (sekundy)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = exBodyPart,
                            onValueChange = { exBodyPart = it },
                            label = { Text("Partia mięśniowa (np. Plecy, Klatka, Nogi)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("Typ rejestracji wyniku:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf(
                                "WEIGHT_AND_REPS" to "Ciężar + Reps",
                                "WEIGHT_AND_DISTANCE" to "Ciężar + Dystans (Spacer farmera)",
                                "BODYWEIGHT_REPS" to "Masa ciała",
                                "TIME" to "Czas (sek)",
                                "DISTANCE" to "Dystans (m)"
                            )) { (typeVal, label) ->
                                FilterChip(
                                    selected = exType == typeVal,
                                    onClick = { exType = typeVal },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exName.isNotBlank()) {
                            val setsInt = exSets.toIntOrNull() ?: 4
                            val restInt = exRest.toIntOrNull() ?: 90
                            val finalCode = if (exCode.isNotBlank()) exCode.trim() else {
                                val prefix = exSection.take(1).uppercase()
                                "$prefix.${planExercises.count { it.section == exSection } + 1}"
                            }
                            val newEx = Exercise(
                                name = exName.trim(),
                                section = exSection,
                                code = finalCode,
                                targetSets = setsInt,
                                targetReps = exReps.trim(),
                                restSeconds = restInt,
                                restDisplay = "${restInt}s",
                                bodyPart = exBodyPart.trim(),
                                equipment = "Dowolny",
                                primaryMuscles = exBodyPart.trim(),
                                secondaryMuscles = "",
                                cues = "Skup się na technice i prawidłowym tempie",
                                instructions = "Wykonaj $setsInt serii w zadanym zakresie",
                                isCustom = true,
                                measurementType = exType,
                                planId = currentSelectedPlan.id
                            )
                            onCreateExercise(newEx)
                            showAddExerciseDialog = false
                        }
                    },
                    enabled = exName.isNotBlank()
                ) {
                    Text("Zapisz ćwiczenie")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Delete Plan Confirmation
    planToDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text("Usunąć plan?", fontWeight = FontWeight.Bold) },
            text = { Text("Czy na pewno chcesz usunąć plan '${plan.name}'? Ta operacja jest nieodwracalna. Jeśli to jedyny plan, zostanie utworzony nowy czysty szablon.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlan(plan)
                        planToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Delete Workout Category from Plan Confirmation
    workoutCategoryToDelete?.let { (plan, catName) ->
        AlertDialog(
            onDismissRequest = { workoutCategoryToDelete = null },
            title = { Text("Usunąć trening / kategorię?", fontWeight = FontWeight.Bold) },
            text = { Text("Czy na pewno chcesz usunąć '$catName' oraz powiązane z nim ćwiczenia z planu '${plan.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWorkoutCategory?.invoke(plan, catName)
                        workoutCategoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutCategoryToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Delete Exercise from Plan Confirmation
    exerciseToDeleteInPlan?.let { ex ->
        AlertDialog(
            onDismissRequest = { exerciseToDeleteInPlan = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Usuń ćwiczenie", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("Czy na pewno chcesz usunąć ćwiczenie '${ex.name}' z aplikacji?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExercise?.invoke(ex)
                        exerciseToDeleteInPlan = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDeleteInPlan = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}
