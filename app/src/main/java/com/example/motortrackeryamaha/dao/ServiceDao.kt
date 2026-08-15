package com.example.motortrackeryamaha.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.motortrackeryamaha.data.Service
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY tanggal DESC")
    fun getAllServices(): Flow<List<Service>>

    @Insert
    suspend fun insertService(service: Service)

    @Update
    suspend fun updateService(service: Service)

    @Delete
    suspend fun deleteService(service: Service)

    @Query("SELECT COUNT(*) FROM services")
    fun getServiceCount(): Flow<Int>

    @Query("SELECT SUM(biaya) FROM services")
    fun getTotalServiceCost(): Flow<Double?>
    
    @Query("SELECT SUM(biaya) FROM services WHERE tanggal >= :startTime")
    fun getServiceCostSince(startTime: Long): Flow<Double?>
    
    @Query("SELECT COUNT(*) FROM services WHERE tanggal >= :startTime")
    fun getServiceCountSince(startTime: Long): Flow<Int>
}
