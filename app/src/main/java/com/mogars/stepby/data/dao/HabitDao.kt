package com.mogars.stepby.data.dao

import androidx.room.*
import com.mogars.stepby.data.entity.HabitEntity
import com.mogars.stepby.data.entity.SubHabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Insert
    suspend fun insertHabit(habit: HabitEntity): Long

    @Insert
    suspend fun insertSubHabits(subHabits: List<SubHabitEntity>)

    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM sub_habits WHERE habit_id = :habitId")
    suspend fun getSubHabits(habitId: Long): List<SubHabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun getHabitById(id: Long): Flow<HabitEntity?>

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Update
    suspend fun updateSubHabit(subHabit: SubHabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)
}
