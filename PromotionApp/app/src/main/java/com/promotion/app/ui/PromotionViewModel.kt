package com.promotion.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.promotion.app.data.local.EmployeeSummary
import com.promotion.app.data.local.Prediction
import com.promotion.app.data.local.YearlyCount
import com.promotion.app.data.repository.PromotionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class PromotionViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PromotionRepository(application)

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val employees: StateFlow<List<EmployeeSummary>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) repo.getAllEmployees()
            else repo.searchEmployees(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected employee predictions
    private val _selectedEmpId = MutableStateFlow<String?>(null)
    val selectedEmpId: StateFlow<String?> = _selectedEmpId

    val predictions: StateFlow<List<Prediction>> = _selectedEmpId
        .filterNotNull()
        .flatMapLatest { repo.getPredictions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Yearly report
    val yearlyReport: StateFlow<List<YearlyCount>> = repo.getYearlyReport()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sync state
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    val apiUrl: String get() = repo.getApiUrl()
    val lastSyncTime: String get() = repo.getLastSyncTime()

    // DB count
    private val _dbCount = MutableStateFlow(0)
    val dbCount: StateFlow<Int> = _dbCount

    init {
        viewModelScope.launch {
            _dbCount.value = repo.getCount()
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun selectEmployee(empId: String) {
        _selectedEmpId.value = empId
    }

    fun updateApiUrl(url: String) {
        repo.setApiUrl(url)
    }

    fun simulateLocal() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            try {
                val db = com.promotion.app.data.local.AppDatabase.getInstance(getApplication())
                val mgr = com.promotion.app.data.simulation.SimulationManager(getApplication(), db.predictionDao())
                val count = mgr.runSimulation()
                _dbCount.value = count
                _syncStatus.value = SyncStatus.Success("Simulated $count predictions offline")
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Simulation failed")
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            val result = repo.syncFromApi()
            result.fold(
                onSuccess = { count ->
                    _dbCount.value = count
                    _syncStatus.value = SyncStatus.Success("Synced $count predictions")
                },
                onFailure = { error ->
                    _syncStatus.value = SyncStatus.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun saveCsvFile(uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _syncStatus.value = SyncStatus.Syncing
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val destFile = File(context.filesDir, fileName)

                if (inputStream != null) {
                    val outputStream = FileOutputStream(destFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    _syncStatus.value = SyncStatus.Success("Successfully updated $fileName")
                } else {
                    _syncStatus.value = SyncStatus.Error("Failed to read file from storage")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error("Error saving file: ${e.message}")
            }
        }
    }
}

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}
