@file:Suppress("DEPRECATION")

package com.mogars.stepby.ui.screens

import com.mogars.stepby.data.entity.HabitActivityEntity
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mogars.stepby.R
import com.mogars.stepby.data.StepByDatabase
import com.mogars.stepby.data.UserPreferences
import com.mogars.stepby.data.entity.HabitEntity
import com.mogars.stepby.ui.components.home_screen.GeneralHeatMap
import com.mogars.stepby.ui.components.GreetingHeader
import com.mogars.stepby.ui.components.home_screen.HabitCard
import com.mogars.stepby.ui.components.home_screen.HabitDetailModal
import com.mogars.stepby.ui.models.HabitUiModel
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ---------------------- HOME ----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = StepByDatabase.getDatabase(context)
    val habitDao = database.habitDao()
    val activityDao = database.habitActivityDao()

    val username by UserPreferences.getUsername(context).collectAsState(initial = "")
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

            GreetingHeader(username.toString())
            GeneralHeatMap(habitActivityDao = activityDao)

            Spacer(Modifier.height(12.dp))

            Text(
                stringResource(R.string.tus_habitos),
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

                                val now = LocalDateTime.now()
                                val today = now.toLocalDate().format(DateTimeFormatter.ISO_DATE)
                                val time = now.toLocalTime()
                                    .format(DateTimeFormatter.ofPattern("HH:mm"))

                                scope.launch(Dispatchers.IO) {
                                    if (newValue >= habit.targetValue) {
                                        activityDao.insertActivity(
                                            HabitActivityEntity(
                                                habitId = habit.id,
                                                date = today,
                                                intensity = 4,
                                                time = time
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
                            },
                            onOpenLongPress = { habit ->
                                navController.navigate("habit_description/${habit.id}")

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
                        stringResource(R.string.no_habitos_creados),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(15.dp))
                    Text(
                        stringResource(R.string.empieza_crear_nuevo),
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

                val index = habits.indexOfFirst { it.id == habitId }
                if (index >= 0) {

                    val updatedHabit = habits[index].copy(currentValue = newValue)
                    habits[index] = updatedHabit

                    val now = LocalDateTime.now()
                    val today = now.toLocalDate().format(DateTimeFormatter.ISO_DATE)
                    val time = now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))


                    scope.launch(Dispatchers.IO) {
                        try {
                            val realHabitId = updatedHabit.id

                            if (newValue >= updatedHabit.targetValue) {
                                activityDao.insertActivity(
                                    HabitActivityEntity(
                                        habitId = realHabitId,
                                        date = today,
                                        intensity = 4,
                                        time = time
                                    )
                                )
                            } else {
                                activityDao.deleteActivitiesForHabitForDay(realHabitId, today)
                            }

                            habitDao.updateHabit(
                                HabitEntity(
                                    id = realHabitId,
                                    name = updatedHabit.name,
                                    goalType = updatedHabit.goalType,
                                    targetValue = updatedHabit.targetValue,
                                    currentValue = newValue,
                                    unit = updatedHabit.unit ?: "",
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