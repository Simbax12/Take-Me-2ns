package com.twonorth.takeme.data

import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val medicationDao: MedicationDao) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()

    fun getDoseLogsForDate(date: String): Flow<List<DoseLog>> = 
        medicationDao.getDoseLogsForDate(date)

    fun getDoseLogsForMedication(medicationId: Long): Flow<List<DoseLog>> =
        medicationDao.getDoseLogsForMedication(medicationId)

    suspend fun insertMedication(medication: Medication) = 
        medicationDao.insertMedication(medication)

    suspend fun deleteMedication(medication: Medication) = 
        medicationDao.deleteMedication(medication)

    suspend fun markAsTaken(medicationId: Long, date: String) {
        medicationDao.insertDoseLog(DoseLog(medicationId = medicationId, date = date))
    }

    suspend fun unmarkAsTaken(medicationId: Long, date: String) {
        medicationDao.deleteDoseLog(medicationId, date)
    }
}
