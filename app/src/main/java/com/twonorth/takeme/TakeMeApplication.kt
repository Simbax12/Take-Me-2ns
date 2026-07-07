package com.twonorth.takeme

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.twonorth.takeme.data.AppDatabase
import com.twonorth.takeme.data.MedicationRepository
import com.twonorth.takeme.data.local.UserPreferences
import com.twonorth.takeme.logic.notifications.NotificationHelper

class TakeMeApplication : Application() {
    // Database, Repository, and Preferences are initialized lazily
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { MedicationRepository(database.medicationDao()) }
    val userPreferences by lazy { UserPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_title)
            val descriptionText = getString(R.string.label_reminder)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NotificationHelper.CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
