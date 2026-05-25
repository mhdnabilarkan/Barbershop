package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.BarberEntity
import com.example.data.database.BookingEntity
import com.example.data.repository.BarberRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BarberViewModel(private val repository: BarberRepository) : ViewModel() {

    // Main Flows
    val barbers: StateFlow<List<BarberEntity>> = repository.allBarbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<BookingEntity>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeQueue: StateFlow<List<BookingEntity>> = repository.activeQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Interactive Selection states for the booking flow
    private val _selectedBarber = MutableStateFlow<BarberEntity?>(null)
    val selectedBarber = _selectedBarber.asStateFlow()

    private val _selectedService = MutableStateFlow<String?>(null)
    val selectedService = _selectedService.asStateFlow()

    private val _selectedTimeSlot = MutableStateFlow<String?>(null)
    val selectedTimeSlot = _selectedTimeSlot.asStateFlow()

    // Form attributes
    private val _customerName = MutableStateFlow("")
    val customerName = _customerName.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone = _customerPhone.asStateFlow()

    // Success overlay or notification
    private val _bookingSuccessMessage = MutableStateFlow<String?>(null)
    val bookingSuccessMessage = _bookingSuccessMessage.asStateFlow()

    // Role switcher ("CUSTOMER" vs "BARBER_ADMIN")
    private val _currentRole = MutableStateFlow("CUSTOMER")
    val currentRole = _currentRole.asStateFlow()

    // Estimated Wait Time (Calculated dynamically)
    val estimatedWaitMinutes: StateFlow<Int> = combine(barbers, activeQueue) { barberList, queueList ->
        val activeWorkingBarbers = barberList.count { it.status != "OFFLINE" }
        val waitingBookings = queueList.count { it.status == "WAITING" }
        
        if (activeWorkingBarbers == 0) {
            45 // Default fallback
        } else {
            // A realistic calculation: Each waiting customer takes ~20-30 mins.
            // Spread across active working barbers.
            val wait = (waitingBookings * 25) / activeWorkingBarbers
            // Show a minimum of 10 minutes wait if there is anyone waiting, up to hours
            if (waitingBookings > 0 && wait < 15) 15 else wait
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    init {
        // Prepopulate data when ViewModel starts up
        viewModelScope.launch {
            repository.prepopulateBarbersIfEmpty()
        }
    }

    fun switchRole(role: String) {
        _currentRole.value = role
    }

    fun selectBarber(barber: BarberEntity?) {
        _selectedBarber.value = barber
    }

    fun selectService(service: String?) {
        _selectedService.value = service
    }

    fun selectTimeSlot(slot: String?) {
        _selectedTimeSlot.value = slot
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun setCustomerPhone(phone: String) {
        _customerPhone.value = phone
    }

    fun clearBookingState() {
        _selectedBarber.value = null
        _selectedService.value = null
        _selectedTimeSlot.value = null
        _customerName.value = ""
        _customerPhone.value = ""
        _bookingSuccessMessage.value = null
    }

    fun bookNow(): Boolean {
        val barber = _selectedBarber.value ?: return false
        val service = _selectedService.value ?: return false
        val timeSlot = _selectedTimeSlot.value ?: return false
        val name = _customerName.value.trim()
        val phone = _customerPhone.value.trim()

        if (name.isEmpty() || phone.isEmpty()) return false

        val price = when (service) {
            "Arkansas Premium Cut" -> 45.0
            "Classic Scissors Craft" -> 35.0
            "Beard Trim & Sculpt" -> 25.0
            "Gentlemen Shave & Hot Towel" -> 30.0
            else -> 30.0
        }

        val booking = BookingEntity(
            customerName = name,
            customerPhone = phone,
            barberId = barber.id,
            barberName = barber.name,
            serviceName = service,
            price = price,
            timeSlot = timeSlot,
            status = "WAITING"
        )

        viewModelScope.launch {
            repository.createBooking(booking)
            _bookingSuccessMessage.value = "Booking berhasil untuk ${name} dengan Barber ${barber.name}!"
        }
        return true
    }

    fun dismissSuccessMessage() {
        clearBookingState()
    }

    fun cancelBooking(id: Int) {
        viewModelScope.launch {
            repository.updateBookingStatus(id, "CANCELED")
        }
    }

    fun updateBarberOnlineStatus(id: Int, currentStatus: String) {
        viewModelScope.launch {
            val nextStatus = when (currentStatus) {
                "AVAILABLE" -> "BUSY"
                "BUSY" -> "OFFLINE"
                else -> "AVAILABLE"
            }
            repository.updateBarberStatus(id, nextStatus)
        }
    }

    fun updateBookingStatus(id: Int, nextStatus: String) {
        viewModelScope.launch {
            repository.updateBookingStatus(id, nextStatus)
            
            // Automatic smart barber state transition to enrich user experience:
            // - If booking goes IN_PROGRESS, set barber to BUSY
            // - If booking goes COMPLETED or CANCELED or NO_SHOW, find if they have any other waiting bookings.
            //   If not, reset barber to AVAILABLE
            if (nextStatus == "IN_PROGRESS" || nextStatus == "COMPLETED") {
                // Find booking to know which barber it is
                val currentBookings = bookings.value
                val booking = currentBookings.find { it.id == id }
                if (booking != null) {
                    if (nextStatus == "IN_PROGRESS") {
                        repository.updateBarberStatus(booking.barberId, "BUSY")
                    } else if (nextStatus == "COMPLETED") {
                        // Check if this barber has any other active bookings.
                        val hasMore = currentBookings.any { 
                            it.barberId == booking.barberId && it.id != id && (it.status == "WAITING" || it.status == "IN_PROGRESS")
                        }
                        if (!hasMore) {
                            repository.updateBarberStatus(booking.barberId, "AVAILABLE")
                        }
                    }
                }
            }
        }
    }

    fun markNoShow(id: Int) {
        viewModelScope.launch {
            repository.updateBookingStatus(id, "NO_SHOW")
            
            // Re-evaluate barber status
            val currentBookings = bookings.value
            val booking = currentBookings.find { it.id == id }
            if (booking != null) {
                val hasMore = currentBookings.any { 
                    it.barberId == booking.barberId && it.id != id && (it.status == "WAITING" || it.status == "IN_PROGRESS")
                }
                if (!hasMore) {
                    repository.updateBarberStatus(booking.barberId, "AVAILABLE")
                }
            }
        }
    }
}

class BarberViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BarberViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context)
            val repo = BarberRepository(db.barberDao(), db.bookingDao())
            @Suppress("UNCHECKED_CAST")
            return BarberViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
