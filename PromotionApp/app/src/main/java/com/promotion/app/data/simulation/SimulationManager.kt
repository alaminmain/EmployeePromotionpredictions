package com.promotion.app.data.simulation

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.promotion.app.data.local.Prediction
import com.promotion.app.data.local.PredictionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

class SimulationManager(
    private val context: Context,
    private val predictionDao: PredictionDao
) {
    private val empCsvPath = "EmpList.csv"
    private val orgCsvPath = "Final_Complete_Master_List.csv"
    private val slCsvPath = "EmpListSL.csv"

    suspend fun runSimulation(): Int = withContext(Dispatchers.IO) {
        val employees = loadEmployees(empCsvPath)
        val orgPosts = loadOrgPosts(orgCsvPath)
        updateSeniorIdFromSlCsv(employees, slCsvPath)

        val activeEmployees = employees
            .filter { it.Status == "Regular" || it.Status == "Prl" }

        initializeEmployeeTrack(activeEmployees, orgPosts)

        // Clear existing predictions
        predictionDao.clearAll()

        val predictions = runSimulationCore(activeEmployees, orgPosts)
        
        // Save to DB
        predictionDao.insertAll(predictions)
        
        return@withContext predictions.size
    }

    private fun getInputStream(path: String): java.io.InputStream {
        val localFile = java.io.File(context.filesDir, path)
        return if (localFile.exists()) {
            java.io.FileInputStream(localFile)
        } else {
            context.assets.open(path)
        }
    }

    private fun loadEmployees(path: String): List<Employee> {
        val employees = mutableListOf<Employee>()
        val dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy")

        getInputStream(path).use { inputStream ->
            csvReader { 
                skipEmptyLine = true 
            }.open(inputStream) {
                readAllWithHeaderAsSequence().forEach { row ->
                    try {
                        val joiningStr = row["jjoin_date"] ?: return@forEach
                        // Gracefully skip empty/bad rows
                        if (joiningStr.isBlank() || row["emp_id"].isNullOrBlank()) return@forEach

                        val joiningDate = LocalDate.parse(joiningStr, dateFormatter)
                        val lastPromoStr = row["lastpromotiondate"]
                        val lastPromoDate = if (!lastPromoStr.isNullOrBlank()) {
                            LocalDate.parse(lastPromoStr, dateFormatter)
                        } else null

                        val emp = Employee(
                            EmpId = row["emp_id"] ?: "",
                            Name = row["emp_nm"] ?: "",
                            DOB = LocalDate.parse(row["dob"] ?: "1/1/1900", dateFormatter),
                            RetirementDate = LocalDate.parse(row["retr_dt"] ?: "1/1/9999", dateFormatter),
                            JoiningDate = joiningDate,
                            GradeNo = row["grade_no"]?.toIntOrNull() ?: 0,
                            DesgNo = row["desg_no"]?.toIntOrNull() ?: 0,
                            DesignationName = row["desg_nm"] ?: "",
                            LastPromotionDate = lastPromoDate,
                            Status = row["status"] ?: ""
                        )
                        
                        emp.SimGradeNo = emp.GradeNo
                        emp.SimDesgNo = emp.DesgNo
                        emp.SimLastPromoDate = emp.LastPromotionDate ?: emp.JoiningDate
                        emp.IsRetired = false
                        
                        employees.add(emp)
                    } catch (e: Exception) {
                        e.printStackTrace() // Skip invalid rows
                    }
                }
            }
        }
        return employees
    }

    private fun updateSeniorIdFromSlCsv(employees: List<Employee>, path: String) {
        val slDict = mutableMapOf<String, Int>()
        getInputStream(path).use { inputStream ->
            csvReader { 
                skipEmptyLine = true 
            }.open(inputStream) {
                readAllWithHeaderAsSequence().forEach { row ->
                    val idNo = row["ID No."] ?: ""
                    val slNo = row["SL. No."]?.toIntOrNull()
                    if (idNo.isNotBlank() && slNo != null) {
                        slDict[idNo.replace("'", "").trim()] = slNo
                    }
                }
            }
        }

        for (emp in employees) {
            emp.SeniorId = null
            if (emp.EmpId.isNotBlank()) {
                val cleanId = emp.EmpId.replace("'", "").trim()
                slDict[cleanId]?.let { slNo ->
                    emp.SeniorId = slNo
                }
            }
        }
    }

    private fun loadOrgPosts(path: String): List<OrgPost> {
        val posts = mutableListOf<OrgPost>()
        getInputStream(path).use { inputStream ->
            csvReader { 
                skipEmptyLine = true 
            }.open(inputStream) {
                readAllWithHeaderAsSequence().forEach { row ->
                    try {
                        posts.add(OrgPost(
                            SL_No = row["SL_No"]?.toIntOrNull() ?: 0,
                            GradeNo = row["grade_no"]?.toIntOrNull() ?: 0,
                            DesgNo = row["desg_no"]?.toIntOrNull() ?: 0,
                            DesignationName = row["desg_nm"] ?: "",
                            Track = row["IT_or_General"],
                            TotalPost = row["TotalPost"]?.toIntOrNull() ?: 0,
                            YearsNeededRaw = row["YearNeedtobepromoted"],
                            FeederPostsRaw = row["PromotionFromPostSL"],
                            PromotionQuotaRaw = row["PreviousPostPercentise"]
                        ))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return posts
    }

    private fun initializeEmployeeTrack(employees: List<Employee>, orgPosts: List<OrgPost>) {
        val postLookup = orgPosts.associateBy { Pair(it.GradeNo, it.DesgNo) }
        for (emp in employees) {
            val targetPost = postLookup[Pair(emp.GradeNo, emp.DesgNo)]
            if (targetPost != null) {
                emp.SimSL_No = targetPost.SL_No
                emp.SimTrack = if (targetPost.Track?.uppercase() == "IT") "IT" else "GENERAL"
            } else {
                emp.SimSL_No = 999
                emp.SimTrack = "GENERAL"
            }
        }
    }

    private fun runSimulationCore(employees: List<Employee>, orgPosts: List<OrgPost>): List<Prediction> {
        val predictions = mutableListOf<Prediction>()
        val currentYr = LocalDate.now().year
        var startYear = currentYr
        if (LocalDate.now().monthValue > 7) startYear++

        for (year in startYear..startYear + 40) {
            val currentDate = LocalDate.of(year, 7, 1)

            // A. Retirements
            val retiringNow = employees.filter { 
                !it.IsRetired && it.RetirementDate <= currentDate && it.RetirementDate > currentDate.minusYears(1) 
            }
            for (emp in retiringNow) {
                emp.IsRetired = true
                predictions.add(Prediction(
                    EmpId = emp.EmpId,
                    Name = emp.Name,
                    FromGrade = "G-${emp.SimGradeNo}",
                    ToGrade = "RETIRED",
                    NewDesignation = "Retirement",
                    PredictedDate = emp.RetirementDate.toString()
                ))
            }

            // B. Promotions
            for (targetPost in orgPosts.sortedBy { it.SL_No }) {
                val quota = targetPost.getPromotionQuota()
                if (quota == 0) continue
                val feederIds = targetPost.getFeederPostIds()
                if (feederIds.isEmpty()) continue

                val currentOccupants = employees.count { !it.IsRetired && it.SimSL_No == targetPost.SL_No }
                val maxPromotable = ceil(targetPost.TotalPost * (quota / 100.0)).toInt()
                val totalVacancy = targetPost.TotalPost - currentOccupants

                if (totalVacancy <= 0) continue
                val promoSlots = minOf(totalVacancy, maxOf(0, maxPromotable - currentOccupants))
                if (promoSlots <= 0) continue

                val feederPosts = orgPosts.filter { it.SL_No in feederIds }
                var candidates = mutableListOf<Employee>()
                for (feeder in feederPosts) {
                    candidates.addAll(employees.filter { !it.IsRetired && it.SimSL_No == feeder.SL_No })
                }

                val targetTrack = if (targetPost.Track?.uppercase() == "IT") "IT" else "GENERAL"
                val isMergePoint = targetPost.GradeNo <= 2 && targetTrack == "GENERAL"

                if (!isMergePoint) {
                    candidates = candidates.filter { it.SimTrack == targetTrack }.toMutableList()
                }

                val yearsNeeded = targetPost.getYearsRequired()

                val eligible = candidates
                    .filter { ChronoUnit.DAYS.between(it.SimLastPromoDate, currentDate) / 365.25 >= yearsNeeded }
                    .sortedWith(
                        compareBy<Employee> { it.GradeNo }
                            .thenBy { it.SeniorId ?: Int.MAX_VALUE }
                            .thenBy { it.EmpId }
                    )
                    .take(promoSlots)

                for (winner in eligible) {
                    predictions.add(Prediction(
                        EmpId = winner.EmpId,
                        Name = winner.Name,
                        FromGrade = "G-${winner.SimGradeNo}",
                        ToGrade = "G-${targetPost.GradeNo}",
                        NewDesignation = targetPost.DesignationName,
                        PredictedDate = currentDate.toString()
                    ))

                    winner.SimGradeNo = targetPost.GradeNo
                    winner.SimSL_No = targetPost.SL_No
                    winner.SimTrack = if (isMergePoint) "GENERAL" else targetTrack
                    winner.SimLastPromoDate = currentDate
                }
            }
        }
        return predictions
    }
}
