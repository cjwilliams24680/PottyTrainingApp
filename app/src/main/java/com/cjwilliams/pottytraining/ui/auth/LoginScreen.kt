package com.cjwilliams.pottytraining.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cjwilliams.pottytraining.R

@Composable
fun LoginScreen(
    onCreateAccount: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        EmailField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            labelRes = R.string.password_label,
            onImeAction = viewModel::submit
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = viewModel::submit,
            enabled = uiState.canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.sign_in_button))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onCreateAccount,
            enabled = !uiState.isSubmitting
        ) {
            Text(stringResource(R.string.create_account_action))
        }

        TextButton(
            onClick = {
                Toast.makeText(
                    context,
                    R.string.forgot_password_not_implemented,
                    Toast.LENGTH_SHORT
                ).show()
            }
        ) {
            Text(stringResource(R.string.forgot_password_action))
        }
    }

    uiState.error?.let { error ->
        AuthErrorDialog(
            error = error,
            titleRes = R.string.login_failed_title,
            credentialsMessageRes = R.string.error_invalid_credentials,
            onDismiss = viewModel::dismissError
        )
    }
}
