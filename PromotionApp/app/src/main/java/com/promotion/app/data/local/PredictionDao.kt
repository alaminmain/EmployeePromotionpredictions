package com.promotion.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionDao {

    @Query("""
        SELECT EmpId, Name, MIN(FromGrade) as FromGrade, MIN(SeniorId) as SeniorId
        FROM Predictions 
        GROUP BY EmpId, Name 
        ORDER BY CAST(REPLACE(MIN(FromGrade), 'G-', '') AS INTEGER) ASC,
                 CASE WHEN MIN(SeniorId) IS NULL THEN 1 ELSE 0 END ASC,
                 MIN(SeniorId) ASC,
                 EmpId ASC
    """)
    fun getAllEmployees(): Flow<List<EmployeeSummary>>

    @Query("""
        SELECT EmpId, Name, MIN(FromGrade) as FromGrade, MIN(SeniorId) as SeniorId
        FROM Predictions 
        WHERE Name LIKE '%' || :query || '%' OR EmpId LIKE '%' || :query || '%'
        GROUP BY EmpId, Name 
        ORDER BY CAST(REPLACE(MIN(FromGrade), 'G-', '') AS INTEGER) ASC,
                 CASE WHEN MIN(SeniorId) IS NULL THEN 1 ELSE 0 END ASC,
                 MIN(SeniorId) ASC,
                 EmpId ASC
    """)
    fun searchEmployees(query: String): Flow<List<EmployeeSummary>>

    @Query("SELECT * FROM Predictions WHERE EmpId = :empId ORDER BY PredictedDate")
    fun getPredictions(empId: String): Flow<List<Prediction>>

    @Query("""
        SELECT substr(PredictedDate, 1, 4) as Year, COUNT(*) as Count 
        FROM Predictions 
        WHERE NewDesignation != 'Retirement'
        GROUP BY substr(PredictedDate, 1, 4) 
        ORDER BY Year
    """)
    fun getYearlyReport(): Flow<List<YearlyCount>>

    @Query("SELECT COUNT(*) FROM Predictions")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(predictions: List<Prediction>)

    @Query("DELETE FROM Predictions")
    suspend fun clearAll()
}
