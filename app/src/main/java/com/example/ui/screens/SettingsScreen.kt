package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.RestoreResult
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onExportBackup: (Uri, String?, (Boolean) -> Unit) -> Unit,
    onRestoreBackup: (Uri, String?, (RestoreResult) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    var isProcessing by remember { mutableStateOf(false) }
    var resultDialogMessage by remember { mutableStateOf<String?>(null) }
    var resultDialogTitle by remember { mutableStateOf("") }

    // Export Password state
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var exportPasswordInput by remember { mutableStateOf("") }
    var isExportPasswordVisible by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf<String?>(null) }

    // Restore Password state
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var restorePasswordInput by remember { mutableStateOf("") }
    var isRestorePasswordVisible by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    // Backup Create Document Launcher
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            onExportBackup(uri, pendingExportPassword) { success ->
                isProcessing = false
                if (success) {
                    resultDialogTitle = "Zaszyfrowana kopia utworzona pomyślnie"
                    resultDialogMessage = if (pendingExportPassword != null) {
                        "Twoja kopia została zaszyfrowana algorytmem AES-256 z podanym hasłem ochronnym. Zapamiętaj to hasło, aby przywrócić dane w przyszłości."
                    } else {
                        "Wszystkie Twoje treningi, serie, pomiary ciała i konfiguracja zostały bezpiecznie zaszyfrowane algorytmem AES-256-GCM i zapisane do pliku kopii."
                    }
                } else {
                    resultDialogTitle = "Błąd eksportu"
                    resultDialogMessage = "Nie udało się zapisać pliku kopii zapasowej. Spróbuj wybrać inne miejsce w pamięci urządzenia."
                }
            }
        }
    }

    // Backup Restore Open Document Launcher
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
            isProcessing = true
            onRestoreBackup(uri, null) { result ->
                isProcessing = false
                if (result.isPasswordRequired) {
                    showRestorePasswordDialog = true
                    restorePasswordInput = ""
                } else if (result.isSuccess) {
                    resultDialogTitle = "Przywracanie zakończone"
                    resultDialogMessage = result.message
                } else {
                    resultDialogTitle = "Błąd przywracania"
                    resultDialogMessage = result.message
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ustawienia",
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme selection card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = AthleticOrange
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Motyw Aplikacji (Dark / Light)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Wybierz wygląd pasujący do Twoich preferencji",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        ThemeOptionRow(
                            title = "Systemowy (automatyczny)",
                            subtitle = "Dopasuj do ustawień urządzenia",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = currentThemeMode == AppThemeMode.SYSTEM,
                            onClick = { onThemeModeChange(AppThemeMode.SYSTEM) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        ThemeOptionRow(
                            title = "Tryb Ciemny (Dark Mode)",
                            subtitle = "Mniejsze zmęczenie oczu i oszczędność baterii",
                            icon = Icons.Default.Brightness4,
                            isSelected = currentThemeMode == AppThemeMode.DARK,
                            onClick = { onThemeModeChange(AppThemeMode.DARK) },
                            testTag = "theme_dark_option"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        ThemeOptionRow(
                            title = "Tryb Jasny (Light Mode)",
                            subtitle = "Klasyczny jasny interfejs o wysokim kontraście",
                            icon = Icons.Default.Brightness7,
                            isSelected = currentThemeMode == AppThemeMode.LIGHT,
                            onClick = { onThemeModeChange(AppThemeMode.LIGHT) },
                            testTag = "theme_light_option"
                        )
                    }
                }
            }

            // Backup & Restore Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Kopia Zapasowa & Przywracanie",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Zabezpiecz historię treningów, serie i pomiary",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Encryption Badge
                        Row(
                            modifier = Modifier
                                .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Szyfrowanie AES-256-GCM (brak otwartego tekstu)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Kopia zapasowa obejmuje historię treningów, serie i pomiary ciała. Wszystkie pliki kopii są w pełni szyfrowane kryptograficznie (AES-256-GCM z PBKDF2). Pliki wideo i zdjęcia są wyłączone z kopii, dzięki czemu proces jest błyskawiczny.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    showExportPasswordDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Utwórz kopię", fontSize = 13.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_backup_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Przywróć dane", fontSize = 13.sp)
                            }
                        }

                        if (isProcessing) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Przetwarzanie danych...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // About Application Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "O Aplikacji",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Trening Tracker v1.2",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kompletny dziennik treningowy z rejestracją serii, wagą, pomiarami ciała, wbudowanym planem (38 ćwiczeń), analizą wykresów, multimediami i szyfrowaną bazą danych.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Export Password Dialog
    if (showExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SuccessGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Szyfrowana Kopia AES-256", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Plik kopii zapasowej jest w 100% chroniony szyfrowaniem AES-256-GCM. Żadne dane nie są zapisywane otwartym tekstem.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Możesz opcjonalnie zdefiniować własne hasło ochronne lub pozostawić to pole puste (wtedy użyte zostanie wbudowane bezpieczne szyfrowanie skarbca aplikacji):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = exportPasswordInput,
                        onValueChange = { exportPasswordInput = it },
                        label = { Text("Własne hasło (opcjonalne)") },
                        placeholder = { Text("Zostaw puste dla szyfrowania standardowego") },
                        singleLine = true,
                        visualTransformation = if (isExportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isExportPasswordVisible = !isExportPasswordVisible }) {
                                Icon(
                                    imageVector = if (isExportPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isExportPasswordVisible) "Ukryj hasło" else "Pokaż hasło"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingExportPassword = if (exportPasswordInput.isNotBlank()) exportPasswordInput else null
                        showExportPasswordDialog = false
                        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                        val defaultName = "trening_tracker_backup_$dateStr.json"
                        exportBackupLauncher.launch(defaultName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange)
                ) {
                    Text("Utwórz i zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPasswordDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Restore Password Dialog
    if (showRestorePasswordDialog && pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = {
                showRestorePasswordDialog = false
                pendingRestoreUri = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AthleticOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wymagane Hasło Odszyfrowania", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Ten plik kopii zapasowej został zabezpieczony hasłem. Wprowadź hasło, aby odszyfrować i przywrócić dane:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = restorePasswordInput,
                        onValueChange = { restorePasswordInput = it },
                        label = { Text("Hasło kopii zapasowej") },
                        singleLine = true,
                        visualTransformation = if (isRestorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isRestorePasswordVisible = !isRestorePasswordVisible }) {
                                Icon(
                                    imageVector = if (isRestorePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isRestorePasswordVisible) "Ukryj hasło" else "Pokaż hasło"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = restorePasswordInput
                        val uri = pendingRestoreUri
                        showRestorePasswordDialog = false
                        if (uri != null) {
                            isProcessing = true
                            onRestoreBackup(uri, pass) { result ->
                                isProcessing = false
                                if (result.isPasswordRequired) {
                                    showRestorePasswordDialog = true
                                    resultDialogTitle = "Błędne hasło"
                                    resultDialogMessage = "Wprowadzone hasło jest niepoprawne."
                                } else if (result.isSuccess) {
                                    resultDialogTitle = "Przywracanie zakończone"
                                    resultDialogMessage = result.message
                                    pendingRestoreUri = null
                                } else {
                                    resultDialogTitle = "Błąd przywracania"
                                    resultDialogMessage = result.message
                                    pendingRestoreUri = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                    enabled = restorePasswordInput.isNotBlank()
                ) {
                    Text("Odszyfruj i przywróć")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestorePasswordDialog = false
                    pendingRestoreUri = null
                }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Result alert dialog
    if (resultDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { resultDialogMessage = null },
            title = {
                Text(resultDialogTitle, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(resultDialogMessage ?: "")
            },
            confirmButton = {
                Button(
                    onClick = { resultDialogMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange)
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) AthleticOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) AthleticOrange else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AthleticOrange)
        )
    }
}
