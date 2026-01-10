package com.mogars.stepby.ui.components.home_screen

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mogars.stepby.R
import com.mogars.stepby.data.StepByDatabase
import com.mogars.stepby.ui.components.CheckCircleButton
import com.mogars.stepby.ui.components.GoalType
import com.mogars.stepby.ui.components.vibrate
import com.mogars.stepby.ui.models.HabitUiModel
import com.mogars.stepby.ui.theme.completeColor
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ChevronRight

@Composable
fun HabitCard(
    habit: HabitUiModel,
    onCheckClick: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onOpenLongPress: ((HabitUiModel) -> Unit)? = null
) {
    val context = LocalContext.current
    rememberCoroutineScope()
    val subHabitDao = StepByDatabase.getDatabase(context).subHabitDao()

    // Flow de subhábitos si existen (solo para mostrar progreso visual)
    val subHabitsFlow =
        if (habit.hasSubHabits) remember { subHabitDao.getSubHabitsForHabit(habit.id) } else null
    val subHabits by subHabitsFlow?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    // Calcular solo PROGRESO (para la barra visual), no completitud
    val completedSubCount = subHabits.count { it.isCompleted }
    val totalSub = subHabits.size
    val progress = when {
        habit.hasSubHabits && totalSub > 0 -> (completedSubCount.toFloat() / totalSub).coerceIn(
            0f,
            1f
        )
        else -> (habit.currentValue / habit.targetValue).coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(progress, label = "progress")

    // ✅ USAR isCompleted DE LA BD, no calcularlo
    val isCompleted = habit.isCompleted

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = completeColor
    val backgroundColor by animateColorAsState(
        if (isCompleted) progressColor else baseColor,
        label = "bgColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(72.dp)
            .combinedClickable(
                onClick = {
                    vibrate(context, 20)
                    onOpen()
                },
                onLongClick = {
                    Log.d("HabitCard", "LONG PRESS en el habito: ${habit.name}")
                    vibrate(context, 20)
                    onOpenLongPress?.invoke(habit)
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box {
            // 🟢 FONDO PROGRESIVO solo para AMOUNT o SUBHABITS
            if (!isCompleted && (habit.goalType == GoalType.AMOUNT || habit.hasSubHabits)) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(20.dp))
                        .background(progressColor)
                )
            }

            // 📝 CONTENIDO
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCompleted)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(2.dp))

                    // Mostrar subhábitos o cantidad
                    Text(
                        when {
                            habit.hasSubHabits && totalSub > 0 ->
                                stringResource(
                                    R.string.subhabitos_completados,
                                    completedSubCount,
                                    totalSub
                                )

                            habit.goalType == GoalType.AMOUNT ->
                                "${habit.currentValue} / ${habit.targetValue} ${habit.unit}"

                            else ->
                                "${habit.currentValue} / ${habit.targetValue}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCompleted)
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 👉 LADO DERECHO
                when {
                    habit.goalType == GoalType.CHECK && !habit.hasSubHabits -> {
                        CheckCircleButton(
                            checked = isCompleted,
                            onClick = {
                                onCheckClick(!isCompleted)
                            },
                            habitId = habit.id
                        )
                    }

                    habit.hasSubHabits || habit.goalType == GoalType.AMOUNT -> {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isCompleted)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}