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

    /**
     * Actualiza el estado de completitud del hábito basado en:
     * - Si tiene subhábitos: completo si TODOS están marcados
     * - Si es AMOUNT: completo si currentValue >= targetValue
     * - Si es CHECK: completo si currentValue >= targetValue
     */
    @Transaction
    suspend fun updateCompletionStatus(habitId: Long) {
        val habit = getHabitByIdSuspend(habitId) ?: return

        val isCompleted = when {
            habit.hasSubHabits -> {
                val subHabits = getSubHabits(habitId)
                val totalCount = subHabits.size
                val completedCount = subHabits.count { it.isCompleted }
                totalCount > 0 && completedCount == totalCount
            }
            else -> habit.currentValue >= habit.targetValue
        }

        updateHabit(habit.copy(isCompleted = isCompleted))
    }

    //Versión suspendible de getHabitById para usar dentro de transacciones
    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitByIdSuspend(id: Long): HabitEntity?

    //Actualiza solo el currentValue y luego recalcula completitud
    suspend fun updateHabitValue(habitId: Long, newValue: Float) {
        val habit = getHabitByIdSuspend(habitId) ?: return
        updateHabit(habit.copy(currentValue = newValue))
        updateCompletionStatus(habitId)
    }

    //Marca todos los subhábitos como completados y actualiza el hábito padre
    @Transaction
    suspend fun completeAllSubHabits(habitId: Long) {
        val subHabits = getSubHabits(habitId)
        subHabits.forEach { subHabit ->
            updateSubHabit(subHabit.copy(isCompleted = true))
        }
        updateCompletionStatus(habitId)
    }
     //Desmarca todos los subhábitos y actualiza el hábito padre
    @Transaction
    suspend fun resetSubHabits(habitId: Long) {
        val subHabits = getSubHabits(habitId)
        subHabits.forEach { subHabit ->
            updateSubHabit(subHabit.copy(isCompleted = false))
        }
        updateCompletionStatus(habitId)
    }
}