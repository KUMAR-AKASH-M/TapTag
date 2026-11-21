package com.kumar.taptag

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Makes the phone pretend to be a card — but only to readers that ask for
 * TapTag's own AID. This is plain Android HCE (ISO-DEP). It CANNOT imitate
 * a Mifare Classic card: the phone's NFC chip has no Crypto-1, and the UID
 * the phone sends at the radio layer is random, not 49:7B:10:AB. So this
 * will not open the college reader. It is a demo: tap this phone to a
 * second phone (or any reader that selects the TapTag AID) and it answers
 * with the card's UID, name and reg number.
 */
class CardEmulationService : HostApduService() {

    companion object {
        /** TapTag's own AID: "F0" + "TapTag" in ASCII hex. */
        const val AID = "F0546170546167"

        private val SELECT_AID_HEADER = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte()
        )
        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        // The card this phone pretends to be (Kumar's own college ID).
        private const val CARD_INFO = "49:7B:10:AB|Kumar Akash|12502685"
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (isSelectAid(commandApdu)) {
            return CARD_INFO.toByteArray(Charsets.US_ASCII) + SW_OK
        }
        return SW_NOT_FOUND
    }

    override fun onDeactivated(reason: Int) {
        // Reader walked away; nothing to clean up.
    }

    private fun isSelectAid(apdu: ByteArray): Boolean {
        if (apdu.size < SELECT_AID_HEADER.size + 1) return false
        for (i in SELECT_AID_HEADER.indices) {
            if (apdu[i] != SELECT_AID_HEADER[i]) return false
        }
        val aidLength = apdu[SELECT_AID_HEADER.size].toInt() and 0xFF
        if (apdu.size < SELECT_AID_HEADER.size + 1 + aidLength) return false
        val aid = apdu.copyOfRange(
            SELECT_AID_HEADER.size + 1,
            SELECT_AID_HEADER.size + 1 + aidLength
        )
        return aid.contentEquals(AID.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
    }
}
