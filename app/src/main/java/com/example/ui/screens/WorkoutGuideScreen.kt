package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import com.example.ui.theme.SuccessGreen

@Composable
fun WorkoutGuideScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Zasady & Wskazówki",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Kluczowe wytyczne techniczne, progresja i fizjologia",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Rozgrzewka & Mobilizacja
        item {
            GuideSectionCard(
                icon = Icons.Default.Whatshot,
                accentColor = AthleticOrange,
                title = "1. Rozgrzewka & Mobilizacja",
                content = """
                    • Rozgrzewka: Wykonywana przed każdym treningiem (Row erg, Air bike) na ok. 50% możliwości. Celem jest bezpieczne podniesienie temperatury ciała i tętna bez wywoływania zmęczenia obwodowego.
                    
                    • Mobilizacja & Aktywacja: Indywidualne odczucie rozciąganego mięśnia powinno mieścić się w skali od 1 do 10 na poziomie 9. Tę część treningu wykonujesz bez przerw między ćwiczeniami (aktywacja, nie zmęczenie!).
                """.trimIndent()
            )
        }

        // 2. Dobór ciężaru & Zasada stagnacji (-10%)
        item {
            GuideSectionCard(
                icon = Icons.Default.TrendingDown,
                accentColor = GoldPr,
                title = "2. Dobór Ciężaru & Zasada Stagnacji (-10%)",
                content = """
                    • Ciężar to kwestia indywidualna: Dobierz go tak, aby wykonać zamierzone powtórzenia w danym tempie oraz zaplanowanym RIR.
                    
                    • Zasada Przełamywania Stagnacji: Jeśli podczas 4 kolejnych takich samych treningów doznasz stagnacji (brak możliwości zwiększenia ciężaru lub powtórzeń), odejmij 10% aktualnego obciążenia i zacznij na nowo stopniowo dodawać ciężar.
                """.trimIndent()
            )
        }

        // 3. Prawidłowe Oddychanie
        item {
            GuideSectionCard(
                icon = Icons.Default.Air,
                accentColor = ElectricCyan,
                title = "3. Oddychanie w Treningu Siłowym",
                content = """
                    • Kluczowa kwestia stabilizacji i bezpieczeństwa kręgosłupa (tłocznia brzuszna).
                    
                    • Złota zasada: Zawsze w cięższej fazie ruchu (faza koncentryczna, np. wypychanie, wstawanie, podciąganie) jesteś na wydechu.
                    
                    • Zanim znajdziesz się w najtrudniejszej fazie, bierzesz głęboki wdech do brzucha i napinasz mięśnie głębokie (core).
                """.trimIndent()
            )
        }

        // 4. Jak czytać Tempo (np. 3120)
        item {
            GuideSectionCard(
                icon = Icons.Default.Speed,
                accentColor = SuccessGreen,
                title = "4. Jak Odczytywać Tempo (np. 3120)",
                content = """
                    Zapis 4 cyfr oznacza czas trwania poszczególnych faz powtórzenia:
                    
                    • 3 – czas w sekundach fazy ekscentrycznej (opuszczanie ciężaru / rozciąganie mięśnia)
                    • 1 – czas w sekundach pauzy izometrycznej na dole (w pełnym rozciągnięciu)
                    • 2 – czas w sekundach fazy koncentrycznej (podnoszenie / skurcz mięśnia)
                    • 0 – czas pauzy na górze (przed kolejnym powtórzeniem)
                    
                    • Znak '-' w planie: oznacza, że ćwiczenie wykonujesz w tempie kontrolowanym z płynnym oddechem.
                """.trimIndent()
            )
        }

        // 5. Co oznacza RIR (Reps in Reserve)
        item {
            GuideSectionCard(
                icon = Icons.Default.FitnessCenter,
                accentColor = MaterialTheme.colorScheme.primary,
                title = "5. Skala RIR (Reps in Reserve)",
                content = """
                    RIR (powtórzenia w zapasie) określa, ile powtórzeń byłbyś jeszcze w stanie wykonać z poprawną techniką przed załamaniem mięśniowym:
                    
                    • RIR 3: kończysz serię z zapasem 3 powtórzeń
                    • RIR 2: kończysz serię z zapasem 2 powtórzeń
                    • RIR 1: kończysz serię z zapasem 1 powtórzenia (wysokie obciążenie)
                    
                    • Zapis "malejące (3-2-2-1)": oznacza, że z każdą kolejną serią ćwiczenia zwiększasz intensywność lub ciężar, zbliżając się do załamania w ostatniej serii.
                """.trimIndent()
            )
        }
    }
}

@Composable
fun GuideSectionCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
