package com.example.moonshineproject.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

class MotionDetector(
    context: Context,
    private val stillnessSecondsForSleep: Int,
    private val onSleepDetected: () -> Unit,
    private val onHeavyMovementDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val stillThreshold = 0.9f
    private val heavyMovementThreshold = 2.2f
    private val heavyMovementToleranceReads = 8

    private var isTracking = false
    private var sleepAlreadyDetected = false
    private var stillnessStartTime: Long = 0
    private var heavyMovementReads = 0

    fun start() {
        if (isTracking) return
        isTracking = true
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        isTracking = false
        sensorManager.unregisterListener(this)
        stillnessStartTime = 0
        heavyMovementReads = 0
        sleepAlreadyDetected = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTracking || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gValue = sqrt(x * x + y * y + z * z)
        val diff = abs(gValue - SensorManager.GRAVITY_EARTH)
        val now = System.currentTimeMillis()

        if (!sleepAlreadyDetected) {
            if (diff < stillThreshold) {
                if (stillnessStartTime == 0L) stillnessStartTime = now
                val secondsStill = (now - stillnessStartTime) / 1000L
                if (secondsStill >= stillnessSecondsForSleep) {
                    sleepAlreadyDetected = true
                    stillnessStartTime = 0
                    onSleepDetected()
                }
            } else {
                stillnessStartTime = 0
            }
            return
        }

        if (diff >= heavyMovementThreshold) {
            heavyMovementReads++
            if (heavyMovementReads >= heavyMovementToleranceReads) {
                heavyMovementReads = 0
                sleepAlreadyDetected = false
                onHeavyMovementDetected()
            }
        } else {
            heavyMovementReads = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
