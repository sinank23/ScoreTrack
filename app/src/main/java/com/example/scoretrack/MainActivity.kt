package com.example.scoretrack

import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scoretrack.ui.MatchAdapter
import com.example.scoretrack.ui.MatchViewModel
import com.example.scoretrack.model.Match // Match modelini import etmeyi unutmuyoruz!
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val viewModel: MatchViewModel by viewModels()
    private lateinit var matchAdapter: MatchAdapter
    private var selectedCalendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        setupLiveSwitch()
        observeData()

        updateDateUIAndFetch()
    }

    private fun setupRecyclerView() {
        // HATA 1 ÇÖZÜLDÜ: Senin XML'deki doğru ID'ni (rvMatches) girdik.
        val recyclerView = findViewById<RecyclerView>(R.id.rvMatches)

        // HATA 2 ÇÖZÜLDÜ: Adapter'ın bizden sadece Int (Maç ID'si) beklediğini belirttik.
        matchAdapter = MatchAdapter(emptyList<Match>()) { matchId ->
            Toast.makeText(this, "Maç ID: $matchId tıklandı", Toast.LENGTH_SHORT).show()
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

    private fun updateDateUIAndFetch() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(selectedCalendar.time)
        viewModel.fetchMatchesByDate(dateString)
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