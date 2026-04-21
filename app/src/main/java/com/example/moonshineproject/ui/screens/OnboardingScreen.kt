package com.example.moonshineproject.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onFinish: () -> Unit, onSkip: () -> Unit = onFinish) {
    val context = LocalContext.current

    // 1. Estados para observar as permissões
    var audioGranted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    var overlayGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var notificationListenerGranted by remember {
        mutableStateOf(Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true)
    }

    // Launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        audioGranted = result[Manifest.permission.RECORD_AUDIO] == true
    }

    // Launcher genérico para quando voltamos das definições do sistema
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        overlayGranted = Settings.canDrawOverlays(context)
        notificationListenerGranted = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true
    }

    val permissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Moonshine Setup", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Text(
            "Vamos configurar o essencial para monitorizar o sono e gravar ruídos relevantes durante a noite.",
            color = Color(0xFFCCCCCC),
            style = MaterialTheme.typography.bodyMedium
        )

        // Passo 1 — Áudio
        SetupStep(
            title = "1) Permissões de áudio e notificações",
            description = if (audioGranted) "✅ Permissões concedidas! O Sentinel já pode ouvir e notificar."
            else "Necessário para detetar ruídos e mostrar estado do Sentinel.",
            buttonText = if (audioGranted) "Concedido ✓" else "Conceder permissões",
            buttonEnabled = !audioGranted
        ) {
            permissionLauncher.launch(permissions)
        }

        // Passo 2 — Controlo de media
        SetupStep(
            title = "2) Controlo de media",
            description = if (notificationListenerGranted) "✅ Acesso concedido! O Moonshine já pode pausar a sua música."
            else "Ativa o acesso de Notificação para o Moonshine conseguir pausar media ao adormecer.",
            buttonText = if (notificationListenerGranted) "Concedido ✓" else "Abrir definições",
            buttonEnabled = !notificationListenerGranted
        ) {
            settingsLauncher.launch(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        // Passo 3 — Overlay
        SetupStep(
            title = "3) Lembrete por cima da media",
            description = if (overlayGranted) "✅ Permissão concedida! O lembrete aparecerá por cima de qualquer app."
            else "Permite ao Moonshine mostrar um lembrete visual por cima da media quando for hora de dormir.",
            buttonText = if (overlayGranted) "Concedido ✓" else "Ativar overlay",
            buttonEnabled = !overlayGranted
        ) {
            settingsLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            )
        }

        // Passo 4 — Como usar (Sempre check)
        SetupStep(
            title = "4) Tudo pronto?",
            description = "Define o tempo de lembrete e inatividade no Início. Ativa o Sentinel antes de dormir. O resto é connosco!",
            buttonText = "Percebi",
            buttonEnabled = true
        ) { /* Apenas informativo */ }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6))
        ) {
            Text("Concluir onboarding")
        }

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFBDA8CF))
        ) {
            Text("Saltar por agora")
        }
    }
}


@Composable
private fun SetupStep(
    title: String,
    description: String,
    buttonText: String,
    buttonEnabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, color = Color(0xFFBBBBBB), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onClick,
                enabled = buttonEnabled
            ) {
                Text(buttonText)
            }
        }
    }
}