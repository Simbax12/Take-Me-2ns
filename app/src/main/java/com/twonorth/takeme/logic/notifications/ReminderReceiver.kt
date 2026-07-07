package com.twonorth.takeme.logic.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.twonorth.takeme.MainActivity
import com.twonorth.takeme.R
import com.twonorth.takeme.TakeMeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra("MEDICATION_ID", -1L)
        val medicationName = intent.getStringExtra("MEDICATION_NAME") ?: "Medication"

        if (medicationId != -1L) {
            showNotification(context, medicationId, medicationName)
            rescheduleNext(context, medicationId)
        }
    }

    private fun showNotification(context: Context, medicationId: Long, medicationName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            medicationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // We'll need to create this or use a default
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_message, medicationName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(medicationId.toInt(), notification)
    }

    private fun rescheduleNext(context: Context, medicationId: Long) {
        val app = context.applicationContext as TakeMeApplication
        val repository = app.repository
        val helper = NotificationHelper(context)

        // Reschedule in a background scope since we're in a BroadcastReceiver
        CoroutineScope(Dispatchers.IO).launch {
            val med = repository.allMedications.first().find { it.id == medicationId }
            if (med != null && med.reminderTime != null) {
                helper.scheduleReminder(med)
            }
        }
    }
}
