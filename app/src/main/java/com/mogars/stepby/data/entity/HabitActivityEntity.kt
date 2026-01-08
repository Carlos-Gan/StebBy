package com.mogars.stepby.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habit_activity",
    indices = [Index(value =
        ["habit_id", "date"], unique = true)]
)
data class HabitActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "habit_id")
    val habitId: Long,
    val date: String,
    val intensity: Int,
    val subHabitIndex: Int? = null
)