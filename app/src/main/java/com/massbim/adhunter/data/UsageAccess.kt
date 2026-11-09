/*
 * Pemeriksaan izin Usage Access.
 * Izin ini bukan runtime permission, jadi dicek lewat AppOpsManager. Mode DEFAULT
 * di sebagian perangkat dikembalikan walau izin sudah diberikan, sehingga hasilnya
 * diverifikasi ulang lewat permission PACKAGE_USAGE_STATS.
 */
package com.massbim.adhunter.data

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process

object UsageAccess {

    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = runCatching {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }.getOrDefault(AppOpsManager.MODE_ERRORED)

        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_DEFAULT -> context.checkSelfPermission(
                Manifest.permission.PACKAGE_USAGE_STATS,
            ) == PackageManager.PERMISSION_GRANTED

            else -> false
        }
    }
}
