package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val weightKg: Float? = null,
    val bodyFatPercentage: Float? = null,
    val chestCm: Float? = null,
    val waistCm: Float? = null,
    val bicepsCm: Float? = null,
    val hipsCm: Float? = null,
    val thighsCm: Float? = null,
    val calvesCm: Float? = null,
    val shouldersCm: Float? = null,
    val notes: String = "",
    val frontPhotoUri: String? = null,
    val backPhotoUri: String? = null,
    val sidePhotoUri: String? = null
)
