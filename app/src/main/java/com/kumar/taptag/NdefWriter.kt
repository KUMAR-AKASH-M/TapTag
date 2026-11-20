package com.kumar.taptag

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

/** Writes an NDEF message to a blank NTAG-style card. Never writes Mifare Classic. */
object NdefWriter {

    sealed class Result {
        data class Ok(val detail: String) : Result()
        data class Failed(val reason: String) : Result()
    }

    fun write(tag: Tag, message: NdefMessage): Result {
        // Safety: the college ID is Mifare Classic. Writing there could destroy it.
        if (MifareClassic.get(tag) != null) {
            return Result.Failed(
                "Not writing to this card: it is a Mifare Classic card (like your " +
                    "college ID). Writing is for blank NTAG cards only."
            )
        }

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return writeToNdef(ndef, message)
        }

        val formattable = NdefFormatable.get(tag)
        if (formattable != null) {
            return try {
                formattable.connect()
                formattable.format(message)
                Result.Ok("Card formatted and message written.")
            } catch (e: Exception) {
                Result.Failed("Could not format this card: " + e.message)
            } finally {
                try {
                    formattable.close()
                } catch (_: Exception) {
                }
            }
        }

        return Result.Failed("This card cannot hold NDEF data.")
    }

    private fun writeToNdef(ndef: Ndef, message: NdefMessage): Result {
        if (!ndef.isWritable) {
            return Result.Failed("This card is locked (read-only).")
        }
        val size = message.toByteArray().size
        if (size > ndef.maxSize) {
            return Result.Failed(
                "Message is $size bytes but the card holds only ${ndef.maxSize}."
            )
        }
        return try {
            ndef.connect()
            ndef.writeNdefMessage(message)
            Result.Ok("Written to card.")
        } catch (e: Exception) {
            Result.Failed("Write failed: " + e.message)
        } finally {
            try {
                ndef.close()
            } catch (_: Exception) {
            }
        }
    }
}
