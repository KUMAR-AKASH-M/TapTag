package com.kumar.taptag

import org.json.JSONArray
import org.json.JSONObject

/** One saved scan, kept in history so it can be viewed again later. */
data class ScanRecord(
    val id: Long,
    val timeMillis: Long,
    val uid: String,
    val tech: String,
    val foundText: List<String>,
    val dump: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("time", timeMillis)
        put("uid", uid)
        put("tech", tech)
        put("foundText", JSONArray(foundText))
        put("dump", dump)
    }

    companion object {
        fun fromJson(obj: JSONObject): ScanRecord {
            val found = mutableListOf<String>()
            val arr = obj.optJSONArray("foundText")
            if (arr != null) {
                for (i in 0 until arr.length()) found.add(arr.getString(i))
            }
            return ScanRecord(
                id = obj.optLong("id"),
                timeMillis = obj.optLong("time"),
                uid = obj.optString("uid"),
                tech = obj.optString("tech"),
                foundText = found,
                dump = obj.optString("dump")
            )
        }
    }
}
