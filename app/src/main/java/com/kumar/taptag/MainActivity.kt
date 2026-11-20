package com.kumar.taptag

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)

        findViewById<Button>(R.id.copyButton).setOnClickListener { copyResult() }
        findViewById<Button>(R.id.clearButton).setOnClickListener { clearResult() }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        when {
            nfcAdapter == null -> statusText.text = "This device has no NFC."
            nfcAdapter?.isEnabled == false -> statusText.text = "NFC is off. Turn it on, then tap a card."
            else -> statusText.text = "Ready. Tap a card to the back of the phone."
        }

        intent?.let { handleIntent(it) }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onTagDiscovered(tag: Tag) {
        showTag(tag)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        if (action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            if (tag != null) showTag(tag)
        }
    }

    /** Runs the (blocking) tag read off the UI thread, then shows the result. */
    private fun showTag(tag: Tag) {
        runOnUiThread { statusText.text = "Reading card…" }
        Thread {
            val dump = try {
                TagDump.dump(tag)
            } catch (e: Exception) {
                "Could not read this card: " + e.message
            }
            runOnUiThread {
                resultText.text = dump
                statusText.text = "Card read. Tap another card to scan again."
            }
        }.start()
    }

    private fun copyResult() {
        val text = resultText.text.toString()
        if (text.isBlank()) {
            Toast.makeText(this, "Nothing to copy yet.", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("TapTag scan", text))
        Toast.makeText(this, "Copied.", Toast.LENGTH_SHORT).show()
    }

    private fun clearResult() {
        resultText.text = ""
        statusText.text = "Ready. Tap a card to the back of the phone."
    }
}
