package com.example.data

import kotlinx.coroutines.flow.Flow

class ToolkitRepository(private val db: AppDatabase) {
    val diagnosticLogDao = db.diagnosticLogDao()
    val usbEventDao = db.usbEventDao()

    val allLogs: Flow<List<DiagnosticLogEntity>> = diagnosticLogDao.getAllLogs()
    val allUsbEvents: Flow<List<UsbEventEntity>> = usbEventDao.getAllEvents()

    fun getLogsByLevel(level: String): Flow<List<DiagnosticLogEntity>> {
        return diagnosticLogDao.getLogsByLevel(level)
    }

    suspend fun insertLog(tag: String, level: String, message: String) {
        diagnosticLogDao.insertLog(
            DiagnosticLogEntity(tag = tag, level = level, message = message)
        )
    }

    suspend fun clearLogs() {
        diagnosticLogDao.clearLogs()
    }

    suspend fun logUsbEvent(
        eventType: String,
        deviceName: String,
        vendorId: Int,
        productId: Int,
        isWifiAdapter: Boolean,
        hasPermission: Boolean,
        powerStatus: String
    ) {
        usbEventDao.insertEvent(
            UsbEventEntity(
                eventType = eventType,
                deviceName = deviceName,
                vendorId = vendorId,
                productId = productId,
                isWifiAdapter = isWifiAdapter,
                hasPermission = hasPermission,
                powerStatus = powerStatus
            )
        )
    }

    suspend fun clearUsbEvents() {
        usbEventDao.clearEvents()
    }
}
