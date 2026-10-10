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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
    savedBackupPassword: String,
    onSaveBackupPassword: (String) -> Unit,
    onExportBackup: (Uri, String, (Boolean) -> Unit) -> Unit,
    onRestoreBackup: (Uri, String?, (RestoreResult) -> Unit) -> Unit,
    timerVibrationEnabled: Boolean = true,
    onTimerVibrationChange: (Boolean) -> Unit = {},
    timerSoundEnabled: Boolean = true,
    onTimerSoundChange: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    var isProcessing by remember { mutableStateOf(false) }
    var resultDialogMessage by remember { mutableStateOf<String?>(null) }
    var resultDialogTitle by remember { mutableStateOf("") }

    // Dialog to set / change encryption password
    var showSetPasswordDialog by remember { mutableStateOf(false) }

    // Dialog when exporting without a saved password
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var exportPasswordInput by remember { mutableStateOf("") }
    var isExportPasswordVisible by remember { mutableStateOf(false) }
    var saveExportPasswordToSettings by remember { mutableStateOf(true) }
    var pendingExportPassword by remember { mutableStateOf<String?>(null) }

    // Restore Password state
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var restorePasswordInput by remember { mutableStateOf("") }
    var isRestorePasswordVisible by remember { mutableStateOf(false) }
    var saveRestorePasswordToSettings by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    // Backup Create Document Launcher
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            val pass = pendingExportPassword ?: savedBackupPassword
            if (pass.isBlank()) {
                resultDialogTitle = "Brak hasła"
                resultDialogMessage = "Wymagane jest hasło do zaszyfrowania kopii zapasowej."
                return@rememberLauncherForActivityResult
            }
            isProcessing = true
            onExportBackup(uri, pass) { success ->
                isProcessing = false
                if (success) {
                    resultDialogTitle = "Zaszyfrowana kopia utworzona pomyślnie"
                    resultDialogMessage = "Wszystkie Twoje treningi, serie, pomiary ciała i konfiguracja zostały bezpiecznie zaszyfrowane algorytmem AES-256-GCM (PBKDF2 600 000 iteracji wg zaleceń OWASP) przy użyciu Twojego hasła."
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
            val initialPassword = if (savedBackupPassword.isNotBlank()) savedBackupPassword else null
            onRestoreBackup(uri, initialPassword) { result ->
                isProcessing = false
                if (result.isPasswordRequired) {
                    showRestorePasswordDialog = true
                    restorePasswordInput = ""
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

            // Timer & Notification Card
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
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = AthleticOrange
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Timer Przerw & Powiadomienia",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Dźwięk i haptyka po zakończeniu czasu odpoczynku",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Vibration switch row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTimerVibrationChange(!timerVibrationEnabled) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = if (timerVibrationEnabled) AthleticOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Wibracja po zakończeniu",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Wyraźny impuls wibracji w kieszeni na koniec serii",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = timerVibrationEnabled,
                                onCheckedChange = onTimerVibrationChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                    checkedTrackColor = AthleticOrange
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Sound switch row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTimerSoundChange(!timerSoundEnabled) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (timerSoundEnabled) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Dźwięk powiadomienia (Beep)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Krótki sygnał audio powiadamiający o gotowości",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = timerSoundEnabled,
                                onCheckedChange = onTimerSoundChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                    checkedTrackColor = ElectricCyan
                                )
                            )
                        }
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
                                text = "Szyfrowanie AES-256-GCM • PBKDF2 600 000 iteracji",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // User Backup Password Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = AthleticOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Twoje hasło szyfrowania",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (savedBackupPassword.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SuccessGreen.copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Ustawione",
                                                    color = SuccessGreen,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AthleticOrange.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Nieustawione",
                                                color = AthleticOrange,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (savedBackupPassword.isNotBlank()) {
                                    Text(
                                        text = "Klucz AES-256 jest generowany z Twojego hasła (600 000 iteracji PBKDF2). Żadne hasło nie jest zaszyte w kodzie aplikacji.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showSetPasswordDialog = true },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Zmień hasło", fontSize = 12.sp)
                                        }
                                        TextButton(
                                            onClick = { onSaveBackupPassword("") },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Wyczyść", fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Zdefiniuj własne hasło do ochrony kopii zapasowych. Klucz tworzony jest wyłącznie z Twojego hasła za pomocą 600 000 iteracji PBKDF2-HMAC-SHA256 (rekomendacja OWASP).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { showSetPasswordDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ustaw hasło szyfrowania")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (savedBackupPassword.isNotBlank()) {
                                        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                        val defaultName = "trening_tracker_backup_$dateStr.json"
                                        pendingExportPassword = savedBackupPassword
                                        exportBackupLauncher.launch(defaultName)
                                    } else {
                                        exportPasswordInput = ""
                                        showExportPasswordDialog = true
                                    }
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
                                    text = "Derywacja klucza (600k iteracji) i przetwarzanie...",
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

    // Set Backup Password Dialog (from settings button)
    if (showSetPasswordDialog) {
        var tempPassword by remember { mutableStateOf(savedBackupPassword) }
        var isTempPasswordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSetPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = AthleticOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hasło Szyfrowania Kopii", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Wprowadź hasło, które posłuży do wygenerowania 256-bitowego klucza AES-GCM z 600 000 iteracji PBKDF2-HMAC-SHA256 (rekomendacja OWASP). Hasło zapisane jest wyłącznie lokalnie w ustawieniach Twojej aplikacji i nigdy nie jest zaszyte w plikach kodu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text("Twoje hasło szyfrowania") },
                        singleLine = true,
                        visualTransformation = if (isTempPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isTempPasswordVisible = !isTempPasswordVisible }) {
                                Icon(
                                    imageVector = if (isTempPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isTempPasswordVisible) "Ukryj hasło" else "Pokaż hasło"
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
                        val pass = tempPassword.trim()
                        onSaveBackupPassword(pass)
                        showSetPasswordDialog = false
                        resultDialogTitle = "Hasło zapisane"
                        resultDialogMessage = if (pass.isNotBlank()) {
                            "Twoje hasło szyfrowania zostało pomyślnie zapisane w ustawieniach aplikacji."
                        } else {
                            "Hasło szyfrowania zostało usunięte."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                    enabled = tempPassword.isNotBlank()
                ) {
                    Text("Zapisz hasło")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPasswordDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Export Password Dialog (when user clicks Utwórz kopię and has no saved password)
    if (showExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AthleticOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Podaj Hasło Szyfrowania", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Aplikacja nie posiada domyślnego hasła zaszytego w kodzie. Aby utworzyć kopię zapasową, wprowadź własne hasło (będzie użyte 600 000 iteracji PBKDF2):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = exportPasswordInput,
                        onValueChange = { exportPasswordInput = it },
                        label = { Text("Hasło kopii zapasowej") },
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { saveExportPasswordToSettings = !saveExportPasswordToSettings }
                    ) {
                        Checkbox(
                            checked = saveExportPasswordToSettings,
                            onCheckedChange = { saveExportPasswordToSettings = it },
                            colors = CheckboxDefaults.colors(checkedColor = AthleticOrange)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Zapisz to hasło w ustawieniach aplikacji",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = exportPasswordInput.trim()
                        if (pass.isNotBlank()) {
                            if (saveExportPasswordToSettings) {
                                onSaveBackupPassword(pass)
                            }
                            pendingExportPassword = pass
                            showExportPasswordDialog = false
                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            val defaultName = "trening_tracker_backup_$dateStr.json"
                            exportBackupLauncher.launch(defaultName)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange),
                    enabled = exportPasswordInput.isNotBlank()
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
                        text = "Ten plik kopii zapasowej jest zaszyfrowany. Wprowadź hasło utworzone podczas eksportu, aby odszyfrować dane:",
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { saveRestorePasswordToSettings = !saveRestorePasswordToSettings }
                    ) {
                        Checkbox(
                            checked = saveRestorePasswordToSettings,
                            onCheckedChange = { saveRestorePasswordToSettings = it },
                            colors = CheckboxDefaults.colors(checkedColor = AthleticOrange)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Zapisz to hasło w ustawieniach jako domyślne",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = restorePasswordInput.trim()
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
                                    if (saveRestorePasswordToSettings) {
                                        onSaveBackupPassword(pass)
                                    }
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
