package com.example.scoretrack

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.scoretrack.model.Lineup
import com.example.scoretrack.ui.MatchViewModel

class MatchDetailActivity : AppCompatActivity() {

    private val viewModel: MatchViewModel by viewModels()
    private lateinit var contentContainer: LinearLayout
    private var fixtureId: Int = -1
    private var currentTab = "EVENTS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F6F9"))
            fitsSystemWindows = true
            setPadding(0, 48, 0, 0)
        }

        val tabLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
            setBackgroundColor(Color.WHITE)
            elevation = 8f
        }

        val btnEvents = createTabButton("Maç Olayları", true)
        val btnLineups = createTabButton("Kadrolar", false)

        tabLayout.addView(btnEvents)
        tabLayout.addView(btnLineups)
        mainLayout.addView(tabLayout)

        val scrollView = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT) }
        contentContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 60) }
        scrollView.addView(contentContainer)
        mainLayout.addView(scrollView)
        setContentView(mainLayout)

        fixtureId = intent.getIntExtra("FIXTURE_ID", -1)
        if (fixtureId != -1) {
            viewModel.fetchMatchEvents(fixtureId)
            viewModel.fetchMatchLineups(fixtureId)
        }

        btnEvents.setOnClickListener { currentTab = "EVENTS"; resetTabs(btnEvents, btnLineups); renderEvents() }
        btnLineups.setOnClickListener { currentTab = "LINEUPS"; resetTabs(btnLineups, btnEvents); renderLineups() }

        viewModel.matchEvents.observe(this) { if (currentTab == "EVENTS") renderEvents() }
        viewModel.matchLineups.observe(this) { if (currentTab == "LINEUPS") renderLineups() }
        viewModel.error.observe(this) { msg -> if (!msg.isNullOrEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun createTabButton(text: String, isSelected: Boolean): TextView {
        return TextView(this).apply {
            this.text = text; textSize = 16f; setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(if (isSelected) Color.parseColor("#2ECC71") else Color.parseColor("#7F8C8D"))
            gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, 32, 0, 32)
        }
    }

    private fun resetTabs(selected: TextView, unselected: TextView) {
        selected.setTypeface(null, Typeface.BOLD); selected.setTextColor(Color.parseColor("#2ECC71"))
        unselected.setTypeface(null, Typeface.NORMAL); unselected.setTextColor(Color.parseColor("#7F8C8D"))
    }

    private fun renderLineups() {
        contentContainer.removeAllViews()
        val lineups = viewModel.matchLineups.value ?: return

        val pitchLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = 24f; setColor(Color.parseColor("#27ae60")); setStroke(6, Color.WHITE) }
            setPadding(16, 48, 16, 48); elevation = 16f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 32, 0, 32) }
        }

        lineups.getOrNull(0)?.let { pitchLayout.addView(createTeamHeader(it)); drawTeamFormation(pitchLayout, it, false) }
        pitchLayout.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4).apply { setMargins(0, 48, 0, 48) }; setBackgroundColor(Color.WHITE) })
        lineups.getOrNull(1)?.let { drawTeamFormation(pitchLayout, it, true); pitchLayout.addView(createTeamHeader(it).apply { setPadding(0, 48, 0, 0) }) }

        contentContainer.addView(pitchLayout)
    }

    private fun createTeamHeader(lineup: Lineup): TextView {
        return TextView(this).apply { text = "${lineup.team?.name ?: "?"} (${lineup.formation ?: "?"})"; textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(0, 0, 0, 32) }
    }

    private fun drawTeamFormation(pitch: LinearLayout, lineup: Lineup, reverse: Boolean) {
        val validPlayers = lineup.startXI?.mapNotNull { it.player } ?: emptyList()
        val grouped = validPlayers.groupBy { it.grid?.substringBefore(":")?.toIntOrNull() ?: 0 }
        val sortedRows = if (reverse) grouped.keys.sortedDescending() else grouped.keys.sorted()

        for (row in sortedRows) {
            if (row == 0) continue
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 24, 0, 24) } }

            for (playerInfo in grouped[row]!!) {
                val playerView = TextView(this).apply {
                    text = "${playerInfo.number ?: ""} ${playerInfo.name?.split(" ")?.lastOrNull() ?: "?"}"
                    textSize = 12f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); gravity = Gravity.CENTER
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#2C3E50")); setStroke(3, Color.WHITE) }
                    setPadding(24, 24, 24, 24); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(8, 0, 8, 0) }

                    setOnClickListener {
                        playerInfo.id?.let { id ->
                            val intent = Intent(this@MatchDetailActivity, PlayerProfileActivity::class.java)
                            intent.putExtra("PLAYER_ID", id)
                            startActivity(intent)
                        } ?: Toast.makeText(this@MatchDetailActivity, "Detay bulunamadı", Toast.LENGTH_SHORT).show()
                    }
                }
                rowLayout.addView(playerView)
            }
            pitch.addView(rowLayout)
        }
    }

    private fun renderEvents() {
        contentContainer.removeAllViews()
        val events = viewModel.matchEvents.value ?: return

        events.sortedBy { it.time.elapsed }.forEach { event ->
            var eventColor = Color.parseColor("#95A5A6"); var icon = "🔄"; var titleText = "Değişiklik"
            val htmlText = "<font color='#2ECC71'>🔼</font> ${event.assist?.name ?: "?"}<br><font color='#E74C3C'>🔽</font> ${event.player.name ?: "?"}"
            var detailText: CharSequence = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)

            when (event.type) {
                "Goal" -> { eventColor = Color.parseColor("#2ECC71"); icon = "⚽"; titleText = "GOOOLL!"; detailText = event.player.name ?: "?" }
                "Card" -> { if (event.detail.contains("Yellow")) { eventColor = Color.parseColor("#F1C40F"); icon = "🟨"; titleText = "Sarı Kart" } else { eventColor = Color.parseColor("#E74C3C"); icon = "🟥"; titleText = "Kırmızı Kart" }; detailText = event.player.name ?: "?" }
            }

            val cardLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(32, 32, 32, 32)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 32) }
                background = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = 30f; setColor(Color.WHITE) }; elevation = 12f

                setOnClickListener {
                    event.player.id?.let { id ->
                        val intent = Intent(this@MatchDetailActivity, PlayerProfileActivity::class.java)
                        intent.putExtra("PLAYER_ID", id)
                        startActivity(intent)
                    }
                }
            }

            val colorStrip = View(this).apply { layoutParams = LinearLayout.LayoutParams(12, LinearLayout.LayoutParams.MATCH_PARENT).apply { setMargins(0, 0, 24, 0) }; background = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = 8f; setColor(eventColor) } }
            val extraTime = if (event.time.extra != null) "+${event.time.extra}" else ""
            val tvMinute = TextView(this).apply { text = "${event.time.elapsed}$extraTime'"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(eventColor); layoutParams = LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT); gravity = Gravity.CENTER }

            val contentLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
            val tvTeam = TextView(this).apply { text = event.team.name ?: "?"; textSize = 12f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#95A5A6")) }
            val tvTitleEvent = TextView(this).apply { text = "$icon $titleText"; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#2C3E50")); setPadding(0, 4, 0, 4) }
            val tvDetail = TextView(this).apply { text = detailText; textSize = 14f; setTextColor(Color.parseColor("#34495E")) }

            contentLayout.addView(tvTeam); contentLayout.addView(tvTitleEvent); contentLayout.addView(tvDetail)
            cardLayout.addView(colorStrip); cardLayout.addView(tvMinute); cardLayout.addView(contentLayout)
            contentContainer.addView(cardLayout)
        }
    }
}