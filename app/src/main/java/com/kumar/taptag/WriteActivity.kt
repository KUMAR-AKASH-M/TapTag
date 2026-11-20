package com.kumar.taptag

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WriteActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var waitingForCard = false
    private var pendingMessage: NdefMessage? = null
    private lateinit var contentInput: EditText
    private lateinit var statusText: TextView
    private lateinit var writeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        contentInput = findViewById(R.id.contentInput)
        statusText = findViewById(R.id.statusText)
        writeButton = findViewById(R.id.writeButton)
        val textRadio = findViewById<RadioButton>(R.id.textRadio)
        val urlRadio = findViewById<RadioButton>(R.id.urlRadio)

        textRadio.setOnCheckedChangeListener { _, checked ->
            if (checked) contentInput.setHint(R.string.write_text_hint)
        }
        urlRadio.setOnCheckedChangeListener { _, checked ->
            if (checked) contentInput.setHint(R.string.write_url_hint)
        }

        writeButton.setOnClickListener { armWrite() }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            statusText.text = "This device has no NFC."
            writeButton.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag) {
        if (!waitingForCard) return
        waitingForCard = false
        val message = pendingMessage ?: return
        Thread {
            val result = NdefWriter.write(tag, message)
            runOnUiThread {
                when (result) {
                    is NdefWriter.Result.Ok -> {
                        statusText.text = result.detail
                        Toast.makeText(this, "Written.", Toast.LENGTH_SHORT).show()
                    }
                    is NdefWriter.Result.Failed -> statusText.text = result.reason
                }
                writeButton.isEnabled = true
            }
        }.start()
    }

    private fun armWrite() {
        val text = contentInput.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Type something to write first.", Toast.LENGTH_SHORT).show()
            return
        }
        val isUrl = findViewById<RadioButton>(R.id.urlRadio).isChecked
        pendingMessage = if (isUrl) {
            val url = if ("://" in text) text else "https://$text"
            NdefMessage(arrayOf(NdefRecord.createUri(url)))
        } else {
            NdefMessage(arrayOf(NdefRecord.createTextRecord("en", text)))
        }
        waitingForCard = true
        writeButton.isEnabled = false
        statusText.text = "Hold a blank NTAG card to the back of the phone…"
    }
}
