package com.example.moonshineproject.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Dao
interface SleepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSession): Long

    @Query("UPDATE sleep_sessions SET sleepEndTime = :sleepEndTime WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long, sleepEndTime: Long): Int

    @Transaction
    @Query("SELECT * FROM sleep_sessions ORDER BY sleepStartTime DESC")
    fun observeSessionsWithEvents(): Flow<List<SleepSessionWithEvents>>

    @Transaction
    @Query("SELECT * FROM sleep_sessions WHERE id = :sessionId LIMIT 1")
    fun observeSessionDetail(sessionId: Long): Flow<SleepSessionWithEvents?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SleepEvent): Long

    @Query("UPDATE sleep_events SET title = :title WHERE id = :eventId")
    suspend fun renameEvent(eventId: Long, title: String): Int

    @Query("UPDATE sleep_events SET isFavorite = :favorite WHERE id = :eventId")
    suspend fun setFavorite(eventId: Long, favorite: Boolean): Int

    @Query("DELETE FROM sleep_events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long): Int

    @Query("SELECT * FROM sleep_events WHERE id = :eventId LIMIT 1")
    suspend fun getEventById(eventId: Long): SleepEvent?
}

@Database(entities = [SleepSession::class, SleepEvent::class], version = 1, exportSchema = false)
abstract class MoonshineDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao

    companion object {
        @Volatile
        private var INSTANCE: MoonshineDatabase? = null

        fun getInstance(context: Context): MoonshineDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MoonshineDatabase::class.java,
                    "moonshine.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}

data class SleepSessionUi(
    val id: Long,
    val sleepStartTime: Long,
    val sleepEndTime: Long?,
    val durationMs: Long,
    val eventCount: Int
) {
    val dateLabel: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(sleepStartTime))

    val startLabel: String
        get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sleepStartTime))

    val endLabel: String
        get() = sleepEndTime?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
        } ?: "--"

    val durationLabel: String
        get() {
            val minutes = (durationMs / 60000).toInt().coerceAtLeast(0)
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            return "${hours}h ${remainingMinutes}m"
        }
}

class SleepRepository(context: Context) {
    private val dao = MoonshineDatabase.getInstance(context).sleepDao()

    fun observeSessions(): Flow<List<SleepSessionUi>> {
        return dao.observeSessionsWithEvents().map { sessions ->
            sessions.map { item ->
                val end = item.session.sleepEndTime ?: System.currentTimeMillis()
                SleepSessionUi(
                    id = item.session.id,
                    sleepStartTime = item.session.sleepStartTime,
                    sleepEndTime = item.session.sleepEndTime,
                    durationMs = end - item.session.sleepStartTime,
                    eventCount = item.events.size
                )
            }
        }
    }

    fun observeSessionDetail(sessionId: Long): Flow<SleepSessionWithEvents?> =
        dao.observeSessionDetail(sessionId)

    suspend fun startSleepSession(startTime: Long): Long {
        return dao.insertSession(SleepSession(sleepStartTime = startTime))
    }

    suspend fun endSleepSession(sessionId: Long, endTime: Long) {
        dao.endSession(sessionId, endTime)
    }

    suspend fun saveEvent(
        sessionId: Long,
        timestamp: Long,
        audioFilePath: String,
        decibelLevel: Int
    ): Long {
        val title = "Clip ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))}"
        return dao.insertEvent(
            SleepEvent(
                sessionId = sessionId,
                timestamp = timestamp,
                decibelLevel = decibelLevel,
                audioFilePath = audioFilePath,
                title = title
            )
        )
    }

    suspend fun renameEvent(eventId: Long, newTitle: String) {
        dao.renameEvent(eventId, newTitle)
    }

    suspend fun setFavorite(eventId: Long, favorite: Boolean) {
        dao.setFavorite(eventId, favorite)
    }

    suspend fun deleteEvent(eventId: Long): SleepEvent? {
        val event = dao.getEventById(eventId)
        dao.deleteEventById(eventId)
        return event
    }
}
