package com.mogars.stepby.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mogars.stepby.R
import com.mogars.stepby.data.LanguageSelector
import com.mogars.stepby.data.UserPreferences
import com.mogars.stepby.ui.theme.completeColor
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettigsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentNombre by UserPreferences.getUsername(context).collectAsState(initial = "")

    var editNombre by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(currentNombre) {
        if (editNombre.isBlank()) {
            editNombre = currentNombre.toString()
        }
    }
    //SnackBar
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                AnimatedVisibility(
                    visible = data.visuals.message.isNotEmpty(),
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    Snackbar(
                        snackbarData = data,
                        containerColor = completeColor,
                        contentColor = MaterialTheme.colorScheme.background,
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.configuracion)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.ArrowLeft,
                            contentDescription = null,
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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.peril),
                style = MaterialTheme.typography.titleLarge
            )
            //Tarjeta de nombre
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.nombre),
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                currentNombre ?: "",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = {
                                isEditing = true
                                editNombre = currentNombre ?: ""
                            }) {
                                Text(stringResource(R.string.cambiar))
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editNombre,
                                onValueChange = { editNombre = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.tu_nombre)) },
                                singleLine = true
                            )
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = {
                                    isEditing = false
                                }) {
                                    Text(stringResource(R.string.cancelar))
                                }
                                Spacer(Modifier.width(8.dp))

                                Button(
                                    enabled = editNombre.isNotBlank(),
                                    onClick = {
                                        keyboardController?.hide()
                                        scope.launch {
                                            UserPreferences.saveUsername(context, editNombre.trim())
                                            isEditing = false

                                            //SnackBar
                                            snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.nombre_guardado_correctamente),
                                                duration = SnackbarDuration.Short,
                                            )
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.guardar))
                                }
                            }
                        }
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.idioma),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(8.dp))
                        LanguageSelector()
                }
            }
        }

    }
}