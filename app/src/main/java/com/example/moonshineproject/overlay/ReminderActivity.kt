package com.example.moonshineproject.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.moonshineproject.service.SentinelService
import com.example.moonshineproject.ui.theme.MoonshineProjectTheme

class ReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mostra por cima do ecrã bloqueado
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        setContent {
            MoonshineProjectTheme {
                ReminderScreen(
                    onSleep = {
                        val intent = Intent(this, SentinelService::class.java).apply {
                            action = SentinelService.ACTION_START
                        }
                        ContextCompat.startForegroundService(this, intent)
                        finish()
                    },
                    onSnooze = {
                        // TODO: snooze logic
                        finish()
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ReminderScreen(
    onSleep: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("🌙", fontSize = 64.sp)

            Text(
                "Hora de descansar",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Já passaram 30 minutos de media.\nQuer ativar o monitor de sono?",
                color = Color(0xFF8888AA),
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSleep,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6))
            ) {
                Text("Vou dormir 😴", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Snooze 15 min", fontSize = 16.sp)
            }

            TextButton(onClick = onDismiss) {
                Text("Dispensar", color = Color(0xFF8888AA))
            }
        }
    }
}