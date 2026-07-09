package com.twonorth.takeme.logic

import android.content.Context
import com.twonorth.takeme.R
import com.twonorth.takeme.data.Medication
import java.time.LocalDate

object FrequencyLogic {

    /**
     * Determines if a medication is scheduled for a specific date.
     * Specific Days: Monday=1 ... Sunday=7
     */
    fun isScheduled(medication: Medication, date: LocalDate): Boolean {
        return when (medication.frequency) {
            "daily" -> true
            "per_week" -> true
            "specific_days" -> {
                val dayOfWeek = date.dayOfWeek.value // 1 (Monday) to 7 (Sunday)
                val scheduledDays = medication.frequencyDays?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
                scheduledDays.contains(dayOfWeek)
            }
            else -> true // Default to daily for legacy/unexpected values
        }
    }

    /**
     * Returns a human-readable label for the medication's frequency.
     */
    fun getFrequencyLabel(context: Context, medication: Medication): String {
        return when (medication.frequency) {
            "daily" -> context.getString(R.string.freq_daily)
            "per_week" -> {
                val target = medication.frequencyTarget ?: 1
                context.getString(R.string.freq_per_week_format, target)
            }
            "specific_days" -> {
                val scheduledDays = medication.frequencyDays?.split(",")?.mapNotNull { it.toIntOrNull() }?.sorted() ?: emptyList()
                if (scheduledDays.isEmpty()) return context.getString(R.string.freq_no_days)
                
                scheduledDays.joinToString(", ") { day ->
                    when (day) {
                        1 -> context.getString(R.string.day_mon_short)
                        2 -> context.getString(R.string.day_tue_short)
                        3 -> context.getString(R.string.day_wed_short)
                        4 -> context.getString(R.string.day_thu_short)
                        5 -> context.getString(R.string.day_fri_short)
                        6 -> context.getString(R.string.day_sat_short)
                        7 -> context.getString(R.string.day_sun_short)
                        else -> ""
                    }
                }
            }
            else -> context.getString(R.string.freq_daily)
        }
    }
}
