package com.example.motortrackeryamaha.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.motortrackeryamaha.MaintenanceActivity
import com.example.motortrackeryamaha.R
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.data.NotificationRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "motor_maintenance_channel"
        const val OIL_NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Perawatan Motor"
            val descriptionText = "Pengingat perawatan Yamaha Mio S 125"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendOilReminder(title: String, message: String, type: Int) {
        val intent = Intent(context, MaintenanceActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_oil)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(OIL_NOTIFICATION_ID, builder.build())
            }
            
            // Log to database
            saveNotificationToDb(title, message, type, "Oil")
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun saveNotificationToDb(title: String, message: String, type: Int, category: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            db.notificationDao().insertNotification(
                NotificationRecord(
                    title = title,
                    message = message,
                    type = type,
                    category = category
                )
            )
        }
    }
}
