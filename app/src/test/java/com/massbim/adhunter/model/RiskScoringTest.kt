/*
 * Unit test untuk aturan penilaian risiko dan sorotan log Pemantau.
 * Model diuji tanpa Android runtime; ikon selalu null.
 */
package com.massbim.adhunter.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScoringTest {

    private fun audit(
        permissions: Set<RiskyPermission> = emptySet(),
        installer: InstallerSource = InstallerSource.OTHER,
    ) = AppAuditEntry(
        packageName = "com.example.app",
        appName = "Contoh",
        icon = null,
        riskyPermissions = permissions,
        installer = installer,
        installerPackage = null,
        firstInstallTime = 0L,
        lastUpdateTime = 0L,
        isSystemApp = false,
        hasLauncherIntent = true,
        canForceStop = true,
    )

    @Test
    fun `skor risiko menghitung setiap izin berisiko`() {
        assertEquals(
            2,
            audit(setOf(RiskyPermission.OVERLAY, RiskyPermission.ACCESSIBILITY)).riskScore,
        )
    }

    @Test
    fun `sideload menambah satu poin`() {
        assertEquals(1, audit(installer = InstallerSource.SIDELOAD).riskScore)
        assertEquals(
            3,
            audit(
                permissions = setOf(RiskyPermission.OVERLAY, RiskyPermission.DEVICE_ADMIN),
                installer = InstallerSource.SIDELOAD,
            ).riskScore,
        )
    }

    @Test
    fun `play store tanpa izin berisiko bernilai nol`() {
        val entry = audit(installer = InstallerSource.PLAY_STORE)
        assertEquals(0, entry.riskScore)
        assertFalse(entry.hasRiskyPermissions)
    }

    @Test
    fun `izin berisiko terdeteksi`() {
        assertTrue(audit(setOf(RiskyPermission.NOTIFICATION_LISTENER)).hasRiskyPermissions)
    }

    @Test
    fun `log disorot untuk app tersembunyi atau berizin berisiko`() {
        val log = ForegroundLogEntry(
            id = 1L,
            timestamp = 0L,
            packageName = "com.example.app",
            appName = "Contoh",
            activityName = null,
        )
        val normal = MonitorLogItem(
            entry = log,
            icon = null,
            riskyPermissions = emptySet(),
            hiddenFromLauncher = false,
            canForceStop = true,
        )

        assertFalse(normal.isHighlighted)
        assertTrue(normal.copy(hiddenFromLauncher = true).isHighlighted)
        assertTrue(
            normal.copy(riskyPermissions = setOf(RiskyPermission.OVERLAY)).isHighlighted,
        )
    }
}
