package com.mogars.stepby.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mogars.stepby.ui.components.GoalType

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    //Check or amount
    @ColumnInfo(name = "goal_type")
    val goalType: GoalType,
    @ColumnInfo(name = "target_value")
    val targetValue: Float,
    @ColumnInfo(name = "current_value")
    val currentValue: Float = 0f,
    @ColumnInfo(name = "unit")
    val unit: String?,
    @ColumnInfo(name = "has_sub_habits")
    val hasSubHabits: Boolean,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false
)