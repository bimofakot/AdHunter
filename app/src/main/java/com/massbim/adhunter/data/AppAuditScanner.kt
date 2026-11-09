/*
 * Scanner audit izin.
 * Seluruh pemindaian berjalan di Dispatchers.Default karena memanggil
 * PackageManager, AppOpsManager, dan memuat ikon untuk semua aplikasi.
 */
package com.massbim.adhunter.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import com.massbim.adhunter.model.AppAuditEntry
import com.massbim.adhunter.model.InstallerSource
import com.massbim.adhunter.model.RiskyPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale

class AppAuditScanner(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val appOpsManager = appContext.getSystemService(AppOpsManager::class.java)
    private val accessibilityManager =
        appContext.getSystemService(AccessibilityManager::class.java)
    private val devicePolicyManager =
        appContext.getSystemService(DevicePolicyManager::class.java)

    suspend fun scan(includeSystemApps: Boolean): List<AppAuditEntry> =
        withContext(Dispatchers.Default) {
            val accessibilityPackages = enabledAccessibilityPackages()
            val adminPackages = activeDeviceAdminPackages()
            val listenerPackages = enabledNotificationListenerPackages()
            val launcherPackages = launcherVisiblePackages()
            val homePackages = homePackages()
            val applications = installedApplications()
            val entries = ArrayList<AppAuditEntry>(applications.size)

            for (application in applications) {
                currentCoroutineContext().ensureActive()

                val packageInfo = runCatching {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(application.packageName, 0)
                }.getOrNull()

                val firstInstallTime = packageInfo?.firstInstallTime ?: 0L
                val lastUpdateTime = packageInfo?.lastUpdateTime ?: firstInstallTime
                val isSystemApp = application.flags and ApplicationInfo.FLAG_SYSTEM != 0
                val isUntouchedSystemApp =
                    isSystemApp && packageInfo != null && firstInstallTime == lastUpdateTime

                if (!includeSystemApps && isUntouchedSystemApp) continue

                val installerPackage = installerPackageName(application.packageName)
                val installer = classifyInstaller(installerPackage, isSystemApp)
                val permissions = buildSet {
                    if (hasOverlayPermission(application)) {
                        add(RiskyPermission.OVERLAY)
                    }
                    if (application.packageName in accessibilityPackages) {
                        add(RiskyPermission.ACCESSIBILITY)
                    }
                    if (application.packageName in adminPackages) {
                        add(RiskyPermission.DEVICE_ADMIN)
                    }
                    if (application.packageName in listenerPackages) {
                        add(RiskyPermission.NOTIFICATION_LISTENER)
                    }
                }

                entries += AppAuditEntry(
                    packageName = application.packageName,
                    appName = loadAppName(application),
                    icon = loadIcon(application),
                    riskyPermissions = permissions,
                    installer = installer,
                    installerPackage = installerPackage,
                    firstInstallTime = firstInstallTime,
                    lastUpdateTime = lastUpdateTime,
                    isSystemApp = isSystemApp,
                    hasLauncherIntent = application.packageName in launcherPackages,
                    canForceStop = isForceStoppable(isSystemApp, application.packageName, homePackages),
                )
            }

            entries.sortWith(
                compareByDescending<AppAuditEntry> { it.riskScore }
                    .thenBy { it.appName.lowercase(Locale.ROOT) },
            )
            entries
        }

    @Suppress("DEPRECATION")
    private fun installedApplications(): List<ApplicationInfo> =
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    // Dipakai layar Pemantau: app tanpa launcher dan bukan app sistem dianggap mencurigakan.
    @Suppress("DEPRECATION")
    private fun launcherVisiblePackages(): Set<String> = runCatching {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }.getOrDefault(emptySet())

    // Launcher yang sedang jadi home screen tidak boleh ditawari "hentikan paksa".
    @Suppress("DEPRECATION")
    private fun homePackages(): Set<String> = runCatching {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }.getOrDefault(emptySet())

    // Hanya app pengguna biasa yang aman dihentikan: bukan app sistem, bukan launcher,
    // bukan AdHunter sendiri, dan bukan paket inti yang membuat HP tidak responsif.
    private fun isForceStoppable(
        isSystemApp: Boolean,
        packageName: String,
        homePackages: Set<String>,
    ): Boolean = !isSystemApp &&
        packageName != appContext.packageName &&
        packageName !in homePackages &&
        packageName !in PROTECTED_PACKAGES

    private fun loadAppName(application: ApplicationInfo): String {
        val label = runCatching { application.loadLabel(packageManager).toString() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        return label ?: application.packageName
    }

    private fun loadIcon(application: ApplicationInfo): Bitmap? = runCatching {
        application.loadIcon(packageManager)
            .toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
    }.getOrNull()

    private fun hasOverlayPermission(application: ApplicationInfo): Boolean {
        val appOps = appOpsManager ?: return false
        return runCatching {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                application.uid,
                application.packageName,
            ) == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    private fun enabledAccessibilityPackages(): Set<String> {
        val manager = accessibilityManager ?: return emptySet()
        return runCatching {
            manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .mapNotNull { it.resolveInfo?.serviceInfo?.packageName }
                .toSet()
        }.getOrDefault(emptySet())
    }

    @Suppress("DEPRECATION")
    private fun activeDeviceAdminPackages(): Set<String> {
        val manager = devicePolicyManager ?: return emptySet()
        return runCatching {
            manager.getActiveAdmins()
                ?.mapNotNull { it.packageName }
                ?.toSet()
                .orEmpty()
        }.getOrDefault(emptySet())
    }

    private fun enabledNotificationListenerPackages(): Set<String> =
        runCatching { NotificationManagerCompat.getEnabledListenerPackages(appContext) }
            .getOrDefault(emptySet())

    @Suppress("DEPRECATION")
    private fun installerPackageName(packageName: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            packageManager.getInstallerPackageName(packageName)
        }
    }.getOrNull()

    private fun classifyInstaller(
        installerPackage: String?,
        isSystemApp: Boolean,
    ): InstallerSource = when {
        // App preinstalled biasanya tidak punya installer; jangan dihitung sideload.
        installerPackage == null && isSystemApp -> InstallerSource.OTHER
        installerPackage == null -> InstallerSource.SIDELOAD
        installerPackage == PLAY_STORE_PACKAGE -> InstallerSource.PLAY_STORE
        installerPackage in SIDELOAD_INSTALLERS -> InstallerSource.SIDELOAD
        else -> InstallerSource.OTHER
    }

    private companion object {
        const val PLAY_STORE_PACKAGE = "com.android.vending"
        const val ICON_SIZE_PX = 96

        val PROTECTED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
        )

        val SIDELOAD_INSTALLERS = setOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.documentsui",
            "com.google.android.documentsui",
            "com.google.android.apps.nbu.files",
            "com.android.chrome",
            "com.android.shell",
        )
    }
}
