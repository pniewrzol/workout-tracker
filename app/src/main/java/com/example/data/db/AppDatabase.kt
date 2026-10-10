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
import com.example.data.model.WorkoutPlan
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
        BodyMeasurement::class,
        WorkoutPlan::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun mediaDao(): MediaDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun workoutPlanDao(): WorkoutPlanDao

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

        private fun addColumnIfNotExists(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnDef: String
        ) {
            val cursor = db.query("PRAGMA table_info(`$tableName`)")
            var exists = false
            cursor.use {
                val nameIndex = it.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (it.moveToNext()) {
                        if (it.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                            exists = true
                            break
                        }
                    }
                }
            }
            if (!exists) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDef")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "exercises", "measurementType", "TEXT NOT NULL DEFAULT 'WEIGHT_AND_REPS'")
                addColumnIfNotExists(db, "workout_set_logs", "timeSeconds", "INTEGER")
                addColumnIfNotExists(db, "workout_set_logs", "distanceMeters", "REAL")
                addColumnIfNotExists(db, "workout_set_logs", "rir", "REAL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "body_measurements", "frontPhotoUri", "TEXT")
                addColumnIfNotExists(db, "body_measurements", "backPhotoUri", "TEXT")
                addColumnIfNotExists(db, "body_measurements", "sidePhotoUri", "TEXT")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workout_plans` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT OR IGNORE INTO `workout_plans` (`id`, `name`, `description`, `createdAt`, `isActive`)
                    VALUES (1, 'Plan Główny (A/B/C)', 'Domyślny 3-dniowy plan FBW z rozgrzewką i mobilizacją', 0, 1)
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "exercises", "planId", "INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "workout_plans", "workoutsRaw", "TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
                scope.launch(Dispatchers.IO) {
                    db.execSQL("""
                        INSERT OR IGNORE INTO `workout_plans` (`id`, `name`, `description`, `createdAt`, `isActive`)
                        VALUES (1, 'Plan Główny (A/B/C)', 'Domyślny 3-dniowy plan FBW z rozgrzewką i mobilizacją', ${System.currentTimeMillis()}, 1)
                    """.trimIndent())
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                scope.launch(Dispatchers.IO) {
                    try {
                        db.execSQL("UPDATE exercises SET planId = 1 WHERE planId <= 0")
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
