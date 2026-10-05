package com.example.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Exercise
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import com.example.ui.theme.SuccessGreen

@Composable
fun ExerciseListScreen(
    exercises: List<Exercise>,
    onExerciseClick: (Long) -> Unit,
    onCreateExercise: ((Exercise) -> Unit)? = null,
    onDeleteExercise: ((Exercise) -> Unit)? = null,
    onDeleteCategory: ((String) -> Unit)? = null,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    lastViewedExerciseId: Long? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSection by rememberSaveable { mutableStateOf("Wszystkie") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    // Distinct sections dynamically plus standard ones
    val sections = remember(exercises) {
        val list = mutableListOf("Wszystkie")
        val exerciseSections = exercises.map { it.section }.distinct().filter { it.isNotBlank() }
        list.addAll(exerciseSections)
        list.distinct()
    }

    val filteredExercises = remember(exercises, searchQuery, selectedSection) {
        exercises.filter { ex ->
            val matchesSection = when (selectedSection) {
                "Wszystkie" -> true
                else -> ex.section.equals(selectedSection, ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                ex.name.contains(searchQuery, ignoreCase = true) ||
                        ex.code.contains(searchQuery, ignoreCase = true) ||
                        ex.primaryMuscles.contains(searchQuery, ignoreCase = true) ||
                        ex.equipment.contains(searchQuery, ignoreCase = true) ||
                        ex.bodyPart.contains(searchQuery, ignoreCase = true)
            }
            matchesSection && matchesSearch
        }
    }

    LaunchedEffect(lastViewedExerciseId, filteredExercises) {
        if (lastViewedExerciseId != null && filteredExercises.isNotEmpty()) {
            val index = filteredExercises.indexOfFirst { it.id == lastViewedExerciseId }
            if (index >= 0) {
                listState.scrollToItem(index)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("exercise_search_input"),
                placeholder = { Text("Szukaj ćwiczenia, mięśni, sprzętu...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Wyczyść"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Section filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sections) { sec ->
                    val isSelected = selectedSection == sec
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSection = sec },
                        label = { Text(sec) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AthleticOrange,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Counter & info & category actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ćwiczenia: ${filteredExercises.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedSection != "Wszystkie" && onDeleteCategory != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            modifier = Modifier.clickable { categoryToDelete = selectedSection }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Usuń kategorię",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Usuń kategorię",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                if (onCreateExercise != null) {
                    Text(
                        text = "+ Dodaj ćwiczenie",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AthleticOrange,
                        modifier = Modifier.clickable { showCreateDialog = true }
                    )
                }
            }

            // Exercise List with persistent scroll state
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredExercises, key = { it.id }) { exercise ->
                    ExerciseListItemCard(
                        exercise = exercise,
                        onClick = { onExerciseClick(exercise.id) },
                        onDeleteClick = if (onDeleteExercise != null) {
                            { exerciseToDelete = exercise }
                        } else null
                    )
                }
            }
        }

        // Floating Action Button to create exercise directly
        if (onCreateExercise != null) {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nowe ćwiczenie", fontWeight = FontWeight.Bold) },
                containerColor = AthleticOrange,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
                    .testTag("fab_add_exercise")
            )
        }
    }

    // Dialog: Create Exercise Directly
    if (showCreateDialog && onCreateExercise != null) {
        var exName by remember { mutableStateOf("") }
        var exCode by remember { mutableStateOf("") }
        var exSection by remember { mutableStateOf(if (selectedSection != "Wszystkie") selectedSection else "Trening A") }
        var isCustomSection by remember { mutableStateOf(false) }
        var customSectionName by remember { mutableStateOf("") }
        var exBodyPart by remember { mutableStateOf("Klatka piersiowa") }
        var exEquipment by remember { mutableStateOf("Hantle") }
        var exType by remember { mutableStateOf("WEIGHT_AND_REPS") }
        var exSets by remember { mutableStateOf("4") }
        var exReps by remember { mutableStateOf("10,10,8,8") }
        var exRest by remember { mutableStateOf("90") }
        var exCues by remember { mutableStateOf("") }

        val activeSection = if (isCustomSection && customSectionName.isNotBlank()) customSectionName.trim() else exSection

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null, tint = AthleticOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nowe Ćwiczenie", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = exName,
                            onValueChange = { exName = it },
                            label = { Text("Nazwa ćwiczenia *") },
                            placeholder = { Text("np. Wyciskanie hantli na skosie") },
                            modifier = Modifier.fillMaxWidth().testTag("input_ex_name"),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = exCode,
                            onValueChange = { exCode = it },
                            label = { Text("Własna numeracja / Kod (opcjonalnie)") },
                            placeholder = { Text("np. A.5, B.1, W1, 1.") },
                            modifier = Modifier.fillMaxWidth().testTag("input_ex_code"),
                            singleLine = true
                        )
                    }

                    item {
                        Text("Kategoria / Sekcja treningowa:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val availableSections = listOf("Trening A", "Trening B", "Trening C", "Rozgrzewka", "Mobilizacja", "+ Własna sekcja")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableSections) { sec ->
                                val isSelected = if (sec == "+ Własna sekcja") isCustomSection else (!isCustomSection && exSection == sec)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (sec == "+ Własna sekcja") {
                                            isCustomSection = true
                                        } else {
                                            isCustomSection = false
                                            exSection = sec
                                        }
                                    },
                                    label = { Text(sec, fontSize = 11.sp) }
                                )
                            }
                        }
                        if (isCustomSection) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customSectionName,
                                onValueChange = { customSectionName = it },
                                label = { Text("Nazwa własnej sekcji") },
                                placeholder = { Text("np. Trening D, Ramiona") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Text("Główna partia mięśniowa:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val bodyParts = listOf("Klatka piersiowa", "Plecy", "Barki", "Biceps", "Triceps", "Uda / Czworogłowe", "Dwugłowe (Tył ud)", "Pośladki", "Łydki", "Brzuch / Core", "Cardio")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(bodyParts) { part ->
                                FilterChip(
                                    selected = exBodyPart == part,
                                    onClick = { exBodyPart = part },
                                    label = { Text(part, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Sprzęt:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val equipments = listOf("Hantle", "Sztanga", "Maszyna", "Wyciąg", "Masa ciała", "Gumy / Taśmy", "Inny")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(equipments) { eq ->
                                FilterChip(
                                    selected = exEquipment == eq,
                                    onClick = { exEquipment = eq },
                                    label = { Text(eq, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Typ rejestracji wyniku:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf(
                                "WEIGHT_AND_REPS" to "Ciężar + Powt.",
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
                                label = { Text("Powtórzenia / Czas") },
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
                            value = exCues,
                            onValueChange = { exCues = it },
                            label = { Text("Wskazówki techniczne / uwagi") },
                            placeholder = { Text("np. Kontrola fazy ekscentrycznej, pauza 1s") },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                val prefix = activeSection.take(1).uppercase()
                                "$prefix.${exercises.count { it.section.equals(activeSection, ignoreCase = true) } + 1}"
                            }

                            val newExercise = Exercise(
                                name = exName.trim(),
                                section = activeSection,
                                code = finalCode,
                                targetSets = setsInt,
                                targetReps = exReps.trim(),
                                restSeconds = restInt,
                                restDisplay = "${restInt}s",
                                bodyPart = exBodyPart,
                                equipment = exEquipment,
                                primaryMuscles = exBodyPart,
                                secondaryMuscles = "",
                                cues = exCues.trim().ifEmpty { "Prawidłowa technika i kontrola powtórzenia" },
                                instructions = "Wykonaj $setsInt serii roboczych z zachowaniem optymalnego tempa.",
                                isCustom = true,
                                measurementType = exType
                            )
                            onCreateExercise(newExercise)
                            showCreateDialog = false
                        }
                    },
                    enabled = exName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                    modifier = Modifier.testTag("btn_save_exercise")
                ) {
                    Text("Zapisz ćwiczenie")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Confirmation dialog: Delete Exercise
    exerciseToDelete?.let { ex ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Usuń ćwiczenie", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Czy na pewno chcesz usunąć ćwiczenie \"${ex.name}\"? Spowoduje to usunięcie go z aplikacji.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExercise?.invoke(ex)
                        exerciseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Confirmation dialog: Delete Category
    categoryToDelete?.let { catName ->
        val count = exercises.count { it.section.equals(catName, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Usuń kategorię", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Czy na pewno chcesz usunąć kategorię \"$catName\"? Wszystkie ćwiczenia ($count) należące do tej kategorii zostaną usunięte z aplikacji.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory?.invoke(catName)
                        if (selectedSection == catName) selectedSection = "Wszystkie"
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń kategorię", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun ExerciseListItemCard(
    exercise: Exercise,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val sectionColor = when (exercise.section) {
        "Trening A" -> AthleticOrange
        "Trening B" -> ElectricCyan
        "Trening C" -> GoldPr
        "Rozgrzewka" -> SuccessGreen
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("exercise_item_${exercise.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = sectionColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(2.dp)) {
                        Text(
                            text = exercise.code,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = if (exercise.code.length > 4) 10.sp else if (exercise.code.length > 2) 12.sp else 14.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = sectionColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${exercise.section} • ${exercise.equipment}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exercise.primaryMuscles,
                            style = MaterialTheme.typography.labelSmall,
                            color = sectionColor,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (exercise.isDistanceBased() || exercise.isTimeBased()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (exercise.isDistanceBased()) ElectricCyan.copy(alpha = 0.2f) else AthleticOrange.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (exercise.isDistanceBased()) "📏 ${exercise.targetReps}" else "⏱ ${exercise.targetReps}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (exercise.isDistanceBased()) ElectricCyan else AthleticOrange,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Usuń ćwiczenie",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
