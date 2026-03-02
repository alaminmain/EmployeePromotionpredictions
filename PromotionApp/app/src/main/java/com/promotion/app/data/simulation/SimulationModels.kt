package com.promotion.app.data.simulation

import java.time.LocalDate

data class Employee(
    val EmpId: String,
    val Name: String,
    val DOB: LocalDate,
    val RetirementDate: LocalDate,
    val JoiningDate: LocalDate,
    val GradeNo: Int,
    val DesgNo: Int,
    val DesignationName: String,
    val LastPromotionDate: LocalDate?,
    val Status: String,
    var SeniorId: Int? = null,
    
    // Simulation state
    var SimGradeNo: Int = 0,
    var SimDesgNo: Int = 0,
    var SimSL_No: Int = 0,
    var SimTrack: String = "GENERAL",
    var SimLastPromoDate: LocalDate = LocalDate.now(),
    var IsRetired: Boolean = false
)

data class OrgPost(
    val SL_No: Int,
    val GradeNo: Int,
    val DesgNo: Int,
    val DesignationName: String,
    val Track: String?,
    val TotalPost: Int,
    val YearsNeededRaw: String?,
    val FeederPostsRaw: String?,
    val PromotionQuotaRaw: String?
) {
    fun getYearsRequired(): Int {
        if (YearsNeededRaw.isNullOrBlank()) return 0
        val digits = YearsNeededRaw.takeWhile { it.isDigit() || it == ' ' }.filter { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }

    fun getFeederPostIds(): List<Int> {
        if (FeederPostsRaw.isNullOrBlank() || FeederPostsRaw == "N/A") return emptyList()
        val cleaned = FeederPostsRaw.replace(" and ", ";").replace(",", ";")
        return cleaned.split(";")
            .map { it.trim() }
            .filter { it.toIntOrNull() != null }
            .map { it.toInt() }
    }

    fun getPromotionQuota(): Int {
        if (PromotionQuotaRaw.isNullOrBlank()) return 0
        val clean = PromotionQuotaRaw.replace("%", "").trim()
        val value = clean.toIntOrNull()
        return if (value != null) {
             if (value > 100) 100 else value
        } else 0
    }
}

data class EmpSl(
    val SlNo: Int,
    val IdNo: String
)
