package com.mogars.stepby.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mogars.stepby.R
import com.mogars.stepby.data.StepByDatabase
import com.mogars.stepby.data.entity.HabitActivityEntity
import com.mogars.stepby.data.entity.HabitEntity
import com.mogars.stepby.ui.components.formatHourToAmPm
import com.mogars.stepby.ui.components.habit_description.HabitHeatMap
import com.mogars.stepby.ui.components.habit_description.StatCard
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import compose.icons.fontawesomeicons.solid.Trash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HabitDescriptionScreen(
    onBack: () -> Unit,
    habitId: Long
) {
    val context = LocalContext.current
    val db = StepByDatabase.getDatabase(context)

    val habit by db.habitDao()
        .getHabitById(habitId)
        .collectAsState(initial = null)

    val activities by db.habitActivityDao()
        .getActivitiesForHabit(habitId)
        .collectAsState(initial = emptyList())

    var showDeleteDialog by remember { mutableStateOf(false) }

    // ✨ Calcular estadísticas
    val statistics = remember(activities) {
        calculateHabitStatistics(activities)
    }

    // ================== UI ==================

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.habito, habit?.name ?: "")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            FontAwesomeIcons.Solid.ArrowLeft,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            FontAwesomeIcons.Solid.Trash,
                            contentDescription = stringResource(R.string.eliminar_habito),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (showDeleteDialog && habit != null) {
            DeleteHabitDialog(
                onConfirm = {
                    deleteHabit(habit!!, db) { onBack() }
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ===== HeatMap =====
            Text(
                stringResource(R.string.historial_habito),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMediumEmphasized
            )

            Spacer(Modifier.height(10.dp))

            HabitHeatMap(
                habitId = habitId,
                habitActivityDao = db.habitActivityDao()
            )

            // ===== Estadísticas =====
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.resumen),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMediumEmphasized
            )

            Spacer(Modifier.height(8.dp))

            StatisticsSection(statistics)
        }
    }
}

// ==================== FUNCIONES AUXILIARES ====================

@Composable
private fun DeleteHabitDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.eliminar_habito)) },
        text = { Text(stringResource(R.string.dialog_eliminar_habito)) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.eliminar))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancelar))
            }
        }
    )
}

