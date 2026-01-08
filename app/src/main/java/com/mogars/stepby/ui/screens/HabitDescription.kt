package com.mogars.stepby.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mogars.stepby.data.StepByDatabase
import com.mogars.stepby.ui.components.habit_description.HabitHeatMap
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HabitDescriptionScreen(
    onBack: () -> Unit,
    habitId: Long
) {
    val context = LocalContext.current
    val habitDao = StepByDatabase.getDatabase(context).habitDao()

    val habit by habitDao.getHabitById(habitId)
        .collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habito ${habit?.name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            FontAwesomeIcons.Solid.ArrowLeft,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Text(
                "HeatMap del hábito",
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMediumEmphasized
            )
            Spacer(Modifier.height(10.dp))
            HabitHeatMap(
                habitId = habitId,
                habitActivityDao = StepByDatabase.getDatabase(context).habitActivityDao()
            )
            Text(
                "Descripción del hábito",
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }

    }
}