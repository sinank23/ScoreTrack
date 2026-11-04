package com.example.scoretrack

import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scoretrack.ui.MatchAdapter
import com.example.scoretrack.ui.MatchViewModel
import com.example.scoretrack.model.Match
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private val viewModel: MatchViewModel by viewModels()
    private lateinit var matchAdapter: MatchAdapter
    private var selectedCalendar: Calendar = Calendar.getInstance()

    private lateinit var tvSelectedDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        setupLiveSwitch()
        setupDateNavigation()
        observeData()

        updateDateUIAndFetch()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rvMatches)

        matchAdapter = MatchAdapter(emptyList<Match>()) { matchId ->
            val intent = Intent(this, MatchDetailActivity::class.java)
            intent.putExtra("FIXTURE_ID", matchId)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = matchAdapter
    }

    private fun setupLiveSwitch() {
        val switchLive = findViewById<Switch>(R.id.switchLive)
        switchLive.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.fetchLiveMatches()
            } else {
                updateDateUIAndFetch()
            }
        }
    }

    private fun setupDateNavigation() {
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        val btnPrevDay = findViewById<TextView>(R.id.btnPrevDay)
        val btnNextDay = findViewById<TextView>(R.id.btnNextDay)

        btnPrevDay.setOnClickListener {
            selectedCalendar.add(Calendar.DAY_OF_MONTH, -1)
            updateDateUIAndFetch()
        }

        btnNextDay.setOnClickListener {
            selectedCalendar.add(Calendar.DAY_OF_MONTH, 1)
            updateDateUIAndFetch()
        }
    }

    private fun updateDateUIAndFetch() {
        val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = apiDateFormat.format(selectedCalendar.time)

        tvSelectedDate.text = formatDisplayDate(selectedCalendar)

        viewModel.fetchMatchesByDate(dateString)
    }

    private fun formatDisplayDate(calendar: Calendar): String {
        val today = Calendar.getInstance()

        val isSameDay = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

        return if (isSameDay) {
            "BUGÜN"
        } else {
            val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            displayFormat.format(calendar.time)
        }
    }

    private fun observeData() {
        viewModel.matches.observe(this) { matches ->
            matchAdapter.updateData(matches)
        }

        viewModel.error.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}