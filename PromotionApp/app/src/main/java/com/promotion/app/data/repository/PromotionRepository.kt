package com.promotion.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.promotion.app.data.local.AppDatabase
import com.promotion.app.data.local.EmployeeSummary
import com.promotion.app.data.local.Prediction
import com.promotion.app.data.local.YearlyCount
import com.promotion.app.data.remote.PromotionApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class PromotionRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.predictionDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("promotion_prefs", Context.MODE_PRIVATE)

    // --- Local queries ---

    fun getAllEmployees(): Flow<List<EmployeeSummary>> = dao.getAllEmployees()

    fun searchEmployees(query: String): Flow<List<EmployeeSummary>> = dao.searchEmployees(query)

    fun getPredictions(empId: String): Flow<List<Prediction>> = dao.getPredictions(empId)

    fun getYearlyReport(): Flow<List<YearlyCount>> = dao.getYearlyReport()

    suspend fun getCount(): Int = dao.getCount()

    // --- Sync ---

    fun getApiUrl(): String = prefs.getString("api_url", "http://10.0.2.2:5223") ?: "http://10.0.2.2:5223"

    fun setApiUrl(url: String) = prefs.edit().putString("api_url", url).apply()

    fun getLastSyncTime(): String = prefs.getString("last_sync", "Never") ?: "Never"

    private fun createApi(): PromotionApiService {
        return Retrofit.Builder()
            .baseUrl(getApiUrl().trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PromotionApiService::class.java)
    }

    suspend fun syncFromApi(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val api = createApi()
            val employees = api.getEmployees()

            val allPredictions = mutableListOf<Prediction>()
            var idCounter = 1

            for (emp in employees) {
                val wrapper = api.getPredictions(emp.empId)
                for (pred in wrapper.predictions) {
                    allPredictions.add(
                        Prediction(
                            Id = idCounter++,
                            EmpId = pred.empId,
                            Name = pred.name,
                            FromGrade = pred.fromGrade,
                            ToGrade = pred.toGrade,
                            NewDesignation = pred.newDesignation,
                            PredictedDate = pred.predictedDate
                        )
                    )
                }
            }

            // Replace all local data
            dao.clearAll()
            dao.insertAll(allPredictions)

            // Save sync timestamp
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            prefs.edit().putString("last_sync", sdf.format(Date())).apply()

            Result.success(allPredictions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
