/*
 * State dan logika layar Pemantau.
 * Snapshot risiko diambil sekali dari AppAuditScanner (K1) agar log bisa disorot
 * tanpa menduplikasi logika deteksi izin. Log dibaca ulang berkala dari SQLite
 * selama layar terlihat.
 */
package com.massbim.adhunter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.massbim.adhunter.data.AppAuditScanner
import com.massbim.adhunter.data.ForegroundLogStore
import com.massbim.adhunter.data.ForegroundMonitorService
import com.massbim.adhunter.data.UsageAccess
import com.massbim.adhunter.model.AppAuditEntry
import com.massbim.adhunter.model.ForegroundLogEntry
import com.massbim.adhunter.model.MonitorLogItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MonitorUiState(
    val isLoading: Boolean = true,
    val logs: List<MonitorLogItem> = emptyList(),
    val onlyHighlighted: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val isServiceRunning: Boolean = false,
    val errorMessage: String? = null,
)

class MonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = AppAuditScanner(application)
    private val logStore = ForegroundLogStore(application)

    private val _uiState = MutableStateFlow(
        MonitorUiState(hasUsageAccess = UsageAccess.isGranted(application)),
    )
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    private var riskIndex: Map<String, AppAuditEntry> = emptyMap()
    private var loadJob: Job? = null
    private val updateMutex = Mutex()

    init {
        viewModelScope.launch {
            ForegroundMonitorService.running.collect { running ->
                _uiState.update { it.copy(isServiceRunning = running) }
            }
        }
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                riskIndex = scanner.scan(includeSystemApps = true)
                    .associateBy { it.packageName }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message) }
            }

            val records = runCatching { logStore.loadRecent() }.getOrDefault(emptyList())
            updateMutex.withLock {
                _uiState.update {
                    it.copy(logs = mapLogs(records), isLoading = false)
                }
            }
        }
    }

    fun refreshLogs() {
        viewModelScope.launch {
            val records = runCatching { logStore.loadRecent() }.getOrNull() ?: return@launch
            updateMutex.withLock {
                _uiState.update { it.copy(logs = mapLogs(records)) }
            }
        }
    }

    fun setOnlyHighlighted(onlyHighlighted: Boolean) {
        _uiState.update { it.copy(onlyHighlighted = onlyHighlighted) }
    }

    fun refreshAccessState() {
        val granted = UsageAccess.isGranted(getApplication())
        _uiState.update { it.copy(hasUsageAccess = granted) }
    }

    fun startMonitoring() {
        ForegroundMonitorService.start(getApplication())
    }

    fun stopMonitoring() {
        ForegroundMonitorService.stop(getApplication())
    }

    fun clearLogs() {
        viewModelScope.launch {
            runCatching { logStore.clear() }
            updateMutex.withLock {
                _uiState.update { it.copy(logs = emptyList()) }
            }
        }
    }

    private fun mapLogs(records: List<ForegroundLogEntry>): List<MonitorLogItem> =
        records.map { record ->
            val appEntry = riskIndex[record.packageName]
            MonitorLogItem(
                entry = record,
                icon = appEntry?.icon,
                riskyPermissions = appEntry?.riskyPermissions.orEmpty(),
                hiddenFromLauncher = appEntry != null &&
                    !appEntry.hasLauncherIntent &&
                    !appEntry.isSystemApp,
            )
        }
}
