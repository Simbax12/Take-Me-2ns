package com.twonorth.takeme

import android.app.Application
import com.twonorth.takeme.data.AppDatabase
import com.twonorth.takeme.data.MedicationRepository
import com.twonorth.takeme.data.local.UserPreferences

class TakeMeApplication : Application() {
    // Database, Repository, and Preferences are initialized lazily
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { MedicationRepository(database.medicationDao()) }
    val userPreferences by lazy { UserPreferences(this) }
}
