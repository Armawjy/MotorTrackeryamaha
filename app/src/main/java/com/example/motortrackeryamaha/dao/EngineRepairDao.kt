package com.example.motortrackeryamaha.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.motortrackeryamaha.data.EngineRepair
import kotlinx.coroutines.flow.Flow

@Dao
interface EngineRepairDao {
    @Query("SELECT * FROM engine_repairs ORDER BY tanggal DESC")
    fun getAllRepairs(): Flow<List<EngineRepair>>

    @Insert
    suspend fun insertRepair(repair: EngineRepair)

    @Update
    suspend fun updateRepair(repair: EngineRepair)

    @Delete
    suspend fun deleteRepair(repair: EngineRepair)

    @Query("SELECT SUM(biaya) FROM engine_repairs")
    fun getTotalRepairCost(): Flow<Double?>
    
    @Query("SELECT COUNT(*) FROM engine_repairs")
    fun getRepairCount(): Flow<Int>
    
    @Query("SELECT SUM(biaya) FROM engine_repairs WHERE tanggal >= :startTime")
    fun getRepairCostSince(startTime: Long): Flow<Double?>
}
