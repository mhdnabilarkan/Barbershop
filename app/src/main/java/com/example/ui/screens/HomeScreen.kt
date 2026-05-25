package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.BarberEntity
import com.example.data.database.BookingEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BarberViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: BarberViewModel, modifier: Modifier = Modifier) {
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val barbers by viewModel.barbers.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val activeQueue by viewModel.activeQueue.collectAsStateWithLifecycle()
    val waitTime by viewModel.estimatedWaitMinutes.collectAsStateWithLifecycle()
    val successMessage by viewModel.bookingSuccessMessage.collectAsStateWithLifecycle()

    var showBookingDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("app_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentRole == "CUSTOMER",
                    onClick = { viewModel.switchRole("CUSTOMER") },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Pelanggan") },
                    label = { Text("Client Side") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldAccent,
                        selectedTextColor = GoldAccent,
                        indicatorColor = LightSurface,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )
                NavigationBarItem(
                    selected = currentRole == "BARBER_ADMIN",
                    onClick = { viewModel.switchRole("BARBER_ADMIN") },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Barber Queue") },
                    label = { Text("Barber Board") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldAccent,
                        selectedTextColor = GoldAccent,
                        indicatorColor = LightSurface,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )
                NavigationBarItem(
                    selected = currentRole == "OWNER_ANALYTICS",
                    onClick = { viewModel.switchRole("OWNER_ANALYTICS") },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Analytics") }, // Using Star representing premium metrics
                    label = { Text("Owner Stats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldAccent,
                        selectedTextColor = GoldAccent,
                        indicatorColor = LightSurface,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Header Bar
            HeaderSection()

            // Dynamic view based on role
            AnimatedContent(
                targetState = currentRole,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "MainContent"
            ) { role ->
                when (role) {
                    "CUSTOMER" -> CustomerView(
                        barbers = barbers,
                        bookings = bookings,
                        activeQueueCount = activeQueue.size,
                        waitTime = waitTime,
                        onOpenBooking = {
                            viewModel.clearBookingState()
                            showBookingDialog = true
                        },
                        onQuickBookBarber = { barber ->
                            viewModel.clearBookingState()
                            viewModel.selectBarber(barber)
                            showBookingDialog = true
                        },
                        onCancelBooking = { id ->
                            viewModel.cancelBooking(id)
                        }
                    )
                    "BARBER_ADMIN" -> BarberQueueView(
                        barbers = barbers,
                        activeQueue = activeQueue,
                        onToggleBarber = { id, current ->
                            viewModel.updateBarberOnlineStatus(id, current)
                        },
                        onMulaiCukur = { id ->
                            viewModel.updateBookingStatus(id, "IN_PROGRESS")
                        },
                        onSelesaiCukur = { id ->
                            viewModel.updateBookingStatus(id, "COMPLETED")
                        },
                        onMarkNoShow = { id ->
                            viewModel.markNoShow(id)
                        }
                    )
                    "OWNER_ANALYTICS" -> OwnerView(
                        barbers = barbers,
                        bookings = bookings
                    )
                }
            }
        }

        // Booking Modal Dialog
        if (showBookingDialog) {
            BookingWizardDialog(
                viewModel = viewModel,
                barbers = barbers,
                onDismiss = { showBookingDialog = false }
            )
        }

        // Success Confirmation Popup
        if (successMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissSuccessMessage() },
                containerColor = DarkSurface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SignalGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Sukses", tint = SignalGreen)
                        }
                        Text("Booking Berhasil", color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        successMessage ?: "",
                        color = TextWhite,
                        fontSize = 15.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.dismissSuccessMessage()
                            showBookingDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = GoldAccent)
                    ) {
                        Text("MANTAP", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Customized barber logo using Canvas
                Canvas(modifier = Modifier.size(24.dp)) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    // Draw gold circle
                    drawCircle(color = GoldAccent, radius = radius, style = Stroke(width = 2.dp.toPx()))
                    // Draw mini cross strokes symbolizing scissors
                    drawLine(
                        color = GoldAccent,
                        start = Offset(center.x - radius/2, center.y - radius/2),
                        end = Offset(center.x + radius/2, center.y + radius/2),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = GoldAccent,
                        start = Offset(center.x + radius/2, center.y - radius/2),
                        end = Offset(center.x - radius/2, center.y + radius/2),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                Text(
                    text = "ARKANSAS BARBER",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            Text(
                text = "Premium Grooming & Gentlemen Craft",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.Light
            )
        }

        // Live flashing neon status tag
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(LightSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SignalGreen)
            )
            Text(
                "OPEN TODAY",
                color = TextWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ==========================================
// 1. CLIENT / CUSTOMER VIEW (UC-01 - UC-05)
// ==========================================
@Composable
fun CustomerView(
    barbers: List<BarberEntity>,
    bookings: List<BookingEntity>,
    activeQueueCount: Int,
    waitTime: Int,
    onOpenBooking: () -> Unit,
    onQuickBookBarber: (BarberEntity) -> Unit,
    onCancelBooking: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Bento Widget 1: Hero Radar Status Antrian (UC-01)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_queue_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .drawBehind {
                        // Drawing geometric retro line pattern inside the background
                        val path = Path().apply {
                            moveTo(size.width * 0.7f, 0f)
                            lineTo(size.width, size.height * 0.4f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path = path, color = DarkGold.copy(alpha = 0.15f))
                    }
                    .padding(20.dp)
            ) {
                Text(
                    text = "ESTIMASI ANTRIAN LIVE",
                    fontSize = 11.sp,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$waitTime",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                            Text(
                                text = " MENIT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                            )
                        }
                        Text(
                            text = "Estimasi waktu tunggu saat ini",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }

                    // Separation Line
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .background(TextGray.copy(alpha = 0.2f))
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$activeQueueCount Orang",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Dalam Antrian",
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOpenBooking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("book_now_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Icon(
                        Icons.Default.Add, // Standard add icon representing booking reservation creation
                        contentDescription = "Book icon",
                        tint = DarkBg,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "BOOKING RESERVASI",
                        fontWeight = FontWeight.Bold,
                        color = DarkBg,
                        letterSpacing = 1.sp,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Section Title: Temukan Barbermu (UC-04)
        Text(
            text = "PILIH BARBER PROFESSIONAL",
            fontSize = 13.sp,
            color = GoldAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Barber List Grid Card Style
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            barbers.forEach { barber ->
                BarberRowCard(
                    barber = barber,
                    onQuickBook = { onQuickBookBarber(barber) }
                )
            }
        }

        // Section Title: Reservasi Aktif Saya (UC-03 & UC-05)
        Text(
            text = "RESERVASI SAYA HARI INI",
            fontSize = 13.sp,
            color = GoldAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 12.dp)
        )

        val myBookings = bookings.filter { it.status == "WAITING" || it.status == "IN_PROGRESS" }
        if (myBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "💈",
                        fontSize = 32.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Belum Ada Reservasi Aktif",
                        fontWeight = FontWeight.Medium,
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Buat reservasi pertamamu sekarang juga!",
                        fontSize = 11.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                myBookings.forEach { booking ->
                    BookingListItem(
                        booking = booking,
                        onCancel = { onCancelBooking(booking.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun BarberRowCard(
    barber: BarberEntity,
    onQuickBook: () -> Unit
) {
    val statusColor = when (barber.status) {
        "AVAILABLE" -> SignalGreen
        "BUSY" -> SignalOrange
        else -> TextGray
    }

    val statusText = when (barber.status) {
        "AVAILABLE" -> "Tersedia"
        "BUSY" -> "Sedang Mencukur"
        else -> "Rest / Istirahat"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(
            width = if (barber.status == "AVAILABLE") 1.dp else 0.dp,
            color = if (barber.status == "AVAILABLE") GoldAccent.copy(alpha = 0.25f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Avatar Frame
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = barber.avatarEmoji, fontSize = 26.sp)
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = barber.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Rating Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GoldAccent.copy(alpha = 0.1f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Rating Star",
                                tint = GoldAccent,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${barber.rating}",
                                fontSize = 9.sp,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = barber.specialty,
                        fontSize = 12.sp,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Real-time Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick reservation CTA
            if (barber.status != "OFFLINE") {
                Button(
                    onClick = onQuickBook,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (barber.status == "AVAILABLE") GoldAccent else LightSurface,
                        contentColor = if (barber.status == "AVAILABLE") DarkBg else TextWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Booking",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "LIBUR",
                    fontSize = 11.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
fun BookingListItem(
    booking: BookingEntity,
    onCancel: () -> Unit
) {
    val statusColor = when (booking.status) {
        "WAITING" -> SignalYellow
        "IN_PROGRESS" -> SignalGreen
        else -> TextGray
    }

    val statusBahasa = when (booking.status) {
        "WAITING" -> "Menunggu Antrian"
        "IN_PROGRESS" -> "Sedang Dicukur ✂️"
        else -> booking.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status Badge Indicator
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = statusBahasa,
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Jam ${booking.timeSlot}",
                    fontSize = 12.sp,
                    color = GoldAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Divider(color = LightSurface, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.serviceName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Barber: ${booking.barberName}   |   ${booking.customerName}",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                Text(
                    text = "Rp ${booking.price.toInt()}.000",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GoldAccent
                )
            }

            // Can only cancel if still WAITING status
            if (booking.status == "WAITING") {
                Button(
                    onClick = onCancel,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalRed.copy(alpha = 0.15f),
                        contentColor = SignalRed
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close, // Close/cancel representing Cancellation Action
                        contentDescription = "Batal icon",
                        modifier = Modifier.size(12.dp),
                        tint = SignalRed
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Batalkan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. BARBER BACK-END KANBAN (UC-06, 09, 07)
// ==========================================
@Composable
fun BarberQueueView(
    barbers: List<BarberEntity>,
    activeQueue: List<BookingEntity>,
    onToggleBarber: (Int, String) -> Unit,
    onMulaiCukur: (Int) -> Unit,
    onSelesaiCukur: (Int) -> Unit,
    onMarkNoShow: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Toggle Barber Availability (UC-07)
        Text(
            text = "BARBER LIVE STATUS ADJUSTER (UC-07)",
            fontSize = 11.sp,
            color = GoldAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tekan tombol status untuk mengganti ketersediaan barber:",
                    fontSize = 11.sp,
                    color = TextGray
                )
                barbers.forEach { barber ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(barber.avatarEmoji, fontSize = 20.sp)
                            Text(barber.name, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 14.sp)
                        }

                        // Status Badge Box
                        val badgeColor = when (barber.status) {
                            "AVAILABLE" -> SignalGreen
                            "BUSY" -> SignalOrange
                            else -> TextGray
                        }
                        val stringLabel = when (barber.status) {
                            "AVAILABLE" -> "ONLINE"
                            "BUSY" -> "SIBUK"
                            else -> "OFFLINE"
                        }

                        Button(
                            onClick = { onToggleBarber(barber.id, barber.status) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = badgeColor.copy(alpha = 0.15f),
                                contentColor = badgeColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(stringLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Kanban Antrian Board (UC-06 & UC-09)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                Icon(Icons.Default.Menu, contentDescription = "Kanban Icon", tint = GoldAccent, modifier = Modifier.size(16.dp))
                Text(
                    text = "BOARD ANTRIAN KANBAN (UC-06 / 09)",
                    fontSize = 11.sp,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        )

        // Kanban Sections Stack
        val waitingQueue = activeQueue.filter { it.status == "WAITING" }
        val inProgressQueue = activeQueue.filter { it.status == "IN_PROGRESS" }

        // Section 1: Sedang Dicukur / In Progress
        Text(
            text = "SEDANG DICUKUR (${inProgressQueue.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SignalGreen
        )

        if (inProgressQueue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightSurface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada pelanggan sedang dicukur", color = TextGray, fontSize = 12.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                inProgressQueue.forEach { item ->
                    KanbanCard(
                        booking = item,
                        actionLabel = "SELESAI",
                        buttonColor = GoldAccent,
                        textColor = DarkBg,
                        onAction = { onSelesaiCukur(item.id) },
                        onNoShow = null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Section 2: Menunggu / Waiting
        Text(
            text = "MENUNGGU DI SALON (${waitingQueue.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SignalYellow
        )

        if (waitingQueue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightSurface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Kamar tunggu kosong", color = TextGray, fontSize = 12.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                waitingQueue.forEach { item ->
                    KanbanCard(
                        booking = item,
                        actionLabel = "MULAI CUKUR",
                        buttonColor = SignalGreen,
                        textColor = DarkBg,
                        onAction = { onMulaiCukur(item.id) },
                        onNoShow = { onMarkNoShow(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun KanbanCard(
    booking: BookingEntity,
    actionLabel: String,
    buttonColor: Color,
    textColor: Color,
    onAction: () -> Unit,
    onNoShow: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.customerName,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Phone: ${booking.customerPhone}",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }

                Text(
                    text = "Jam ${booking.timeSlot}",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.serviceName,
                        fontSize = 12.sp,
                        color = TextWhite
                    )
                    Text(
                        text = "Barber: ${booking.barberName}",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Alternative No-Show button for salon admins
                    if (onNoShow != null) {
                        Button(
                            onClick = onNoShow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SignalRed.copy(alpha = 0.15f),
                                contentColor = SignalRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("No Show ❌", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(actionLabel, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. OWNER ANALYTICS VIEW (UC-10)
// ==========================================
@Composable
fun OwnerView(
    barbers: List<BarberEntity>,
    bookings: List<BookingEntity>
) {
    val scrollState = rememberScrollState()

    // Key Stats computations
    val totalBookingsCount = bookings.size
    val completedCount = bookings.count { it.status == "COMPLETED" }
    val noShowCount = bookings.count { it.status == "NO_SHOW" }
    
    // Revenue calculations: count items that are COMPLETED or IN_PROGRESS (already ongoing)
    val estimatedRevenue = bookings
        .filter { it.status == "COMPLETED" || it.status == "IN_PROGRESS" }
        .sumOf { it.price }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                Icon(Icons.Default.Star, contentDescription = "Trending Icon", tint = GoldAccent, modifier = Modifier.size(16.dp))
                Text(
                    text = "SALON METRICS & ANALYTICS (UC-10)",
                    fontSize = 11.sp,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        )

        // Triple Bento Grid Stat Widgets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Revenue
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("EST. REVENUE", fontSize = 9.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Rp ${estimatedRevenue.toInt()}.000", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GoldAccent)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Dari total order", fontSize = 9.sp, color = TextGray)
                }
            }

            // Card 2: Bookings Count
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("JML RESERVASI", fontSize = 9.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalBookingsCount", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextWhite)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Dipesan pelanggan", fontSize = 9.sp, color = TextGray)
                }
            }

            // Card 3: Completed Rates
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("SELESAI / NO SHOW", fontSize = 9.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$completedCount / $noShowCount", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SignalGreen)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Status penyelesaian", fontSize = 9.sp, color = TextGray)
                }
            }
        }

        // Beautiful Custom Canvas Line/Bar Chart (Daily Bookings Curve simulation)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, LightSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "GRAFIK KUNJUNGAN PELANGGAN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = GoldAccent
                )
                Text(
                    text = "Statistik 6 hari terakhir (Simulasi waktu nyata)",
                    fontSize = 10.sp,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Canvas Chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Drawing background grids
                    val gridLines = 4
                    val stepY = height / gridLines
                    for (i in 0..gridLines) {
                        drawLine(
                            color = LightSurface.copy(alpha = 0.5f),
                            start = Offset(0f, i * stepY),
                            end = Offset(width, i * stepY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Simulated data nodes
                    val dataPoints = listOf(
                        Offset(width * 0.05f, height * 0.8f),
                        Offset(width * 0.23f, height * 0.5f),
                        Offset(width * 0.41f, height * 0.65f),
                        Offset(width * 0.59f, height * 0.3f),
                        Offset(width * 0.77f, height * 0.45f),
                        Offset(width * 0.95f, height * 0.15f) // Peak today
                    )

                    // Draw connecting line
                    val path = Path().apply {
                        moveTo(dataPoints[0].x, dataPoints[0].y)
                        for (i in 1 until dataPoints.size) {
                            lineTo(dataPoints[i].x, dataPoints[i].y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = GoldAccent,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw gradient fill under the curve
                    val fillPath = Path().apply {
                        moveTo(dataPoints[0].x, dataPoints[0].y)
                        for (i in 1 until dataPoints.size) {
                            lineTo(dataPoints[i].x, dataPoints[i].y)
                        }
                        lineTo(dataPoints.last().x, height)
                        lineTo(dataPoints.first().x, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(GoldAccent.copy(alpha = 0.3f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Draw nodes point circles
                    dataPoints.forEach { point ->
                        drawCircle(
                            color = GoldAccent,
                            radius = 5.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = DarkBg,
                            radius = 2.dp.toPx(),
                            center = point
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab (Hari ini)")
                    days.forEach { day ->
                        Text(day, fontSize = 9.sp, color = TextGray)
                    }
                }
            }
        }

        // Barber Popularity Rating and bookings completed ratio
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PERFORMANSI PROFESSIONAL BARBER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = GoldAccent
                )

                barbers.forEach { barber ->
                    // Calculate bookings for this barber completed in database
                    val processedCount = bookings.count { it.barberId == barber.id && it.status == "COMPLETED" }
                    val activeCount = bookings.count { it.barberId == barber.id && (it.status == "WAITING" || it.status == "IN_PROGRESS") }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(barber.avatarEmoji, fontSize = 16.sp)
                                Text(barber.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                            Text(
                                "Selesai: $processedCount  |  Antre: $activeCount",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Custom Progress Bar based on completed bookings
                        val fillRatio = if (processedCount == 0 && activeCount == 0) 0.05f else {
                            val total = processedCount + activeCount
                            processedCount.toFloat() / total.toFloat()
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(LightSurface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fillRatio)
                                    .fillMaxHeight()
                                    .background(GoldAccent)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. BOOKING FLOW DIALOG WIZARD (UC-02)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingWizardDialog(
    viewModel: BarberViewModel,
    barbers: List<BarberEntity>,
    onDismiss: () -> Unit
) {
    val selectedBarber by viewModel.selectedBarber.collectAsStateWithLifecycle()
    val selectedService by viewModel.selectedService.collectAsStateWithLifecycle()
    val selectedTimeSlot by viewModel.selectedTimeSlot.collectAsStateWithLifecycle()
    val customerName by viewModel.customerName.collectAsStateWithLifecycle()
    val customerPhone by viewModel.customerPhone.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Services Definition list
    val services = listOf(
        Pair("Arkansas Premium Cut", "Cuci, Pijit Relaksasi, Hair Tonik & Cukur Premium -> Rp 45.000"),
        Pair("Classic Scissors Craft", "Pangkas gunting tradisional & stylish -> Rp 35.000"),
        Pair("Beard Trim & Sculpt", "Merapikan kumis/jenggot bento dengan handuk hangat -> Rp 25.000"),
        Pair("Gentlemen Shave & Hot Towel", "Shaving dahi premium lengkap pijit -> Rp 30.000")
    )

    // Hour slots list
    val slots = listOf("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(4.dp),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            tonalElevation = 12.dp,
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stepper Header
                Text(
                    text = "RESERVASI BARBERSHOP",
                    fontSize = 11.sp,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Current Wizard Step Indicator Box
                val currentStep = when {
                    selectedBarber == null -> 1
                    selectedService == null -> 2
                    selectedTimeSlot == null -> 3
                    else -> 4
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (i <= currentStep) GoldAccent else LightSurface
                                )
                        )
                    }
                }

                when (currentStep) {
                    1 -> {
                        // STEP 1: SELECT BARBER
                        Text("Pilih Stylist Barber Anda:", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 15.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Filter only Online/Busy barbers for booking
                            val onlineBarbers = barbers.filter { it.status != "OFFLINE" }
                            if (onlineBarbers.isEmpty()) {
                                Text("Maaf, tidak ada Barber tersedia saat ini.", color = SignalRed, fontSize = 12.sp)
                            } else {
                                onlineBarbers.forEach { barber ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selectedBarber?.id == barber.id) LightSurface else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (selectedBarber?.id == barber.id) GoldAccent else LightSurface,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.selectBarber(barber) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(barber.avatarEmoji, fontSize = 24.sp)
                                        Column {
                                            Text(barber.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(barber.specialty, color = TextGray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // STEP 2: SELECT SERVICE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pilih Layanan Cukur:", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 15.sp)
                            TextButton(onClick = { viewModel.selectBarber(null) }) {
                                Text("Kembali", color = GoldAccent, fontSize = 12.sp)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            services.forEach { service ->
                                val (name, detail) = service
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedService == name) LightSurface else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (selectedService == name) GoldAccent else LightSurface,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.selectService(name) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(detail, color = TextGray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // STEP 3: CHOOSE HOUR
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pilih Jam Kedatangan:", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 15.sp)
                            TextButton(onClick = { viewModel.selectService(null) }) {
                                Text("Kembali", color = GoldAccent, fontSize = 12.sp)
                            }
                        }

                        // Horizontal Layout or Grid of Slots
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(slots) { slot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selectedTimeSlot == slot) GoldAccent else LightSurface)
                                        .clickable { viewModel.selectTimeSlot(slot) }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = slot,
                                        color = if (selectedTimeSlot == slot) DarkBg else TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    4 -> {
                        // STEP 4: CONTACT FORM
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lengkapi Informasi Anda:", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 15.sp)
                            TextButton(onClick = { viewModel.selectTimeSlot(null) }) {
                                Text("Kembali", color = GoldAccent, fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { viewModel.setCustomerName(it) },
                            label = { Text("Nama Lengkap", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = LightSurface,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = LightSurface,
                                unfocusedContainerColor = LightSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("name_field")
                        )

                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { viewModel.setCustomerPhone(it) },
                            label = { Text("Nomor WhatsApp (WhatsApp OTP ready)", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = LightSurface,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = LightSurface,
                                unfocusedContainerColor = LightSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Confirmation Summary Box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("RINGKASAN REKAP:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                Text("Barber: ${selectedBarber?.name}", color = TextWhite, fontSize = 12.sp)
                                Text("Layanan: $selectedService", color = TextWhite, fontSize = 12.sp)
                                Text("Kedatangan: Jam $selectedTimeSlot", color = TextWhite, fontSize = 12.sp)
                            }
                        }

                        // Complete Action button
                        val isCompleteActive = customerName.isNotBlank() && customerPhone.isNotBlank()
                        Button(
                            onClick = {
                                if (isCompleteActive) {
                                    val success = viewModel.bookNow()
                                    if (success) {
                                        // Succcess will open the confirmation card
                                    }
                                }
                            },
                            enabled = isCompleteActive,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleteActive) GoldAccent else GoldAccent.copy(alpha = 0.3f),
                                contentColor = DarkBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_booking_button")
                        ) {
                            Text("KONFIRMASI BOOKING", fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Close Cancel dialog Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batalkan Layanan", color = SignalRed, fontSize = 12.sp)
                }
            }
        }
    }
}
