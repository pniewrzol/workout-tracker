package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BodyMeasurement
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {
    @Query("SELECT * FROM body_measurements ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurements ORDER BY timestamp ASC")
    fun getAllMeasurementsAsc(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurements ORDER BY timestamp ASC")
    suspend fun getAllMeasurementsDirect(): List<BodyMeasurement>

    @Query("SELECT * FROM body_measurements WHERE id = :id LIMIT 1")
    suspend fun getMeasurementById(id: Long): BodyMeasurement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: BodyMeasurement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(measurements: List<BodyMeasurement>)

    @Update
    suspend fun update(measurement: BodyMeasurement)

    @Delete
    suspend fun delete(measurement: BodyMeasurement)

    @Query("DELETE FROM body_measurements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM body_measurements")
    suspend fun deleteAll()
}
