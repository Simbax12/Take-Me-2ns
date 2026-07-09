package com.twonorth.takeme.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.twonorth.takeme.TakeMeApplication
import com.twonorth.takeme.data.Medication
import com.twonorth.takeme.data.MedicationRepository
import com.twonorth.takeme.data.SkipRecord
import com.twonorth.takeme.logic.FrequencyLogic
import com.twonorth.takeme.logic.notifications.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MedicationStatus(
    val medication: Medication,
    val isTakenToday: Boolean,
    val isSkippedToday: Boolean,
    val skipRecord: SkipRecord? = null
)

data class TodayUiState(
    val medications: List<MedicationStatus> = emptyList(),
    val isLoading: Boolean = true
)

class TodayViewModel(
    private val repository: MedicationRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val todayDateString = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    private var lastRemovedSkip: SkipRecord? = null

    val uiState: StateFlow<TodayUiState> = combine(
        repository.allMedications,
        repository.getDoseLogsForDate(todayDateString),
        repository.getSkipsForDate(todayDateString)
    ) { medications, doseLogs, skips ->
        val today = LocalDate.now()
        val scheduledMeds = medications.filter { FrequencyLogic.isScheduled(it, today) }
        
        val takenIds = doseLogs.map { it.medicationId }.toSet()
        val skipMap = skips.associateBy { it.medicationId }
        
        val statusList = scheduledMeds.map { med ->
            MedicationStatus(
                medication = med,
                isTakenToday = takenIds.contains(med.id),
                isSkippedToday = skipMap.containsKey(med.id),
                skipRecord = skipMap[med.id]
            )
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

    fun skipDose(status: MedicationStatus, reason: String, note: String?) {
        viewModelScope.launch {
            repository.skipDose(status.medication.id, todayDateString, reason, note)
        }
    }

    fun unskipDose(status: MedicationStatus) {
        viewModelScope.launch {
            lastRemovedSkip = status.skipRecord
            repository.unskipDose(status.medication.id, todayDateString)
        }
    }

    fun restoreLastSkip() {
        viewModelScope.launch {
            lastRemovedSkip?.let { skip ->
                repository.skipDose(skip.medicationId, skip.date, skip.reason, skip.note)
                lastRemovedSkip = null
            }
        }
    }

    fun addMedication(
        name: String,
        reminderTime: String? = null,
        frequency: String = "daily",
        frequencyDays: String? = null,
        frequencyTarget: Int? = null
    ) {
        viewModelScope.launch {
            val med = Medication(
                name = name,
                reminderTime = reminderTime,
                frequency = frequency,
                frequencyDays = frequencyDays,
                frequencyTarget = frequencyTarget
            )
            val id = repository.insertMedication(med)
            if (reminderTime != null) {
                notificationHelper.scheduleReminder(med.copy(id = id))
            }
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
            notificationHelper.cancelReminder(medication.id)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as TakeMeApplication
                return TodayViewModel(application.repository, NotificationHelper(application)) as T
            }
        }
    }
}
