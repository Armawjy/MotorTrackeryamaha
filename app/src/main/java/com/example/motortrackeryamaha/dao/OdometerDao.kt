package com.example.motortrackeryamaha.dao

import androidx.room.*
import com.example.motortrackeryamaha.data.MotorOdometer
import kotlinx.coroutines.flow.Flow

@Dao
interface OdometerDao {
    @Query("SELECT * FROM motor_odometer WHERE id = 1")
    fun getOdometer(): Flow<MotorOdometer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOdometer(odometer: MotorOdometer)

    @Update
    suspend fun updateOdometer(odometer: MotorOdometer)
}
