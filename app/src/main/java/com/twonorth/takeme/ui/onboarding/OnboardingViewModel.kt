package com.twonorth.takeme.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.twonorth.takeme.TakeMeApplication
import com.twonorth.takeme.data.Medication
import com.twonorth.takeme.data.MedicationRepository
import com.twonorth.takeme.data.local.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class OnboardingStep {
    NAME,
    LOG_DOSE,
    ADD_DETAILS
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.NAME,
    val medicationName: String = "",
    val isComplete: Boolean = false
)

class OnboardingViewModel(
    private val repository: MedicationRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var createdMedicationId: Long? = null

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(medicationName = name)
    }

    fun submitName() {
        val name = _uiState.value.medicationName
        if (name.isNotBlank()) {
            viewModelScope.launch {
                val med = Medication(name = name)
                repository.insertMedication(med)
                // We need the ID to log a dose later.
                // We fetch the first list update to find the medication we just created.
                val meds = repository.allMedications.first()
                createdMedicationId = meds.find { it.name == name }?.id
            }
            _uiState.value = _uiState.value.copy(currentStep = OnboardingStep.LOG_DOSE)
        }
    }

    fun logDoseNow() {
        viewModelScope.launch {
            createdMedicationId?.let { id ->
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                repository.markAsTaken(id, today)
            }
            _uiState.value = _uiState.value.copy(currentStep = OnboardingStep.ADD_DETAILS)
        }
    }

    fun skipLogDose() {
        finishOnboarding()
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            userPreferences.setOnboardingComplete(true)
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as TakeMeApplication
                return OnboardingViewModel(application.repository, application.userPreferences) as T
            }
        }
    }
}
