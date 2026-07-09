package com.twonorth.takeme.logic.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.twonorth.takeme.MainActivity
import com.twonorth.takeme.data.Medication
import com.twonorth.takeme.logic.FrequencyLogic
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class NotificationHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(medication: Medication) {
        val time = medication.reminderTime ?: return
        val parts = time.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        // Find the next scheduled day starting from today
        var scheduledDate = LocalDate.now()
        val calendar = Calendar.getInstance()
        
        // If time has already passed today, start checking from tomorrow
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            scheduledDate = scheduledDate.plusDays(1)
        }

        // Loop until we find a day where it is scheduled
        var daysChecked = 0
        while (!FrequencyLogic.isScheduled(medication, scheduledDate) && daysChecked < 7) {
            scheduledDate = scheduledDate.plusDays(1)
            daysChecked++
        }

        // Finalize the calendar to the found date
        val zone = ZoneId.systemDefault()
        val epochMillis = scheduledDate.atStartOfDay(zone).toInstant().toEpochMilli()
        calendar.timeInMillis = epochMillis
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)

        // Use a unique action to ensure the Intent is unique for AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_PREFIX + medication.id
            putExtra("MEDICATION_ID", medication.id)
            putExtra("MEDICATION_NAME", medication.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Failed to schedule exact alarm", e)
            // Final fallback to inexact to avoid crash
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(medicationId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_PREFIX + medicationId
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        private const val ACTION_REMINDER_PREFIX = "com.twonorth.takeme.ACTION_REMINDER_"
    }
}
