package com.raival.compose.file.explorer.screen.viewer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.raival.compose.file.explorer.R

@Composable
fun ConversionErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    onOpenExternal: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(R.string.conversion_failed_retry))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        },
        dismissButton = {
            if (onOpenExternal != null) {
                TextButton(onClick = onOpenExternal) {
                    Text(text = stringResource(R.string.conversion_failed_open_in))
                }
            } else if (onRetry != null) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        },
        title = { Text(text = stringResource(R.string.conversion_failed)) },
        text = {
            Text(
                text = message.ifBlank {
                    stringResource(R.string.conversion_failed_msg)
                }
            )
        }
    )
}
