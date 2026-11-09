/*
 * Layar Pemantau aplikasi yang tampil di layar.
 * Menampilkan kendali service, status izin yang perlu diaktifkan manual, dan log
 * terbaru. Entri disorot merah bila paketnya punya izin berisiko menurut scanner
 * K1 atau tidak punya launcher dan bukan app sistem.
 */
@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.massbim.adhunter.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.massbim.adhunter.R
import com.massbim.adhunter.model.MonitorLogItem
import com.massbim.adhunter.model.RiskyPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonitorScreen(viewModel: MonitorViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var batteryExempt by remember { mutableStateOf(isBatteryOptimizationExempt(context)) }
    var notificationsGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    var confirmClear by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAccessState()
        batteryExempt = isBatteryOptimizationExempt(context)
        notificationsGranted = hasNotificationPermission(context)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted
        // Service tetap dijalankan walau notifikasi ditolak; Android hanya
        // menyembunyikan notifikasi permanennya.
        viewModel.startMonitoring()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                viewModel.refreshLogs()
                delay(LOG_REFRESH_INTERVAL_MS)
            }
        }
    }

    val highlightedCount = remember(state.logs) {
        state.logs.count { it.isHighlighted }
    }
    val visibleLogs = remember(state.logs, state.onlyHighlighted) {
        if (state.onlyHighlighted) state.logs.filter { it.isHighlighted } else state.logs
    }

    val onStartMonitoring = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startMonitoring()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.monitor_title),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.monitor_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.refresh()
                            viewModel.refreshAccessState()
                            batteryExempt = isBatteryOptimizationExempt(context)
                            notificationsGranted = hasNotificationPermission(context)
                        },
                        enabled = !state.isLoading,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = stringResource(R.string.monitor_refresh),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonitorControlCard(
                    isRunning = state.isServiceRunning,
                    canStart = state.hasUsageAccess,
                    onStart = onStartMonitoring,
                    onStop = viewModel::stopMonitoring,
                )
            }

            if (!state.hasUsageAccess) {
                item {
                    PermissionWarningCard(
                        title = stringResource(R.string.monitor_usage_missing_title),
                        description = stringResource(R.string.monitor_usage_missing_desc),
                        actionLabel = stringResource(R.string.monitor_open_usage_settings),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onAction = { openUsageAccessSettings(context) },
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
                item {
                    PermissionWarningCard(
                        title = stringResource(R.string.monitor_notifications_missing_title),
                        description = stringResource(R.string.monitor_notifications_missing_desc),
                        actionLabel = stringResource(R.string.monitor_open_notification_settings),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onAction = { openNotificationSettings(context) },
                    )
                }
            }

            item {
                BatteryCard(
                    exempt = batteryExempt,
                    onRequestExemption = { requestBatteryExemption(context) },
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = state.onlyHighlighted,
                        onClick = {
                            viewModel.setOnlyHighlighted(!state.onlyHighlighted)
                        },
                        label = {
                            Text(
                                stringResource(
                                    R.string.monitor_filter_highlighted,
                                    highlightedCount,
                                ),
                            )
                        },
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { confirmClear = true },
                        enabled = state.logs.isNotEmpty(),
                    ) {
                        Text(stringResource(R.string.monitor_clear_log))
                    }
                }
            }

            // Jumlah disembunyikan saat muat pertama agar tidak sempat terbaca "0 aktivitas".
            if (!state.isLoading || state.logs.isNotEmpty()) {
                item {
                    Text(
                        text = if (state.onlyHighlighted) {
                            stringResource(
                                R.string.monitor_log_count_filtered,
                                visibleLogs.size,
                                state.logs.size,
                            )
                        } else {
                            stringResource(R.string.monitor_log_count, state.logs.size)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = stringResource(
                            R.string.monitor_error,
                            state.errorMessage.orEmpty(),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            when {
                state.isLoading && state.logs.isEmpty() -> item { MonitorLoadingState() }
                visibleLogs.isEmpty() -> item {
                    MonitorEmptyState(
                        filtered = state.onlyHighlighted,
                    )
                }

                else -> items(
                    items = visibleLogs,
                    key = { it.entry.id },
                ) { item ->
                    MonitorLogCard(item = item)
                }
            }

            item {
                Text(
                    text = stringResource(R.string.monitor_current_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.monitor_clear_confirm_title)) },
            text = {
                Text(stringResource(R.string.monitor_clear_confirm_desc, state.logs.size))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        viewModel.clearLogs()
                    },
                ) {
                    Text(stringResource(R.string.monitor_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.monitor_cancel))
                }
            },
        )
    }
}

@Composable
private fun MonitorControlCard(
    isRunning: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRunning) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (isRunning) R.string.monitor_running else R.string.monitor_stopped,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    if (isRunning) R.string.monitor_running_desc else R.string.monitor_stopped_desc,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (isRunning) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            )
            Spacer(Modifier.height(14.dp))
            if (isRunning) {
                OutlinedButton(onClick = onStop) {
                    Text(stringResource(R.string.monitor_stop))
                }
            } else {
                Button(
                    onClick = onStart,
                    enabled = canStart,
                ) {
                    Text(stringResource(R.string.monitor_start))
                }
            }
        }
    }
}

