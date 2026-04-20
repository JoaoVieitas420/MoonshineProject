package com.example.moonshineproject.sensors

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.log10

class AudioMonitor {

    private var audioRecord: AudioRecord? = null
    private var isMonitoring = false
    private val SAMPLE_RATE = 44100
    private val DB_THRESHOLD = 55.0 // dB mínimo para gravar
    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    @SuppressLint("MissingPermission")
    fun startMonitoring(
        outputDir: File,
        onAudioDetected: (filePath: String, db: Int) -> Unit
    ) {
        if (isMonitoring) return
        isMonitoring = true

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()

        Thread {
            val buffer = ShortArray(bufferSize)
            while (isMonitoring) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val db = calculateDb(buffer, read)
                    Log.d("Moonshine", "dB: ${db.toInt()}")

                    if (db > DB_THRESHOLD) {
                        // Guarda clip de áudio
                        val filePath = saveAudioClip(buffer, read, outputDir)
                        onAudioDetected(filePath, db.toInt())
                    }
                }
                Thread.sleep(1000)
            }
        }.start()
    }

    private fun calculateDb(buffer: ShortArray, read: Int): Double {
        var sum = 0.0
        for (i in 0 until read) sum += buffer[i] * buffer[i]
        val rms = Math.sqrt(sum / read)
        return if (rms > 0) 20 * log10(rms) else 0.0
    }

    private fun saveAudioClip(buffer: ShortArray, read: Int, outputDir: File): String {
        if (!outputDir.exists()) outputDir.mkdirs()
        val timestamp = System.currentTimeMillis()
        val file = File(outputDir, "sleep_$timestamp.pcm")
        FileOutputStream(file).use { fos ->
            val byteBuffer = ByteArray(read * 2)
            for (i in 0 until read) {
                byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                byteBuffer[i * 2 + 1] = (buffer[i].toInt() shr 8).toByte()
            }
            fos.write(byteBuffer)
        }
        Log.d("Moonshine", "Áudio guardado: ${file.absolutePath}")
        return file.absolutePath
    }

    fun stopMonitoring() {
        isMonitoring = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}