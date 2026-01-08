@file:Suppress("DEPRECATION")

package com.mogars.stepby.ui.screens

import SubHabitDao
import com.mogars.stepby.data.entity.HabitActivityEntity
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mogars.stepby.R
import com.mogars.stepby.data.StepByDatabase
import com.mogars.stepby.data.dao.HabitActivityDao
import com.mogars.stepby.data.entity.HabitEntity
import com.mogars.stepby.ui.components.CheckCircleButton
import com.mogars.stepby.ui.components.GoalType
import com.mogars.stepby.ui.components.GreetingHeader
import com.mogars.stepby.ui.components.SubHabitList
import com.mogars.stepby.ui.components.vibrate
import com.mogars.stepby.ui.models.HabitUiModel
import com.mogars.stepby.ui.theme.completeColor
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ---------------------- HOME ----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = StepByDatabase.getDatabase(context)
    val habitDao = database.habitDao()
    val activityDao = database.habitActivityDao()

    val username = "Carlos"
    val habits = remember { mutableStateListOf<HabitUiModel>() }
    var selectedHabitForModal by remember { mutableStateOf<HabitUiModel?>(null) }
    var showSubHabitModal by remember { mutableStateOf(false) }

    // 🔄 Cargar hábitos desde BD
    LaunchedEffect(Unit) {
        habitDao.getAllHabits().collect { habitEntities ->
            val habitUiModels = habitEntities.map { entity ->
                HabitUiModel(
                    id = entity.id,
                    name = entity.name,
                    goalType = entity.goalType,
                    currentValue = entity.currentValue,
                    targetValue = entity.targetValue,
                    unit = entity.unit,
                    hasSubHabits = entity.hasSubHabits
                )
            }
            habits.clear()
            habits.addAll(habitUiModels)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onAddClick) {
                        Icon(FontAwesomeIcons.Solid.Plus, null, modifier = Modifier.size(16.dp))
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(FontAwesomeIcons.Solid.Tools, null, modifier = Modifier.size(16.dp))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            GreetingHeader(username)
            GeneralHeatMap(habitActivityDao = activityDao)

            Spacer(Modifier.height(12.dp))

            Text(
                "Tus hábitos",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge
            )

            if (habits.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(habits.size) { index ->
                        val habit = habits[index]

                        HabitCard(
                            habit = habit,
                            onCheckClick = {
                                // 🔹 Actualizar currentValue según check
                                val newValue =
                                    if (habit.currentValue >= habit.targetValue) 0f else habit.targetValue
                                habits[index] = habit.copy(currentValue = newValue)

                                val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                                scope.launch(Dispatchers.IO) {
                                    if (newValue >= habit.targetValue) {
                                        activityDao.insertActivity(
                                            HabitActivityEntity(
                                                habitId = habit.id,
                                                date = today,
                                                intensity = 4
                                            )
                                        )
                                    } else {
                                        activityDao.deleteActivitiesForHabitForDay(habit.id, today)
                                    }
                                    // 🔹 Guardar en HabitEntity
                                    habitDao.updateHabit(
                                        HabitEntity(
                                            id = habit.id,
                                            name = habit.name,
                                            goalType = habit.goalType,
                                            targetValue = habit.targetValue,
                                            currentValue = newValue,
                                            unit = habit.unit,
                                            hasSubHabits = habit.hasSubHabits
                                        )
                                    )
                                }
                            },
                            onOpen = {
                                selectedHabitForModal = habit
                                showSubHabitModal = true
                            }
                        )
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        "No tienes hábitos creados.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(15.dp))
                    Text(
                        "Empieza por crear uno nuevo.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 🔹 Modal para subhabits / amount
    if (showSubHabitModal && selectedHabitForModal != null) {
        HabitDetailModal(
            habit = selectedHabitForModal!!,
            onDismiss = {
                showSubHabitModal = false
                selectedHabitForModal = null
            },
            onValueChange = { newValue ->
                val habitId = selectedHabitForModal?.id ?: return@HabitDetailModal
                val targetValue = selectedHabitForModal?.targetValue ?: return@HabitDetailModal

                val index = habits.indexOfFirst { it.id == habitId }
                if (index >= 0) {
                    val updatedHabit = habits[index].copy(currentValue = newValue)
                    habits[index] = updatedHabit

                    val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    scope.launch(Dispatchers.IO) {
                        try {
                            if (newValue >= targetValue) {
                                val intensity = ((newValue / targetValue) * 4)
                                    .toInt().coerceIn(0, 4)
                                activityDao.insertActivity(
                                    HabitActivityEntity(
                                        habitId = habitId,
                                        date = today,
                                        intensity = intensity
                                    )
                                )
                            } else {
                                activityDao.deleteActivitiesForHabitForDay(habitId, today)
                            }
                            habitDao.updateHabit(
                                HabitEntity(
                                    id = updatedHabit.id,
                                    name = updatedHabit.name,
                                    goalType = updatedHabit.goalType,
                                    targetValue = updatedHabit.targetValue,
                                    currentValue = updatedHabit.currentValue,
                                    unit = updatedHabit.unit,
                                    hasSubHabits = updatedHabit.hasSubHabits
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Error al guardar: ${e.message}", e)
                        }
                    }
                }
            }
        )
    }
}


// ---------------------- HABIT DETAIL MODAL ----------------------

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
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            "km" -> listOf(1, 5, 10)
                            "ml" -> listOf(250, 500, 750, 1000)
                            "pág" -> listOf(10, 20, 30)
                            else -> listOf(
                                habit.targetValue / 4,
                                habit.targetValue / 2,
                                habit.targetValue
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                suggestions.forEach { value ->
                                    Button(
                                        onClick = { inputValue = value.toString() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if ((inputValue.toFloatOrNull()
                                                    ?: 0f) == value.toFloat()
                                            )
                                                completeColor
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Text("$value ${habit.unit ?: ""}")
                                    }
                                }
                            }

                            Divider()
                            Text(
                                "O ingresa un valor personalizado:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valueFloat = inputValue.toFloatOrNull() ?: habit.currentValue
                    val clampedValue = valueFloat.coerceIn(0f, habit.targetValue)

                    // Guardar intensidad solo si llega al objetivo
                    if (clampedValue >= habit.targetValue) {
                        val intensity =
                            ((clampedValue / habit.targetValue) * 4).toInt().coerceIn(0, 4)
                        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                        scope.launch(Dispatchers.IO) {
                            StepByDatabase.getDatabase(context)
                                .habitActivityDao()
                                .insertActivity(
                                    HabitActivityEntity(
                                        habitId = habit.id,
                                        date = today,
                                        intensity = intensity
                                    )
                                )
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

@Composable
fun HabitCard(
    habit: HabitUiModel,
    onCheckClick: (Boolean) -> Unit, // ahora pasamos si está completo
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subHabitDao = StepByDatabase.getDatabase(context).subHabitDao()

    // Flow de subhábitos si existen
    val subHabitsFlow =
        if (habit.hasSubHabits) remember { subHabitDao.getSubHabitsForHabit(habit.id) } else null
    val subHabits by subHabitsFlow?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    // Calcular progreso y completitud
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

    // Marcar solo si todos los subhábitos están completos
    val isCompleted = when {
        habit.goalType == GoalType.AMOUNT -> habit.currentValue >= habit.targetValue
        habit.hasSubHabits -> habit.currentValue >= habit.targetValue
        else -> progress >= 1f
    }

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
            .height(72.dp),
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

                // Click solo para abrir detalles
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            vibrate(context, 20)
                            onOpen()
                        }
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
                                "$completedSubCount / $totalSub subhábitos completados"

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
                                // Cambiar estado manualmente si es hábito simple
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
        Text("Actividad total últimos 28 días", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(130.dp)
        ) {
            items(days.size) { index ->
                val totalIntensity = days[index]

                // Colores según la intensidad general
                val color = when {
                    totalIntensity == 0 -> Color(0xFFEBEDF0)
                    totalIntensity in 1..2 -> Color(0xFF9BE9A8)
                    totalIntensity in 3..5 -> Color(0xFF40C463)
                    totalIntensity in 6..9 -> Color(0xFF30A14E)
                    totalIntensity >= 10 -> Color(0xFF216E39)
                    else -> Color(0xFFEBEDF0)
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
