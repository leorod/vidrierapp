package com.vidrierapp.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vidrierapp.R

@Composable
fun LoginScreen(loginState: LoginUiState, onLogin: (String, String) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.login_description), modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.login_username)) },
            singleLine = true,
            enabled = !loginState.isLoading,
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            label = { Text(stringResource(R.string.login_password)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !loginState.isLoading,
        )
        loginState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        Button(
            onClick = { onLogin(username, password) },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            enabled = !loginState.isLoading,
        ) {
            if (loginState.isLoading) {
                CircularProgressIndicator(contentDescription = stringResource(R.string.login_loading))
            } else {
                Text(stringResource(R.string.login_submit))
            }
        }
    }
}
