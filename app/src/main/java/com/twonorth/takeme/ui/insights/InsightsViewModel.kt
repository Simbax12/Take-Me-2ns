package com.twonorth.takeme.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.twonorth.takeme.TakeMeApplication
import com.twonorth.takeme.data.DoseLog
import com.twonorth.takeme.data.Medication
import com.twonorth.takeme.data.MedicationRepository
import com.twonorth.takeme.logic.MedicationStats
import com.twonorth.takeme.logic.MedicationStatsCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MedicationWithStats(
    val medication: Medication,
    val stats: MedicationStats
)

data class InsightsUiState(
    val medications: List<MedicationWithStats> = emptyList(),
    val isLoading: Boolean = true
)

class InsightsViewModel(private val repository: MedicationRepository) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = repository.allMedications.flatMapLatest { meds ->
        if (meds.isEmpty()) {
            flowOf(InsightsUiState(isLoading = false))
        } else {
            val flows = meds.map { med ->
                repository.getDoseLogsForMedication(med.id).map { logs ->
                    MedicationWithStats(med, MedicationStatsCalculator.calculateStats(med, logs))
                }
            }
            combine(flows) { it.toList() }.map { InsightsUiState(it, false) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState()
    )

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as TakeMeApplication
                return InsightsViewModel(application.repository) as T
            }
        }
    }
}
