package com.example.scoretrack.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import coil.transform.CircleCropTransformation
import com.example.scoretrack.PlayerProfileActivity
import com.example.scoretrack.R
import com.example.scoretrack.model.MatchLineups
import com.example.scoretrack.model.SquadPlayer
import com.example.scoretrack.model.TeamLineup

// Kadrolar sekmesini çizer: önce saha dizilişi, sonra iki takımın listeleri.
// Sahadaki yuvarlaklara ya da listedeki oyunculara dokununca oyuncu profili açılır.
object LineupRenderer {

    fun render(
        container: LinearLayout,
        lineups: MatchLineups,
        homeName: String,
        awayName: String
    ) {
        val context = container.context

        if (lineups.pitch.isNotEmpty()) {
            val pitch = PitchView(context).apply {
                setData(
                    lineups.pitch,
                    homeLabel = label(homeName, lineups.home.formation),
                    awayLabel = label(awayName, lineups.away.formation)
                )
                onPlayerClick = { player ->
                    player.profileUrl?.let { PlayerProfileActivity.start(context, it, player.name) }
                }
            }
            container.addView(pitch)
        }

        addTeam(container, homeName, lineups.home)
        addTeam(container, awayName, lineups.away)
    }

    private fun label(name: String, formation: String?) =
        if (formation.isNullOrEmpty()) name else "$name  ·  $formation"

    private fun addTeam(container: LinearLayout, teamName: String, team: TeamLineup) {
        val context = container.context
        container.addView(sectionTitle(context, label(teamName, team.formation), big = true))

        if (team.starters.isNotEmpty()) {
            container.addView(sectionTitle(context, "İLK 11", big = false))
            team.starters.forEach { container.addView(playerRow(context, it)) }
        }
        if (team.substitutes.isNotEmpty()) {
            container.addView(sectionTitle(context, "YEDEKLER", big = false))
            team.substitutes.forEach { container.addView(playerRow(context, it)) }
        }
        team.coach?.let {
            container.addView(sectionTitle(context, "TEKNİK DİREKTÖR", big = false))
            container.addView(playerRow(context, SquadPlayer("", it, "", "")))
        }
    }

    private fun sectionTitle(context: Context, text: String, big: Boolean) = TextView(context).apply {
        this.text = text
        textSize = if (big) 16f else 12f
        setTypeface(null, Typeface.BOLD)
        setTextColor(ContextCompat.getColor(context, if (big) R.color.accent else R.color.text_secondary))
        val top = if (big) dp(context, 24) else dp(context, 12)
        setPadding(dp(context, 4), top, 0, dp(context, 4))
    }

    private fun playerRow(context: Context, player: SquadPlayer): View {
        val primary = ContextCompat.getColor(context, R.color.text_primary)
        val secondary = ContextCompat.getColor(context, R.color.text_secondary)

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 4), dp(context, 6), dp(context, 4), dp(context, 6))

            // Oyuncu fotoğrafı (yuvarlak)
            addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(context, 32), dp(context, 32)).apply {
                    marginEnd = dp(context, 6)
                }
                if (player.photo != null) {
                    load(player.photo) { transformations(CircleCropTransformation()) }
                }
            })
            addView(TextView(context).apply {
                text = player.number
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(primary)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(context, 28), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(context).apply {
                text = player.position
                textSize = 12f
                setTextColor(secondary)
                layoutParams = LinearLayout.LayoutParams(dp(context, 34), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(context).apply {
                text = player.name
                textSize = 14f
                setTextColor(primary)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(context).apply {
                text = player.notes
                textSize = 12f
                setTextColor(secondary)
            })

            // Profili olan oyunculara dokunulabilsin
            player.profileUrl?.let { url ->
                val outValue = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setOnClickListener { PlayerProfileActivity.start(context, url, player.name) }
            }
        }
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}