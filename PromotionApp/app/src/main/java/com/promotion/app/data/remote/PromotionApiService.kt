package com.promotion.app.data.remote

import com.promotion.app.data.local.Prediction
import com.promotion.app.data.local.YearlyCount
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class EmployeeResponse(
    val empId: String,
    val name: String,
    val currentGrade: String
)

data class PredictionResponse(
    val step: Int,
    val empId: String,
    val name: String,
    val fromGrade: String,
    val toGrade: String,
    val newDesignation: String,
    val predictedDate: String
)

data class PredictionsWrapper(
    val employeeId: String,
    val predictions: List<PredictionResponse>
)

data class YearlyResponse(
    val year: String,
    val count: Int
)

data class SimulationResult(
    val message: String
)

interface PromotionApiService {

    @GET("api/employees")
    suspend fun getEmployees(): List<EmployeeResponse>

    @GET("api/predictions/{empId}")
    suspend fun getPredictions(@Path("empId") empId: String): PredictionsWrapper

    @GET("api/reports/promotions-per-year")
    suspend fun getYearlyReport(): List<YearlyResponse>

    @POST("api/simulation/run")
    suspend fun runSimulation(): SimulationResult
}
