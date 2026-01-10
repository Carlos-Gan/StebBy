package com.mogars.stepby.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mogars.stepby.data.entity.HabitActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitActivityDao {

    // Insertar o actualizar una actividad del hábito
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: HabitActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivity(activity: HabitActivityEntity)


    // Obtener todas las actividades de un hábito, ordenadas por fecha
    @Query("SELECT * FROM habit_activity WHERE habit_id = :habitId ORDER BY date ASC")
    fun getActivitiesForHabit(habitId: Long): Flow<List<HabitActivityEntity>>

    // Obtener la actividad de un hábito para un día específico
    @Query("SELECT * FROM habit_activity WHERE habit_id = :habitId AND date = :date LIMIT 1")
    suspend fun getActivityForDay(habitId: Long, date: String): HabitActivityEntity?

    // Eliminar todas las actividades de un hábito (opcional)
    @Query("DELETE FROM habit_activity WHERE habit_id = :habitId")
    suspend fun deleteActivitiesForHabit(habitId: Long)

    @Query("SELECT * FROM habit_activity ORDER BY date ASC")
    fun getAllActivities(): Flow<List<HabitActivityEntity>>

    @Query("DELETE FROM habit_activity WHERE habit_id = :habitId AND date = :date")
    suspend fun deleteActivitiesForHabitForDay(habitId: Long, date: String)

    @Query("DELETE FROM habit_activity WHERE habit_id = :habitId AND date = :date AND subHabitIndex = :index")
    suspend fun deleteSubHabit(habitId: Long, date: String, index: Int)
}
