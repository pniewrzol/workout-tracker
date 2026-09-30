package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BodyMeasurement
import com.example.data.model.Exercise
import com.example.data.model.ExerciseMedia
import com.example.data.model.InitialWorkoutData
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSetLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Exercise::class,
        ExerciseMedia::class,
        WorkoutSession::class,
        WorkoutSetLog::class,
        BodyMeasurement::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun mediaDao(): MediaDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `body_measurements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `weightKg` REAL,
                        `bodyFatPercentage` REAL,
                        `chestCm` REAL,
                        `waistCm` REAL,
                        `bicepsCm` REAL,
                        `hipsCm` REAL,
                        `thighsCm` REAL,
                        `calvesCm` REAL,
                        `shouldersCm` REAL,
                        `notes` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `exercises` ADD COLUMN `measurementType` TEXT NOT NULL DEFAULT 'WEIGHT_AND_REPS'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `workout_set_logs` ADD COLUMN `timeSeconds` INTEGER")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `workout_set_logs` ADD COLUMN `distanceMeters` REAL")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `workout_set_logs` ADD COLUMN `rir` REAL")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // Removed fallbackToDestructiveMigration() to protect existing user workout data
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seeding and deduplication is handled deterministically by WorkoutRepository.checkAndSeedExercises()
            }
        }
    }
}
