package com.promotion.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Predictions")
data class Prediction(
    @PrimaryKey val Id: Int,
    val EmpId: String,
    val Name: String,
    val FromGrade: String,
    val ToGrade: String,
    val NewDesignation: String,
    val PredictedDate: String,
    val CreatedAt: String? = null
)

data class EmployeeSummary(
    val EmpId: String,
    val Name: String,
    val FromGrade: String
)

data class YearlyCount(
    val Year: String,
    val Count: Int
)
