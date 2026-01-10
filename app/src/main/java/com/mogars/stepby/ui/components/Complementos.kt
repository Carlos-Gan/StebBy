package com.mogars.stepby.ui.components

import SubHabitDao
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.room.TypeConverter
import com.mogars.stepby.R
import com.mogars.stepby.data.StepByDatabase
import com.mogars.stepby.data.entity.HabitActivityEntity
import com.mogars.stepby.ui.theme.completeColor
import com.mogars.stepby.ui.theme.principalColor
import com.mogars.stepby.ui.theme.secundarioColor
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Check
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class GoalType {
    CHECK,      // Marcar completado
    AMOUNT      //Ej. 2 litros, 30 min, 5km
}

class GoalTypeConverter {
    @TypeConverter
    fun fromGoalType(value: GoalType): String = value.name

    @TypeConverter
    fun toGoalType(value: String): GoalType = GoalType.valueOf(value)
}

// ---------------------- VIBRATION ----------------------

fun vibrate(context: Context, millis: Long = 30) {
    val vibratorManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    } else {
        null
    }

    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    vibrator?.let {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(
                millis,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        } else {
            null
        }

        if (effect != null) {
            it.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(millis)
        }
    }
}

// ---------------------- GREETING ----------------------

@Composable
fun GreetingHeader(username: String) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> stringResource(R.string.buenosdias)
        in 12..18 -> stringResource(R.string.buenastardes)
        else -> stringResource(R.string.buenasnoches)
    }

    Column(Modifier.padding(16.dp)) {
        Text(
            text = "$greeting, $username 👋",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.asi_va_tu_progreso),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ---------------------- CHECK BUTTON ----------------------
@Composable
fun CheckCircleButton(
    checked: Boolean,
    habitId: Long,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(
                2.dp,
                if (checked) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.outline,
                CircleShape
            )
            .background(
                if (checked) completeColor else MaterialTheme.colorScheme.background
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                vibrate(context, 35)
                onClick()

                val now = LocalDateTime.now()
                val date = now.toLocalDate().format(DateTimeFormatter.ISO_DATE)
                val time = now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))


                scope.launch(Dispatchers.IO) {
                    val db = StepByDatabase.getDatabase(context)
                    val habitdao = db.habitDao()

                    if (!checked) {
                        // Se marcó → Insertar intensidad 4
                        db.habitActivityDao().upsertActivity(
                            HabitActivityEntity(
                                habitId = habitId,
                                date = date,
                                intensity = 4,
                                time = time
                            )
                        )
                    } else {
                        // Se desmarcó → Borrar actividad
                        db.habitActivityDao().deleteActivitiesForHabitForDay(habitId, date)
                    }

                    habitdao.updateCompletionStatus(habitId)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                FontAwesomeIcons.Solid.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.background
            )
        }
    }
}

@Composable
fun SubHabitList(
    habitId: Long,
    subHabitDao: SubHabitDao,
    onChange: (Float) -> Unit // Actualiza progreso total del hábito
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subHabitsFlow = remember { subHabitDao.getSubHabitsForHabit(habitId) }
    val subHabits by subHabitsFlow.collectAsState(initial = emptyList())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        subHabits.forEach { sub ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = sub.isCompleted,
                    onCheckedChange = { checked ->
                        scope.launch(Dispatchers.IO) {
                            // 1️⃣ Actualizar el subhábito
                            subHabitDao.update(sub.copy(isCompleted = checked))

                            // 2️⃣ Calcular el nuevo conteo DESPUÉS de actualizar
                            val updatedCompletedCount = subHabits.count { it.isCompleted } +
                                    if (checked) 1 else -1
                            onChange(updatedCompletedCount.toFloat())

                            val db = StepByDatabase.getDatabase(context)
                            val habitDao = db.habitDao()
                            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

                            // ✅ ACTUALIZAR currentValue del hábito padre
                            habitDao.updateHabitValue(habitId, updatedCompletedCount.toFloat())

                            // 3️⃣ Guardar o borrar actividad según si TODOS están completos
                            if (updatedCompletedCount == subHabits.size && subHabits.isNotEmpty()) {
                                // ✅ TODOS los subhábitos completados → Guardar actividad
                                db.habitActivityDao()
                                    .upsertActivity(
                                        HabitActivityEntity(
                                            habitId = habitId,
                                            date = today,
                                            intensity = 4,
                                            time = time
                                        )
                                    )
                            } else {
                                // ❌ NO están todos completos → Borrar actividad
                                db.habitActivityDao()
                                    .deleteActivitiesForHabitForDay(habitId, today)
                            }

                            // 4️⃣ Actualizar estado de completitud del hábito padre
                            habitDao.updateCompletionStatus(habitId)
                        }
                    }
                )
                Text(
                    text = sub.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

// ---------------------- ROUNDED FIELD ----------------------
@Composable
fun RoundedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

// ---------------------- FORMATO A PM Y AM ----------------------

fun formatHourToAmPm(hour: Double): String {
    val h = hour.toInt()
    val minutes = ((hour - h) * 60).toInt()

    val amPm = if (h < 12) "AM" else "PM"
    val hour12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }

    return String.format("%d:%02d %s", hour12, minutes, amPm)
}