package com.example.moonshineproject.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.moonshineproject.R
import com.example.moonshineproject.data.AppPreferences
import com.example.moonshineproject.data.SleepRepository
import com.example.moonshineproject.sensors.AudioMonitor
import com.example.moonshineproject.sensors.MotionDetector
import com.example.moonshineproject.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.jvm.java

class SentinelService : Service() {

    private val audioMonitor = AudioMonitor()
    private lateinit var motionDetector: MotionDetector

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: SleepRepository
    private lateinit var prefs: AppPreferences

    private var reminderJob: Job? = null
    private var currentSessionId: Long? = null
    private var isSleeping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = SleepRepository(this)
        prefs = AppPreferences.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> {
                startSentinel()
                START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopSentinel("Sentinel desativado manualmente")
                START_NOT_STICKY
            }
            else -> {
                // No app restart the service should not relaunch itself unless explicit action exists.
                stopSelf()
                START_NOT_STICKY
            }
        }
    }

    private fun startSentinel() {
        prefs.sentinelExplicitlyActive = true
        val settings = prefs.loadSettings()

        startForeground(NOTIFICATION_ID, buildNotification("Sentinel ativo - à espera de inatividade"))
        SentinelStatusStore.update(
            SentinelUiStatus(
                phase = SentinelPhase.WATCHING_AWAKE,
                isServiceRunning = true,
                isSleepDetected = false,
                statusMessage = "A monitorizar movimento..."
            )
        )

        motionDetector = MotionDetector(
            context = this,
            stillnessSecondsForSleep = settings.afkStillnessSeconds,
            onSleepDetected = { onSleepDetected(settings.calibrationSeconds, settings.thresholdMarginDb) },
            onHeavyMovementDetected = { onHeavyMovementDetected() }
        )
        motionDetector.start()

        reminderJob?.cancel()
        reminderJob = serviceScope.launch {
            delay(settings.reminderMinutes * 60_000L)
            if (!isSleeping) {
                // Mostra o overlay por cima da media
                val overlayIntent = Intent(
                    this@SentinelService,
                    com.example.moonshineproject.overlay.ReminderOverlayService::class.java
                ).apply {
                    action = com.example.moonshineproject.overlay.ReminderOverlayService.ACTION_SHOW
                }
                startService(overlayIntent)

                SentinelStatusStore.update(
                    SentinelUiStatus(
                        phase = SentinelPhase.WATCHING_AWAKE,
                        isServiceRunning = true,
                        isSleepDetected = false,
                        statusMessage = "Lembrete enviado para dormir"
                    )
                )
            }
        }
    }

    private fun onSleepDetected(calibrationSeconds: Int, thresholdMarginDb: Int) {
        if (isSleeping) return
        isSleeping = true
        reminderJob?.cancel()

        updateNotification("Sono detetado. A pausar media e iniciar monitorização...")
        SentinelStatusStore.update(
            SentinelUiStatus(
                phase = SentinelPhase.SLEEP_DETECTED,
                isServiceRunning = true,
                isSleepDetected = true,
                statusMessage = "Sono detetado — monitorização ativa"
            )
        )

        pauseMediaPlayback()

        serviceScope.launch {
            val sessionId = repository.startSleepSession(System.currentTimeMillis())
            currentSessionId = sessionId

            val outputDir = File(filesDir, "sleep_audio")
            audioMonitor.startMonitoring(
                outputDir = outputDir,
                calibrationSeconds = calibrationSeconds,
                thresholdMarginDb = thresholdMarginDb
            ) { filePath, peakDb, timestamp ->
                val activeSession = currentSessionId ?: return@startMonitoring
                serviceScope.launch {
                    repository.saveEvent(
                        sessionId = activeSession,
                        timestamp = timestamp,
                        audioFilePath = filePath,
                        decibelLevel = peakDb
                    )
                }
                updateNotification("Ruído relevante gravado (${peakDb} dB)")
                SentinelStatusStore.update(
                    SentinelUiStatus(
                        phase = SentinelPhase.MONITORING_SLEEP,
                        isServiceRunning = true,
                        isSleepDetected = true,
                        statusMessage = "Sono monitorizado — clipes capturados"
                    )
                )
            }
        }
    }

    private fun onHeavyMovementDetected() {
        if (!isSleeping) return
        stopSentinel("Movimento intenso detetado - sessão terminada")
    }

    private fun stopSentinel(reason: String) {
        serviceScope.launch {
            currentSessionId?.let { sessionId ->
                repository.endSleepSession(sessionId, System.currentTimeMillis())
            }
            currentSessionId = null
        }

        reminderJob?.cancel()
        audioMonitor.stopMonitoring()
        if (::motionDetector.isInitialized) motionDetector.stop()

        isSleeping = false
        prefs.sentinelExplicitlyActive = false

        SentinelStatusStore.update(
            SentinelUiStatus(
                phase = SentinelPhase.INACTIVE,
                isServiceRunning = false,
                isSleepDetected = false,
                statusMessage = reason
            )
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pauseMediaPlayback() {
        runCatching {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(this, NotificationListener::class.java)
            val sessions = mediaSessionManager.getActiveSessions(componentName)
            sessions.forEach { controller ->
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                }
            }
        }.onFailure {
            Log.e("Moonshine", "Erro ao pausar media", it)
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            100,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, SentinelService::class.java).apply { action = ACTION_STOP }
        val pendingStop = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Moonshine Sentinel")
            .setContentText(content)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(0, "Parar", pendingStop)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Moonshine Sentinel",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        reminderJob?.cancel()
        audioMonitor.stopMonitoring()
        if (::motionDetector.isInitialized) motionDetector.stop()
        prefs.sentinelExplicitlyActive = false
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "moonshine_sentinel_channel"
        private const val NOTIFICATION_ID = 8711

        const val ACTION_START = "com.example.moonshineproject.START_SENTINEL"
        const val ACTION_STOP = "com.example.moonshineproject.STOP_SENTINEL"
    }
}
