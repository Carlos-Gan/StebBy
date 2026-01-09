package com.mogars.stepby.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.mogars.stepby.R
import java.util.Locale

object LanguageManager {
    fun setLanguage(context: Context, languageCode: String) {
        val locale = Locale.forLanguageTag(languageCode)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.create(locale)
        )

    }
}

data class AppLanguage(
    val name: String,
    val code: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector() {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val languages = listOf(
        AppLanguage("Español", "es"),
        AppLanguage("English", "en")
    )

    val currentLang =
        AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "es"

    var selected by remember {
        mutableStateOf(
            languages.find { it.code == currentLang } ?: languages.first()
        )
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
                        selected = lang
                        expanded = false
                        LanguageManager.setLanguage(context, lang.code)

                        (context as Activity).recreate()
                    }
                )
            }
        }
    }
}