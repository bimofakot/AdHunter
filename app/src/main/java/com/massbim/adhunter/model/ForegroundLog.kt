/*
 * Model log aplikasi yang tampil di layar.
 * Entri disimpan di perangkat dan dipetakan ke data risk scanner K1 saat ditampilkan.
 */
package com.massbim.adhunter.model

import android.graphics.Bitmap

data class ForegroundLogEntry(
    val id: Long,
    val timestamp: Long,
    val packageName: String,
    val appName: String,
    val activityName: String?,
)

data class MonitorLogItem(
    val entry: ForegroundLogEntry,
    val icon: Bitmap?,
    val riskyPermissions: Set<RiskyPermission>,
    val hiddenFromLauncher: Boolean,
) {
    val isHighlighted: Boolean
        get() = riskyPermissions.isNotEmpty() || hiddenFromLauncher
}
