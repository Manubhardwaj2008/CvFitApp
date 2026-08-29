package com.manu.bsw017

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private const val TARGET_MAC = "F8:0F:75:E0:06:AE"

@Composable
fun HealthMonitorScreen(
    viewModel: HealthMonitorViewModel = viewModel(),
    showTitle: Boolean = true
) {
    ParentTheme {
        val context = LocalContext.current
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            if (result.values.all { it }) viewModel.startScan()
            else viewModel.status = "Bluetooth permission denied"
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (showTitle) 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showTitle) {
                Text("Fire-Boltt BSW017", style = MaterialTheme.typography.headlineSmall)
                Text("MAC: $TARGET_MAC")
            }
            Text("Status: ${viewModel.status}")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    viewModel.ensurePermissionsAndScan(context) { permissions ->
                        permissionLauncher.launch(permissions)
                    }
                }) { Text("Scan & Connect") }
                OutlinedButton(onClick = { viewModel.disconnect() }) { Text("Disconnect") }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Activity", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${viewModel.steps} Steps",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("${viewModel.calories} kcal")
                        Text("${viewModel.distance / 100.0} km")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Heart Rate", style = MaterialTheme.typography.titleSmall)
                    Text(
                        viewModel.heartRate?.let { "$it BPM" } ?: "Waiting...",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Battery", style = MaterialTheme.typography.titleSmall)
                    Text(
                        viewModel.battery?.let { "$it%" } ?: "Not read",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Backend Sync", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { viewModel.syncToBackend() }, enabled = !viewModel.isSyncing) {
                            Text(if (viewModel.isSyncing) "Syncing..." else "Sync Now")
                        }
                    }
                    Text(
                        "Status: ${viewModel.syncStatus}",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
