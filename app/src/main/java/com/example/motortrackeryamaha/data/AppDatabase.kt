package com.example.motortrackeryamaha.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.motortrackeryamaha.dao.*

@Database(
    entities = [
        Motor::class,
        AppSettings::class,
        Trip::class,
        OilChange::class,
        Service::class,
        ComponentReplacement::class,
        EngineRepair::class,
        NotificationRecord::class,
        UserProfile::class,
        TripPoint::class,
        MotorOdometer::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun motorDao(): MotorDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun oilChangeDao(): OilChangeDao
    abstract fun tripDao(): TripDao
    abstract fun serviceDao(): ServiceDao
    abstract fun componentReplacementDao(): ComponentReplacementDao
    abstract fun engineRepairDao(): EngineRepairDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userDao(): UserDao
    abstract fun tripPointDao(): TripPointDao
    abstract fun odometerDao(): OdometerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "motor_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
