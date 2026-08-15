package com.example.motortrackeryamaha.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.motortrackeryamaha.data.Motor
import kotlinx.coroutines.flow.Flow

@Dao
interface MotorDao {
    @Query("SELECT * FROM motor LIMIT 1")
    fun getMotorProfile(): Flow<Motor?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(motor: Motor)

    @Update
    suspend fun updateProfile(motor: Motor)
}
