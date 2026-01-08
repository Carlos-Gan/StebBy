package com.mogars.stepby.ui.components

import SubHabitDao
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.room.TypeConverter
import com.mogars.stepby.R
import com.mogars.stepby.data.StepByDatabase
import com.mogars.stepby.data.entity.HabitActivityEntity
import com.mogars.stepby.ui.theme.completeColor
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Check
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator

        vibrator.vibrate(
            VibrationEffect.createOneShot(
                millis,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    } else {
        // Android 11 o menos
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    millis,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(millis)
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
            text = "Así va tu progreso:",
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


                scope.launch {
                    val db = StepByDatabase.getDatabase(context)

                    if (!checked) {
                        // Se marcó → Insertar intensidad 4
                        db.habitActivityDao().insertActivity(
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

// ---------------------- SUBHABIT LIST ----------------------
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
                        scope.launch {
                            subHabitDao.update(sub.copy(isCompleted = checked))
                            val updatedCompletedCount = subHabits.count { it.isCompleted } +
                                    if (checked) 1 else 0
                            onChange(updatedCompletedCount.toFloat())

                            // Guardar intensidad solo si todos los subhábitos completados
                            if (updatedCompletedCount == subHabits.size && subHabits.isNotEmpty()) {
                                val today = LocalDate.now()
                                    .format(DateTimeFormatter.ISO_DATE)
                                StepByDatabase.getDatabase(context)
                                    .habitActivityDao()
                                    .insertActivity(
                                        HabitActivityEntity(
                                            habitId = habitId,
                                            date = today,
                                            intensity = 4,
                                            time = LocalTime.now()
                                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                                        )
                                    )
                            }
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