@Composable
private fun StatisticsSection(statistics: HabitStatistics) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {

        // ===== RACHAS =====
        Text(
            "🔥 Rachas",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatCard(
                "Racha Actual",
                "${statistics.currentStreak} días",
                icon = "🔥",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Mejor Racha",
                "${statistics.bestStreak} días",
                icon = "🏆",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ===== CONTEOS GENERALES =====
        Text(
            "📊 Resumen General",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatCard(
                "Completados",
                "${statistics.totalCompletedDays}",
                icon = "✅",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Registros",
                "${statistics.totalRecords}",
                icon = "📅",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatCard(
                "Tasa Finalización",
                String.format("%.1f%%", statistics.completionRate),
                icon = "📈",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Días sin Completar",
                "${statistics.daysWithoutCompletion}",
                icon = "⏸️",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ===== HORARIOS =====
        Text(
            "⏰ Horarios",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                "Promedio",
                statistics.averageTimeFormatted,
                icon = "🕐",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                "Mañana",
                "${statistics.morningCount}",
                icon = "☀️",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Tarde",
                "${statistics.afternoonCount}",
                icon = "🌇",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Noche",
                "${statistics.eveningCount}",
                icon = "🌙",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ===== INTENSIDAD =====
        Text(
            "💪 Intensidad",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                "Promedio",
                String.format("%.1f%%", statistics.averageIntensity),
                icon = "💪",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Máxima",
                "${statistics.maxIntensity}%",
                icon = "🚀",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                "Total Puntos",
                "${statistics.totalIntensityPoints}",
                icon = "⚡",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Mínima",
                "${statistics.minIntensity}%",
                icon = "📉",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ===== ACTIVIDAD SEMANAL =====
        Text(
            "📈 Actividad",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                "Día Más Activo",
                statistics.mostActiveDay,
                icon = "🎯",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Semanas Activas",
                "${statistics.totalWeeksActive}",
                icon = "📅",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                "Por Semana",
                String.format("%.1f", statistics.averageActivitiesPerWeek),
                icon = "📊",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Mayor Brecha",
                "${statistics.longestGapDays} días",
                icon = "⏱️",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ===== TIMELINE =====
        Text(
            "📆 Timeline",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                "Primera",
                statistics.firstActivityDate,
                icon = "📍",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Última",
                statistics.lastActivityDate,
                icon = "⏰",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                "Meses Activo",
                "${statistics.totalMonthsActive}",
                icon = "🗓️",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Próximo Hito",
                "${statistics.daysUntilNextMilestone} días",
                icon = "🎁",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                "Este Mes",
                "${statistics.thisMonthCompletions}",
                icon = "📍",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                "Mes Pasado",
                "${statistics.lastMonthCompletions}",
                icon = "📊",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ==================== DATA CLASSES ====================

data class HabitStatistics(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalCompletedDays: Int = 0,
    val totalRecords: Int = 0,
    val averageTimeFormatted: String = "00:00",
    val morningCount: Int = 0,
    val afternoonCount: Int = 0,
    val eveningCount: Int = 0,
    val completionRate: Float = 0f,
    val averageIntensity: Float = 0f,
    val totalIntensityPoints: Int = 0,
    val maxIntensity: Int = 0,
    val minIntensity: Int = 0,
    val daysWithoutCompletion: Int = 0,
    val mostActiveDay: String = "",
    val totalWeeksActive: Int = 0,
    val averageActivitiesPerWeek: Float = 0f,
    val longestGapDays: Int = 0,
    val lastActivityDate: String = "",
    val firstActivityDate: String = "",
    val daysUntilNextMilestone: Int = 0,
    val totalMonthsActive: Int = 0,
    val thisMonthCompletions: Int = 0,
    val lastMonthCompletions: Int = 0
)

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ==================== FUNCIONES ESTADÍSTICAS ====================

fun calculateHabitStatistics(activities: List<HabitActivityEntity>): HabitStatistics {
    val completedDates = activities
        .filter { it.intensity > 0 }
        .map { LocalDate.parse(it.date) }
        .toSet()

    val today = LocalDate.now()
    val currentStreak = calculateCurrentStreak(completedDates, today)
    val bestStreak = calculateBestStreak(completedDates)
    val (morningCount, afternoonCount, eveningCount, averageTime) = calculateTimeStatistics(activities)
    val completionRate = calculateCompletionRate(completedDates, activities)
    val (avgIntensity, totalIntensity, maxInt, minInt) = calculateIntensityStats(activities)
    val daysWithoutCompletion = calculateDaysWithoutCompletion(completedDates, today)
    val mostActiveDay = calculateMostActiveDay(activities)
    val (weeksActive, avgPerWeek) = calculateWeeklyStats(completedDates)
    val longestGap = calculateLongestGap(completedDates)
    val (lastDate, firstDate) = calculateActivityDates(completedDates)
    val daysUntilMilestone = calculateDaysUntilMilestone(currentStreak)
    val (totalMonths, thisMonth, lastMonth) = calculateMonthlyStats(activities, today)

    return HabitStatistics(
        currentStreak = currentStreak,
        bestStreak = bestStreak,
        totalCompletedDays = completedDates.size,
        totalRecords = activities.size,
        averageTimeFormatted = formatHourToAmPm(averageTime),
        morningCount = morningCount,
        afternoonCount = afternoonCount,
        eveningCount = eveningCount,
        completionRate = completionRate,
        averageIntensity = avgIntensity,
        totalIntensityPoints = totalIntensity,
        maxIntensity = maxInt,
        minIntensity = minInt,
        daysWithoutCompletion = daysWithoutCompletion,
        mostActiveDay = mostActiveDay,
        totalWeeksActive = weeksActive,
        averageActivitiesPerWeek = avgPerWeek,
        longestGapDays = longestGap,
        lastActivityDate = lastDate,
        firstActivityDate = firstDate,
        daysUntilNextMilestone = daysUntilMilestone,
        totalMonthsActive = totalMonths,
        thisMonthCompletions = thisMonth,
        lastMonthCompletions = lastMonth
    )
}

private fun calculateCurrentStreak(completedDates: Set<LocalDate>, today: LocalDate): Int {
    var streak = 0
    var cursor = today
    while (completedDates.contains(cursor)) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun calculateBestStreak(completedDates: Set<LocalDate>): Int {
    if (completedDates.isEmpty()) return 0
    val sortedDays = completedDates.sorted()
    var bestStreak = 0
    var tempStreak = 1
    var lastDate = sortedDays.first()

    for (i in 1 until sortedDays.size) {
        val currentDate = sortedDays[i]
        if (currentDate == lastDate.plusDays(1)) {
            tempStreak++
        } else {
            bestStreak = maxOf(bestStreak, tempStreak)
            tempStreak = 1
        }
        lastDate = currentDate
    }
    return maxOf(bestStreak, tempStreak)
}

private fun calculateTimeStatistics(
    activities: List<HabitActivityEntity>
): Quad<Int, Int, Int, Double> {
    val hours = activities.mapNotNull { activity ->
        try {
            activity.time.substring(0, 2).toIntOrNull()
        } catch (e: Exception) {
            Log.e("HabitDescription", "Error parsing time: ${e.message}")
            null
        }
    }

    val averageHora = if (hours.isNotEmpty()) hours.average() else 0.0
    val morningCount = hours.count { it in 5..11 }
    val afternoonCount = hours.count { it in 12..17 }
    val eveningCount = hours.count { it in 18..23 || it in 0..4 }

    return Quad(morningCount, afternoonCount, eveningCount, averageHora)
}

private fun calculateCompletionRate(
    completedDates: Set<LocalDate>,
    activities: List<HabitActivityEntity>
): Float {
    if (activities.isEmpty()) return 0f
    val firstDate = activities.minByOrNull { it.date }?.date?.let { LocalDate.parse(it) } ?: return 0f
    val lastDate = activities.maxByOrNull { it.date }?.date?.let { LocalDate.parse(it) } ?: return 0f
    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(firstDate, lastDate).toInt() + 1
    return if (totalDays > 0) (completedDates.size.toFloat() / totalDays) * 100 else 0f
}

private fun calculateIntensityStats(
    activities: List<HabitActivityEntity>
): Quad<Float, Int, Int, Int> {
    if (activities.isEmpty()) return Quad(0f, 0, 0, 0)
    val intensities = activities.map { it.intensity }
    val avgIntensity = intensities.average().toFloat()
    val totalIntensity = intensities.sum()
    val maxIntensity = intensities.maxOrNull() ?: 0
    val minIntensity = intensities.minOrNull() ?: 0
    return Quad(avgIntensity, totalIntensity, maxIntensity, minIntensity)
}

private fun calculateDaysWithoutCompletion(
    completedDates: Set<LocalDate>,
    today: LocalDate
): Int {
    var cursor = today
    var days = 0
    while (!completedDates.contains(cursor) && days < 365) {
        days++
        cursor = cursor.minusDays(1)
    }
    return days
}

private fun calculateMostActiveDay(activities: List<HabitActivityEntity>): String {
    if (activities.isEmpty()) return "N/A"
    val dayCount = mutableMapOf<Int, Int>()
    activities.forEach { activity ->
        try {
            val date = LocalDate.parse(activity.date)
            val dayOfWeek = date.dayOfWeek.value
            dayCount[dayOfWeek] = (dayCount[dayOfWeek] ?: 0) + 1
        } catch (e: Exception) {
            Log.e("HabitStats", "Error parsing date: ${e.message}")
        }
    }
    val mostActiveDay = dayCount.maxByOrNull { it.value }?.key ?: 1
    return getDayName(mostActiveDay)
}

private fun calculateWeeklyStats(completedDates: Set<LocalDate>): Pair<Int, Float> {
    if (completedDates.isEmpty()) return Pair(0, 0f)

    val weeks = mutableSetOf<Int>()
    completedDates.forEach { date ->
        weeks.add(date.get(java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR))
    }

    val avgPerWeek = if (weeks.isNotEmpty()) completedDates.size.toFloat() / weeks.size else 0f
    return Pair(weeks.size, avgPerWeek)
}

private fun calculateLongestGap(completedDates: Set<LocalDate>): Int {
    if (completedDates.isEmpty()) return 0
    val sortedDates = completedDates.sorted()
    var maxGap = 0

    for (i in 1 until sortedDates.size) {
        val gap = java.time.temporal.ChronoUnit.DAYS
            .between(sortedDates[i - 1], sortedDates[i])
            .toInt()
        maxGap = maxOf(maxGap, gap)
    }

    return maxGap
}

private fun calculateActivityDates(completedDates: Set<LocalDate>): Pair<String, String> {
    if (completedDates.isEmpty()) return Pair("N/A", "N/A")
    val lastDate = completedDates.maxOrNull()?.toString() ?: "N/A"
    val firstDate = completedDates.minOrNull()?.toString() ?: "N/A"
    return Pair(lastDate, firstDate)
}

private fun calculateDaysUntilMilestone(currentStreak: Int): Int {
    val milestones = listOf(7, 14, 30, 60, 90, 100, 365)
    val nextMilestone = milestones.firstOrNull { it > currentStreak } ?: 365
    return nextMilestone - currentStreak
}

private fun calculateMonthlyStats(
    activities: List<HabitActivityEntity>,
    today: LocalDate
): Triple<Int, Int, Int> {
    val thisMonthActivities = activities.count { activity ->
        try {
            val actDate = LocalDate.parse(activity.date)
            actDate.year == today.year && actDate.monthValue == today.monthValue
        } catch (e: Exception) {
            false
        }
    }

    val lastMonthDate = today.minusMonths(1)
    val lastMonthActivities = activities.count { activity ->
        try {
            val actDate = LocalDate.parse(activity.date)
            actDate.year == lastMonthDate.year && actDate.monthValue == lastMonthDate.monthValue
        } catch (e: Exception) {
            false
        }
    }

    val firstActivity = activities.minByOrNull { it.date }?.let {
        try {
            LocalDate.parse(it.date)
        } catch (e: Exception) {
            today
        }
    } ?: today

    val totalMonths = java.time.temporal.ChronoUnit.MONTHS
        .between(firstActivity, today).toInt() + 1

    return Triple(maxOf(totalMonths, 1), thisMonthActivities, lastMonthActivities)
}

private fun getDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "Lunes"
        2 -> "Martes"
        3 -> "Miércoles"
        4 -> "Jueves"
        5 -> "Viernes"
        6 -> "Sábado"
        7 -> "Domingo"
        else -> "N/A"
    }
}

private fun deleteHabit(
    habit: HabitEntity,
    db: StepByDatabase,
    onSuccess: () -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            db.habitActivityDao().deleteActivitiesForHabit(habit.id)
            db.habitDao().deleteHabit(habit)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e("HabitDescription", "Error deleting habit: ${e.message}")
        }
    }
}