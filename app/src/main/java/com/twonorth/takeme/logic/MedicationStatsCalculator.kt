package com.twonorth.takeme.logic

import com.twonorth.takeme.data.DoseLog
import com.twonorth.takeme.data.Medication
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class MedicationStats(
    val currentStreak: Int,
    val bestStreak: Int,
    val adherenceRate: Int, // Percentage
    val weeklyProgress: Int? = null,
    val weeklyTarget: Int? = null,
    val isPerWeek: Boolean = false,
    val hasData: Boolean
)

object MedicationStatsCalculator {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun calculateStats(
        medication: Medication,
        logs: List<DoseLog>,
        today: LocalDate = LocalDate.now()
    ): MedicationStats {
        if (logs.isEmpty()) {
            return MedicationStats(
                currentStreak = 0,
                bestStreak = 0,
                adherenceRate = 0,
                weeklyProgress = if (medication.frequency == "per_week") 0 else null,
                weeklyTarget = medication.frequencyTarget,
                isPerWeek = medication.frequency == "per_week",
                hasData = false
            )
        }

        val uniqueSortedDates = logs.map { LocalDate.parse(it.date, dateFormatter) }
            .distinct()
            .sortedDescending()

        if (medication.frequency == "per_week") {
            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weeklyProgress = uniqueSortedDates.count { !it.isBefore(startOfWeek) && !it.isAfter(today) }
            val adherenceRate = calculatePerWeekAdherence(medication, uniqueSortedDates.size, today)
            
            return MedicationStats(
                currentStreak = 0,
                bestStreak = 0,
                adherenceRate = adherenceRate,
                weeklyProgress = weeklyProgress,
                weeklyTarget = medication.frequencyTarget,
                isPerWeek = true,
                hasData = true
            )
        } else {
            val currentStreak = calculateCurrentStreak(medication, uniqueSortedDates, today)
            val bestStreak = calculateBestStreak(medication, uniqueSortedDates)
            val adherenceRate = calculateStandardAdherence(medication, uniqueSortedDates.size, today)

            return MedicationStats(
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                adherenceRate = adherenceRate,
                hasData = true
            )
        }
    }

    private fun calculateCurrentStreak(medication: Medication, sortedDates: List<LocalDate>, today: LocalDate): Int {
        if (sortedDates.isEmpty()) return 0

        val mostRecentLog = sortedDates[0]
        val mostRecentRequired = getMostRecentScheduledDay(medication, today)

        // Streak is active if most recent log is today OR on the most recent scheduled day
        if (mostRecentLog != today && mostRecentLog != mostRecentRequired) {
            return 0
        }

        var streak = 0
        var checkDate = mostRecentLog
        var logIndex = 0

        while (logIndex < sortedDates.size && sortedDates[logIndex] == checkDate) {
            streak++
            logIndex++
            checkDate = getPreviousScheduledDay(medication, checkDate)
        }

        return streak
    }

    private fun calculateBestStreak(medication: Medication, sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0
        
        val ascendingDates = sortedDates.sorted()
        var maxStreak = 0
        var currentStreak = 0
        var lastDate: LocalDate? = null

        for (date in ascendingDates) {
            if (lastDate == null) {
                currentStreak = 1
            } else {
                val nextScheduled = getNextScheduledDay(medication, lastDate)
                if (date == nextScheduled) {
                    currentStreak++
                } else {
                    maxStreak = maxOf(maxStreak, currentStreak)
                    currentStreak = 1
                }
            }
            lastDate = date
        }
        
        return maxOf(maxStreak, currentStreak)
    }

    private fun calculateStandardAdherence(medication: Medication, takenCount: Int, today: LocalDate): Int {
        val startDate = LocalDate.ofEpochDay(medication.createdAt / (24 * 60 * 60 * 1000))
        var scheduledCount = 0
        var d = startDate
        while (!d.isAfter(today)) {
            if (FrequencyLogic.isScheduled(medication, d)) {
                scheduledCount++
            }
            d = d.plusDays(1)
        }
        
        if (scheduledCount == 0) return 0
        return ((takenCount.toDouble() / scheduledCount.toDouble()) * 100).toInt().coerceIn(0, 100)
    }

    private fun calculatePerWeekAdherence(medication: Medication, takenCount: Int, today: LocalDate): Int {
        val startDate = LocalDate.ofEpochDay(medication.createdAt / (24 * 60 * 60 * 1000))
        val startOfWeekOfCreation = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val startOfCurrentWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        
        val weeks = (java.time.temporal.ChronoUnit.WEEKS.between(startOfWeekOfCreation, startOfCurrentWeek) + 1).toInt()
        val target = medication.frequencyTarget ?: 1
        val totalTarget = weeks * target
        
        if (totalTarget == 0) return 0
        return ((takenCount.toDouble() / totalTarget.toDouble()) * 100).toInt().coerceIn(0, 100)
    }

    private fun getMostRecentScheduledDay(medication: Medication, today: LocalDate): LocalDate {
        if (FrequencyLogic.isScheduled(medication, today)) return today
        return getPreviousScheduledDay(medication, today)
    }

    private fun getPreviousScheduledDay(medication: Medication, date: LocalDate): LocalDate {
        var d = date.minusDays(1)
        // Guard against infinite loop if nothing is scheduled (shouldn't happen with valid frequency)
        var count = 0
        while (!FrequencyLogic.isScheduled(medication, d) && count < 8) {
            d = d.minusDays(1)
            count++
        }
        return d
    }

    private fun getNextScheduledDay(medication: Medication, date: LocalDate): LocalDate {
        var d = date.plusDays(1)
        var count = 0
        while (!FrequencyLogic.isScheduled(medication, d) && count < 8) {
            d = d.plusDays(1)
            count++
        }
        return d
    }
}
