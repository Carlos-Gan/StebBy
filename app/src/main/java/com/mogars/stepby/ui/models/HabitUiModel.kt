package com.mogars.stepby.ui.models

import com.mogars.stepby.ui.components.GoalType

data class HabitUiModel(
    val id: Long,
    val name: String,
    val goalType: GoalType,
    val currentValue: Float,
    val targetValue: Float,
    val unit: String? = null,
    val hasSubHabits: Boolean = false,
    val isCompleted: Boolean = false
    )

data class SubHabitUiModel(
    val id: Long = 0,
    val habitId: Long,
    val name: String,
    val isCompleted: Boolean = false
)