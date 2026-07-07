package com.twonorth.takeme.logic.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.twonorth.takeme.MainActivity
import com.twonorth.takeme.data.Medication
import java.util.Calendar

class NotificationHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(medication: Medication) {
        val time = medication.reminderTime ?: return
        val parts = time.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If time has already passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
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
                    val info = AlarmManager.AlarmClockInfo(
                        calendar.timeInMillis,
                        getOpenAppPendingIntent()
                    )
                    alarmManager.setAlarmClock(info, pendingIntent)
                } else {
                    // Fallback to inexact if for some reason we lost permission
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                val info = AlarmManager.AlarmClockInfo(
                    calendar.timeInMillis,
                    getOpenAppPendingIntent()
                )
                alarmManager.setAlarmClock(info, pendingIntent)
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
        val intent = Intent(context, ReminderReceiver::class.java)
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

    private fun getOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
    }
}
