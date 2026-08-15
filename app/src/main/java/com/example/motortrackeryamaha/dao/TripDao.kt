package com.example.motortrackeryamaha.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.motortrackeryamaha.data.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY tanggal DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Insert
    suspend fun insertTrip(trip: Trip)

    @Insert
    suspend fun insertTripWithId(trip: Trip): Long

    @Query("SELECT COUNT(*) FROM trips")
    fun getTripCount(): Flow<Int>

    @Query("SELECT SUM(jarak) FROM trips")
    fun getTotalTripDistance(): Flow<Double?>

    @Query("SELECT * FROM trips WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveTrip(): Trip?

    @Query("UPDATE trips SET status = :status, endLat = :endLat, endLng = :endLng, titikAkhir = :endPoint, endTime = :endTime, jarak = :distance, durasi = :duration, avgSpeed = :avg, maxSpeed = :max WHERE id = :id")
    suspend fun updateTripStatus(id: Int, status: String, endLat: Double, endLng: Double, endPoint: String, endTime: Long, distance: Double, duration: Long, avg: Double, max: Double)

    @androidx.room.Transaction
    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripWithPoints(tripId: Int): Flow<com.example.motortrackeryamaha.data.TripWithPoints?>

    @androidx.room.Delete
    suspend fun deleteTrip(trip: Trip)
}
