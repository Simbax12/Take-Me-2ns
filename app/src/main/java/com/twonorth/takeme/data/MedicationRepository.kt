package com.twonorth.takeme.data

import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val medicationDao: MedicationDao) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()

    fun getDoseLogsForDate(date: String): Flow<List<DoseLog>> = 
        medicationDao.getDoseLogsForDate(date)

    fun getDoseLogsForMedication(medicationId: Long): Flow<List<DoseLog>> =
        medicationDao.getDoseLogsForMedication(medicationId)

    fun getSkipsForDate(date: String): Flow<List<SkipRecord>> =
        medicationDao.getSkipsForDate(date)

    suspend fun insertMedication(medication: Medication) = 
        medicationDao.insertMedication(medication)

    suspend fun deleteMedication(medication: Medication) = 
        medicationDao.deleteMedication(medication)

    suspend fun markAsTaken(medicationId: Long, date: String) {
        // Mutual exclusivity: marking taken removes any skip for today
        medicationDao.deleteSkip(medicationId, date)
        medicationDao.insertDoseLog(DoseLog(medicationId = medicationId, date = date))
    }

    suspend fun unmarkAsTaken(medicationId: Long, date: String) {
        medicationDao.deleteDoseLog(medicationId, date)
    }

    suspend fun skipDose(medicationId: Long, date: String, reason: String, note: String? = null) {
        // Mutual exclusivity: skipping removes any taken record for today
        medicationDao.deleteDoseLog(medicationId, date)
        medicationDao.insertSkip(SkipRecord(medicationId = medicationId, date = date, reason = reason, note = note))
    }

    suspend fun unskipDose(medicationId: Long, date: String) {
        medicationDao.deleteSkip(medicationId, date)
    }
}
