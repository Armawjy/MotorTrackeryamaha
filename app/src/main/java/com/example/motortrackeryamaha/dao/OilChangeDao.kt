package com.example.motortrackeryamaha.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.motortrackeryamaha.data.OilChange
import kotlinx.coroutines.flow.Flow

@Dao
interface OilChangeDao {
    @Query("SELECT * FROM oil_changes ORDER BY tanggal DESC LIMIT 1")
    fun getLastOilChange(): Flow<OilChange?>

    @Query("SELECT * FROM oil_changes ORDER BY tanggal DESC")
    fun getAllOilChanges(): Flow<List<OilChange>>

    @Insert
    suspend fun insertOilChange(oilChange: OilChange)

    @Update
    suspend fun updateOilChange(oilChange: OilChange)

    @Delete
    suspend fun deleteOilChange(oilChange: OilChange)

    @Query("SELECT SUM(biaya) FROM oil_changes")
    fun getTotalOilCost(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM oil_changes")
    fun getOilChangeCount(): Flow<Int>
    
    @Query("SELECT SUM(biaya) FROM oil_changes WHERE tanggal >= :startTime")
    fun getOilCostSince(startTime: Long): Flow<Double?>
    
    @Query("SELECT COUNT(*) FROM oil_changes WHERE tanggal >= :startTime")
    fun getOilCountSince(startTime: Long): Flow<Int>
}
