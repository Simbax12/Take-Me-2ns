package com.twonorth.takeme.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY position ASC, createdAt DESC")
    fun getAllMedications(): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("SELECT * FROM dose_logs WHERE date = :date")
    fun getDoseLogsForDate(date: String): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId ORDER BY date DESC")
    fun getDoseLogsForMedication(medicationId: Long): Flow<List<DoseLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseLog(doseLog: DoseLog)

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId AND date = :date")
    suspend fun deleteDoseLog(medicationId: Long, date: String)

    @Query("SELECT * FROM skip_records WHERE date = :date")
    fun getSkipsForDate(date: String): Flow<List<SkipRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkip(skipRecord: SkipRecord)

    @Query("DELETE FROM skip_records WHERE medicationId = :medicationId AND date = :date")
    suspend fun deleteSkip(medicationId: Long, date: String)

    @Query("DELETE FROM medications")
    suspend fun deleteAllMedications()
}
