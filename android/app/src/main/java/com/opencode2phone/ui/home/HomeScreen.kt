package com.opencode2phone.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode2phone.domain.model.Session
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onChatClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (uiState.phase == HomePhase.SESSION_LIST && uiState.selectedDirectory != null)
                                extractFolderName(uiState.selectedDirectory!!)
                            else
                                "Opencode2Phone",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.phase == HomePhase.SESSION_LIST && uiState.selectedDirectory != null) {
                            Text(
                                uiState.selectedDirectory!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.phase == HomePhase.SESSION_LIST) {
                        IconButton(onClick = viewModel::changeDirectory) {
                            Icon(Icons.Default.Folder, contentDescription = "Change folder")
                        }
                        IconButton(onClick = viewModel::refreshSessions) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    } else {
                        IconButton(onClick = viewModel::loadDirectories) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (uiState.phase == HomePhase.SESSION_LIST) {
                ExtendedFloatingActionButton(
                    onClick = onNewChatClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Chat") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null && uiState.directories.isEmpty() -> {
                    errorView(uiState.error!!, viewModel, onSettingsClick)
                }
                uiState.phase == HomePhase.DIRECTORY_PICKER -> {
                    directoryPicker(uiState.directories, viewModel::selectDirectory)
                }
                uiState.phase == HomePhase.SESSION_LIST -> {
                    sessionList(uiState, viewModel, onChatClick)
                }
            }
        }
    }
}

@Composable
private fun errorView(error: String, viewModel: HomeViewModel, onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Connection Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Text(
            error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Check server address in Settings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::loadDirectories) {
                Text("Retry")
            }
            OutlinedButton(onClick = onSettingsClick) {
                Text("Settings")
            }
        }
    }
    }
}

@Composable
private fun directoryPicker(directories: List<String>, onSelect: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Select Project Folder",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                "Choose a project folder to view its chat sessions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (directories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No project folders found.\nStart a chat on your computer first.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(directories) { dir ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(dir) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            extractFolderName(dir),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            dir,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun sessionList(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    onChatClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 8.dp, bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.isRefreshing) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        if (uiState.sessions.isEmpty() && !uiState.isRefreshing) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No sessions in this folder.\nStart a new chat!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(uiState.sessions, key = { it.id }) { session ->
            SessionCard(session = session, onClick = { onChatClick(session.id) })
        }
    }
}

@Composable
private fun SessionCard(session: Session, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTimestamp(session.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${session.messageCount} messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun extractFolderName(path: String): String {
    val parts = path.replace("\\", "/").trimEnd('/').split("/")
    return parts.lastOrNull() ?: path
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
