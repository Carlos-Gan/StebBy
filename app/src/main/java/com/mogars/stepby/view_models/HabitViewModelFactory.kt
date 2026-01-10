package com.mogars.stepby.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mogars.stepby.data.StepByDatabase

class HabitViewModelFactory(private val database: StepByDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}