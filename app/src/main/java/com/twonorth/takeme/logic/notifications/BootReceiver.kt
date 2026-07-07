package com.twonorth.takeme.logic.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.twonorth.takeme.TakeMeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as TakeMeApplication
            val repository = app.repository
            val helper = NotificationHelper(context)

            CoroutineScope(Dispatchers.IO).launch {
                val medications = repository.allMedications.first()
                medications.forEach { med ->
                    if (med.reminderTime != null) {
                        helper.scheduleReminder(med)
                    }
                }
            }
        }
    }
}
