package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barbers")
data class BarberEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val specialty: String,
    val rating: Float,
    val status: String, // "AVAILABLE" (Tersedia), "BUSY" (Sibuk / Sedang Mencukur), "OFFLINE" (Offline / Istirahat)
    val avatarEmoji: String, // Simple emoji avatar since we can't bundle custom drawables easily
    val estimatedMinutes: Int = 30
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val customerPhone: String,
    val barberId: Int,
    val barberName: String,
    val serviceName: String,
    val price: Double,
    val timeSlot: String,
    val status: String, // "WAITING", "IN_PROGRESS", "COMPLETED", "CANCELED", "NO_SHOW"
    val timestamp: Long = System.currentTimeMillis()
)
