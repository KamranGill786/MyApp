package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_logs")
data class DiagnosticLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val level: String, // "INFO", "WARN", "ERROR"
    val message: String
)

@Entity(tableName = "usb_events")
data class UsbEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "ATTACH", "DETACH"
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val isWifiAdapter: Boolean,
    val hasPermission: Boolean,
    val powerStatus: String
)
