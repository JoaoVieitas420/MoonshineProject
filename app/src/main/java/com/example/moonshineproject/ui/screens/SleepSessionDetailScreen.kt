package com.example.moonshineproject.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moonshineproject.data.SleepEvent
import com.example.moonshineproject.data.SleepRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SleepSessionDetailScreen(
    contentPadding: PaddingValues,
    sessionId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SleepRepository(context) }
    val scope = rememberCoroutineScope()
    val detail by repository.observeSessionDetail(sessionId).collectAsState(initial = null)

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentlyPlayingId by remember { mutableLongStateOf(-1L) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    var renameDialogEvent by remember { mutableStateOf<SleepEvent?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Detalhe da sessão", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(1.dp))
        }

        val session = detail?.session
        if (session == null) {
            Text("Sessão não encontrada.", color = Color(0xFFAAAAAA))
            return@Column
        }

        val end = session.sleepEndTime ?: System.currentTimeMillis()
        val durationMinutes = ((end - session.sleepStartTime) / 60000).toInt().coerceAtLeast(0)
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(session.sleepStartTime)),
                    color = Color(0xFF9B59B6),
                    fontWeight = FontWeight.Bold
                )
                Text("Duração: ${hours}h ${minutes}m", color = Color.White)
                Text(
                    "Início: ${formatTime(session.sleepStartTime)}  •  Fim: ${session.sleepEndTime?.let { formatTime(it) } ?: "--"}",
                    color = Color(0xFFBEBEBE),
                    fontSize = 13.sp
                )
            }
        }

        Text("Eventos de áudio", color = Color.White, fontWeight = FontWeight.SemiBold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(detail?.events.orEmpty(), key = { it.id }) { event ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(event.title, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(
                            "${formatTime(event.timestamp)} • ${event.decibelLevel} dB",
                            color = Color(0xFFB9B9B9),
                            fontSize = 12.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(onClick = {
                                if (currentlyPlayingId == event.id) {
                                    mediaPlayer?.stop()
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    currentlyPlayingId = -1L
                                } else {
                                    runCatching {
                                        mediaPlayer?.release()
                                        mediaPlayer = MediaPlayer().apply {
                                            setDataSource(event.audioFilePath)
                                            prepare()
                                            start()
                                            setOnCompletionListener {
                                                currentlyPlayingId = -1L
                                                release()
                                            }
                                        }
                                        currentlyPlayingId = event.id
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (currentlyPlayingId == event.id) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color(0xFF9B59B6)
                                )
                            }

                            IconButton(onClick = {
                                renameDialogEvent = event
                                renameText = event.title
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFFBDBDBD))
                            }

                            IconButton(onClick = {
                                scope.launch { repository.setFavorite(event.id, !event.isFavorite) }
                            }) {
                                Icon(
                                    imageVector = if (event.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (event.isFavorite) Color(0xFFE74C3C) else Color(0xFFBDBDBD)
                                )
                            }

                            IconButton(onClick = {
                                scope.launch {
                                    val deleted = repository.deleteEvent(event.id)
                                    deleted?.audioFilePath?.let { File(it).delete() }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE57373))
                            }
                        }
                    }
                }
            }
        }
    }

    if (renameDialogEvent != null) {
        AlertDialog(
            onDismissRequest = { renameDialogEvent = null },
            title = { Text("Renomear áudio") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Nome") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val event = renameDialogEvent ?: return@TextButton
                    scope.launch { repository.renameEvent(event.id, renameText.ifBlank { event.title }) }
                    renameDialogEvent = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogEvent = null }) { Text("Cancelar") }
            }
        )
    }
}

private fun formatTime(value: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(value))
}