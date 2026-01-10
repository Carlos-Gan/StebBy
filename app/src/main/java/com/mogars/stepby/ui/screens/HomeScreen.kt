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
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.mogars.stepby.view_models.HabitViewModel
import com.mogars.stepby.view_models.HabitViewModelFactory
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

    // ✅ Usar ViewModel
    val viewModel: HabitViewModel = viewModel(
        factory = HabitViewModelFactory(database)
    )

    val username by UserPreferences.getUsername(context).collectAsState(initial = "")
    val habits by viewModel.allHabits.collectAsState(initial = emptyList())
    var selectedHabitForModal by remember { mutableStateOf<HabitUiModel?>(null) }
    var showSubHabitModal by remember { mutableStateOf(false) }

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
            GeneralHeatMap(habitActivityDao = database.habitActivityDao())

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

                                scope.launch(Dispatchers.IO) {
                                    val now = LocalDateTime.now()
                                    val today = now.toLocalDate().format(DateTimeFormatter.ISO_DATE)
                                    val time = now.toLocalTime()
                                        .format(DateTimeFormatter.ofPattern("HH:mm"))

                                    // Guardar o borrar actividad
                                    if (newValue >= habit.targetValue) {
                                        database.habitActivityDao().upsertActivity(
                                            HabitActivityEntity(
                                                habitId = habit.id,
                                                date = today,
                                                intensity = 4,
                                                time = time
                                            )
                                        )
                                    } else {
                                        database.habitActivityDao().deleteActivitiesForHabitForDay(habit.id, today)
                                    }

                                    // Actualizar valor y completitud
                                    viewModel.updateHabitValue(habit.id, newValue)
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

                scope.launch(Dispatchers.IO) {
                    try {
                        // Actualizar valor
                        viewModel.updateHabitValue(habitId, newValue)
                    } catch (e: Exception) {
                        Log.e("HomeScreen", "Error al guardar: ${e.message}", e)
                    }
                }
            }
        )
    }
}