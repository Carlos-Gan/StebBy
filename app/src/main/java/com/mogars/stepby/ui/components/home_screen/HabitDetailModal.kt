package com.mogars.stepby.ui.components.home_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mogars.stepby.data.StepByDatabase
import com.mogars.stepby.data.entity.HabitActivityEntity
import com.mogars.stepby.ui.components.GoalType
import com.mogars.stepby.ui.components.SubHabitList
import com.mogars.stepby.ui.models.HabitUiModel
import com.mogars.stepby.ui.theme.completeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Composable
fun HabitDetailModal(
    habit: HabitUiModel,
    onDismiss: () -> Unit,
    onValueChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subHabitDao = StepByDatabase.getDatabase(context).subHabitDao()
    var inputValue by remember { mutableStateOf(habit.currentValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(habit.name) },
        modifier = Modifier.fillMaxWidth(),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    habit.hasSubHabits -> {
                        Text(
                            "Marca tus subhábitos completados:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        SubHabitList(
                            habitId = habit.id,
                            subHabitDao = subHabitDao,
                            onChange = { completedCount ->
                                onValueChange(completedCount)
                            }
                        )
                    }

                    habit.goalType == GoalType.AMOUNT -> {
                        Text(
                            "Cuánto completaste hoy:",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        val suggestions = when (habit.unit) {
                            "km" -> listOf(1, 5, 10, 15)
                            "ml" -> listOf(250, 500, 750, 1000)
                            "pág" -> listOf(10, 20, 30, 40)
                            else -> listOf(
                                habit.targetValue / 4,
                                habit.targetValue / 3,
                                habit.targetValue / 2,
                                habit.targetValue
                            )
                        }

                        val chunked = suggestions.chunked(2)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chunked.forEach { rowItems ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowItems.forEach { value ->
                                        Button(
                                            onClick = { inputValue = value.toString() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if ((inputValue.toFloatOrNull()
                                                        ?: 0f) == value.toFloat()
                                                )
                                                    completeColor
                                                else MaterialTheme.colorScheme.surfaceDim
                                            )
                                        ) {
                                            Text(
                                                "$value ${habit.unit ?: ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.inverseSurface
                                            )
                                        }
                                    }
                                }
                            }

                        }


                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            thickness = 1.dp
                        )
                        Text(
                            "O ingresa un valor personalizado:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = { newValue ->
                                val filtered = newValue.filter { it.isDigit() || it == '.' }
                                val parts = filtered.split('.')
                                inputValue =
                                    if (parts.size <= 2) filtered else parts[0] + "." + parts.drop(
                                        1
                                    ).joinToString("")
                            },
                            label = { Text("Valor") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valueFloat = inputValue.toFloatOrNull() ?: habit.currentValue
                    val clampedValue = valueFloat.coerceIn(0f, habit.targetValue)

                    val now = LocalDateTime.now()
                    val today = now.toLocalDate().format(DateTimeFormatter.ISO_DATE)

                    // Guardar intensidad solo si llega al objetivo
                    if (clampedValue >= habit.targetValue) {

                        val intensity =
                            ((clampedValue / habit.targetValue) * 4).toInt().coerceIn(0, 4)

                        val time = now.toLocalTime().format(DateTimeFormatter.ISO_TIME)

                        scope.launch(Dispatchers.IO) {
                            StepByDatabase.getDatabase(context)
                                .habitActivityDao()
                                .insertActivity(
                                    HabitActivityEntity(
                                        habitId = habit.id,
                                        date = today,
                                        time = time,
                                        intensity = intensity
                                    )
                                )
                        }
                    } else {
                        scope.launch(Dispatchers.IO) {
                            StepByDatabase.getDatabase(context)
                                .habitActivityDao()
                                .deleteActivitiesForHabitForDay(habit.id, today)
                        }
                    }

                    onValueChange(clampedValue)
                    onDismiss()

                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}