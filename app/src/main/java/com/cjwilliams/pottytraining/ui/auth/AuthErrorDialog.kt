package com.cjwilliams.pottytraining.ui.auth

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cjwilliams.pottytraining.R
import com.cjwilliams.pottytraining.domain.AppError

@Composable
fun AuthErrorDialog(
    error: AppError,
    @StringRes titleRes: Int,
    @StringRes credentialsMessageRes: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = { Text(stringResource(error.messageRes(credentialsMessageRes))) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok_button))
            }
        }
    )
}

/**
 * Server wording never reaches the user: [AppError.Server] carries backend text that isn't
 * written for them. [credentialsMessageRes] differs by screen — a rejected sign in means bad
 * credentials, while a rejected sign up usually means the email is taken.
 */
@StringRes
private fun AppError.messageRes(@StringRes credentialsMessageRes: Int): Int = when (this) {
    is AppError.Auth -> credentialsMessageRes
    is AppError.Network -> R.string.error_network
    is AppError.Server, is AppError.NotSupported -> R.string.error_generic
}
