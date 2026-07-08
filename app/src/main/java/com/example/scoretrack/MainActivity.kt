package com.example.scoretrack

import android.app.DatePickerDialog
import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val viewModel: MatchViewModel by viewModels()
    private lateinit var matchAdapter: MatchAdapter
    private var selectedCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        setupDateSelector()
        setupLiveSwitch()
        observeData()

        updateDateUIAndFetch()
    }

    private fun setupRecyclerView() {
        matchAdapter = MatchAdapter(emptyList()) { fixtureId ->
            // YENİ: Tıkladığımızda pop-up yerine MatchDetailActivity sayfasına geçiyoruz
            val intent = Intent(this, MatchDetailActivity::class.java)
            intent.putExtra("FIXTURE_ID", fixtureId) // Maçın kimliğini yeni sayfaya yolluyoruz
            startActivity(intent)
        }
        val recyclerView = findViewById<RecyclerView>(R.id.rvMatches)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = matchAdapter
    }

    private fun setupDateSelector() {
        val btnPrevDay = findViewById<TextView>(R.id.btnPrevDay)
        val btnNextDay = findViewById<TextView>(R.id.btnNextDay)
        val tvSelectedDate = findViewById<TextView>(R.id.tvSelectedDate)

        btnPrevDay.setOnClickListener {
            selectedCalendar.add(Calendar.DAY_OF_YEAR, -1)
            updateDateUIAndFetch()
        }

        btnNextDay.setOnClickListener {
            selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
            updateDateUIAndFetch()
        }

        tvSelectedDate.setOnClickListener {
            val dpd = DatePickerDialog(this, { _, y, m, d ->
                selectedCalendar.set(Calendar.YEAR, y)
                selectedCalendar.set(Calendar.MONTH, m)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, d)
                updateDateUIAndFetch()
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH))
            dpd.show()
        }
    }

    private fun updateDateUIAndFetch() {
        val tvSelectedDate = findViewById<TextView>(R.id.tvSelectedDate)
        val uiFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
        val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

        if (isToday(selectedCalendar)) tvSelectedDate.text = "BUGÜN"
        else tvSelectedDate.text = uiFormat.format(selectedCalendar.time)

        viewModel.fetchMatchesByDate(apiFormat.format(selectedCalendar.time))
    }

    private fun isToday(cal: Calendar): Boolean {
        val today = Calendar.getInstance()
        return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun setupLiveSwitch() {
        val switchLive = findViewById<Switch>(R.id.switchLive)
        switchLive.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.fetchLiveMatches()
            else updateDateUIAndFetch()
        }
    }

    private fun observeData() {
        viewModel.matches.observe(this) { matchAdapter.updateData(it) }
        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}