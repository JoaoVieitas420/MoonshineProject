package com.example.moonshineproject.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.moonshineproject.R
import com.example.moonshineproject.data.SleepEvent
import com.example.moonshineproject.data.SleepRepository
import com.example.moonshineproject.sensors.AudioMonitor
import com.example.moonshineproject.sensors.MotionDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SentinelService : Service() {

    private lateinit var motionDetector: MotionDetector
    private val audioMonitor = AudioMonitor()
    private var isAsleep = false // Garante que só ativa uma vez por sessão

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground(1, createNotification())
            motionDetector = MotionDetector(this) {
                if (!isAsleep) {
                    isAsleep = true
                    stopMediaAndLock()
                }
            }
            motionDetector.start()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopMediaAndLock() {
        updateNotification("Moonshine: Sono detetado. Media pausada. 😴")

        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = android.content.ComponentName(this, NotificationListener::class.java)
            val activeSessions = mediaSessionManager.getActiveSessions(componentName)

            for (session in activeSessions) {
                if (session.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    session.transportControls.pause()
                    Log.d("Moonshine", "PAUSADO: ${session.packageName}")
                }
            }
        } catch (e: Exception) {
            Log.e("Moonshine", "Erro ao pausar media: ${e.message}")
        }

        startSleepMonitoring()
    }

    private fun startSleepMonitoring() {
        updateNotification("Moonshine: A monitorizar sono... 💤")

        val outputDir = File(filesDir, "sleep_audio")
        val repo = SleepRepository(this)

        audioMonitor.startMonitoring(outputDir) { filePath, decibelLevel ->
            val now = Calendar.getInstance()
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)

            val event = SleepEvent(
                date = date,
                time = time,
                audioFilePath = filePath,
                decibelLevel = decibelLevel
            )

            repo.saveEvent(event)
            Log.d("Moonshine", "Evento guardado: $time - ${decibelLevel}dB")
            updateNotification("Moonshine: Agitação às $time (${decibelLevel}dB) 😟")
        }
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, createNotification(content))
    }

    private fun createNotification(content: String = "A monitorizar atividade..."): Notification {
        val channelId = "moonshine_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Moonshine Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Moonshine ativo 🌙")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    override fun onDestroy() {
        motionDetector.stop()
        audioMonitor.stopMonitoring()
        super.onDestroy()
    }
}