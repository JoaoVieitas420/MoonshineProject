package com.example.moonshineproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.moonshineproject.data.SleepEvent
import com.example.moonshineproject.data.SleepRepository
import com.example.moonshineproject.data.SleepSession

@Composable
fun SleepLogScreen() {
    val context = LocalContext.current
    val repo = remember { SleepRepository(context) }
    var sessions by remember { mutableStateOf<List<SleepSession>>(emptyList()) }
    var eventsByDate by remember { mutableStateOf<Map<String, List<SleepEvent>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        sessions = repo.getAllSessions()
        eventsByDate = repo.getAllEvents().groupBy { it.date }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🌙 Diário de Sono",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${sessions.size} sessão(ões) registada(s)",
            color = Color(0xFF888888),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😴", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nenhum registo ainda.", color = Color(0xFF888888), fontSize = 16.sp)
                    Text("Ativa o Sentinel esta noite!", color = Color(0xFF555555), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(sessions) { session ->
                    val events = eventsByDate[session.date] ?: emptyList()
                    SessionCard(session = session, events = events)
                }
            }
        }
    }
}

@Composable
fun SessionCard(session: SleepSession, events: List<SleepEvent>) {
    val hours = session.durationMinutes / 60
    val minutes = session.durationMinutes % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header da sessão
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 ${session.date}",
                    color = Color(0xFF9B59B6),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1A3E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${hours}h ${minutes}m",
                        color = Color(0xFF9B59B6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horário
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("🛏 ${session.startTime}", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                Text("☀️ ${session.endTime}", color = Color(0xFFAAAAAA), fontSize = 13.sp)
            }

            // Qualidade do sono
            Spacer(modifier = Modifier.height(8.dp))
            val quality = when {
                hours >= 7 -> Pair("Boa noite 😊", Color(0xFF1D6A39))
                hours >= 5 -> Pair("Sono curto 😐", Color(0xFF784212))
                else -> Pair("Sono insuficiente 😟", Color(0xFF922B21))
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = quality.second),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = quality.first,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Eventos de áudio
            if (events.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2A2A3E))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🎙 ${events.size} evento(s) de áudio",
                    color = Color(0xFF888888),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                events.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when {
                                event.decibelLevel > 70 -> "😱 ${event.time}"
                                event.decibelLevel > 60 -> "😟 ${event.time}"
                                else -> "💤 ${event.time}"
                            },
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${event.decibelLevel} dB",
                            color = when {
                                event.decibelLevel > 70 -> Color(0xFFE74C3C)
                                event.decibelLevel > 60 -> Color(0xFFE67E22)
                                else -> Color(0xFF27AE60)
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}