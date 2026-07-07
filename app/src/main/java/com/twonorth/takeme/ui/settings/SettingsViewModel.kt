package com.twonorth.takeme.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.twonorth.takeme.TakeMeApplication
import com.twonorth.takeme.data.MedicationRepository
import com.twonorth.takeme.data.local.AppTheme
import com.twonorth.takeme.data.local.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: MedicationRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = userPreferences.appTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            userPreferences.setAppTheme(theme)
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            userPreferences.setOnboardingComplete(false)
            onComplete()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as TakeMeApplication
                return SettingsViewModel(application.repository, application.userPreferences) as T
            }
        }
    }
}
