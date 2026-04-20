package com.example.moonshineproject.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.moonshineproject.data.SleepRepository
import com.example.moonshineproject.service.SentinelService

fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(context.packageName) == true
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val repo = remember { SleepRepository(context) }
    var sentinelActive by remember { mutableStateOf(false) }
    var hasMediaPermission by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var sentinelStartTime by remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌙", fontSize = 64.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Moonshine",
            fontSize = 36.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (sentinelActive) "● SENTINEL ATIVO" else "● SISTEMA PRONTO",
            fontSize = 12.sp,
            color = if (sentinelActive) Color(0xFF27AE60) else Color(0xFF9B59B6),
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Card de estado
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                StatusRow(label = "Acelerómetro", active = sentinelActive)
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(label = "Monitor de Sono", active = sentinelActive)
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(label = "Pausa de Media", active = hasMediaPermission)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botão permissão media
        if (!hasMediaPermission) {
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9B59B6)),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("⚠️ Ativar Permissão de Media")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Botão principal — Ativar / Parar
        Button(
            onClick = {
                hasMediaPermission = isNotificationListenerEnabled(context)
                if (!sentinelActive) {
                    sentinelStartTime = System.currentTimeMillis()
                    val intent = Intent(context, SentinelService::class.java)
                    intent.putExtra("startTime", sentinelStartTime)
                    ContextCompat.startForegroundService(context, intent)
                    sentinelActive = true
                } else {
                    // Para o sentinel e guarda horas dormidas
                    val endTime = System.currentTimeMillis()
                    val durationMs = endTime - sentinelStartTime
                    val hours = (durationMs / 3600000).toInt()
                    val minutes = ((durationMs % 3600000) / 60000).toInt()
                    repo.saveSession(sentinelStartTime, endTime)
                    context.stopService(Intent(context, SentinelService::class.java))
                    sentinelActive = false
                    sentinelStartTime = 0L
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (sentinelActive) Color(0xFF922B21) else Color(0xFF9B59B6)
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                text = if (sentinelActive) "⏹ Parar Sentinel" else "▶ Ativar Sentinel",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun StatusRow(label: String, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFAAAAAA), fontSize = 14.sp)
        Text(
            text = if (active) "ATIVO" else "STANDBY",
            color = if (active) Color(0xFF27AE60) else Color(0xFF555555),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}