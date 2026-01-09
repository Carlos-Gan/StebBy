package com.mogars.stepby.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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

    // ================== CALCULAR ESTADÍSTICAS ==================

    val completedDates = activities
        .filter { it.intensity > 0 } // cambia a >=4 si quieres solo completos reales
        .map { LocalDate.parse(it.date) }
        .toSet()

    val today = LocalDate.now()

    // 🔥 Racha actual
    var currentStreak = 0
    var cursor = today
    while (completedDates.contains(cursor)) {
        currentStreak++
        cursor = cursor.minusDays(1)
    }

    // 🏆 Mejor racha
    val sortedDays = completedDates.sorted()
    var bestStreak = 0
    var tempStreak = 0
    var lastDate: LocalDate? = null

    for (date in sortedDays) {
        if (lastDate == null || date == lastDate.plusDays(1)) {
            tempStreak++
        } else {
            tempStreak = 1
        }
        bestStreak = maxOf(bestStreak, tempStreak)
        lastDate = date
    }

    val totalCompletedDays = completedDates.size
    val totalRecords = activities.size

    // =============== ESTADISTICAS DE HORARIO ==================

    val hours = activities
        .map {
            try {
                it.time.substring(0, 2).toInt()

            } catch (e: Exception) {
                Log.e("HabitDescription", " Error en : {$e.toString()}")
            }
        }
    val averageHora = if (hours.isNotEmpty()) hours.average() else 0.0

    val morningCount = hours.count { it in 5..11 }
    val afternoonCount = hours.count { it in 12..17 }
    val eveningCount = hours.count { it in 18..23 || it in 0..4 }


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
                    IconButton(
                        onClick = {
                            showDeleteDialog = true
                        }
                    ) {
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
        if (showDeleteDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.eliminar_habito)) },
                text = {
                    Text(stringResource(R.string.dialog_eliminar_habito))
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showDeleteDialog = false

                            habit?.let {
                                CoroutineScope(Dispatchers.IO).launch {
                                    db.habitActivityDao().deleteActivitiesForHabit(it.id)
                                    db.habitDao().deleteHabit(it)

                                    withContext(Dispatchers.Main) {
                                        onBack()
                                    }
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.eliminar))
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text(stringResource(R.string.cancelar))
                    }
                }
            )
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
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

            Column(modifier = Modifier.padding(horizontal = 8.dp))
            {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Absolute.SpaceBetween
                ) {
                    StatCard(stringResource(R.string.racha_actual), "$currentStreak días", icon =  "🔥",Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    StatCard(stringResource(R.string.mejor_racha), "$bestStreak días", icon= "🏆",Modifier.weight(1f))
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCard(stringResource(R.string.completados), "$totalCompletedDays",icon= "📊", Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    StatCard(stringResource(R.string.registros), "$totalRecords", icon= "📅",Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.horarios), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        stringResource(R.string.promedio),
                        formatHourToAmPm(averageHora),
                        icon = "⏳",
                        Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(stringResource(R.string.manana), "$morningCount",icon = "☀️", Modifier.weight(1f))
                    StatCard(stringResource(R.string.tarde), "$afternoonCount",icon = "🌇", Modifier.weight(1f))
                    StatCard(stringResource(R.string.noche), "$eveningCount",icon = "🌙", Modifier.weight(1f))
                }
            }
        }
    }
}
