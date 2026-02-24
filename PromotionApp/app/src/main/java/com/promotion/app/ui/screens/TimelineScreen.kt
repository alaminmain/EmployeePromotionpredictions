package com.promotion.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promotion.app.data.local.Prediction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    predictions: List<Prediction>,
    onBack: () -> Unit
) {
    val employeeName = predictions.firstOrNull()?.Name ?: "Employee"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top bar
        TopAppBar(
            title = {
                Column {
                    Text(employeeName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "${predictions.size} promotion steps",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1E293B),
                titleContentColor = Color.White
            )
        )

        // Timeline
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(predictions) { index, pred ->
                val yearsBetween = if (index > 0) {
                    calculateYearsBetween(predictions[index - 1].PredictedDate, pred.PredictedDate)
                } else null

                TimelineStep(
                    prediction = pred,
                    stepNumber = index + 1,
                    yearsBetween = yearsBetween,
                    isLast = index == predictions.lastIndex,
                    isRetirement = pred.NewDesignation == "Retirement"
                )
            }
        }
    }
}

@Composable
fun TimelineStep(
    prediction: Prediction,
    stepNumber: Int,
    yearsBetween: String?,
    isLast: Boolean,
    isRetirement: Boolean
) {
    val accentColor = if (isRetirement) Color(0xFFEF4444) else Color(0xFF6366F1)

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline line + dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(100.dp)
                        .background(Color(0xFF334155))
                )
            }
        }

        // Card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Grade transition
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(prediction.FromGrade, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(18.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(prediction.ToGrade, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Designation
                Text(
                    prediction.NewDesignation,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                // Date + years between
                Row {
                    Text(
                        prediction.PredictedDate,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    if (yearsBetween != null) {
                        Text(
                            "  (+$yearsBetween yrs)",
                            color = Color(0xFF22D3EE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

fun calculateYearsBetween(date1: String, date2: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val d1 = LocalDate.parse(date1.take(10), formatter)
        val d2 = LocalDate.parse(date2.take(10), formatter)
        val months = ChronoUnit.MONTHS.between(d1, d2)
        String.format("%.1f", months / 12.0)
    } catch (e: Exception) {
        "?"
    }
}
