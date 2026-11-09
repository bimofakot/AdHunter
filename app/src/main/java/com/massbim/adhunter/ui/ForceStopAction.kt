/*
 * Tombol "hentikan paksa" yang aman.
 * Android tidak mengizinkan app biasa menghentikan proses app lain, jadi tombol
 * ini membuka halaman Info App dan pengguna menekan "Hentikan paksa" sendiri.
 */
package com.massbim.adhunter.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.massbim.adhunter.R

@Composable
fun ForceStopAction(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val context = LocalContext.current
    var confirming by remember(packageName) { mutableStateOf(false) }

    TextButton(
        onClick = { confirming = true },
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.force_stop_action),
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.force_stop_title, appName)) },
            text = { Text(stringResource(R.string.force_stop_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        openAppInfo(context, packageName)
                    },
                ) {
                    Text(stringResource(R.string.force_stop_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(R.string.force_stop_cancel))
                }
            },
        )
    }
}

private fun openAppInfo(context: Context, packageName: String) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
