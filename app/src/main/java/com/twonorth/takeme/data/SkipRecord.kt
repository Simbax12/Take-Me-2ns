package com.twonorth.takeme.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "skip_records",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicationId"])]
)
data class SkipRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val date: String, // ISO yyyy-MM-dd
    val reason: String, // forgot | side_effects | ran_out | felt_better | other
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
