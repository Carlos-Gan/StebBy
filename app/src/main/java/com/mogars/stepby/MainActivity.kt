package com.mogars.stepby

import android.R.attr.type
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mogars.stepby.data.UserPreferences
import com.mogars.stepby.ui.components.Routes
import com.mogars.stepby.ui.screens.AddHabitScreen
import com.mogars.stepby.ui.screens.HabitDescriptionScreen
import com.mogars.stepby.ui.screens.HomeScreen
import com.mogars.stepby.ui.screens.SettigsScreen
import com.mogars.stepby.ui.theme.StepByTheme


class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        AppCompatDelegate.setApplicationLocales(
            AppCompatDelegate.getApplicationLocales()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StepByTheme {
                val context = LocalContext.current
                val username by UserPreferences.getUsername(context).collectAsState(initial = null)

                if (username == null) {
                    WelcomeScreen()
                } else {
                    AppNavigation()
                }
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
                },
                navController = navController
            )
        }

        composable(Routes.ADD_HABIT) {
            AddHabitScreen(
                onBack = { navController.popBackStack() },
            )
        }


        composable(Routes.SETTINGS) {
            SettigsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.HABIT_DESCRIPTION,
            arguments = listOf(navArgument("habitId") { type = NavType.LongType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: return@composable

            HabitDescriptionScreen(
                onBack = { navController.popBackStack() },
                habitId = habitId
            )
        }
    }
}
