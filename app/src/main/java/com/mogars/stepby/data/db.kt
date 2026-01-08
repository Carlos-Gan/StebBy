package com.mogars.stepby.data

import SubHabitDao
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mogars.stepby.data.dao.HabitActivityDao
import com.mogars.stepby.data.dao.HabitDao
import com.mogars.stepby.data.entity.HabitActivityEntity
import com.mogars.stepby.data.entity.HabitEntity
import com.mogars.stepby.data.entity.SubHabitEntity
import com.mogars.stepby.ui.components.GoalTypeConverter

@Database(
    entities = [HabitEntity::class, SubHabitEntity::class ,HabitActivityEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(GoalTypeConverter::class)
abstract class StepByDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitActivityDao(): HabitActivityDao
    abstract fun subHabitDao(): SubHabitDao


    companion object {
        @Volatile
        private var INSTANCE: StepByDatabase? = null

        fun getDatabase(context: Context): StepByDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StepByDatabase::class.java,
                    "step_by_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

