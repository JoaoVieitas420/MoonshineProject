package com.example.moonshineproject.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


data class SleepSession(
    val id: Long = System.currentTimeMillis(),
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int
)

class SleepRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("sleep_data", Context.MODE_PRIVATE)

    // ── Eventos de áudio ──────────────────────────────────────────
    fun saveEvent(event: SleepEvent) {
        val all = getAllEventsRaw().put(eventToJson(event))
        prefs.edit().putString("events", all.toString()).apply()
    }

    fun getAllEvents(): List<SleepEvent> {
        val array = getAllEventsRaw()
        val list = mutableListOf<SleepEvent>()
        for (i in 0 until array.length()) list.add(jsonToEvent(array.getJSONObject(i)))
        return list.sortedByDescending { it.id }
    }

    fun getEventsByDate(date: String) = getAllEvents().filter { it.date == date }

    fun getAllDates(): List<String> = getAllEvents().map { it.date }.distinct().sortedDescending()

    private fun getAllEventsRaw(): JSONArray {
        return JSONArray(prefs.getString("events", "[]") ?: "[]")
    }

    private fun eventToJson(e: SleepEvent) = JSONObject().apply {
        put("id", e.id); put("date", e.date); put("time", e.time)
        put("audioFilePath", e.audioFilePath); put("decibelLevel", e.decibelLevel)
    }

    private fun jsonToEvent(o: JSONObject) = SleepEvent(
        id = o.getLong("id"), date = o.getString("date"), time = o.getString("time"),
        audioFilePath = o.getString("audioFilePath"), decibelLevel = o.getInt("decibelLevel")
    )

    // ── Sessões de sono (horas dormidas) ─────────────────────────
    fun saveSession(startMs: Long, endMs: Long) {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val durationMinutes = ((endMs - startMs) / 60000).toInt()
        val session = SleepSession(
            date = dateFmt.format(Date(startMs)),
            startTime = fmt.format(Date(startMs)),
            endTime = fmt.format(Date(endMs)),
            durationMinutes = durationMinutes
        )
        val all = getAllSessionsRaw().put(sessionToJson(session))
        prefs.edit().putString("sessions", all.toString()).apply()
    }

    fun getAllSessions(): List<SleepSession> {
        val array = getAllSessionsRaw()
        val list = mutableListOf<SleepSession>()
        for (i in 0 until array.length()) list.add(jsonToSession(array.getJSONObject(i)))
        return list.sortedByDescending { it.id }
    }

    private fun getAllSessionsRaw(): JSONArray {
        return JSONArray(prefs.getString("sessions", "[]") ?: "[]")
    }

    private fun sessionToJson(s: SleepSession) = JSONObject().apply {
        put("id", s.id); put("date", s.date); put("startTime", s.startTime)
        put("endTime", s.endTime); put("durationMinutes", s.durationMinutes)
    }

    private fun jsonToSession(o: JSONObject) = SleepSession(
        id = o.getLong("id"), date = o.getString("date"),
        startTime = o.getString("startTime"), endTime = o.getString("endTime"),
        durationMinutes = o.getInt("durationMinutes")
    )
}