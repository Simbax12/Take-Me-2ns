package com.twonorth.takeme.logic.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.twonorth.takeme.TakeMeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_USER_PRESENT ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            val pendingResult = goAsync()
            val app = context.applicationContext as TakeMeApplication
            val repository = app.repository
            val helper = NotificationHelper(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Use sync query to avoid Flow issues during boot
                    val medications = repository.getAllMedicationsSync()
                    medications.forEach { med ->
                        if (med.reminderTime != null) {
                            helper.scheduleReminder(med)
                        }
                    }
                } catch (e: Exception) {
                    // Log or handle error if needed
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
