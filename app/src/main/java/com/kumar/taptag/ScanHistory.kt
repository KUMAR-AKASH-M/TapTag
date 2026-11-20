package com.kumar.taptag

import android.content.Context
import org.json.JSONArray

/** Stores past scans on the phone (SharedPreferences, newest first). */
object ScanHistory {
    private const val PREFS = "taptag_history"
    private const val KEY_SCANS = "scans"
    private const val MAX_SCANS = 50

    fun save(context: Context, record: ScanRecord) {
        val records = load(context).toMutableList()
        records.add(0, record)
        while (records.size > MAX_SCANS) records.removeAt(records.lastIndex)
        persist(context, records)
    }

    fun load(context: Context): List<ScanRecord> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SCANS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { ScanRecord.fromJson(arr.getJSONObject(it)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun delete(context: Context, id: Long) {
        persist(context, load(context).filter { it.id != id })
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_SCANS).apply()
    }

    private fun persist(context: Context, records: List<ScanRecord>) {
        val arr = JSONArray()
        records.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SCANS, arr.toString()).apply()
    }
}
