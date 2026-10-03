package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.BodyMeasurement
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HtmlReportExporter {

    private fun encodeUriToBase64(context: Context, uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: if (File(uriString).exists()) FileInputStream(File(uriString)) else null

            inputStream?.use { stream ->
                val bytes = stream.readBytes()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val maxDim = 800
                    val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                    val scaled = if (ratio < 1f) {
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * ratio).toInt(),
                            (bitmap.height * ratio).toInt(),
                            true
                        )
                    } else bitmap
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                    Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                } else {
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun encodeDrawableToBase64(context: Context, drawableResId: Int): String? {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableResId) ?: return null
            val maxDim = 800
            val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
            } else bitmap
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun getMuscleColorHex(level: Int): String = when (level) {
        4 -> "#FF6D00"
        3 -> "#FFB703"
        2 -> "#00E5FF"
        1 -> "#38BDF8"
        else -> "#233048"
    }

    fun generateAndShareHtmlReport(
        context: Context,
        reportData: GeneratedReportData,
        sessions: List<WorkoutSession>,
        allSets: List<WorkoutSetLog>,
        allExercises: List<Exercise>,
        allMeasurements: List<BodyMeasurement>,
        cutoffTime: Long
    ) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val generatedDateStr = dateFormat.format(Date())

        val exerciseMap = allExercises.associateBy { it.id }

        // Filter sessions and measurements in period
        val periodSessions = sessions.filter { it.isCompleted && it.startTime >= cutoffTime }.sortedByDescending { it.startTime }
        val periodSessionIds = periodSessions.map { it.id }.toSet()
        val periodSets = allSets.filter { it.isCompleted && it.sessionId in periodSessionIds }
        val periodMeasurements = allMeasurements.filter { it.timestamp >= cutoffTime }.sortedBy { it.timestamp }

        // Exclude warmup & mobility from main workout sessions
        val mainPeriodSessions = periodSessions.filter { it.isMainWorkout }
        val warmupPeriodSessions = periodSessions.filter { !it.isMainWorkout }
        val completedMainWorkoutsCount = mainPeriodSessions.size

        // Best set per exercise
        val bestSetsMap = mutableMapOf<Long, WorkoutSetLog>()
        for (set in periodSets) {
            val currentBest = bestSetsMap[set.exerciseId]
            if (currentBest == null) {
                bestSetsMap[set.exerciseId] = set
            } else {
                val currentScore = currentBest.weightKg * currentBest.reps
                val setScore = set.weightKg * set.reps
                if (setScore > currentScore || (setScore == currentScore && set.weightKg > currentBest.weightKg)) {
                    bestSetsMap[set.exerciseId] = set
                }
            }
        }

        // Muscle groups activity
        val muscleActivities = calculateMuscleActivities(allExercises, periodSets, 3650)
        val sortedMuscles = muscleActivities.values.sortedByDescending { it.setsCount }

        // Preload photos
        data class ReportPhoto(val dateStr: String, val label: String, val base64: String, val weightStr: String?)
        val photosList = mutableListOf<ReportPhoto>()
        for (m in periodMeasurements.reversed()) {
            val dStr = dateFormat.format(Date(m.timestamp))
            val wStr = m.weightKg?.let { "${it} kg" }
            m.frontPhotoUri?.let { uri ->
                encodeUriToBase64(context, uri)?.let { b64 ->
                    photosList.add(ReportPhoto(dStr, "Przód", b64, wStr))
                }
            }
            m.sidePhotoUri?.let { uri ->
                encodeUriToBase64(context, uri)?.let { b64 ->
                    photosList.add(ReportPhoto(dStr, "Bok", b64, wStr))
                }
            }
            m.backPhotoUri?.let { uri ->
                encodeUriToBase64(context, uri)?.let { b64 ->
                    photosList.add(ReportPhoto(dStr, "Tył", b64, wStr))
                }
            }
        }

        val htmlBuilder = StringBuilder()
        htmlBuilder.append("""
            <!DOCTYPE html>
            <html lang="pl">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Kompleksowy Raport Treningowy - $generatedDateStr</title>
                <style>
                    :root {
                        --bg: #0B111E;
                        --card-bg: #151E2E;
                        --card-border: #243248;
                        --text: #F8FAFC;
                        --text-muted: #94A3B8;
                        --accent-orange: #FF6D00;
                        --accent-cyan: #00E5FF;
                        --accent-gold: #FFB703;
                        --accent-green: #10B981;
                        --danger: #EF4444;
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
                    body { background: var(--bg); color: var(--text); padding: 24px 16px; line-height: 1.5; }
                    .container { max-width: 1040px; margin: 0 auto; }
                    .header { background: linear-gradient(135deg, #1E293B, #0F172A); border: 1px solid var(--card-border); border-radius: 16px; padding: 24px; margin-bottom: 24px; position: relative; }
                    .header h1 { font-size: 26px; font-weight: 800; color: #FFFFFF; display: flex; align-items: center; gap: 10px; }
                    .header .period-badge { display: inline-block; background: var(--accent-orange); color: #fff; font-size: 12px; font-weight: 700; padding: 4px 10px; border-radius: 20px; margin-top: 8px; text-transform: uppercase; }
                    .header p { color: var(--text-muted); font-size: 13px; margin-top: 6px; }

                    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; margin-bottom: 24px; }
                    .stat-card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 14px; padding: 18px; }
                    .stat-card .label { font-size: 12px; color: var(--text-muted); text-transform: uppercase; font-weight: 600; }
                    .stat-card .value { font-size: 24px; font-weight: 800; margin-top: 4px; }
                    .stat-card .subtext { font-size: 11px; color: var(--text-muted); margin-top: 3px; }
                    .stat-card .value.cyan { color: var(--accent-cyan); }
                    .stat-card .value.orange { color: var(--accent-orange); }
                    .stat-card .value.green { color: var(--accent-green); }
                    .stat-card .value.gold { color: var(--accent-gold); }

                    .section { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 16px; padding: 20px; margin-bottom: 24px; }
                    .section h2 { font-size: 18px; font-weight: 700; margin-bottom: 16px; color: #FFFFFF; border-left: 4px solid var(--accent-cyan); padding-left: 10px; display: flex; align-items: center; justify-content: space-between; }

                    /* Charts */
                    .chart-container { width: 100%; margin: 16px 0; overflow-x: auto; }
                    .svg-chart { width: 100%; height: auto; display: block; border-radius: 10px; }

                    /* Muscle map layout */
                    .muscle-model-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; align-items: center; }
                    @media (max-width: 768px) { .muscle-model-grid { grid-template-columns: 1fr; } }
                    .muscle-model-box { background: #0C1019; border: 1px solid #1C273A; border-radius: 14px; padding: 14px; text-align: center; }
                    .muscle-legend { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: 14px; font-size: 11px; }
                    .legend-item { display: flex; align-items: center; gap: 6px; }
                    .legend-dot { width: 12px; height: 12px; border-radius: 3px; }

                    /* Photo Gallery */
                    .photo-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; margin-top: 12px; }
                    .photo-card { background: #0F172A; border: 1px solid var(--card-border); border-radius: 12px; overflow: hidden; }
                    .photo-card img { width: 100%; height: 280px; object-fit: cover; display: block; }
                    .photo-meta { padding: 10px; font-size: 12px; color: var(--text-muted); display: flex; justify-content: space-between; }

                    /* Table styling */
                    .table-wrapper { overflow-x: auto; }
                    table { width: 100%; border-collapse: collapse; text-align: left; font-size: 13px; }
                    th { background: #0F172A; color: var(--text-muted); padding: 12px 10px; font-weight: 600; text-transform: uppercase; font-size: 11px; border-bottom: 1px solid var(--card-border); }
                    td { padding: 12px 10px; border-bottom: 1px solid rgba(255,255,255,0.06); }
                    tr:hover { background: rgba(255,255,255,0.02); }

                    /* Muscle progress bar */
                    .muscle-bar-container { background: #0F172A; border-radius: 8px; height: 10px; width: 100%; overflow: hidden; margin-top: 6px; }
                    .muscle-bar { height: 100%; border-radius: 8px; }

                    /* PR Badge */
                    .pr-badge { background: rgba(255, 183, 3, 0.15); border: 1px solid var(--accent-gold); color: var(--accent-gold); font-size: 10px; font-weight: 800; padding: 2px 6px; border-radius: 6px; display: inline-flex; align-items: center; gap: 4px; }
                    .warmup-badge { background: rgba(16, 185, 129, 0.2); border: 1px solid var(--accent-green); color: var(--accent-green); font-size: 10px; font-weight: 800; padding: 2px 6px; border-radius: 6px; margin-left: 6px; }

                    /* Session item */
                    .session-card { background: #0F172A; border: 1px solid var(--card-border); border-radius: 12px; padding: 16px; margin-bottom: 14px; }
                    .session-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
                    .session-card-title { font-size: 15px; font-weight: 700; color: #FFF; display: flex; align-items: center; gap: 6px; }
                    .session-meta { font-size: 12px; color: var(--text-muted); }
                    .badge-pill { background: var(--card-border); color: #CBD5E1; font-size: 11px; padding: 3px 8px; border-radius: 6px; font-weight: 600; }

                    .footer { text-align: center; color: var(--text-muted); font-size: 12px; padding: 24px 0; border-top: 1px solid var(--card-border); margin-top: 20px; }
                </style>
            </head>
            <body>
            <div class="container">
                <div class="header">
                    <h1>Raport Treningowy</h1>
                    <div class="period-badge">Zakres: ${reportData.periodLabel} (${reportData.dateRangeStr})</div>
                    <p>Wygenerowano: $generatedDateStr | Gym Workout & Body Tracker</p>
                </div>

                <!-- KPI STATS -->
                <div class="stats-grid">
                    <div class="stat-card">
                        <div class="label">Ukończone Treningi</div>
                        <div class="value orange">$completedMainWorkoutsCount sesji</div>
                        ${if (warmupPeriodSessions.isNotEmpty()) "<div class='subtext'>+ ${warmupPeriodSessions.size} sesji rozgrzewki (pomocniczych)</div>" else "<div class='subtext'>Pełne treningi siłowe</div>"}
                    </div>
                    <div class="stat-card">
                        <div class="label">Łączny Tonaż</div>
                        <div class="value cyan">${reportData.totalTonnageKg} kg</div>
                        <div class="subtext">${String.format(Locale.US, "%.1f", reportData.totalTonnageKg / 1000f)} ton przerzuconego ciężaru</div>
                    </div>
                    <div class="stat-card">
                        <div class="label">Czas na Siłowni</div>
                        <div class="value gold">${reportData.totalTrainingMinutes / 60}h ${reportData.totalTrainingMinutes % 60}m</div>
                        <div class="subtext">${reportData.totalTrainingMinutes} minut łącznego wysiłku</div>
                    </div>
                    <div class="stat-card">
                        <div class="label">Wykonane Serie</div>
                        <div class="value green">${reportData.totalSetsCount} serii</div>
                        <div class="subtext">${reportData.totalRepsCount} powtórzeń łącznie</div>
                    </div>
                </div>
        """.trimIndent())

        // 1. WYKRESY (Charts Section)
        htmlBuilder.append("""
            <div class="section">
                <h2>Wykresy i Dynamika Progresu</h2>
        """.trimIndent())

        // Chart 1: Tonnage per session (SVG Bar Chart)
        val sessionsChronological = mainPeriodSessions.reversed()
        if (sessionsChronological.isNotEmpty()) {
            val maxTonnage = sessionsChronological.maxOfOrNull { s ->
                val sSets = periodSets.filter { it.sessionId == s.id }
                sSets.sumOf { (it.weightKg * it.reps).toLong() }
            }?.coerceAtLeast(100L) ?: 1000L

            val chartWidth = 900
            val chartHeight = 220
            val barWidth = (chartWidth - 100) / sessionsChronological.size.coerceAtLeast(1)
            val actualBarW = (barWidth * 0.65).coerceIn(16.0, 48.0)

            htmlBuilder.append("""
                <div style="margin-bottom: 20px;">
                    <h3 style="font-size: 14px; color: var(--text-muted); text-transform: uppercase; margin-bottom: 8px;">Tonaż treningowy na poszczególnych sesjach (kg)</h3>
                    <div class="chart-container">
                        <svg class="svg-chart" viewBox="0 0 $chartWidth $chartHeight" style="background: #0C1019; border: 1px solid #1E293B;">
                            <!-- Grid lines -->
                            <line x1="50" y1="30" x2="880" y2="30" stroke="#1E293B" stroke-dasharray="4" />
                            <line x1="50" y1="90" x2="880" y2="90" stroke="#1E293B" stroke-dasharray="4" />
                            <line x1="50" y1="150" x2="880" y2="150" stroke="#1E293B" stroke-dasharray="4" />
                            <line x1="50" y1="180" x2="880" y2="180" stroke="#334155" />
            """.trimIndent())

            sessionsChronological.forEachIndexed { idx, s ->
                val sSets = periodSets.filter { it.sessionId == s.id }
                val ton = sSets.sumOf { (it.weightKg * it.reps).toLong() }
                val h = ((ton.toDouble() / maxTonnage) * 140).coerceAtLeast(4.0)
                val x = 70 + idx * barWidth + (barWidth - actualBarW) / 2
                val y = 180 - h
                val dateLabel = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(s.startTime))

                htmlBuilder.append("""
                    <rect x="$x" y="$y" width="$actualBarW" height="$h" rx="4" fill="url(#tonGrad)" opacity="0.9" />
                    <text x="${x + actualBarW / 2}" y="${y - 6}" font-size="10" fill="#00E5FF" text-anchor="middle" font-weight="bold">${ton}</text>
                    <text x="${x + actualBarW / 2}" y="196" font-size="10" fill="#94A3B8" text-anchor="middle">$dateLabel</text>
                """.trimIndent())
            }

            htmlBuilder.append("""
                            <defs>
                                <linearGradient id="tonGrad" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="0%" stop-color="#00E5FF" />
                                    <stop offset="100%" stop-color="#0284C7" />
                                </linearGradient>
                            </defs>
                        </svg>
                    </div>
                </div>
            """.trimIndent())
        }

        // Chart 2: Weight & Body fat progression (SVG Line Chart)
        val measurementsWithWeight = periodMeasurements.filter { it.weightKg != null }
        if (measurementsWithWeight.size >= 2) {
            val minWeight = measurementsWithWeight.minOf { it.weightKg!! } - 1f
            val maxWeight = measurementsWithWeight.maxOf { it.weightKg!! } + 1f
            val weightRange = (maxWeight - minWeight).coerceAtLeast(1f)

            val chartWidth = 900
            val chartHeight = 200

            val pointsStr = measurementsWithWeight.mapIndexed { idx, m ->
                val x = 60 + (idx.toFloat() / (measurementsWithWeight.size - 1)) * 800
                val y = 160 - ((m.weightKg!! - minWeight) / weightRange) * 120
                "$x,$y"
            }.joinToString(" ")

            htmlBuilder.append("""
                <div style="margin-bottom: 10px;">
                    <h3 style="font-size: 14px; color: var(--text-muted); text-transform: uppercase; margin-bottom: 8px;">Progres masy ciała (kg)</h3>
                    <div class="chart-container">
                        <svg class="svg-chart" viewBox="0 0 $chartWidth $chartHeight" style="background: #0C1019; border: 1px solid #1E293B;">
                            <line x1="50" y1="40" x2="880" y2="40" stroke="#1E293B" stroke-dasharray="4" />
                            <line x1="50" y1="100" x2="880" y2="100" stroke="#1E293B" stroke-dasharray="4" />
                            <line x1="50" y1="160" x2="880" y2="160" stroke="#334155" />
                            <polyline fill="none" stroke="#FF6D00" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" points="$pointsStr" />
            """.trimIndent())

            measurementsWithWeight.forEachIndexed { idx, m ->
                val x = 60 + (idx.toFloat() / (measurementsWithWeight.size - 1)) * 800
                val y = 160 - ((m.weightKg!! - minWeight) / weightRange) * 120
                val dLabel = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(m.timestamp))
                htmlBuilder.append("""
                    <circle cx="$x" cy="$y" r="5" fill="#FFB703" stroke="#0B111E" stroke-width="2" />
                    <text x="$x" y="${y - 10}" font-size="11" fill="#FFFFFF" text-anchor="middle" font-weight="bold">${String.format(Locale.US, "%.1f", m.weightKg)} kg</text>
                    <text x="$x" y="178" font-size="10" fill="#94A3B8" text-anchor="middle">$dLabel</text>
                """.trimIndent())
            }

            htmlBuilder.append("</svg></div></div>")
        }

        htmlBuilder.append("</div>") // Close charts section

        // 2. MODEL MIĘŚNI (Muscle Map Section)
        htmlBuilder.append("""
            <div class="section">
                <h2>Model Mięśni i Zaangażowanie Sylwetki</h2>
                <div style="max-width: 680px; margin: 0 auto;">
                    <!-- SVG Interactive Vector Heatmap (Front & Back) -->
                    <div class="muscle-model-box">
                        <h3 style="font-size: 13px; color: #CBD5E1; margin-bottom: 10px; font-weight: bold;">INTERAKTYWNY MODEL STYMULACJI (PRZÓD I TYŁ)</h3>
                        <svg viewBox="0 0 540 380" style="width: 100%; max-height: 380px; display: inline-block;">
                            <!-- PRZÓD (FRONT) -->
                            <g transform="translate(10, 10)">
                                <text x="130" y="20" fill="#94A3B8" font-size="12" font-weight="bold" text-anchor="middle">PRZÓD</text>
                                <!-- Head & Neck -->
                                <circle cx="130" cy="48" r="16" fill="#1C273A" />
                                <polygon points="122,64 138,64 140,78 120,78" fill="#243248" />

                                <!-- Shoulders -->
                                <polygon points="90,80 115,75 110,105 85,102" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.SHOULDERS]?.level ?: 0)}" />
                                <polygon points="170,80 145,75 150,105 175,102" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.SHOULDERS]?.level ?: 0)}" />

                                <!-- Chest -->
                                <polygon points="112,80 130,82 128,116 108,110" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.CHEST]?.level ?: 0)}" />
                                <polygon points="148,80 130,82 132,116 152,110" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.CHEST]?.level ?: 0)}" />

                                <!-- Biceps -->
                                <polygon points="82,104 105,106 100,140 78,135" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.BICEPS]?.level ?: 0)}" />
                                <polygon points="178,104 155,106 160,140 182,135" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.BICEPS]?.level ?: 0)}" />

                                <!-- Forearms -->
                                <polygon points="76,140 98,142 90,195 70,188" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.FOREARMS]?.level ?: 0)}" />
                                <polygon points="184,140 162,142 170,195 190,188" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.FOREARMS]?.level ?: 0)}" />

                                <!-- Abs / Core -->
                                <polygon points="112,120 148,120 144,145 116,145" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.ABS]?.level ?: 0)}" />
                                <polygon points="116,148 144,148 140,172 120,172" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.ABS]?.level ?: 0)}" />
                                <polygon points="120,175 140,175 136,198 124,198" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.ABS]?.level ?: 0)}" />

                                <!-- Quads / Thighs -->
                                <polygon points="110,205 128,205 125,275 102,270" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.QUADS]?.level ?: 0)}" />
                                <polygon points="150,205 132,205 135,275 158,270" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.QUADS]?.level ?: 0)}" />

                                <!-- Calves -->
                                <polygon points="104,282 124,282 120,345 102,340" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.CALVES]?.level ?: 0)}" />
                                <polygon points="156,282 136,282 140,345 158,340" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.CALVES]?.level ?: 0)}" />
                            </g>

                            <!-- TYŁ (BACK) -->
                            <g transform="translate(280, 10)">
                                <text x="130" y="20" fill="#94A3B8" font-size="12" font-weight="bold" text-anchor="middle">TYŁ</text>
                                <!-- Head & Neck -->
                                <circle cx="130" cy="48" r="16" fill="#1C273A" />
                                <polygon points="122,64 138,64 140,78 120,78" fill="#243248" />

                                <!-- Traps & Upper Back -->
                                <polygon points="110,75 150,75 160,115 100,115" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.BACK]?.level ?: 0)}" />

                                <!-- Lats -->
                                <polygon points="100,118 125,120 120,165 95,155" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.BACK]?.level ?: 0)}" />
                                <polygon points="160,118 135,120 140,165 165,155" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.BACK]?.level ?: 0)}" />

                                <!-- Triceps -->
                                <polygon points="80,102 98,104 94,142 74,138" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.TRICEPS]?.level ?: 0)}" />
                                <polygon points="180,102 162,104 166,142 186,138" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.TRICEPS]?.level ?: 0)}" />

                                <!-- Forearms back -->
                                <polygon points="72,144 92,146 86,195 68,188" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.FOREARMS]?.level ?: 0)}" />
                                <polygon points="188,144 168,146 174,195 192,188" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.FOREARMS]?.level ?: 0)}" />

                                <!-- Glutes -->
                                <polygon points="106,175 130,175 128,212 102,208" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.GLUTES]?.level ?: 0)}" />
                                <polygon points="154,175 130,175 132,212 158,208" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.GLUTES]?.level ?: 0)}" />

                                <!-- Hamstrings -->
                                <polygon points="104,215 128,215 124,275 102,270" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.HAMSTRINGS]?.level ?: 0)}" />
                                <polygon points="156,215 132,215 136,275 158,270" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.HAMSTRINGS]?.level ?: 0)}" />

                                <!-- Calves Rear -->
                                <polygon points="104,282 124,282 120,345 102,340" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.CALVES]?.level ?: 0)}" />
                                <polygon points="156,282 136,282 140,345 158,340" fill="${getMuscleColorHex(muscleActivities[MuscleGroup.CALVES]?.level ?: 0)}" />
                            </g>
                        </svg>

                        <!-- Legend -->
                        <div class="muscle-legend">
                            <div class="legend-item"><div class="legend-dot" style="background:#FF6D00;"></div><span>Bardzo wysoka (16+ serii)</span></div>
                            <div class="legend-item"><div class="legend-dot" style="background:#FFB703;"></div><span>Wysoka (10-15)</span></div>
                            <div class="legend-item"><div class="legend-dot" style="background:#00E5FF;"></div><span>Umiarkowana (5-9)</span></div>
                            <div class="legend-item"><div class="legend-dot" style="background:#38BDF8;"></div><span>Wstępna (1-4)</span></div>
                            <div class="legend-item"><div class="legend-dot" style="background:#233048;"></div><span>Brak</span></div>
                        </div>
                    </div>
                </div>

                <!-- Table of muscle volumes -->
                <div class="table-wrapper" style="margin-top: 20px;">
                    <table>
                        <thead>
                            <tr>
                                <th>Grupa Mięśniowa</th>
                                <th>Liczba Serii</th>
                                <th>Tonaż (kg)</th>
                                <th>Udział w Treningu</th>
                            </tr>
                        </thead>
                        <tbody>
        """.trimIndent())

        val maxSets = sortedMuscles.maxOfOrNull { it.setsCount }?.coerceAtLeast(1) ?: 1
        for (item in sortedMuscles) {
            val pct = (item.setsCount.toFloat() / maxSets * 100f).toInt()
            val color = getMuscleColorHex(item.level)
            htmlBuilder.append("""
                <tr>
                    <td><strong>${item.muscle.displayName}</strong></td>
                    <td>${item.setsCount}</td>
                    <td>${item.totalTonnage.toLong()} kg</td>
                    <td style="width: 45%;">
                        <div style="display: flex; justify-content: space-between; font-size: 10px; color: var(--text-muted);">
                            <span>${item.setsCount} serii</span>
                            <span>$pct%</span>
                        </div>
                        <div class="muscle-bar-container">
                            <div class="muscle-bar" style="width: ${pct}%; background: $color;"></div>
                        </div>
                    </td>
                </tr>
            """.trimIndent())
        }
        htmlBuilder.append("</tbody></table></div></div>")

        // 3. ZDJĘCIA SYLWETKI (Photos Gallery Section)
        htmlBuilder.append("""
            <div class="section">
                <h2>Zdjęcia Sylwetki i Postępów Wizualnych</h2>
        """.trimIndent())

        if (photosList.isEmpty()) {
            htmlBuilder.append("""
                <div style="text-align: center; padding: 24px; color: var(--text-muted); font-size: 13px; background: #0C1019; border-radius: 12px;">
                    📷 Brak dodanych zdjęć sylwetki w wybranym okresie pomiarowym.<br>
                    <small>Dodaj zdjęcia sylwetki (przód, bok, tył) w sekcji Pomiary Ciała, aby automatycznie dołączyć je do raportu.</small>
                </div>
            """.trimIndent())
        } else {
            htmlBuilder.append("<div class=\"photo-grid\">")
            for (p in photosList) {
                htmlBuilder.append("""
                    <div class="photo-card">
                        <img src="data:image/jpeg;base64,${p.base64}" alt="Zdjęcie sylwetki: ${p.label}" />
                        <div class="photo-meta">
                            <span><strong>${p.label}</strong> • ${p.dateStr}</span>
                            ${p.weightStr?.let { "<span>$it</span>" } ?: ""}
                        </div>
                    </div>
                """.trimIndent())
            }
            htmlBuilder.append("</div>")
        }
        htmlBuilder.append("</div>") // Close photos section

        // 4. BODY MEASUREMENTS TABLE
        if (periodMeasurements.isNotEmpty()) {
            val initialM = periodMeasurements.first()
            val finalM = periodMeasurements.last()

            fun diffStr(vInit: Float?, vFinal: Float?, unit: String = "cm"): String {
                if (vInit == null || vFinal == null) return "-"
                val diff = vFinal - vInit
                val sign = if (diff > 0) "+" else ""
                val color = if (diff > 0) "var(--accent-cyan)" else if (diff < 0) "var(--accent-orange)" else "var(--text-muted)"
                return "<span style='color:$color; font-weight:bold;'>$sign${String.format(Locale.US, "%.1f", diff)} $unit</span>"
            }

            htmlBuilder.append("""
                <div class="section">
                    <h2>Pomiary Sylwetki i Obwody</h2>
                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 10px; margin-bottom: 16px;">
                        <div class="stat-card">
                            <div class="label">Waga</div>
                            <div class="value">${finalM.weightKg?.let { "${it}kg" } ?: "-"}</div>
                            <div style="font-size: 11px;">Zmiana: ${diffStr(initialM.weightKg, finalM.weightKg, "kg")}</div>
                        </div>
                        <div class="stat-card">
                            <div class="label">Klatka</div>
                            <div class="value">${finalM.chestCm?.let { "${it}cm" } ?: "-"}</div>
                            <div style="font-size: 11px;">Zmiana: ${diffStr(initialM.chestCm, finalM.chestCm)}</div>
                        </div>
                        <div class="stat-card">
                            <div class="label">Talia</div>
                            <div class="value">${finalM.waistCm?.let { "${it}cm" } ?: "-"}</div>
                            <div style="font-size: 11px;">Zmiana: ${diffStr(initialM.waistCm, finalM.waistCm)}</div>
                        </div>
                        <div class="stat-card">
                            <div class="label">Biceps</div>
                            <div class="value">${finalM.bicepsCm?.let { "${it}cm" } ?: "-"}</div>
                            <div style="font-size: 11px;">Zmiana: ${diffStr(initialM.bicepsCm, finalM.bicepsCm)}</div>
                        </div>
                    </div>

                    <div class="table-wrapper">
                        <table>
                            <thead>
                                <tr>
                                    <th>Data</th>
                                    <th>Waga (kg)</th>
                                    <th>Tłuszcz (%)</th>
                                    <th>Klatka</th>
                                    <th>Talia</th>
                                    <th>Biceps</th>
                                    <th>Uda</th>
                                    <th>Łydki</th>
                                    <th>Notatki</th>
                                </tr>
                            </thead>
                            <tbody>
            """.trimIndent())

            for (m in periodMeasurements.reversed()) {
                htmlBuilder.append("""
                    <tr>
                        <td><strong>${dateFormat.format(Date(m.timestamp))}</strong></td>
                        <td>${m.weightKg?.let { String.format(Locale.US, "%.1f", it) } ?: "-"}</td>
                        <td>${m.bodyFatPercentage?.let { "${it}%" } ?: "-"}</td>
                        <td>${m.chestCm ?: "-"}</td>
                        <td>${m.waistCm ?: "-"}</td>
                        <td>${m.bicepsCm ?: "-"}</td>
                        <td>${m.thighsCm ?: "-"}</td>
                        <td>${m.calvesCm ?: "-"}</td>
                        <td><small>${m.notes}</small></td>
                    </tr>
                """.trimIndent())
            }
            htmlBuilder.append("</tbody></table></div></div>")
        }

        // 5. WORKOUT SESSIONS & BEST SETS
        htmlBuilder.append("""
            <div class="section">
                <h2>Historia Treningów i Najlepsze Serie</h2>
        """.trimIndent())

        if (periodSessions.isEmpty()) {
            htmlBuilder.append("<p style='color: var(--text-muted);'>Brak ukończonych treningów w wybranym okresie.</p>")
        } else {
            for (session in periodSessions) {
                val sSets = periodSets.filter { it.sessionId == session.id }
                val sessionTonnage = sSets.sumOf { (it.weightKg * it.reps).toLong() }
                val durationMin = if (session.endTime > session.startTime) {
                    ((session.endTime - session.startTime) / 60000).coerceAtLeast(1)
                } else 0

                htmlBuilder.append("""
                    <div class="session-card">
                        <div class="session-card-header">
                            <div>
                                <span class="session-card-title">
                                    ${session.workoutName}
                                    ${if (!session.isMainWorkout) "<span class='warmup-badge'>ROZGRZEWKA / MOBILITY</span>" else ""}
                                </span>
                                <div class="session-meta">${dateFormat.format(Date(session.startTime))} o ${timeFormat.format(Date(session.startTime))}</div>
                            </div>
                            <div style="display:flex; gap: 6px;">
                                <span class="badge-pill">⏱️ ${durationMin} min</span>
                                <span class="badge-pill" style="color:var(--accent-cyan);">🏋️ ${sessionTonnage} kg</span>
                                <span class="badge-pill">${sSets.size} serii</span>
                            </div>
                        </div>
                """.trimIndent())

                if (session.notes.isNotBlank()) {
                    htmlBuilder.append("""<p style="font-size: 12px; color: #CBD5E1; margin-bottom: 10px; background: rgba(255,255,255,0.04); padding: 8px; border-radius: 6px;">📝 ${session.notes}</p>""")
                }

                // Exercise grouped sets
                val groupedByExercise = sSets.groupBy { it.exerciseId }
                htmlBuilder.append("""
                    <div class="table-wrapper">
                        <table>
                            <thead>
                                <tr>
                                    <th>Ćwiczenie</th>
                                    <th>Serie i Ciężary</th>
                                    <th>Najlepsza Seria</th>
                                </tr>
                            </thead>
                            <tbody>
                """.trimIndent())

                for ((exId, exSets) in groupedByExercise) {
                    val ex = exerciseMap[exId]
                    val exName = ex?.name ?: "Ćwiczenie"
                    val bestOverall = bestSetsMap[exId]

                    val setsStr = exSets.joinToString(" | ") { s ->
                        val isBest = s.id == bestOverall?.id
                        val str = if (s.weightKg > 0) "${s.weightKg}kg × ${s.reps}" else "${s.reps} powt."
                        if (isBest) "<strong style='color:var(--accent-gold);'>$str</strong>" else str
                    }

                    val bestSet = exSets.maxByOrNull { it.weightKg * it.reps }
                    val bestSetStr = if (bestSet != null) {
                        "<span class='pr-badge'>★ ${if (bestSet.weightKg > 0) "${bestSet.weightKg}kg × ${bestSet.reps}" else "${bestSet.reps} powt."}</span>"
                    } else "-"

                    htmlBuilder.append("""
                        <tr>
                            <td><strong>${ex?.code?.let { "$it. " } ?: ""}$exName</strong></td>
                            <td>$setsStr</td>
                            <td>$bestSetStr</td>
                        </tr>
                    """.trimIndent())
                }

                htmlBuilder.append("</tbody></table></div></div>")
            }
        }

        htmlBuilder.append("""
                </div>
                <div class="footer">
                    Wygenerowano przez Gym Workout & Body Tracker • Raport Treningowy
                </div>
            </div>
            </body>
            </html>
        """.trimIndent())

        // Save file to cache and share
        try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val fileName = "Raport_Treningowy_${System.currentTimeMillis()}.html"
            val reportFile = File(reportsDir, fileName)
            FileOutputStream(reportFile).use { fos ->
                fos.write(htmlBuilder.toString().toByteArray(Charsets.UTF_8))
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Kompleksowy Raport Treningowy - $generatedDateStr")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Udostępnij lub Otwórz Raport HTML"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
