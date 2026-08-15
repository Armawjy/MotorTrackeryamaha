package com.example.motortrackeryamaha.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "motor")
data class Motor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merk: String = "Yamaha",
    val model: String = "Mio S 125",
    val engineCc: String = "125 cc",
    val tahun: String = "2022",
    val nomorPolisi: String = "DD XXXX XX",
    val warna: String = "Merah Hitam",
    val kilometerAwal: Double = 20663.0,
    val catatan: String = "",
    val photoUri: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "oil_changes")
data class OilChange(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: Long,
    val kilometerSaatGanti: Double,
    val jarakSejakGanti: Double,
    val merkOli: String,
    val jenisOli: String,
    val volume: String,
    val biaya: Double,
    val bengkel: String,
    val catatan: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: Long, // Use as startTime
    val jarak: Double,
    val durasi: Long,
    val titikAwal: String = "",
    val titikAkhir: String = "",
    val startLat: Double = 0.0,
    val startLng: Double = 0.0,
    val endLat: Double = 0.0,
    val endLng: Double = 0.0,
    val endTime: Long = 0,
    val avgSpeed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val status: String = "COMPLETED", // "ACTIVE", "COMPLETED"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trip_points")
data class TripPoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tripId: Int,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float,
    val altitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "services")
data class Service(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: Long,
    val jenisService: String,
    val kilometer: Double,
    val biaya: Double,
    val bengkel: String,
    val catatan: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "component_replacements")
data class ComponentReplacement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: Long,
    val namaKomponen: String,
    val kilometer: Double,
    val biaya: Double,
    val catatan: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "engine_repairs")
data class EngineRepair(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: Long,
    val kilometer: Double,
    val jenisPerbaikan: String,
    val biaya: Double,
    val bengkel: String,
    val catatan: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val totalTrackedDistance: Double = 0.0,
    val distanceSinceOilChange: Double = 0.0,
    val lastOilChangeDate: Long = System.currentTimeMillis(),
    val oilIntervalKm: Double = 2000.0,
    val oilIntervalMonths: Int = 2,
    val trackingStartDate: Long = 0L,
    val notificationEnabled: Boolean = true,
    val oilReminderEnabled: Boolean = true,
    val serviceReminderEnabled: Boolean = true,
    val maintenanceReminderEnabled: Boolean = true,
    val lastOilNotificationType: Int = 0, // 0: None, 1: Soon, 2: Now
    val autoTrackingEnabled: Boolean = false,
    val gpsIntervalSeconds: Int = 5,
    val gpsAccuracy: String = "Tinggi", // "Rendah", "Sedang", "Tinggi"
    val safetyRadiusMeters: Int = 1000,
    val referenceLat: Double? = null,
    val referenceLng: Double? = null,
    val manualStop: Boolean = false
)

@Entity(tableName = "notifications")
data class NotificationRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val type: Int, // 1: Info, 2: Warning, 3: Danger
    val category: String, // "Oil", "Service", "Trip"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "Arma Wijaya",
    val email: String = "arma.wijaya@email.com",
    val phone: String = "+62 812-3456-7890",
    val birthDate: String = "15 Mei 2001",
    val address: String = "Jl. Sungai Saddang Baru No.12 Makassar, Sulawesi Selatan",
    val gender: String = "Laki-laki",
    val profilePhotoUri: String? = null,
    val joinDate: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val accountStatus: String = "Aktif"
)

@Entity(tableName = "motor_odometer")
data class MotorOdometer(
    @PrimaryKey val id: Int = 1,
    val odometerFisik: Double = 0.0,
    val isOdometerFisikSet: Boolean = false,
    val odometerFisikDate: Long? = null,
    val odometerDigital: Double = 0.0,
    val odometerDigitalSince: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
