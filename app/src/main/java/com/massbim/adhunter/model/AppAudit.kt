/*
 * Model hasil audit untuk satu aplikasi terpasang.
 * Model menyimpan Bitmap agar lapisan data tidak bergantung pada Compose.
 */
package com.massbim.adhunter.model

import android.graphics.Bitmap

enum class RiskyPermission {
    OVERLAY,
    ACCESSIBILITY,
    DEVICE_ADMIN,
    NOTIFICATION_LISTENER,
}

enum class InstallerSource {
    PLAY_STORE,
    SIDELOAD,
    OTHER,
}

data class AppAuditEntry(
    val packageName: String,
    val appName: String,
    val icon: Bitmap?,
    val riskyPermissions: Set<RiskyPermission>,
    val installer: InstallerSource,
    val installerPackage: String?,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystemApp: Boolean,
    val hasLauncherIntent: Boolean,
    // App sistem, launcher, dan AdHunter sendiri tidak boleh dihentikan paksa.
    val canForceStop: Boolean,
) {
    val riskScore: Int
        get() = riskyPermissions.size + if (installer == InstallerSource.SIDELOAD) 1 else 0

    val hasRiskyPermissions: Boolean
        get() = riskyPermissions.isNotEmpty()
}
