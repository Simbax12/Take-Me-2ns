package com.twonorth.takeme

import android.app.Application
import com.twonorth.takeme.data.AppDatabase
import com.twonorth.takeme.data.MedicationRepository

class TakeMeApplication : Application() {
    // Database and Repository are initialized lazily
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { MedicationRepository(database.medicationDao()) }
}
