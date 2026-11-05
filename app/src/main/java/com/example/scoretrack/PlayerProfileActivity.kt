package com.example.scoretrack

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import coil.load
import coil.transform.CircleCropTransformation
import com.example.scoretrack.model.CareerRow
import com.example.scoretrack.model.PlayerProfile
import com.example.scoretrack.ui.PlayerState
import com.example.scoretrack.ui.PlayerViewModel

class PlayerProfileActivity : AppCompatActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    private lateinit var llDetails: LinearLayout
    private lateinit var scroll: View
    private lateinit var tvEmpty: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_profile)

        llDetails = findViewById(R.id.llDetails)
        scroll = findViewById(R.id.scroll)
        tvEmpty = findViewById(R.id.tvEmpty)
        progress = findViewById(R.id.progress)

        val profileUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        findViewById<TextView>(R.id.tvTopTitle).text = intent.getStringExtra(EXTRA_NAME).orEmpty()
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        tvEmpty.setOnClickListener { viewModel.load(profileUrl, force = true) }

        viewModel.state.observe(this) { render(it) }
        viewModel.load(profileUrl)
    }

    private fun render(state: PlayerState) {
        progress.visibility = View.GONE
        tvEmpty.visibility = View.GONE
        scroll.visibility = View.GONE

        when (state) {
            PlayerState.Loading -> progress.visibility = View.VISIBLE
            is PlayerState.Failed -> {
                tvEmpty.text = "${state.message}\n\nTekrar denemek için dokun."
                tvEmpty.visibility = View.VISIBLE
            }
            is PlayerState.Ready -> {
                scroll.visibility = View.VISIBLE
                showProfile(state.profile)
            }
        }
    }

    // ---------------- ÜST KART ----------------

    private fun showProfile(profile: PlayerProfile) {
        val info = profile.info
        findViewById<TextView>(R.id.tvTopTitle).text = info.name
        findViewById<TextView>(R.id.tvName).text = info.name
        findViewById<ImageView>(R.id.ivPhoto).load(info.photo) {
            transformations(CircleCropTransformation())
        }
        findViewById<TextView>(R.id.tvNumberPosition).text =
            listOfNotNull(info.shirtNumber?.let { "#$it" }, info.position).joinToString("  ·  ")
        findViewById<TextView>(R.id.tvClub).text = info.clubName.orEmpty()
        findViewById<ImageView>(R.id.ivClub).load(info.clubLogo)
        findViewById<TextView>(R.id.tvNationality).text = info.nationality.orEmpty()
        findViewById<ImageView>(R.id.ivFlag).load(info.flag)

        llDetails.removeAllViews()

        // Kişisel bilgiler
        addSectionTitle("BİLGİLER")
        val birth = listOfNotNull(info.birthDate, info.birthPlace?.let { "($it)" }).joinToString(" ")
        addInfoRow("Yaş", info.age?.toString())
        addInfoRow("Doğum", birth.ifEmpty { null })
        addInfoRow("Boy", info.height)
        addInfoRow("Kilo", info.weight)
        addInfoRow("Ayak", info.foot)

        // Oynadığı takımlar
        if (profile.teams.isNotEmpty()) {
            addSectionTitle("KARİYER — OYNADIĞI TAKIMLAR")
            addStatHeader()
            profile.teams.forEach { addStatRow(it, showCompetition = false) }
        }

        // Sezon sezon
        if (profile.seasons.isNotEmpty()) {
            addSectionTitle("SEZONLAR")
            for (season in profile.seasons) {
                if (season.rows.none { !it.isTotal }) continue
                addSeasonTitle(season.season)
                addStatHeader()
                season.rows.forEach { addStatRow(it, showCompetition = true) }
            }
        }
    }

    // ---------------- YARDIMCI ÇİZİMLER ----------------

    private fun addSectionTitle(text: String) {
        llDetails.addView(TextView(this).apply {
            this.text = text
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color(R.color.accent))
            setPadding(dp(16), dp(20), dp(16), dp(6))
        })
    }

    private fun addSeasonTitle(text: String) {
        llDetails.addView(TextView(this).apply {
            this.text = text
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color(R.color.text_primary))
            setPadding(dp(16), dp(12), dp(16), dp(2))
        })
    }

    private fun addInfoRow(label: String, value: String?) {
        if (value.isNullOrBlank()) return
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(color(R.color.bg_surface))
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(color(R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(dp(90), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(color(R.color.text_primary))
        })
        llDetails.addView(row)
    }

    // Sütun başlıkları: M (maç), G (gol), A (asist), İ11 (ilk 11)
    private fun addStatHeader() {
        val row = statRowContainer()
        row.addView(cell("", weight = true, bold = true, secondary = true))
        listOf("M", "G", "A", "İ11").forEach { row.addView(cell(it, bold = true, secondary = true)) }
        llDetails.addView(row)
    }

    private fun addStatRow(item: CareerRow, showCompetition: Boolean) {
        val row = statRowContainer().apply {
            setBackgroundColor(color(if (item.isTotal) R.color.bg_score else R.color.bg_surface))
        }

        // Sol kısım: logo + takım (sezon tablosunda üstte turnuva adı, altta takım)
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        if (!item.isTotal) {
            left.addView(ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) }
                load(item.teamLogo)
            })
        }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val mainText = if (showCompetition && !item.isTotal) item.competition ?: item.team else item.team
        texts.addView(TextView(this).apply {
            text = mainText.orEmpty()
            textSize = 14f
            setTextColor(color(R.color.text_primary))
            setTypeface(null, if (item.isTotal) Typeface.BOLD else Typeface.NORMAL)
        })
        if (showCompetition && !item.isTotal && item.team != null) {
            texts.addView(TextView(this).apply {
                text = item.team
                textSize = 12f
                setTextColor(color(R.color.text_secondary))
            })
        }
        left.addView(texts)
        row.addView(left)

        listOf(item.appearances, item.goals, item.assists, item.starts).forEach {
            row.addView(cell(it.toString(), bold = item.isTotal))
        }
        llDetails.addView(row)
    }

    private fun statRowContainer() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(8), dp(12), dp(8))
    }

    private fun cell(text: String, weight: Boolean = false, bold: Boolean = false, secondary: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(null, if (bold) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(color(if (secondary) R.color.text_secondary else R.color.text_primary))
            layoutParams = if (weight) LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            else LinearLayout.LayoutParams(dp(36), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

    private fun color(id: Int) = ContextCompat.getColor(this, id)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_URL = "PROFILE_URL"
        private const val EXTRA_NAME = "PLAYER_NAME"

        fun start(context: Context, profileUrl: String, playerName: String) {
            val intent = Intent(context, PlayerProfileActivity::class.java).apply {
                putExtra(EXTRA_URL, profileUrl)
                putExtra(EXTRA_NAME, playerName)
            }
            context.startActivity(intent)
        }
    }
}