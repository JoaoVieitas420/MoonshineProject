package com.example.moonshineproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moonshineproject.data.SleepRepository
import com.example.moonshineproject.data.SleepSessionUi

@Composable
fun SleepLogScreen(
    contentPadding: PaddingValues,
    onOpenSession: (Long) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SleepRepository(context) }
    val sessions by repository.observeSessions().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .padding(contentPadding)
            .padding(20.dp)
    ) {
        Text("Diário de Sono", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("${sessions.size} sessão(ões)", color = Color(0xFF999999))
        Spacer(modifier = Modifier.height(14.dp))

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sem sessões registadas ainda.", color = Color(0xFF777777))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(session = session, onOpen = { onOpenSession(session.id) })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: SleepSessionUi,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${session.dateLabel}", color = Color(0xFF9B59B6), fontWeight = FontWeight.Bold)
            Text("${session.startLabel} → ${session.endLabel}", color = Color(0xFFCFCFCF), fontSize = 13.sp)
            Text("Duração: ${session.durationLabel}", color = Color.White, fontWeight = FontWeight.Medium)
            Text("Eventos de áudio: ${session.eventCount}", color = Color(0xFFAAAAAA), fontSize = 12.sp)
        }
    }
}
