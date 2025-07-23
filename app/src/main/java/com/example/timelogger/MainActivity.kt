package com.example.timelogger

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var categorySpinner: Spinner
    private lateinit var subcategorySpinner: Spinner
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var historyText: TextView
    private lateinit var historyScrollView: ScrollView

    private val logList = mutableListOf<String>()
    private var lastActivityStartTime: Date? = null
    private var lastActivityInfo: String? = null
    private var isFirstActivity = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        categorySpinner = findViewById(R.id.category_spinner)
        subcategorySpinner = findViewById(R.id.subcategory_spinner)
        startButton = findViewById(R.id.start_button)
        stopButton = findViewById(R.id.stop_button)
        historyText = findViewById(R.id.history_text)
        historyScrollView = findViewById(R.id.history_scroll_view)

        val categories = arrayOf("Reading", "Exercise", "Writing", "Programming")
        val subcategories = mapOf(
            "Reading" to arrayOf("Fiction", "Non-fiction"),
            "Exercise" to arrayOf("Running", "Yoga"),
            "Writing" to arrayOf("Journal", "Essay"),
            "Programming" to arrayOf("Python", "Kotlin")
        )

        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View, position: Int, id: Long) {
                val selected = categories[position]
                subcategorySpinner.adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_item,
                    subcategories[selected] ?: arrayOf("None")
                )
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        startButton.setOnClickListener {
            val currentTime = Date()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentTimeStr = sdf.format(currentTime)
            val category = categorySpinner.selectedItem.toString()
            val subcategory = subcategorySpinner.selectedItem.toString()

            // Clear placeholder text on first entry
            if (isFirstActivity) {
                historyText.text = ""
                isFirstActivity = false
            }

            // Calculate duration of previous activity if exists
            if (lastActivityStartTime != null && lastActivityInfo != null) {
                val duration = calculateDuration(lastActivityStartTime!!, currentTime)
                val completedRecord = "⏹️ $lastActivityInfo (Duration: $duration)"
                logList.add(completedRecord)
                historyText.append("$completedRecord\n\n")
            }

            // Start new activity
            val newActivityInfo = "$currentTimeStr, $category, $subcategory"
            lastActivityStartTime = currentTime
            lastActivityInfo = newActivityInfo

            historyText.append("▶️ Started: $newActivityInfo\n")

            // Update UI state
            updateButtonStates()

            // Scroll to bottom to show latest entry
            scrollToBottom()
        }

        stopButton.setOnClickListener {
            stopCurrentActivity()
        }

        val exportButton = findViewById<Button>(R.id.export_button)
        exportButton.setOnClickListener {
            exportCsv()
        }

        // Initialize UI state
        updateButtonStates()
    }

    private fun stopCurrentActivity() {
        if (lastActivityStartTime != null && lastActivityInfo != null) {
            val currentTime = Date()
            val duration = calculateDuration(lastActivityStartTime!!, currentTime)
            val completedRecord = "⏹️ $lastActivityInfo (Duration: $duration)"
            logList.add(completedRecord)
            historyText.append("$completedRecord\n\n")

            // Reset current activity
            lastActivityStartTime = null
            lastActivityInfo = null

            // Update UI state
            updateButtonStates()

            // Scroll to bottom to show latest entry
            scrollToBottom()

            Toast.makeText(this, "Activity stopped and logged!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateButtonStates() {
        val hasCurrentActivity = lastActivityStartTime != null
        stopButton.visibility = if (hasCurrentActivity) android.view.View.VISIBLE else android.view.View.GONE
        startButton.text = if (hasCurrentActivity) "🔄 START NEW ACTIVITY" else "🚀 START FIRST ACTIVITY"
    }

    private fun scrollToBottom() {
        // Post to ensure the text has been updated before scrolling
        historyScrollView.post {
            historyScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun calculateDuration(startTime: Date, endTime: Date): String {
        val durationMs = endTime.time - startTime.time
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))

        return when {
            hours > 0 -> String.format("%dh %dm %ds", hours, minutes, seconds)
            minutes > 0 -> String.format("%dm %ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }

    private fun exportCsv() {
        try {
            // If there's a current activity, calculate its duration up to now
            if (lastActivityStartTime != null && lastActivityInfo != null) {
                val currentTime = Date()
                val duration = calculateDuration(lastActivityStartTime!!, currentTime)
                val currentRecord = "⏸️ $lastActivityInfo (Duration: $duration - ongoing)"
                logList.add(currentRecord)
            }

            val file = File(getExternalFilesDir(null), "timelog.csv")
            val writer = FileWriter(file)
            writer.write("Activity Log\n")
            for (line in logList) {
                writer.write("$line\n")
            }
            writer.flush()
            writer.close()
            Toast.makeText(this, "CSV exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}