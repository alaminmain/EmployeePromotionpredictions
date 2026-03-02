package com.promotion.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promotion.app.ui.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    apiUrl: String,
    lastSyncTime: String,
    syncStatus: SyncStatus,
    dbCount: Int,
    onApiUrlChange: (String) -> Unit,
    onSyncNow: () -> Unit,
    onSimulateLocal: () -> Unit,
    onSaveCsv: (Uri, String) -> Unit,
    onBack: () -> Unit
) {
    var urlInput by remember { mutableStateOf(apiUrl) }

    val empListLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onSaveCsv(it, "EmpList.csv") }
    }
    val orgPostLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onSaveCsv(it, "Final_Complete_Master_List.csv") }
    }
    val slListLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onSaveCsv(it, "EmpListSL.csv") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        TopAppBar(
            title = { Text("Settings & Sync", fontWeight = FontWeight.Bold) },
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

        Column(modifier = Modifier.padding(16.dp)) {
            // Database info
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Database", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$dbCount predictions stored locally", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Last sync: $lastSyncTime", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // API URL
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("API Server URL", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            onApiUrlChange(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("http://10.0.2.2:5223") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155),
                            cursorColor = Color(0xFF6366F1),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Use 10.0.2.2 for Android Emulator → localhost",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sync button
            Button(
                onClick = onSyncNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = syncStatus !is SyncStatus.Syncing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(12.dp)
            ) {
                when (syncStatus) {
                    is SyncStatus.Syncing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing...")
                    }
                    else -> {
                        Icon(Icons.Default.CloudSync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Now", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulate Local button
            Button(
                onClick = onSimulateLocal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = syncStatus !is SyncStatus.Syncing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), // Emerald
                shape = RoundedCornerShape(12.dp)
            ) {
                when (syncStatus) {
                    is SyncStatus.Syncing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulating Offline...")
                    }
                    else -> {
                        Icon(Icons.Default.CloudSync, contentDescription = null) // Using same icon for simplicity
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Offline (Device)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(16.dp))

            Text("Update Local Data", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // File pickers
            CsvPickerCard(
                title = "Employees List",
                subtitle = "EmpList.csv",
                onClick = { empListLauncher.launch("text/csv") } // Note: some devices might need "*/*" to show CSVs properly
            )
            Spacer(modifier = Modifier.height(8.dp))
            CsvPickerCard(
                title = "Seniority List",
                subtitle = "EmpListSL.csv",
                onClick = { slListLauncher.launch("text/csv") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            CsvPickerCard(
                title = "Organization Posts",
                subtitle = "Master List.csv",
                onClick = { orgPostLauncher.launch("text/csv") }
            )

            // Sync status message
            Spacer(modifier = Modifier.height(16.dp))
            when (syncStatus) {
                is SyncStatus.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(syncStatus.message, color = Color(0xFF10B981), fontSize = 14.sp)
                    }
                }
                is SyncStatus.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(syncStatus.message, color = Color(0xFFEF4444), fontSize = 14.sp)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun CsvPickerCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF6366F1))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        }
    }
}
