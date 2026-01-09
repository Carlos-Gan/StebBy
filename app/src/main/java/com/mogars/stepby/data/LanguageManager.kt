package com.mogars.stepby.data

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.mogars.stepby.R
import java.util.Locale

object LanguageManager {
    private const val TAG = "LanguageManager"
    private const val LANGUAGE_PREF = "selected_language"
    private const val SHOW_SNACKBAR_PREF = "show_language_snackbar"
    private const val LANGUAGE_NAME_PREF = "language_name"

    fun setLanguage(context: Context, languageCode: String, languageName: String) {
        try {
            Log.d(TAG, "=== Iniciando cambio de idioma a: $languageCode ===")

            val locale = Locale(languageCode)
            Log.d(TAG, "Locale creado: $locale")
            Locale.setDefault(locale)

            // Guardar preferencias
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString(LANGUAGE_PREF, languageCode)
                putString(LANGUAGE_NAME_PREF, languageName)
                putBoolean(SHOW_SNACKBAR_PREF, true) // Bandera para mostrar Snackbar
                apply()
            }
            Log.d(TAG, "Preferencias guardadas")

            // Aplicar locale a nivel de sistema
            val config = Configuration()
            config.locale = locale

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Log.d(TAG, "Usando API 33+ (setApplicationLocales)")
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.create(locale)
                )
            } else {
                Log.d(TAG, "Usando API < 33 (updateConfiguration)")
                context.resources.updateConfiguration(config, context.resources.displayMetrics)
            }

            Log.d(TAG, "Locale aplicado: ${Locale.getDefault()}")

            // Recrear Activity
            if (context is Activity) {
                Log.d(TAG, "Recreando Activity...")
                context.recreate()
                Log.d(TAG, "Activity recreada")
            } else {
                Log.w(TAG, "Context no es una Activity, no se puede recrear")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al cambiar idioma: ${e.message}", e)
            e.printStackTrace()
        }
    }

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString(LANGUAGE_PREF, "es") ?: "es"
        Log.d(TAG, "Idioma guardado recuperado: $saved")
        return saved
    }

    fun shouldShowSnackbar(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(SHOW_SNACKBAR_PREF, false)
    }

    fun getSavedLanguageName(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString(LANGUAGE_NAME_PREF, "Español") ?: "Español"
    }

    fun clearSnackbarFlag(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(SHOW_SNACKBAR_PREF, false).apply()
    }

    fun applySavedLanguage(context: Context) {
        val savedLang = getSavedLanguage(context)
        Log.d(TAG, "Aplicando idioma guardado: $savedLang")

        val locale = Locale.Builder()
            .setLanguageTag(savedLang)
            .build()
        Locale.setDefault(locale)

        val config = Configuration()
        config.locale = locale
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.create(locale)
            )
        }
    }
}

data class AppLanguage(
    val name: String,
    val code: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    snackbarHostState: SnackbarHostState
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val languages = listOf(
        AppLanguage("Español", "es"),
        AppLanguage("English", "en")
    )

    val currentLang = LanguageManager.getSavedLanguage(context)
    Log.d("LanguageSelector", "Idioma actual: $currentLang")

    var selected by remember {
        mutableStateOf(
            languages.find { it.code == currentLang } ?: languages.first()
        )
    }

    LaunchedEffect(Unit) {
        if (LanguageManager.shouldShowSnackbar(context)) {
            val languageName = LanguageManager.getSavedLanguageName(context)
            snackbarHostState.showSnackbar(
                message  = context.getString(R.string.idioma_cambiado_a, languageName),
                duration = SnackbarDuration.Short,
            )
            LanguageManager.clearSnackbarFlag(
                context
            )
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
                .fillMaxWidth(),
            readOnly = true,
            value = selected.name,
            onValueChange = {},
            label = { Text(stringResource(R.string.idioma)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.name) },
                    onClick = {
                        Log.d("LanguageSelector", "Cambiar a: ${lang.code}")
                        selected = lang
                        expanded = false
                        LanguageManager.setLanguage(context, lang.code, lang.name)
                    }
                )
            }
        }
    }
}