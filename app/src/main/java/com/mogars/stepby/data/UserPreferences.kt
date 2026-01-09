package com.mogars.stepby.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPreferences {
    private val USER_NAME = stringPreferencesKey("user_name")

    fun getUsername(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[USER_NAME]
        }
    }

    suspend fun saveUsername(context: Context, name: String){
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
        }
    }
}