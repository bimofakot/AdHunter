/*
 * Foreground service pemantau aplikasi yang tampil di layar.
 *
 * Pengganti `adb shell dumpsys window | findstr mCurrentFocus`: service mem-poll
 * UsageStatsManager setiap detik hanya saat layar menyala, lalu mencatat setiap
 * pergantian paket ke ForegroundLogStore. Jenis specialUse dipakai karena fitur
 * ini bukan media playback/lokasi/mikrofon, dan deklarasinya wajib di Android 14+.
 */
package com.massbim.adhunter.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.massbim.adhunter.MainActivity
import com.massbim.adhunter.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ForegroundMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var logStore: ForegroundLogStore
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var powerManager: PowerManager
    private var pollJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        logStore = ForegroundLogStore(this)
        usageStatsManager = getSystemService(UsageStatsManager::class.java)
        powerManager = getSystemService(PowerManager::class.java)

        createNotificationChannel()
        goForeground()
        _running.value = true
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return Service.START_NOT_STICKY
        }
        // START_STICKY: system membuat ulang service setelah proses aplikasi dimatikan.
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        pollJob?.cancel()
        serviceScope.cancel()
        _running.value = false
        super.onDestroy()
    }

    private fun goForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startPolling() {
        pollJob = serviceScope.launch {
            // Lookback pendek hanya untuk menangkap app yang baru tampil saat service dimulai.
            var since = System.currentTimeMillis() - INITIAL_LOOKBACK_MS
            var lastPackage: String? = null

            while (isActive) {
                val now = System.currentTimeMillis()
                if (powerManager.isInteractive) {
                    val events = runCatching {
                        usageStatsManager.queryEvents(since, now)
                    }.getOrNull()
                    if (events != null) {
                        lastPackage = recordEvents(events, lastPackage)
                    }
                }
                // Saat layar mati, rentang waktu tetap dimajukan agar event lama tidak
                // diproses ulang ketika layar menyala kembali.
                since = now
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun recordEvents(
        events: UsageEvents,
        lastPackage: String?,
    ): String? {
        var lastRecorded = lastPackage
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            if (!events.getNextEvent(event)) continue
            if (!event.isForegroundTransition()) continue

            val packageName = event.packageName ?: continue
            if (packageName == lastRecorded) continue

            lastRecorded = packageName
            logStore.append(
                timestamp = event.timeStamp,
                packageName = packageName,
                appName = resolveAppName(packageName),
                activityName = event.className?.takeIf { it.isNotBlank() },
            )
        }
        return lastRecorded
    }

    @Suppress("DEPRECATION")
    private fun UsageEvents.Event.isForegroundTransition(): Boolean {
        if (eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) return true
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            eventType == UsageEvents.Event.ACTIVITY_RESUMED
    }

    private fun resolveAppName(packageName: String): String = runCatching {
        packageManager.getApplicationInfo(packageName, 0)
            .loadLabel(packageManager)
            .toString()
    }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: packageName

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.monitor_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, ForegroundMonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(R.string.monitor_notification_stop), stopIntent)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "foreground_monitor"
        private const val REQUEST_OPEN = 10
        private const val REQUEST_STOP = 11
        private const val ACTION_STOP = "com.massbim.adhunter.action.STOP_MONITOR"
        private const val INITIAL_LOOKBACK_MS = 1_500L
        private val POLL_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1)

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, ForegroundMonitorService::class.java)
            ContextCompat.startForegroundService(context.applicationContext, intent)
        }

        fun stop(context: Context) {
            context.applicationContext.stopService(
                Intent(context, ForegroundMonitorService::class.java),
            )
        }
    }
}
