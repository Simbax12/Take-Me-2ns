package com.twonorth.takeme.logic

import com.twonorth.takeme.data.DoseLog
import com.twonorth.takeme.data.Medication
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class MedicationStats(
    val currentStreak: Int,
    val bestStreak: Int,
    val adherenceRate: Int, // Percentage
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
            return MedicationStats(0, 0, 0, false)
        }

        // Distinct dates only, sorted descending for current streak
        val uniqueSortedDates = logs.map { LocalDate.parse(it.date, dateFormatter) }
            .distinct()
            .sortedDescending()

        val currentStreak = calculateCurrentStreak(uniqueSortedDates, today)
        val bestStreak = calculateBestStreak(uniqueSortedDates)
        val adherenceRate = calculateAdherenceRate(medication.createdAt, uniqueSortedDates.size, today)

        return MedicationStats(currentStreak, bestStreak, adherenceRate, true)
    }

    private fun calculateCurrentStreak(sortedDates: List<LocalDate>, today: LocalDate): Int {
        if (sortedDates.isEmpty()) return 0

        val mostRecent = sortedDates[0]
        // Streak is active if most recent is today or yesterday
        if (mostRecent != today && mostRecent != today.minusDays(1)) {
            return 0
        }

        var streak = 0
        var expectedDate = mostRecent
        
        for (date in sortedDates) {
            if (date == expectedDate) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    private fun calculateBestStreak(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0
        
        // Use ascending for easier forward counting
        val ascendingDates = sortedDates.sorted()
        var maxStreak = 0
        var currentStreak = 0
        var expectedDate: LocalDate? = null

        for (date in ascendingDates) {
            if (expectedDate == null || date == expectedDate) {
                currentStreak++
            } else {
                maxStreak = maxOf(maxStreak, currentStreak)
                currentStreak = 1
            }
            expectedDate = date.plusDays(1)
        }
        
        return maxOf(maxStreak, currentStreak)
    }

    private fun calculateAdherenceRate(createdAtMillis: Long, takenCount: Int, today: LocalDate): Int {
        val startDate = LocalDate.ofEpochDay(createdAtMillis / (24 * 60 * 60 * 1000))
        val totalDays = ChronoUnit.DAYS.between(startDate, today) + 1
        
        if (totalDays <= 0) return 0
        
        return ((takenCount.toDouble() / totalDays.coerceAtLeast(1).toDouble()) * 100).toInt()
    }
}
