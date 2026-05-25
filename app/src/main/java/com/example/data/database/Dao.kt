package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BarberDao {
    @Query("SELECT * FROM barbers ORDER BY id ASC")
    fun getAllBarbers(): Flow<List<BarberEntity>>

    @Query("SELECT * FROM barbers WHERE id = :id")
    suspend fun getBarberById(id: Int): BarberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarbers(barbers: List<BarberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarber(barber: BarberEntity)

    @Query("UPDATE barbers SET status = :status WHERE id = :id")
    suspend fun updateBarberStatus(id: Int, status: String)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY timestamp DESC")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE status = 'WAITING' OR status = 'IN_PROGRESS' ORDER BY timestamp ASC")
    fun getActiveQueue(): Flow<List<BookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    suspend fun updateBookingStatus(id: Int, status: String)

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBookingById(id: Int)
}
