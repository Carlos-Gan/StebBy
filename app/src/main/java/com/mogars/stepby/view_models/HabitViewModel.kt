package com.mogars.stepby.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mogars.stepby.data.StepByDatabase
import com.mogars.stepby.data.entity.HabitEntity
import com.mogars.stepby.ui.models.HabitUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HabitViewModel(
    private val database: StepByDatabase
) : ViewModel() {

    //Flow de todos los habitos mapeados en UiModel
    val allHabits: Flow<List<HabitUiModel>> = database.habitDao().getAllHabits()
        .map { habitEntities ->
            habitEntities.map { it.toUiModel() }
        }

    //Mapear Entity a UiModel
    private fun HabitEntity.toUiModel(): HabitUiModel {
        return HabitUiModel(
            id = id,
            name = name,
            goalType = goalType,
            currentValue = currentValue,
            targetValue = targetValue,
            unit = unit,
            hasSubHabits = hasSubHabits,
            isCompleted = isCompleted
        )
    }

    //Actualiza valor del habito
    fun updateHabitValue(habitId: Long, newValue: Float) {
        viewModelScope.launch {
            database.habitDao().updateHabitValue(habitId, newValue)
            database.habitDao().updateCompletionStatus(habitId)
        }
    }

    //Actualizar completitud
    fun updateCompletionStatus(habitId: Long) {
        viewModelScope.launch {
            database.habitDao().updateCompletionStatus(habitId)
        }
    }

    //Insertar nuevo habito
    fun insertHabit(habit: HabitEntity) {
        viewModelScope.launch {
            database.habitDao().insertHabit(habit)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            database.habitDao().deleteHabit(habit)
        }
    }
}