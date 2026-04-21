package com.example.moonshineproject.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moonshineproject.data.AppPreferences
import com.example.moonshineproject.data.SentinelSettings

private val SPurple = Color(0xFF9B59B6)
private val SPurpleDark = Color(0xFF6C3483)
private val SPurpleLight = Color(0xFFBB8FCE)
private val SBgDark = Color(0xFF0D0D1A)
private val SBgCard = Color(0xFF13132A)
private val SBgCardLight = Color(0xFF1C1C3A)
private val STextMuted = Color(0xFF8888AA)

@Composable
fun SettingsScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val settings by prefs.settingsFlow.collectAsState()

    var afkSeconds by remember(settings) { mutableIntStateOf(settings.afkStillnessSeconds) }
    var reminderMinutes by remember(settings) { mutableIntStateOf(settings.reminderMinutes) }
    var marginDb by remember(settings) { mutableIntStateOf(settings.thresholdMarginDb) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D1A), Color(0xFF110D22), Color(0xFF0D0D1A))
                )
            )
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Toolbar ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = SPurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Configurações",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Ajusta o comportamento do Sentinel", color = STextMuted, fontSize = 12.sp)
            }
        }

        // ── Card de configurações ─────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = SBgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    "Deteção de sono",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                HorizontalDivider(color = SBgCardLight)

                SSettingSlider(
                    label = "Inatividade para detetar sono",
                    description = "Tempo sem movimento para considerar que adormeceste",
                    value = "${afkSeconds}s",
                    sliderValue = afkSeconds.toFloat(),
                    range = 10f..1800f,
                    onValueChange = { afkSeconds = it.toInt() }
                )

                SSettingSlider(
                    label = "Margem de ruído",
                    description = "Decibéis acima do ruído ambiente para registar evento",
                    value = "+${marginDb} dB",
                    sliderValue = marginDb.toFloat(),
                    range = 10f..30f,
                    onValueChange = { marginDb = it.toInt() }
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SBgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    "Lembretes",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                HorizontalDivider(color = SBgCardLight)

                SSettingSlider(
                    label = "Lembrete para ir dormir",
                    description = "Minutos de uso de media antes de sugerir descanso",
                    value = "${reminderMinutes} min",
                    sliderValue = reminderMinutes.toFloat(),
                    range = 1f..120f,
                    onValueChange = { reminderMinutes = it.toInt() }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                prefs.saveSettings(
                    SentinelSettings(
                        afkStillnessSeconds = afkSeconds,
                        reminderMinutes = reminderMinutes,
                        thresholdMarginDb = marginDb,
                        calibrationSeconds = settings.calibrationSeconds,
                        bedtimeHour = settings.bedtimeHour,
                        bedtimeMinute = settings.bedtimeMinute
                    )
                )
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SPurpleDark)
        ) {
            Text("Guardar e voltar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SSettingSlider(
    label: String,
    description: String,
    value: String,
    sliderValue: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(
                value,
                color = SPurpleLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
        }
        Text(description, color = STextMuted, fontSize = 11.sp)
        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = SPurple,
                activeTrackColor = SPurple,
                inactiveTrackColor = SBgCardLight
            )
        )
    }
}