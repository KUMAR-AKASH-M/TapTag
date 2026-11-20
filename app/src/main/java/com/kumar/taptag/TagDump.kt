package com.kumar.taptag

import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import java.io.IOException

/** One scan, with the pieces History needs plus the full readable dump. */
data class ScanResult(
    val uid: String,
    val tech: String,
    val foundText: List<String>,
    val dump: String
)

/** Builds a human-readable dump of a scanned tag. Reader only, never writes. */
object TagDump {

    fun dump(tag: Tag): String = scan(tag).dump

    fun scan(tag: Tag): ScanResult {
        val body = StringBuilder()
        val foundText = mutableListOf<String>()
        val uid = tag.id.toHex(":")
        val tech = tag.techList.joinToString(", ") { it.substringAfterLast('.') }

        body.appendLine("UID: " + uid)
        body.appendLine("Tech: " + tech)
        body.appendLine()

        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            body.appendLine("ATQA: 0x" + nfcA.atqa.toHex(""))
            body.appendLine("SAK: 0x" + "%02X".format(nfcA.sak.toInt() and 0xFF))
            body.appendLine()
        }

        val classic = MifareClassic.get(tag)
        if (classic != null) {
            body.appendLine("Mifare Classic")
            body.appendLine("Type: " + classicTypeName(classic.type))
            body.appendLine("Size: " + classic.size + " bytes")
            body.appendLine("Sectors: " + classic.sectorCount + ", blocks: " + classic.blockCount)
            body.appendLine()
            dumpSectors(classic, body, foundText)
        }

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            body.appendLine("NDEF: " + ndef.type)
            body.appendLine("Max size: " + ndef.maxSize + " bytes, writable: " + ndef.isWritable)
            val message = ndef.cachedNdefMessage
            if (message != null) {
                for (record in message.records) {
                    body.appendLine("Record: " + describeRecord(record))
                }
            } else {
                body.appendLine("No NDEF message stored.")
            }
            body.appendLine()
        } else if (NdefFormatable.get(tag) != null) {
            body.appendLine("NDEF: not formatted yet (NdefFormatable).")
            body.appendLine()
        }

        val out = StringBuilder()
        if (foundText.isNotEmpty()) {
            out.appendLine("Text found on card:")
            for (text in foundText) out.appendLine("  • $text")
            out.appendLine()
        }
        out.append(body.toString().trimEnd())
        return ScanResult(uid, tech, foundText, out.toString().trimEnd())
    }

    private fun dumpSectors(classic: MifareClassic, out: StringBuilder, foundText: MutableList<String>) {
        val emptySectors = mutableListOf<Int>()
        try {
            classic.connect()
            for (sector in 0 until classic.sectorCount) {
                val authed = classic.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT)
                if (!authed) {
                    flushEmpty(out, emptySectors)
                    out.appendLine("Sector $sector")
                    out.appendLine("  (default Key A did not open this sector)")
                    continue
                }
                val firstBlock = classic.sectorToBlock(sector)
                val blocks = classic.getBlockCountInSector(sector)
                val lines = mutableListOf<String>()
                var sectorEmpty = true
                for (i in 0 until blocks) {
                    val blockIndex = firstBlock + i
                    val isTrailer = (i == blocks - 1)
                    try {
                        val data = classic.readBlock(blockIndex)
                        if (!isTrailer && data.any { it != 0.toByte() }) sectorEmpty = false
                        if (!isTrailer) collectText(data, foundText)
                        if (isTrailer) {
                            lines.add("  [$blockIndex] Trailer — access " + data.copyOfRange(6, 10).toHex(" ") + " (keys hidden by card)")
                        } else {
                            lines.add("  [$blockIndex] " + data.toHex(" ") + "  |" + data.toAscii() + "|")
                        }
                    } catch (e: IOException) {
                        sectorEmpty = false
                        lines.add("  [$blockIndex] (read failed: " + e.message + ")")
                    }
                }
                if (sectorEmpty) {
                    emptySectors.add(sector)
                } else {
                    flushEmpty(out, emptySectors)
                    out.appendLine("Sector $sector")
                    lines.forEach { out.appendLine(it) }
                }
            }
            flushEmpty(out, emptySectors)
        } catch (e: IOException) {
            out.appendLine("(could not connect to the card: " + e.message + ")")
        } finally {
            try {
                classic.close()
            } catch (_: IOException) {
            }
        }
    }

    /** Collapses consecutive all-zero sectors into one line. */
    private fun flushEmpty(out: StringBuilder, emptySectors: MutableList<Int>) {
        if (emptySectors.isEmpty()) return
        val label = if (emptySectors.size == 1) {
            "Sector ${emptySectors[0]}"
        } else {
            "Sectors ${emptySectors.first()}–${emptySectors.last()}"
        }
        out.appendLine("$label: empty (all zeros)")
        out.appendLine()
        emptySectors.clear()
    }

    /** Picks out readable text (names, numbers, codes) from raw block bytes. */
    private fun collectText(data: ByteArray, foundText: MutableList<String>) {
        val current = StringBuilder()
        fun flush() {
            val text = current.toString()
            if (text.length >= 5 && (text.any { it.isLetter() } || text.length >= 8)) {
                if (!foundText.contains(text)) foundText.add(text)
            }
            current.clear()
        }
        for (b in data) {
            val c = b.toInt()
            if (c in 32..126) current.append(c.toChar()) else flush()
        }
        flush()
    }

    private fun classicTypeName(type: Int): String = when (type) {
        MifareClassic.TYPE_CLASSIC -> "Mifare Classic"
        MifareClassic.TYPE_PLUS -> "Mifare Plus"
        MifareClassic.TYPE_PRO -> "Mifare Pro"
        else -> "Unknown ($type)"
    }

    private fun describeRecord(record: NdefRecord): String {
        return when {
            record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT) ->
                "Text: \"" + parseTextRecord(record) + "\""
            record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI) ->
                "URI: " + record.toUri()
            else ->
                "TNF=" + record.tnf + ", type=" + record.type.toHex("") +
                    ", payload=" + record.payload.toHex(" ")
        }
    }

    private fun parseTextRecord(record: NdefRecord): String {
        val payload = record.payload
        if (payload.isEmpty()) return ""
        val isUtf16 = (payload[0].toInt() and 0x80) != 0
        val langLength = payload[0].toInt() and 0x3F
        val textBytes = payload.copyOfRange(1 + langLength, payload.size)
        return String(textBytes, if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8)
    }

    private fun ByteArray.toHex(separator: String): String =
        joinToString(separator) { "%02X".format(it.toInt() and 0xFF) }

    private fun ByteArray.toAscii(): String =
        map { if (it.toInt() in 32..126) it.toInt().toChar() else '.' }.joinToString("")
}
