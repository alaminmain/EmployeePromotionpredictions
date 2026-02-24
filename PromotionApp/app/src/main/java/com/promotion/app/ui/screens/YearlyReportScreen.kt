package com.promotion.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promotion.app.data.local.YearlyCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyReportScreen(
    yearlyData: List<YearlyCount>,
    onBack: () -> Unit
) {
    val maxCount = yearlyData.maxOfOrNull { it.Count } ?: 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        TopAppBar(
            title = { Text("Yearly Promotion Report", fontWeight = FontWeight.Bold) },
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

        // Summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox("Total Years", yearlyData.size.toString(), Color(0xFF6366F1))
                StatBox("Total Promotions", yearlyData.sumOf { it.Count }.toString(), Color(0xFF22D3EE))
                StatBox("Avg/Year", if (yearlyData.isNotEmpty()) String.format("%.1f", yearlyData.sumOf { it.Count }.toDouble() / yearlyData.size) else "0", Color(0xFF10B981))
            }
        }

        // Bar chart list
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(yearlyData) { _, item ->
                YearlyBar(item, maxCount)
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
    }
}

@Composable
fun YearlyBar(item: YearlyCount, maxCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            item.Year,
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.width(50.dp)
        )

        val fraction = item.Count.toFloat() / maxCount.toFloat()
        Box(
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF334155))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            item.Count.toString(),
            color = Color(0xFF818CF8),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End
        )
    }
}
