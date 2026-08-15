package com.example.motortrackeryamaha

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.motortrackeryamaha.utils.OilReminderWorker
import java.util.concurrent.TimeUnit

class MotorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupPeriodicWork()
    }

    private fun setupPeriodicWork() {
        val workRequest = PeriodicWorkRequestBuilder<OilReminderWorker>(
            4, TimeUnit.HOURS // Check every 4 hours
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OilReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
