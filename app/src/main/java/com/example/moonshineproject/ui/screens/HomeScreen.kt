package com.example.moonshineproject.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.moonshineproject.data.AppPreferences
import com.example.moonshineproject.service.SentinelPhase
import com.example.moonshineproject.service.SentinelService
import com.example.moonshineproject.service.SentinelStatusStore

fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(context.packageName) == true
}

private fun isSentinelServiceRunning(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return manager.getRunningServices(Int.MAX_VALUE).any {
        it.service.className == SentinelService::class.java.name
    }
}

private val Purple = Color(0xFF9B59B6)
private val PurpleDark = Color(0xFF6C3483)
private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE74C3C)
private val BgCard = Color(0xFF13132A)
private val BgCardLight = Color(0xFF1C1C3A)
private val TextMuted = Color(0xFF8888AA)

@Composable
fun HomeScreen(contentPadding: PaddingValues, onSettingsClick: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val status by SentinelStatusStore.status.collectAsState()

    val hasMediaPermission = isNotificationListenerEnabled(context)

    LaunchedEffect(Unit) {
        if (prefs.sentinelExplicitlyActive && !isSentinelServiceRunning(context)) {
            prefs.sentinelExplicitlyActive = false
        }
    }

    val effectiveRunning = status.isServiceRunning || prefs.sentinelExplicitlyActive

    val statusColor by animateColorAsState(
        targetValue = when (status.phase) {
            SentinelPhase.INACTIVE -> TextMuted
            SentinelPhase.WATCHING_AWAKE -> Purple
            SentinelPhase.SLEEP_DETECTED -> Green
            SentinelPhase.MONITORING_SLEEP -> Green
        },
        animationSpec = tween(600), label = "statusColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D1A), Color(0xFF110D22), Color(0xFF0D0D1A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "🌙 Moonshine",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Monitor de Sono",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BgCard)
                            .border(1.dp, BgCardLight, CircleShape)
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ── Orb de estado ────────────────────────────────────────
            SentinelOrb(
                isRunning = effectiveRunning,
                phase = status.phase,
                statusColor = statusColor
            )

            // ── Status card ──────────────────────────────────────────
            StatusCard(
                phase = status.phase,
                statusMessage = status.statusMessage,
                isSleepDetected = status.isSleepDetected,
                statusColor = statusColor
            )

            // ── Aviso permissão media ────────────────────────────────
            if (!hasMediaPermission) {
                WarningCard(
                    message = "Permissão de controlo de media não ativa",
                    actionLabel = "Ativar",
                    onClick = {
                        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }
                )
            }

            // ── Botão principal ──────────────────────────────────────
            Button(
                onClick = {
                    val serviceIntent = Intent(context, SentinelService::class.java).apply {
                        action = if (!effectiveRunning) SentinelService.ACTION_START else SentinelService.ACTION_STOP
                    }
                    if (!effectiveRunning) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!effectiveRunning) Purple else Red
                )
            ) {
                Icon(
                    imageVector = if (!effectiveRunning) Icons.Default.PlayArrow else Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (!effectiveRunning) "Ativar Sentinel" else "Parar Sentinel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Orb animado ──────────────────────────────────────────────────────────────
@Composable
private fun SentinelOrb(
    isRunning: Boolean,
    phase: SentinelPhase,
    statusColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val scale = if (isRunning) pulse else 1f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(160.dp)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = if (isRunning) 0.15f else 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = if (isRunning) 0.3f else 0.1f),
                            BgCard
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(statusColor.copy(alpha = 0.8f), statusColor.copy(alpha = 0.2f))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val icon: ImageVector = when (phase) {
                SentinelPhase.INACTIVE -> Icons.Default.Bedtime
                SentinelPhase.WATCHING_AWAKE -> Icons.Default.Bedtime
                SentinelPhase.SLEEP_DETECTED -> Icons.Default.Bedtime
                SentinelPhase.MONITORING_SLEEP -> Icons.Default.GraphicEq
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

// ── Status card ───────────────────────────────────────────────────────────────
@Composable
private fun StatusCard(
    phase: SentinelPhase,
    statusMessage: String,
    isSleepDetected: Boolean,
    statusColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (phase) {
                        SentinelPhase.INACTIVE -> "Sentinel inativo"
                        SentinelPhase.WATCHING_AWAKE -> "Aguardando sono..."
                        SentinelPhase.SLEEP_DETECTED -> "Sono detetado"
                        SentinelPhase.MONITORING_SLEEP -> "Monitorização ativa"
                    },
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Text(
                text = statusMessage,
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            if (isSleepDetected) {
                HorizontalDivider(color = BgCardLight)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("😴", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Sono detetado com sucesso",
                        color = Green,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Warning card ──────────────────────────────────────────────────────────────
@Composable
private fun WarningCard(message: String, actionLabel: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1A0A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = Color(0xFFE67E22),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(message, color = Color(0xFFE67E22), fontSize = 13.sp)
            }
            TextButton(onClick = onClick) {
                Text(actionLabel, color = Color(0xFFE67E22), fontWeight = FontWeight.Bold)
            }
        }
    }
}