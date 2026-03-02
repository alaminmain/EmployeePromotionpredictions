package com.promotion.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Predictions",
    indices = [
        Index(value = ["PredictedDate"], name = "idx_date"),
        Index(value = ["EmpId"], name = "idx_empid")
    ]
)
data class Prediction(
    @PrimaryKey(autoGenerate = true) val Id: Int = 0,
    val EmpId: String,
    val Name: String,
    val FromGrade: String,
    val ToGrade: String,
    val NewDesignation: String,
    val PredictedDate: String,
    val SeniorId: Int? = null,
    @ColumnInfo(name = "CreatedAt", defaultValue = "CURRENT_TIMESTAMP")
    val CreatedAt: String? = null
)

data class EmployeeSummary(
    val EmpId: String,
    val Name: String,
    val FromGrade: String,
    val SeniorId: Int?
)

data class YearlyCount(
    val Year: String,
    val Count: Int = 0
)
