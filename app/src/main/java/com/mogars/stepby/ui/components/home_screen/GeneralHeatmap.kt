package com.mogars.stepby.ui.components.home_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mogars.stepby.R
import com.mogars.stepby.data.dao.HabitActivityDao
import java.time.LocalDate

@Composable
fun GeneralHeatMap(
    habitActivityDao: HabitActivityDao
) {
    val today = LocalDate.now()
    val startDate = today.minusDays(27) // últimos 28 días

    // Flow de todas las actividades
    val activitiesFlow = remember { habitActivityDao.getAllActivities() }
    val activities by activitiesFlow.collectAsState(initial = emptyList())

    // Crear lista de intensidad total por día
    val days = (0..27).map { offset ->
        val date = startDate.plusDays(offset.toLong()).toString() // yyyy-MM-dd
        // Sumar intensidades de todos los hábitos ese día
        activities.filter { it.date == date }.sumOf { it.intensity }
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.actividad_total_ultimos_28_dias), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(130.dp)
        ) {
            items(days.size) { index ->
                val totalIntensity = days[index]

                // Colores según la intensidad general
                val color = when {
                    totalIntensity == 0 ->  MaterialTheme.colorScheme.surfaceVariant
                    totalIntensity in 1..2 -> Color(0xFF9BE9A8)
                    totalIntensity in 3..5 -> Color(0xFF40C463)
                    totalIntensity in 6..9 -> Color(0xFF30A14E)
                    totalIntensity >= 10 -> Color(0xFF216E39)
                    else ->  MaterialTheme.colorScheme.surfaceVariant
                }

                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
    }
}
