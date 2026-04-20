package com.example.moonshineproject.data

data class SleepEvent(
    val id: Long = System.currentTimeMillis(),
    val date: String,
    val time: String,
    val audioFilePath: String,
    val decibelLevel: Int
)