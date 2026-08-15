package com.example.motortrackeryamaha.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.motortrackeryamaha.data.NotificationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationRecord>>

    @Insert
    suspend fun insertNotification(notification: NotificationRecord)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
