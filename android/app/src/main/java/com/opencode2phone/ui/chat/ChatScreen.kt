package com.opencode2phone.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode2phone.domain.model.Message
import com.opencode2phone.domain.model.MessageRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String?,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(sessionId) {
        viewModel.initSession(sessionId)
    }

    LaunchedEffect(uiState.messages.size, uiState.streamingContent) {
        if (uiState.messages.isNotEmpty() || uiState.streamingContent.isNotEmpty()) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopStreaming()
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = viewModel::toggleModelPicker) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Model")
                        }
                        ModelDropdown(
                            expanded = uiState.showModelPicker,
                            models = uiState.models,
                            selectedModel = uiState.selectedModel,
                            onSelect = viewModel::selectModel,
                            onDismiss = { viewModel.toggleModelPicker() }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column {
                    AnimatedVisibility(visible = uiState.isStreaming) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.inputText,
                            onValueChange = viewModel::onInputChanged,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            ),
                            maxLines = 4,
                            enabled = !uiState.isStreaming
                        )
                        Spacer(Modifier.width(8.dp))
                        if (uiState.isStreaming) {
                            IconButton(
                                onClick = viewModel::stopStreaming,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop")
                            }
                        } else {
                            IconButton(
                                onClick = viewModel::sendMessage,
                                enabled = uiState.inputText.isNotBlank(),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.messages.isEmpty() && uiState.isLoadingMessages) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Loading messages...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (uiState.messages.isEmpty() && !uiState.isStreaming && !uiState.isLoadingMessages) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Send a message to start chatting",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(uiState.messages, key = { it.id }) { message ->
                MessageBubble(message)
            }

            if (uiState.streamingContent.isNotEmpty()) {
                item {
                    StreamingBubble(uiState.streamingContent)
                }
            }

            if (uiState.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.error ?: "Unknown error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.dismissError() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelDropdown(
    expanded: Boolean,
    models: List<com.opencode2phone.domain.model.OpencodeModel>,
    selectedModel: com.opencode2phone.domain.model.OpencodeModel?,
    onSelect: (com.opencode2phone.domain.model.OpencodeModel) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        Text(
            "Select Model",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        HorizontalDivider()
        models.forEach { model ->
            DropdownMenuItem(
                text = {
                    Text(
                        "${model.providerID}/${model.id}",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                onClick = { onSelect(model) },
                leadingIcon = if (model == selectedModel) {
                    { Text("✓", style = MaterialTheme.typography.bodySmall) }
                } else null
            )
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == MessageRole.USER
    val containerColor = if (isUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (message.reasoning != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = message.reasoning,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 3
                )
            }
        }
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatTime(message.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun StreamingBubble(content: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "streaming...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
