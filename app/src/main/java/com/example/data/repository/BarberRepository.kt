package com.example.data.repository

import com.example.data.database.BarberDao
import com.example.data.database.BookingDao
import com.example.data.database.BarberEntity
import com.example.data.database.BookingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BarberRepository(
    private val barberDao: BarberDao,
    private val bookingDao: BookingDao
) {
    val allBarbers: Flow<List<BarberEntity>> = barberDao.getAllBarbers()
    val allBookings: Flow<List<BookingEntity>> = bookingDao.getAllBookings()
    val activeQueue: Flow<List<BookingEntity>> = bookingDao.getActiveQueue()

    suspend fun prepopulateBarbersIfEmpty() {
        // Since Flow cannot receive empty state immediately unless observed,
        // we can fetch a first list or do a quick query
        val existing = barberDao.getAllBarbers().first()
        if (existing.isEmpty()) {
            val defaultBarbers = listOf(
                BarberEntity(
                    id = 1,
                    name = "Alex",
                    specialty = "Fade Master",
                    rating = 4.9f,
                    status = "AVAILABLE",
                    avatarEmoji = "🧔",
                    estimatedMinutes = 25
                ),
                BarberEntity(
                    id = 2,
                    name = "Jordan",
                    specialty = "Classic Scissors",
                    rating = 4.8f,
                    status = "BUSY",
                    avatarEmoji = "👨",
                    estimatedMinutes = 35
                ),
                BarberEntity(
                    id = 3,
                    name = "Ryan",
                    specialty = "Modern Crop",
                    rating = 4.7f,
                    status = "AVAILABLE", // Let's make Ryan available too or busy
                    avatarEmoji = "💇‍♂️",
                    estimatedMinutes = 30
                ),
                BarberEntity(
                    id = 4,
                    name = "Mike",
                    specialty = "Beard & Grooming",
                    rating = 4.9f,
                    status = "OFFLINE",
                    avatarEmoji = "🧔‍♂️",
                    estimatedMinutes = 20
                )
            )
            barberDao.insertBarbers(defaultBarbers)
        }
    }

    suspend fun getBarberById(id: Int): BarberEntity? {
        return barberDao.getBarberById(id)
    }

    suspend fun insertBarber(barber: BarberEntity) {
        barberDao.insertBarber(barber)
    }

    suspend fun updateBarberStatus(id: Int, status: String) {
        barberDao.updateBarberStatus(id, status)
    }

    suspend fun createBooking(booking: BookingEntity) {
        bookingDao.insertBooking(booking)
    }

    suspend fun updateBooking(booking: BookingEntity) {
        bookingDao.updateBooking(booking)
    }

    suspend fun updateBookingStatus(id: Int, status: String) {
        bookingDao.updateBookingStatus(id, status)
    }

    suspend fun deleteBooking(id: Int) {
        bookingDao.deleteBookingById(id)
    }
}