@Composable
private fun PermissionWarningCard(
    title: String,
    description: String,
    actionLabel: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onAction: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAction) {
                Text(text = actionLabel, color = contentColor)
            }
        }
    }
}

@Composable
private fun BatteryCard(
    exempt: Boolean,
    onRequestExemption: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.monitor_battery_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    if (exempt) {
                        R.string.monitor_battery_exempt
                    } else {
                        R.string.monitor_battery_desc
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!exempt) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRequestExemption) {
                    Text(stringResource(R.string.monitor_battery_action))
                }
            }
        }
    }
}

@Composable
private fun MonitorLogCard(item: MonitorLogItem) {
    val highlighted = item.isHighlighted
    val containerColor = if (highlighted) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (highlighted) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryColor = contentColor.copy(alpha = 0.72f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            AppIcon(icon = item.icon, name = item.entry.appName)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.entry.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatLogTime(item.entry.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryColor,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.entry.packageName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val activityName = item.entry.activityName
                if (!activityName.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.monitor_activity, activityName),
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (highlighted) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RiskyPermission.entries.forEach { permission ->
                            if (permission in item.riskyPermissions) {
                                PermissionBadge(permission = permission)
                            }
                        }
                        if (item.hiddenFromLauncher) {
                            HiddenLauncherBadge()
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = highlightReason(item),
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                    )
                }

                if (item.canForceStop) {
                    ForceStopAction(
                        packageName = item.entry.packageName,
                        appName = item.entry.appName,
                        contentColor = if (highlighted) {
                            contentColor
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenLauncherBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = stringResource(R.string.monitor_badge_no_launcher),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun highlightReason(item: MonitorLogItem): String {
    val permissionReason = if (item.riskyPermissions.isEmpty()) {
        null
    } else {
        val labels = item.riskyPermissions
            .sortedBy { it.ordinal }
            .map { stringResource(it.labelRes()) }
            .joinToString(", ")
        stringResource(R.string.monitor_highlight_reason_permissions, labels)
    }
    val hiddenReason = if (item.hiddenFromLauncher) {
        stringResource(R.string.monitor_highlight_reason_hidden)
    } else {
        null
    }
    return listOfNotNull(permissionReason, hiddenReason).joinToString(" · ")
}

@Composable
private fun MonitorLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.monitor_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MonitorEmptyState(filtered: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                if (filtered) R.string.monitor_empty_filtered else R.string.monitor_empty,
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                if (filtered) {
                    R.string.monitor_empty_filtered_desc
                } else {
                    R.string.monitor_empty_desc
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

private fun isBatteryOptimizationExempt(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return true
    return runCatching {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(true)
}

private fun openUsageAccessSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun openNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun requestBatteryExemption(context: Context) {
    val request = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val opened = runCatching { context.startActivity(request) }.isSuccess
    if (!opened) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

private fun formatLogTime(timestamp: Long): String {
    val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    return if (dateTime.toLocalDate() == LocalDate.now()) {
        dateTime.format(TIME_FORMAT)
    } else {
        dateTime.format(DATE_TIME_FORMAT)
    }
}

private const val LOG_REFRESH_INTERVAL_MS = 1_500L

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val DATE_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM HH:mm:ss", Locale.getDefault())
