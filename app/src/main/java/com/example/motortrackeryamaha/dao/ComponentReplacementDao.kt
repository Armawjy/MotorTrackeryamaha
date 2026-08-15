package com.example.motortrackeryamaha.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.motortrackeryamaha.data.ComponentReplacement
import kotlinx.coroutines.flow.Flow

@Dao
interface ComponentReplacementDao {
    @Query("SELECT * FROM component_replacements ORDER BY tanggal DESC")
    fun getAllReplacements(): Flow<List<ComponentReplacement>>

    @Insert
    suspend fun insertReplacement(replacement: ComponentReplacement)

    @Update
    suspend fun updateReplacement(replacement: ComponentReplacement)

    @Delete
    suspend fun deleteReplacement(replacement: ComponentReplacement)

    @Query("SELECT COUNT(*) FROM component_replacements")
    fun getReplacementCount(): Flow<Int>

    @Query("SELECT SUM(biaya) FROM component_replacements")
    fun getTotalReplacementCost(): Flow<Double?>
    
    @Query("SELECT SUM(biaya) FROM component_replacements WHERE tanggal >= :startTime")
    fun getReplacementCostSince(startTime: Long): Flow<Double?>
    
    @Query("SELECT COUNT(*) FROM component_replacements WHERE tanggal >= :startTime")
    fun getReplacementCountSince(startTime: Long): Flow<Int>
}
