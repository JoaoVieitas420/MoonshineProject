package com.example.moonshineproject.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.moonshineproject.service.SentinelService
import com.example.moonshineproject.ui.overlay.ReminderOverlayContent
import android.provider.Settings

class ReminderOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    private val handler = Handler(Looper.getMainLooper())
    private val snoozeRunnable = Runnable {
        val showIntent = Intent(this, ReminderOverlayService::class.java).apply {
            action = ACTION_SHOW
        }
        startService(showIntent)
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_DISMISS -> dismissOverlay()
            ACTION_SNOOZE -> {
                handler.removeCallbacks(snoozeRunnable)
                handler.postDelayed(snoozeRunnable, 15 * 60 * 1000L)
                dismissOverlay(stopServiceAfterDismiss = false)
            }
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return

        // Verificação crítica — sem isto crasha sempre
        if (!Settings.canDrawOverlays(applicationContext)) {
            android.util.Log.e("Moonshine", "Overlay permission not granted — cannot show overlay")
            stopSelf()
            return
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ReminderOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ReminderOverlayService)
            setContent {
                ReminderOverlayContent(
                    onSleep = {
                        val sentinelIntent = Intent(this@ReminderOverlayService, SentinelService::class.java).apply {
                            action = SentinelService.ACTION_START
                        }
                        ContextCompat.startForegroundService(this@ReminderOverlayService, sentinelIntent)
                        dismissOverlay()
                    },
                    onSnooze = {
                        val snoozeIntent = Intent(this@ReminderOverlayService, ReminderOverlayService::class.java).apply {
                            action = ACTION_SNOOZE
                        }
                        startService(snoozeIntent)
                    },
                    onDismiss = {
                        dismissOverlay()
                    }
                )
            }
        }

        overlayView = view
        windowManager.addView(view, params)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    private fun dismissOverlay(stopServiceAfterDismiss: Boolean = true) {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        if (stopServiceAfterDismiss) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(snoozeRunnable)
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_SHOW = "com.example.moonshineproject.OVERLAY_SHOW"
        const val ACTION_DISMISS = "com.example.moonshineproject.OVERLAY_DISMISS"
        const val ACTION_SNOOZE = "com.example.moonshineproject.OVERLAY_SNOOZE"
    }
}