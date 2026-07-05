package com.twonorth.takeme.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.twonorth.takeme.TakeMeApplication
import com.twonorth.takeme.data.Medication
import com.twonorth.takeme.data.MedicationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MedicationStatus(
    val medication: Medication,
    val isTakenToday: Boolean
)

data class TodayUiState(
    val medications: List<MedicationStatus> = emptyList(),
    val isLoading: Boolean = true
)

class TodayViewModel(private val repository: MedicationRepository) : ViewModel() {

    private val todayDateString = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val uiState: StateFlow<TodayUiState> = combine(
        repository.allMedications,
        repository.getDoseLogsForDate(todayDateString)
    ) { medications, doseLogs ->
        val takenIds = doseLogs.map { it.medicationId }.toSet()
        val statusList = medications.map { med ->
            MedicationStatus(medication = med, isTakenToday = takenIds.contains(med.id))
        }
        TodayUiState(medications = statusList, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    fun toggleTaken(status: MedicationStatus) {
        viewModelScope.launch {
            if (status.isTakenToday) {
                repository.unmarkAsTaken(status.medication.id, todayDateString)
            } else {
                repository.markAsTaken(status.medication.id, todayDateString)
            }
        }
    }

    fun addMedication(name: String) {
        viewModelScope.launch {
            repository.insertMedication(Medication(name = name))
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as TakeMeApplication
                return TodayViewModel(application.repository) as T
            }
        }
    }
}
