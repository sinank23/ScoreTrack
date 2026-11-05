package com.example.scoretrack

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import coil.load
import com.example.scoretrack.model.LeagueTable
import com.example.scoretrack.model.StandingRow
import com.example.scoretrack.ui.StandingsState
import com.example.scoretrack.ui.StandingsViewModel

class StandingsActivity : AppCompatActivity() {

    private val viewModel: StandingsViewModel by viewModels()

    private lateinit var llRows: LinearLayout
    private lateinit var llColumnHeader: View
    private lateinit var tvTitle: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var progress: ProgressBar

    // Maç detayından gelindiyse o maçın iki takımı tabloda vurgulanır
    private var highlightTeams: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_standings)

        llRows = findViewById(R.id.llRows)
        llColumnHeader = findViewById(R.id.llColumnHeader)
        tvTitle = findViewById(R.id.tvTitle)
        tvEmpty = findViewById(R.id.tvEmpty)
        progress = findViewById(R.id.progress)

        val competitionId = intent.getStringExtra(EXTRA_COMPETITION_ID).orEmpty()
        tvTitle.text = intent.getStringExtra(EXTRA_LEAGUE_NAME).orEmpty()
        highlightTeams = intent.getStringArrayListExtra(EXTRA_HIGHLIGHT)?.toSet().orEmpty()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        tvEmpty.setOnClickListener { viewModel.load(competitionId, force = true) }

        viewModel.state.observe(this) { render(it) }
        viewModel.load(competitionId)
    }

    private fun render(state: StandingsState) {
        llRows.removeAllViews()
        progress.visibility = View.GONE
        tvEmpty.visibility = View.GONE
        llColumnHeader.visibility = View.GONE

        when (state) {
            StandingsState.Loading -> progress.visibility = View.VISIBLE
            StandingsState.NoTable -> showEmpty("Bu turnuvanın puan tablosu yok.\n(Kupa maçları eleme usulüyle oynanır.)")
            is StandingsState.Failed -> showEmpty("${state.message}\n\nTekrar denemek için dokun.")
            is StandingsState.Ready -> showTable(state.table)
        }
    }

    private fun showEmpty(message: String) {
        tvEmpty.text = message
        tvEmpty.visibility = View.VISIBLE
    }

    private fun showTable(table: LeagueTable) {
        if (table.title.isNotEmpty()) tvTitle.text = table.title
        llColumnHeader.visibility = View.VISIBLE

        val inflater = LayoutInflater.from(this)
        table.rows.forEachIndexed { index, row ->
            val view = inflater.inflate(R.layout.item_standing, llRows, false)
            bindRow(view, row, index)
            llRows.addView(view)
        }

        if (table.legend.isNotEmpty()) addLegend(table)
    }

    private fun bindRow(view: View, row: StandingRow, index: Int) {
        view.findViewById<View>(R.id.vZone).setBackgroundColor(zoneColor(row.zone))
        view.findViewById<TextView>(R.id.tvRank).text = row.rank.toString()
        view.findViewById<ImageView>(R.id.ivLogo).load(row.logo)
        view.findViewById<TextView>(R.id.tvPlayed).text = row.played.toString()
        view.findViewById<TextView>(R.id.tvWon).text = row.won.toString()
        view.findViewById<TextView>(R.id.tvDrawn).text = row.drawn.toString()
        view.findViewById<TextView>(R.id.tvLost).text = row.lost.toString()
        view.findViewById<TextView>(R.id.tvDiff).text =
            if (row.goalDiff > 0) "+${row.goalDiff}" else row.goalDiff.toString()
        view.findViewById<TextView>(R.id.tvPoints).text = row.points.toString()

        val isHighlighted = row.teamName in highlightTeams
        view.findViewById<TextView>(R.id.tvTeam).apply {
            text = row.teamName
            setTypeface(null, if (isHighlighted) Typeface.BOLD else Typeface.NORMAL)
        }

        // Satırları zebra desenli yap, vurgulanan takımları belirgin göster
        val background = when {
            isHighlighted -> ContextCompat.getColor(this, R.color.bg_score)
            index % 2 == 0 -> ContextCompat.getColor(this, R.color.bg_surface)
            else -> ContextCompat.getColor(this, R.color.bg_page)
        }
        view.setBackgroundColor(background)
    }

    private fun addLegend(table: LeagueTable) {
        val title = TextView(this).apply {
            text = "AÇIKLAMA"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(dp(16), dp(20), dp(16), dp(6))
        }
        llRows.addView(title)

        for (item in table.legend) {
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(4), dp(16), dp(4))
            }
            line.addView(View(this).apply {
                setBackgroundColor(zoneColor(item.zone))
                layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { marginEnd = dp(10) }
            })
            line.addView(TextView(this).apply {
                text = item.label
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            })
            llRows.addView(line)
        }
    }

    // Mackolik'in bölge numaralarına karşılık gelen renkler
    private fun zoneColor(zone: Int?): Int = when (zone) {
        1 -> Color.parseColor("#1E88E5")   // Şampiyonlar Ligi
        2 -> Color.parseColor("#64B5F6")   // Şampiyonlar Ligi eleme
        3 -> Color.parseColor("#FB8C00")   // Avrupa Ligi
        4 -> Color.parseColor("#43A047")   // Konferans Ligi
        5 -> Color.parseColor("#E53935")   // Küme düşme
        6 -> Color.parseColor("#8E24AA")
        7 -> Color.parseColor("#FDD835")
        else -> Color.TRANSPARENT
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_COMPETITION_ID = "COMPETITION_ID"
        private const val EXTRA_LEAGUE_NAME = "LEAGUE_NAME"
        private const val EXTRA_HIGHLIGHT = "HIGHLIGHT"

        fun start(
            context: Context,
            competitionId: String,
            leagueName: String,
            highlightTeams: List<String> = emptyList()
        ) {
            val intent = Intent(context, StandingsActivity::class.java).apply {
                putExtra(EXTRA_COMPETITION_ID, competitionId)
                putExtra(EXTRA_LEAGUE_NAME, leagueName)
                putStringArrayListExtra(EXTRA_HIGHLIGHT, ArrayList(highlightTeams))
            }
            context.startActivity(intent)
        }
    }
}