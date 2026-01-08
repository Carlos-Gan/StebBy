package com.mogars.stepby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mogars.stepby.ui.components.Routes
import com.mogars.stepby.ui.screens.AddHabitScreen
import com.mogars.stepby.ui.screens.HomeScreen
import com.mogars.stepby.ui.theme.StepByTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StepByTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddClick = {
                    navController.navigate(Routes.ADD_HABIT)
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.ADD_HABIT) {
            AddHabitScreen(
                onBack = { navController.popBackStack() },
            )
        }


        composable(Routes.SETTINGS) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pantalla de ajustes (pendiente)")
            }
        }
    }
}
