package com.kumar.taptag

import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import java.io.IOException

/** Builds a human-readable dump of a scanned tag. Reader only, never writes. */
object TagDump {

    fun dump(tag: Tag): String {
        val out = StringBuilder()
        out.appendLine("UID: " + tag.id.toHex(":"))
        out.appendLine("Tech: " + tag.techList.joinToString(", ") { it.substringAfterLast('.') })
        out.appendLine()

        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            out.appendLine("ATQA: 0x" + nfcA.atqa.toHex(""))
            out.appendLine("SAK: 0x" + "%02X".format(nfcA.sak.toInt() and 0xFF))
            out.appendLine()
        }

        val classic = MifareClassic.get(tag)
        if (classic != null) {
            out.appendLine("Mifare Classic")
            out.appendLine("Type: " + classicTypeName(classic.type))
            out.appendLine("Size: " + classic.size + " bytes")
            out.appendLine("Sectors: " + classic.sectorCount + ", blocks: " + classic.blockCount)
            out.appendLine()
            dumpSectors(classic, out)
        }

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            out.appendLine("NDEF: " + ndef.type)
            out.appendLine("Max size: " + ndef.maxSize + " bytes, writable: " + ndef.isWritable)
            val message = ndef.cachedNdefMessage
            if (message != null) {
                for (record in message.records) {
                    out.appendLine("Record: " + describeRecord(record))
                }
            } else {
                out.appendLine("No NDEF message stored.")
            }
            out.appendLine()
        } else if (NdefFormatable.get(tag) != null) {
            out.appendLine("NDEF: not formatted yet (NdefFormatable).")
            out.appendLine()
        }

        return out.toString().trimEnd()
    }

    private fun dumpSectors(classic: MifareClassic, out: StringBuilder) {
        try {
            classic.connect()
            for (sector in 0 until classic.sectorCount) {
                out.appendLine("Sector $sector")
                val authed = classic.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT)
                if (!authed) {
                    out.appendLine("  (default Key A did not open this sector)")
                    continue
                }
                val firstBlock = classic.sectorToBlock(sector)
                val blocks = classic.getBlockCountInSector(sector)
                for (i in 0 until blocks) {
                    val blockIndex = firstBlock + i
                    try {
                        val data = classic.readBlock(blockIndex)
                        out.appendLine("  [$blockIndex] " + data.toHex(" ") + "  |" + data.toAscii() + "|")
                    } catch (e: IOException) {
                        out.appendLine("  [$blockIndex] (read failed: " + e.message + ")")
                    }
                }
            }
        } catch (e: IOException) {
            out.appendLine("(could not connect to the card: " + e.message + ")")
        } finally {
            try {
                classic.close()
            } catch (_: IOException) {
            }
        }
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
