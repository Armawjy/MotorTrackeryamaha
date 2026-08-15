package com.example.motortrackeryamaha.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.motortrackeryamaha.data.AppDatabase
import kotlinx.coroutines.flow.first
import java.util.*

class OilReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val settings = db.appSettingsDao().getSettings().first() ?: return Result.success()

        if (!settings.notificationEnabled || !settings.oilReminderEnabled) {
            return Result.success()
        }

        val distSinceOil = settings.distanceSinceOilChange
        val intervalKm = settings.oilIntervalKm
        val intervalMonths = settings.oilIntervalMonths
        val lastDate = settings.lastOilChangeDate

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastDate
        calendar.add(Calendar.MONTH, intervalMonths)
        val limitDate = calendar.timeInMillis
        val now = System.currentTimeMillis()

        val isTimeUp = now >= limitDate
        val isDistOver = distSinceOil >= intervalKm
        val isSoon = distSinceOil >= (intervalKm - 200.0) // 200 KM threshold

        val notificationHelper = NotificationHelper(applicationContext)

        // Notification Logic
        if (isDistOver || isTimeUp) {
            if (settings.lastOilNotificationType != 2) {
                val title = "🔴 Waktunya Ganti Oli"
                val msg = if (isDistOver) "Jarak penggantian oli sudah mencapai batas $intervalKm KM."
                          else "Interval waktu penggantian oli sudah tercapai."
                notificationHelper.sendOilReminder(title, msg, 3)
                
                // Update state to prevent spam
                db.appSettingsDao().updateSettings(settings.copy(lastOilNotificationType = 2))
            }
        } else if (isSoon) {
            if (settings.lastOilNotificationType == 0) {
                val title = "🟡 Segera Ganti Oli"
                val remaining = (intervalKm - distSinceOil).coerceAtLeast(0.0)
                val msg = "Jarak sejak ganti oli sudah mendekati batas. Sisa sekitar ${String.format("%.0f", remaining)} KM."
                notificationHelper.sendOilReminder(title, msg, 2)
                
                // Update state
                db.appSettingsDao().updateSettings(settings.copy(lastOilNotificationType = 1))
            }
        } else {
            // Reset state if back to safe (e.g. interval changed)
            if (settings.lastOilNotificationType != 0) {
                db.appSettingsDao().updateSettings(settings.copy(lastOilNotificationType = 0))
            }
        }

        return Result.success()
    }
}
