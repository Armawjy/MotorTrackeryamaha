package com.example.motortrackeryamaha.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.motortrackeryamaha.data.TripPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface TripPointDao {
    @Query("SELECT * FROM trip_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getPointsForTrip(tripId: Int): Flow<List<TripPoint>>

    @Insert
    suspend fun insertPoint(point: TripPoint)

    @Query("DELETE FROM trip_points WHERE tripId = :tripId")
    suspend fun deletePointsForTrip(tripId: Int)
}
