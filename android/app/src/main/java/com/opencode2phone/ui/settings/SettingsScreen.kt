package com.opencode2phone.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Server Connection",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Configure your opencode bridge server address.\nThe server must be running on your Tailscale network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = uiState.host,
                onValueChange = viewModel::onHostChanged,
                label = { Text("Host") },
                placeholder = { Text("100.100.100.100") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.port,
                onValueChange = viewModel::onPortChanged,
                label = { Text("Port") },
                placeholder = { Text("3001") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = viewModel::testConnection,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoadingModels
            ) {
                if (uiState.isLoadingModels) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Test Connection")
            }

            uiState.connectionTestResult?.let { result ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.startsWith("Connected"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (uiState.models.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Available Models (${uiState.models.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                uiState.models.take(10).forEach { model ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "${model.providerID}/${model.id}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (uiState.models.size > 10) {
                    Text(
                        "... and ${uiState.models.size - 10} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "About",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Opencode2Phone v1.0.0\nConnects to opencode-ai bridge server via Tailscale.\n\nUses the opencode CLI directly - no API key required.\nAll processing happens on your computer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
