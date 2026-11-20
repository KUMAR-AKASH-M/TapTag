package com.kumar.taptag

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var adapter: HistoryAdapter
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val recycler = findViewById<RecyclerView>(R.id.historyList)
        emptyText = findViewById(R.id.emptyText)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter = HistoryAdapter(
            records = ScanHistory.load(this),
            onClick = { record ->
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra(MainActivity.EXTRA_DUMP, record.dump)
                startActivity(intent)
            },
            onDelete = { record ->
                ScanHistory.delete(this, record.id)
                refresh()
            }
        )
        recycler.adapter = adapter

        findViewById<Button>(R.id.clearButton).setOnClickListener {
            ScanHistory.clear(this)
            refresh()
        }
        refresh()
    }

    private fun refresh() {
        val records = ScanHistory.load(this)
        adapter.setRecords(records)
        emptyText.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
    }

    private class HistoryAdapter(
        private var records: List<ScanRecord>,
        private val onClick: (ScanRecord) -> Unit,
        private val onDelete: (ScanRecord) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        fun setRecords(newRecords: List<ScanRecord>) {
            records = newRecords
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.itemTitle)
            val subtitle: TextView = view.findViewById(R.id.itemSubtitle)
            val delete: Button = view.findViewById(R.id.itemDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val record = records[position]
            holder.title.text = record.uid + "  •  " + formatTime(record.timeMillis)
            holder.subtitle.text = if (record.foundText.isNotEmpty()) {
                record.foundText.joinToString(", ")
            } else {
                record.tech
            }
            holder.itemView.setOnClickListener { onClick(record) }
            holder.delete.setOnClickListener { onDelete(record) }
        }

        override fun getItemCount(): Int = records.size

        private fun formatTime(millis: Long): String =
            SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(millis))
    }
}
