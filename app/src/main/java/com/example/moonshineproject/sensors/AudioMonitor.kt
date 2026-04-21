package com.example.moonshineproject.sensors

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque
import kotlin.math.log10
import kotlin.math.sqrt

class AudioMonitor {

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT

    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)
    private var audioRecord: AudioRecord? = null
    @Volatile
    private var monitoring = false

    private var monitorThread: Thread? = null

    @SuppressLint("MissingPermission")
    fun startMonitoring(
        outputDir: File,
        calibrationSeconds: Int,
        thresholdMarginDb: Int,
        onAudioDetected: (filePath: String, peakDb: Int, timestamp: Long) -> Unit
    ) {
        if (monitoring) return
        if (!outputDir.exists()) outputDir.mkdirs()

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioEncoding,
            bufferSize
        )

        val recorder = audioRecord ?: return
        recorder.startRecording()
        monitoring = true

        val preEventMillis = 2_000L
        val postEventMillis = 2_000L
        val maxEventMillis = 15_000L

        monitorThread = Thread {
            val audioBuffer = ShortArray(bufferSize)
            val chunkDurationMillis = ((bufferSize.toDouble() / sampleRate) * 1000).toLong().coerceAtLeast(1L)
            val preBufferChunks = (preEventMillis / chunkDurationMillis).toInt().coerceAtLeast(1)
            val calibrationChunksTarget = ((calibrationSeconds * 1000L) / chunkDurationMillis).toInt().coerceAtLeast(1)

            val preEventBuffer = ArrayDeque<ShortArray>()
            val calibrationDbValues = mutableListOf<Double>()
            var calibrationChunks = 0
            var baselineDb = 35.0
            var thresholdDb = baselineDb + thresholdMarginDb

            var recordingEvent = false
            var eventChunks = mutableListOf<ShortArray>()
            var eventPeakDb = 0.0
            var eventTimestamp = 0L
            var eventStart = 0L
            var lastAboveThreshold = 0L

            while (monitoring) {
                val read = recorder.read(audioBuffer, 0, audioBuffer.size)
                if (read <= 0) continue

                val chunk = audioBuffer.copyOf(read)
                val now = System.currentTimeMillis()
                val db = calculateDb(chunk)

                if (preEventBuffer.size >= preBufferChunks) preEventBuffer.removeFirst()
                preEventBuffer.addLast(chunk)

                if (calibrationChunks < calibrationChunksTarget) {
                    calibrationDbValues.add(db)
                    calibrationChunks++
                    if (calibrationChunks == calibrationChunksTarget) {
                        baselineDb = calibrationDbValues.average().coerceAtLeast(20.0)
                        thresholdDb = baselineDb + thresholdMarginDb
                        Log.d("Moonshine", "Calibration done. Baseline=$baselineDb dB | Threshold=$thresholdDb dB")
                    }
                    continue
                }

                if (db >= thresholdDb) {
                    if (!recordingEvent) {
                        recordingEvent = true
                        eventChunks = preEventBuffer.map { it.copyOf() }.toMutableList()
                        eventPeakDb = db
                        eventTimestamp = now
                        eventStart = now
                    }
                    eventChunks.add(chunk)
                    eventPeakDb = maxOf(eventPeakDb, db)
                    lastAboveThreshold = now
                } else if (recordingEvent) {
                    eventChunks.add(chunk)
                }

                if (recordingEvent) {
                    val shouldCloseBySilence = (now - lastAboveThreshold) >= postEventMillis
                    val shouldCloseByLength = (now - eventStart) >= maxEventMillis
                    if (shouldCloseBySilence || shouldCloseByLength) {
                        val filePath = writeEventAsWav(outputDir, eventChunks, eventTimestamp)
                        onAudioDetected(filePath, eventPeakDb.toInt(), eventTimestamp)
                        recordingEvent = false
                        eventChunks = mutableListOf()
                        eventPeakDb = 0.0
                        eventTimestamp = 0L
                        eventStart = 0L
                        lastAboveThreshold = 0L
                    }
                }
            }
        }.apply { start() }
    }

    fun stopMonitoring() {
        monitoring = false
        runCatching { monitorThread?.join(800) }
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        monitorThread = null
        audioRecord = null
    }

    private fun calculateDb(samples: ShortArray): Double {
        var sumSquares = 0.0
        for (sample in samples) {
            val normalized = sample / 32768.0
            sumSquares += normalized * normalized
        }
        val rms = sqrt(sumSquares / samples.size.coerceAtLeast(1))
        return if (rms > 0) 20 * log10(rms) + 90 else 0.0
    }

    private fun writeEventAsWav(outputDir: File, chunks: List<ShortArray>, timestamp: Long): String {
        val allSamples = chunks.flatMap { it.asList() }
        val pcmBytes = ByteBuffer.allocate(allSamples.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            allSamples.forEach { putShort(it) }
        }.array()

        val file = File(outputDir, "sleep_event_${timestamp}.wav")
        BufferedOutputStream(FileOutputStream(file)).use { stream ->
            writeWavHeader(stream, pcmBytes.size)
            stream.write(pcmBytes)
        }
        return file.absolutePath
    }

    private fun writeWavHeader(stream: BufferedOutputStream, dataLength: Int) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataLength)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1.toShort())
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray())
            putInt(dataLength)
        }.array()

        stream.write(header)
    }
}
