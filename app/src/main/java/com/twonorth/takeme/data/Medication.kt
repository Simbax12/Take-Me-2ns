package com.twonorth.takeme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String = "",
    val notes: String = "",
    val color: String = "amber",
    val frequency: String = "daily",
    val frequencyDays: String? = null, // "1,2,3" for Mon,Tue,Wed (1=Mon, 7=Sun)
    val frequencyTarget: Int? = null, // for "per_week"
    val reminderTime: String? = null,
    val timesPerDay: Int = 1,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
