package com.mogars.stepby.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.mogars.stepby.data.entity.HabitEntity
import com.mogars.stepby.data.entity.SubHabitEntity
import com.mogars.stepby.ui.components.GoalType
import com.mogars.stepby.ui.components.RoundedField
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import compose.icons.fontawesomeicons.solid.Check
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.Trash
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = StepByDatabase.getDatabase(context)
    val habitDao = database.habitDao()

    var habitName by remember { mutableStateOf("") }
    var hasSubHabits by remember { mutableStateOf(false) }
    var subHabits by remember { mutableStateOf(listOf("")) }
    var goalType by remember { mutableStateOf(GoalType.CHECK) }
    var targetValue by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo hábito") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.ArrowLeft,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            TextButton(
                onClick = {
                    if (habitName.isNotBlank()) {
                        scope.launch {
                            try {
                                // Crear el hábito
                                val habitEntity = HabitEntity(
                                    name = habitName,
                                    goalType = goalType,
                                    targetValue = targetValue.toFloatOrNull() ?: 1f,
                                    unit = unit.ifBlank { null },
                                    hasSubHabits = hasSubHabits
                                )

                                val habitId = habitDao.insertHabit(habitEntity)

                                // Si tiene subhábitos, insertarlos
                                if (hasSubHabits && habitId > 0) {
                                    val cleanSubHabits = subHabits.filter { it.isNotBlank() }
                                    val subHabitEntities = cleanSubHabits.map { subHabitName ->
                                        SubHabitEntity(
                                            habitId = habitId,
                                            name = subHabitName,
                                            isCompleted = false
                                        )
                                    }
                                    habitDao.insertSubHabits(subHabitEntities)
                                }
                                // Volver a la pantalla anterior
                                onBack()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                },
                enabled = !isLoading && habitName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardando...")
                } else {
                    Icon(
                        FontAwesomeIcons.Solid.Check,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear hábito")
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 📛 Nombre del hábito
            RoundedField(
                value = habitName,
                onValueChange = { habitName = it },
                label = "Nombre del hábito"
            )

            // 🎯 Tipo de objetivo
            Text("Tipo de hábito", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                FilterChip(
                    selected = goalType == GoalType.CHECK,
                    onClick = { goalType = GoalType.CHECK },
                    label = { Text("Completar") }
                )

                FilterChip(
                    selected = goalType == GoalType.AMOUNT,
                    onClick = { goalType = GoalType.AMOUNT },
                    label = { Text("Cantidad") }
                )
            }

            // 🔢 Si es por cantidad
            if (goalType == GoalType.AMOUNT) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                    RoundedField(
                        value = targetValue,
                        onValueChange = { newValue ->
                            // Permitir dígitos y un punto decimal
                            val filtered = newValue.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split('.')
                            targetValue = if (parts.size <= 2) {
                                filtered
                            } else {
                                parts[0] + "." + parts.drop(1).joinToString("")
                            }
                        },
                        label = "Cantidad",
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal
                    )

                    RoundedField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = "Unidad",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 🧩 Switch subhábitos
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Tiene subhábitos?",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = hasSubHabits,
                    onCheckedChange = { hasSubHabits = it }
                )
            }

            // Lista de subhábitos
            if (hasSubHabits) {

                Text(
                    text = "Subhábitos",
                    style = MaterialTheme.typography.titleMedium
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {

                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {

                        items(subHabits.size) { index ->
                            val value = subHabits[index]

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                RoundedField(
                                    value = value,
                                    onValueChange = { newText ->
                                        subHabits = subHabits.toMutableList().also {
                                            it[index] = newText
                                        }
                                    },
                                    label = "Subhábito ${index + 1}",
                                    modifier = Modifier.weight(1f)
                                )

                                if (subHabits.size > 1) {
                                    IconButton(
                                        onClick = {
                                            subHabits = subHabits.toMutableList().also {
                                                it.removeAt(index)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = FontAwesomeIcons.Solid.Trash,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            TextButton(
                                onClick = { subHabits = subHabits + "" }
                            ) {
                                Icon(
                                    FontAwesomeIcons.Solid.Plus,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Agregar subhábito")
                            }
                        }
                    }
                }
            }
        }
    }
}