package com.example.scoretrack

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.scoretrack.model.EventKind
import com.example.scoretrack.model.LiveHeader
import com.example.scoretrack.model.Match
import com.example.scoretrack.model.MatchEvent
import com.example.scoretrack.ui.LineupRenderer
import com.example.scoretrack.ui.LineupState
import com.example.scoretrack.ui.MatchDetailViewModel
import com.example.scoretrack.util.MatchStatusFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MatchDetailActivity : AppCompatActivity() {

    private val viewModel: MatchDetailViewModel by viewModels()

    private lateinit var llContent: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var progress: ProgressBar
    private lateinit var tabEvents: TextView
    private lateinit var tabLineups: TextView

    private var matchId: String = ""
    private var homeName: String = ""
    private var awayName: String = ""
    private var showingEvents = true
    private var isLive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match_detail)

        llContent = findViewById(R.id.llContent)
        tvEmpty = findViewById(R.id.tvEmpty)
        progress = findViewById(R.id.progress)
        tabEvents = findViewById(R.id.tabEvents)
        tabLineups = findViewById(R.id.tabLineups)

        matchId = intent.getStringExtra(EXTRA_ID).orEmpty()
        homeName = intent.getStringExtra(EXTRA_HOME).orEmpty()
        awayName = intent.getStringExtra(EXTRA_AWAY).orEmpty()
        isLive = intent.getBooleanExtra(EXTRA_IS_LIVE, false)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        bindHeader()

        tabEvents.setOnClickListener { selectTab(events = true) }
        tabLineups.setOnClickListener { selectTab(events = false) }
        selectTab(events = true)

        observeData()
        if (savedInstanceState == null || viewModel.events.value == null) {
            viewModel.loadEvents(matchId)
        }
        startLiveRefresh()
    }

    // Maç canlıysa ekran açıkken her 30 saniyede skor, dakika ve olayları yenile.
    // Maç biterse döngü durur; uygulama arka plana geçince de duraklar.
    private fun startLiveRefresh() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isLive) {
                    delay(LIVE_REFRESH_MS)
                    viewModel.refreshLive(matchId)
                }
            }
        }
    }

    private fun bindLiveHeader(header: LiveHeader) {
        if (!header.hasStarted) return
        isLive = header.isLive

        val color = ContextCompat.getColor(this, if (header.isLive) R.color.live else R.color.text_primary)
        val statusColor = ContextCompat.getColor(this, if (header.isLive) R.color.live else R.color.text_secondary)

        findViewById<TextView>(R.id.tvScore).apply {
            text = "${header.homeScore} - ${header.awayScore}"
            setTextColor(color)
        }
        findViewById<TextView>(R.id.tvStatus).apply {
            text = header.status
            setTextColor(statusColor)
        }
        findViewById<TextView>(R.id.tvExtra).text = header.detail.orEmpty()
    }

    // ---------------- ÜST KISIM ----------------

    private fun bindHeader() {
        val i = intent
        findViewById<TextView>(R.id.tvLeague).text = i.getStringExtra(EXTRA_LEAGUE)
        findViewById<TextView>(R.id.tvHomeTeam).text = i.getStringExtra(EXTRA_HOME)
        findViewById<TextView>(R.id.tvAwayTeam).text = i.getStringExtra(EXTRA_AWAY)
        findViewById<ImageView>(R.id.ivHomeLogo).load(i.getStringExtra(EXTRA_HOME_LOGO))
        findViewById<ImageView>(R.id.ivAwayLogo).load(i.getStringExtra(EXTRA_AWAY_LOGO))

        val liveColor = ContextCompat.getColor(this, R.color.live)

        findViewById<TextView>(R.id.tvScore).apply {
            text = i.getStringExtra(EXTRA_SCORE)
            if (isLive) setTextColor(liveColor)
        }
        findViewById<TextView>(R.id.tvStatus).apply {
            text = i.getStringExtra(EXTRA_STATUS)
            if (isLive) setTextColor(liveColor)
        }
        findViewById<TextView>(R.id.tvExtra).text = i.getStringExtra(EXTRA_EXTRA).orEmpty()
    }

    // ---------------- SEKMELER ----------------

    private fun selectTab(events: Boolean) {
        showingEvents = events
        if (!events) viewModel.loadLineupsIfNeeded(matchId)
        styleTab(tabEvents, selected = events)
        styleTab(tabLineups, selected = !events)
        render()
    }

    private fun styleTab(tab: TextView, selected: Boolean) {
        val color = if (selected) R.color.accent else R.color.text_secondary
        tab.setTextColor(ContextCompat.getColor(this, color))
        tab.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    // ---------------- VERİ ----------------

    private fun observeData() {
        viewModel.events.observe(this) { if (showingEvents) render() }
        viewModel.lineups.observe(this) { if (!showingEvents) render() }
        viewModel.header.observe(this) { header -> header?.let { bindLiveHeader(it) } }

        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun render() {
        llContent.removeAllViews()
        tvEmpty.visibility = View.GONE
        progress.visibility = View.GONE
        if (showingEvents) renderEvents() else renderLineups()
    }

    private fun renderLineups() {
        when (val state = viewModel.lineups.value) {
            null, LineupState.Loading -> progress.visibility = View.VISIBLE
            LineupState.NotAnnounced -> showEmpty("Kadrolar henüz açıklanmadı.\nGenelde maçtan yaklaşık 1 saat önce açıklanır.")
            is LineupState.Failed -> showEmpty(state.message)
            is LineupState.Ready -> LineupRenderer.render(llContent, state.lineups, homeName, awayName)
        }
    }

    private fun renderEvents() {
        val events = viewModel.events.value
        if (events == null) {  // henüz yükleniyor
            progress.visibility = View.VISIBLE
            return
        }
        if (events.isEmpty()) {
            showEmpty("Gösterilecek maç olayı yok.")
            return
        }

        var lastPeriodLabel: String? = null
        for (event in events) {
            val label = periodLabel(event.periodId)
            if (label != null && label != lastPeriodLabel) {
                llContent.addView(createPeriodHeader(label))
                lastPeriodLabel = label
            }
            llContent.addView(createEventRow(event))
        }
    }

    private fun showEmpty(message: String) {
        tvEmpty.text = message
        tvEmpty.visibility = View.VISIBLE
    }

    private fun periodLabel(periodId: Int): String? = when (periodId) {
        1 -> "1. YARI"
        2 -> "2. YARI"
        3, 4 -> "UZATMALAR"
        5 -> "PENALTILAR"
        else -> null
    }

    private fun createPeriodHeader(label: String): TextView = TextView(this).apply {
        text = label
        textSize = 12f
        setTypeface(null, Typeface.BOLD)
        setTextColor(ContextCompat.getColor(context, R.color.accent))
        gravity = Gravity.CENTER
        setPadding(0, 20, 0, 8)
    }

    private fun createEventRow(event: MatchEvent): View {
        val row = LayoutInflater.from(this).inflate(R.layout.item_event, llContent, false)
        row.findViewById<TextView>(R.id.tvMinute).text = event.minute

        val isGoal = event.kind == EventKind.GOAL ||
                event.kind == EventKind.PENALTY_GOAL ||
                event.kind == EventKind.OWN_GOAL

        // Alt satır: gollerde skor + açıklama, diğerlerinde sadece açıklama
        val sub = listOfNotNull(if (isGoal) event.score else null, event.detail).joinToString(" · ")

        val mainId = if (event.isHome) R.id.tvHomeMain else R.id.tvAwayMain
        val subId = if (event.isHome) R.id.tvHomeSub else R.id.tvAwaySub

        row.findViewById<TextView>(mainId).apply {
            // İkon dakikaya yakın tarafta dursun
            text = if (event.isHome) "${event.player}  ${event.kind.icon}" else "${event.kind.icon}  ${event.player}"
            setTypeface(null, if (isGoal) Typeface.BOLD else Typeface.NORMAL)
        }
        row.findViewById<TextView>(subId).apply {
            text = sub
            visibility = if (sub.isEmpty()) View.GONE else View.VISIBLE
        }
        return row
    }

    companion object {
        private const val EXTRA_ID = "FIXTURE_ID"
        private const val EXTRA_LEAGUE = "LEAGUE"
        private const val EXTRA_HOME = "HOME"
        private const val EXTRA_AWAY = "AWAY"
        private const val EXTRA_HOME_LOGO = "HOME_LOGO"
        private const val EXTRA_AWAY_LOGO = "AWAY_LOGO"
        private const val EXTRA_SCORE = "SCORE"
        private const val EXTRA_STATUS = "STATUS"
        private const val EXTRA_EXTRA = "EXTRA"
        private const val EXTRA_IS_LIVE = "IS_LIVE"
        private const val LIVE_REFRESH_MS = 30_000L

        // Listeden detaya geçerken maçın üst bilgilerini de taşıyoruz,
        // böylece başlık anında dolu görünüyor.
        fun start(context: Context, match: Match) {
            val intent = Intent(context, MatchDetailActivity::class.java).apply {
                putExtra(EXTRA_ID, match.id)
                putExtra(EXTRA_LEAGUE, listOf(match.countryName, match.leagueName)
                    .filter { it.isNotEmpty() }.joinToString(" · "))
                putExtra(EXTRA_HOME, match.homeTeam)
                putExtra(EXTRA_AWAY, match.awayTeam)
                putExtra(EXTRA_HOME_LOGO, match.homeLogo)
                putExtra(EXTRA_AWAY_LOGO, match.awayLogo)
                putExtra(EXTRA_SCORE, if (match.hasScore) "${match.homeScore} - ${match.awayScore}" else "-")
                putExtra(EXTRA_STATUS, MatchStatusFormatter.statusText(match))
                putExtra(EXTRA_EXTRA, MatchStatusFormatter.extraInfo(match))
                putExtra(EXTRA_IS_LIVE, match.isLive)
            }
            context.startActivity(intent)
        }
    }
}