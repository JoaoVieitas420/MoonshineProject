package com.example.moonshineproject.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sleepStartTime: Long,
    val sleepEndTime: Long? = null
)

@Entity(
    tableName = "sleep_events",
    foreignKeys = [
        ForeignKey(
            entity = SleepSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class SleepEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val decibelLevel: Int,
    val audioFilePath: String,
    val title: String,
    val isFavorite: Boolean = false
)

data class SleepSessionWithEvents(
    @Embedded
    val session: SleepSession,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val events: List<SleepEvent>
)
