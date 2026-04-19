package com.example.moonshineproject.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import com.example.moonshineproject.service.SentinelService
import kotlin.jvm.java

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌙 Moonshine",
            fontSize = 32.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "SYSTEM READY",
            fontSize = 12.sp,
            color = Color(0xFF9B59B6),
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                val intent = Intent(context, SentinelService::class.java)
                context.startForegroundService(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9B59B6)
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Activate Sentinel", color = Color.White, fontSize = 16.sp)
        }
    }
}