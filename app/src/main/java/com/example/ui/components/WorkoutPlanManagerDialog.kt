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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatListBulleted
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
    onCreatePlan: (name: String, description: String) -> Unit,
    onDeletePlan: (WorkoutPlan) -> Unit,
    onCreateExercise: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showAddWorkoutDialog by remember { mutableStateOf(false) }

    var planToDelete by remember { mutableStateOf<WorkoutPlan?>(null) }
    var selectedPlanForDetails by remember { mutableStateOf(activePlan ?: plans.firstOrNull()) }

    // Update selectedPlanForDetails when plans change
    val currentSelectedPlan = plans.find { it.id == selectedPlanForDetails?.id } ?: activePlan ?: plans.firstOrNull()

    // Exercises for the inspected plan
    val planExercises = remember(allExercises, currentSelectedPlan) {
        val pId = currentSelectedPlan?.id ?: 1L
        allExercises.filter { it.planId == pId || (pId == 1L && it.planId == 0L) }
    }

    // Workouts (sections) in this plan
    val planWorkouts = remember(planExercises) {
        val distinct = planExercises.map { it.section }.distinct().filter { it != "Rozgrzewka" && it != "Mobilizacja" }
        if (distinct.isEmpty()) listOf("Trening 1") else distinct
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
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
                                            maxLines = 1
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

                                    if (plan.id != 1L) {
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
                                Column {
                                    Text(
                                        text = "Treningi w: ${selPlan.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${planWorkouts.size} treningów • ${planExercises.size} ćwiczeń",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

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
                                            color = AthleticOrange
                                        )
                                        Text(
                                            text = "${exercisesInWorkout.size} ćwiczeń",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                                                    Text(
                                                        text = "${idx + 1}. ",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = ex.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                }
                                                Text(
                                                    text = "${ex.targetSets}x (${ex.targetReps})",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

    // Dialog: Create Plan
    if (showCreatePlanDialog) {
        var planName by remember { mutableStateOf("") }
        var planDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreatePlanDialog = false },
            title = { Text("Nowy Plan Treningowy", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = planName,
                        onValueChange = { planName = it },
                        label = { Text("Nazwa planu (np. Push Pull Legs)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = planDesc,
                        onValueChange = { planDesc = it },
                        label = { Text("Opis / Założenia (opcjonalnie)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (planName.isNotBlank()) {
                            onCreatePlan(planName.trim(), planDesc.trim())
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
        var workoutName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddWorkoutDialog = false },
            title = { Text("Dodaj Trening do Planu", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Wpisz nazwę treningu (np. 'Trening D - Ramiona', 'Push', 'Pull'):", fontSize = 13.sp)
                    OutlinedTextField(
                        value = workoutName,
                        onValueChange = { workoutName = it },
                        label = { Text("Nazwa treningu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (workoutName.isNotBlank()) {
                            // Seed an initial exercise for this workout
                            val defaultEx = Exercise(
                                name = "Pierwsze ćwiczenie ($workoutName)",
                                section = workoutName.trim(),
                                code = "${workoutName.take(1).uppercase()}.1",
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
        var exSection by remember { mutableStateOf(planWorkouts.firstOrNull() ?: "Trening 1") }
        var exSets by remember { mutableStateOf("4") }
        var exReps by remember { mutableStateOf("10,10,8,8") }
        var exRest by remember { mutableStateOf("90") }
        var exBodyPart by remember { mutableStateOf("Klatka piersiowa") }
        var exType by remember { mutableStateOf("WEIGHT_AND_REPS") }

        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Nowe Ćwiczenie", fontWeight = FontWeight.Bold) },
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
                        Text("Przypisz do treningu:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            planWorkouts.forEach { wName ->
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "WEIGHT_AND_REPS" to "Ciężar + Reps",
                                "BODYWEIGHT_REPS" to "Masa ciała",
                                "TIME" to "Czas (sek)",
                                "DISTANCE" to "Dystans (m)"
                            ).forEach { (typeVal, label) ->
                                FilterChip(
                                    selected = exType == typeVal,
                                    onClick = { exType = typeVal },
                                    label = { Text(label, fontSize = 10.sp) }
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
                            val newEx = Exercise(
                                name = exName.trim(),
                                section = exSection,
                                code = "C.${System.currentTimeMillis() % 1000}",
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
            text = { Text("Czy na pewno chcesz usunąć plan '${plan.name}'? Ta operacja jest nieodwracalna.") },
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
}
