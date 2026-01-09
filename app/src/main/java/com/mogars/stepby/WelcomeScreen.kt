package com.mogars.stepby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mogars.stepby.data.UserPreferences
import com.mogars.stepby.ui.components.AnimatedBorderTextField
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen() {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.bienvenido_a_stebby),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.como_te_llamas))
            Spacer(Modifier.height(12.dp))
            AnimatedBorderTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                singleLine = false,
                maxLines = 1,
                gradient = Brush.sweepGradient(
                    listOf(Color.Green, Color.Blue, Color.Red, Color.Green)
                ),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        UserPreferences.saveUsername(context, name)
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text(
                    stringResource(R.string.continuar),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}