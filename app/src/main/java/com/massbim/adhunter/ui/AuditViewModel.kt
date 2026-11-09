/*
 * State dan logika layar Audit Izin.
 * Pemindaian dijalankan di viewModelScope; scanner sendiri berpindah ke
 * Dispatchers.Default sehingga UI tidak diblokir.
 */
package com.massbim.adhunter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.massbim.adhunter.data.AppAuditScanner
import com.massbim.adhunter.model.AppAuditEntry
import com.massbim.adhunter.model.RiskyPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuditUiState(
    val isLoading: Boolean = true,
    val entries: List<AppAuditEntry> = emptyList(),
    val showSystemApps: Boolean = false,
    val selectedPermission: RiskyPermission? = null,
    val query: String = "",
    val errorMessage: String? = null,
)

class AuditViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = AppAuditScanner(application)
    private val _uiState = MutableStateFlow(AuditUiState())
    val uiState: StateFlow<AuditUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val entries = scanner.scan(_uiState.value.showSystemApps)
                _uiState.update {
                    it.copy(isLoading = false, entries = entries, errorMessage = null)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        entries = emptyList(),
                        errorMessage = error.message,
                    )
                }
            }
        }
    }

    fun setShowSystemApps(show: Boolean) {
        if (_uiState.value.showSystemApps == show) return
        _uiState.update { it.copy(showSystemApps = show) }
        refresh()
    }

    fun selectPermission(permission: RiskyPermission?) {
        _uiState.update { it.copy(selectedPermission = permission) }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }
}
