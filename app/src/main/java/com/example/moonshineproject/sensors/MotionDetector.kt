package com.example.moonshineproject.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class MotionDetector(context: Context, private val onStillnessDetected: () -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val SLOW_THRESHOLD = 1.0f
    private val SECONDS_TO_SLEEP = 20L

    private var isStill = false
    private var stillnessStartTime: Long = 0
    private var movementCount = 0
    private val MOVEMENT_TOLERANCE = 5 // ignora até 5 leituras de movimento seguidas

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gValue = sqrt(x * x + y * y + z * z)
        val diff = Math.abs(gValue - SensorManager.GRAVITY_EARTH)

        val currentTime = System.currentTimeMillis()

        if (diff < SLOW_THRESHOLD) {
            // Está parado — reset do contador de movimento
            movementCount = 0

            if (!isStill) {
                isStill = true
                stillnessStartTime = currentTime
            } else {
                val durationSeconds = (currentTime - stillnessStartTime) / 1000L
                if (durationSeconds >= SECONDS_TO_SLEEP) {
                    onStillnessDetected()
                    isStill = false
                }
            }
        } else {
            // Detetou movimento — só reseta se for persistente
            movementCount++
            if (movementCount >= MOVEMENT_TOLERANCE) {
                isStill = false
                movementCount = 0
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}