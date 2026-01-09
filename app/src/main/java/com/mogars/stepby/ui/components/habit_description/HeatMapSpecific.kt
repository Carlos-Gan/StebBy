package com.mogars.stepby.ui.components.habit_description

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
fun HabitHeatMap(
    habitId: Long,
    habitActivityDao: HabitActivityDao
) {
    val today = LocalDate.now()
    val startDate = today.minusDays(27) // últimos 28 días

    // ✅ Flow SOLO de este hábito
    val activitiesFlow = remember(habitId) {
        habitActivityDao.getActivitiesForHabit(habitId)
    }
    val activities by activitiesFlow.collectAsState(initial = emptyList())

    // Crear lista de intensidad por día
    val days = (0..27).map { offset ->
        val date = startDate.plusDays(offset.toLong()).toString() // yyyy-MM-dd

        // 🔥 Buscar actividad de ESTE hábito en ese día
        activities.firstOrNull { it.date == date }?.intensity ?: 0
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            stringResource(R.string.actividad_ltimos_28_dias),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(130.dp)
        ) {
            items(days.size) { index ->
                val intensity = days[index]

                // 🎨 Colores según intensidad DEL HÁBITO
                val color = when {
                    intensity == 0 -> MaterialTheme.colorScheme.surfaceVariant
                    intensity == 1 -> Color(0xFF9BE9A8)
                    intensity == 2 -> Color(0xFF40C463)
                    intensity == 3 -> Color(0xFF30A14E)
                    intensity >= 4 -> Color(0xFF216E39)
                    else -> MaterialTheme.colorScheme.surfaceVariant
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
