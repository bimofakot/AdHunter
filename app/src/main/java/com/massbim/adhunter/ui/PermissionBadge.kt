/*
 * Badge izin berisiko yang dipakai bersama layar Audit Izin dan Pemantau.
 * Warna badge tetap sama agar arti tiap izin konsisten di kedua layar.
 */
package com.massbim.adhunter.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.massbim.adhunter.R
import com.massbim.adhunter.model.RiskyPermission

@Composable
internal fun PermissionBadge(permission: RiskyPermission) {
    val (containerColor, contentColor) = permissionColors(permission)
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = stringResource(permission.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun permissionColors(permission: RiskyPermission): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when (permission) {
        RiskyPermission.OVERLAY ->
            colors.errorContainer to colors.onErrorContainer

        RiskyPermission.ACCESSIBILITY ->
            colors.tertiaryContainer to colors.onTertiaryContainer

        RiskyPermission.DEVICE_ADMIN ->
            colors.secondaryContainer to colors.onSecondaryContainer

        RiskyPermission.NOTIFICATION_LISTENER ->
            colors.primaryContainer to colors.onPrimaryContainer
    }
}

internal fun RiskyPermission.labelRes(): Int = when (this) {
    RiskyPermission.OVERLAY -> R.string.permission_overlay
    RiskyPermission.ACCESSIBILITY -> R.string.permission_accessibility
    RiskyPermission.DEVICE_ADMIN -> R.string.permission_device_admin
    RiskyPermission.NOTIFICATION_LISTENER -> R.string.permission_notification_listener
}
