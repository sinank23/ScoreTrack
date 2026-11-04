package com.example.scoretrack.ui

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.scoretrack.R
import com.example.scoretrack.model.Match
import com.example.scoretrack.model.MatchState
import com.example.scoretrack.util.MatchStatusFormatter

// Listede iki tür satır var: lig başlığı ve maç.
// Başlıkları ayrı satır yapmak, filtreleme sonrası başlıkların kaybolması hatasını engelliyor.
sealed class MatchListItem {
    data class Header(
        val competitionId: String,
        val leagueName: String,
        val countryName: String,
        val flagUrl: String
    ) : MatchListItem()

    data class Row(val match: Match) : MatchListItem()
}

class MatchAdapter(
    private val onMatchClick: (Match) -> Unit
) : ListAdapter<MatchListItem, RecyclerView.ViewHolder>(DIFF) {

    fun submitMatches(matches: List<Match>) {
        val items = ArrayList<MatchListItem>(matches.size + 16)
        var lastCompetition: String? = null
        for (match in matches) {
            if (match.competitionId != lastCompetition) {
                items += MatchListItem.Header(
                    competitionId = match.competitionId,
                    leagueName = match.leagueName,
                    countryName = match.countryName,
                    flagUrl = match.leagueFlag
                )
                lastCompetition = match.competitionId
            }
            items += MatchListItem.Row(match)
        }
        submitList(items)
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MatchListItem.Header -> TYPE_HEADER
        is MatchListItem.Row -> TYPE_MATCH
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_league_header, parent, false))
        } else {
            MatchViewHolder(inflater.inflate(R.layout.item_match, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MatchListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is MatchListItem.Row -> (holder as MatchViewHolder).bind(item.match, onMatchClick)
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivFlag: ImageView = view.findViewById(R.id.ivLeagueFlag)
        private val tvCountry: TextView = view.findViewById(R.id.tvCountry)
        private val tvLeague: TextView = view.findViewById(R.id.tvLeagueName)

        fun bind(header: MatchListItem.Header) {
            tvLeague.text = header.leagueName
            tvCountry.text = header.countryName
            tvCountry.visibility = if (header.countryName.isEmpty()) View.GONE else View.VISIBLE
            ivFlag.load(header.flagUrl)
        }
    }

    class MatchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        private val tvHome: TextView = view.findViewById(R.id.tvHomeTeam)
        private val tvAway: TextView = view.findViewById(R.id.tvAwayTeam)
        private val tvScore: TextView = view.findViewById(R.id.tvScore)
        private val tvExtra: TextView = view.findViewById(R.id.tvExtra)
        private val tvHomeRed: TextView = view.findViewById(R.id.tvHomeRed)
        private val tvAwayRed: TextView = view.findViewById(R.id.tvAwayRed)
        private val ivHomeLogo: ImageView = view.findViewById(R.id.ivHomeLogo)
        private val ivAwayLogo: ImageView = view.findViewById(R.id.ivAwayLogo)

        fun bind(match: Match, onClick: (Match) -> Unit) {
            val context = itemView.context
            val primary = ContextCompat.getColor(context, R.color.text_primary)
            val secondary = ContextCompat.getColor(context, R.color.text_secondary)
            val live = ContextCompat.getColor(context, R.color.live)

            tvHome.text = match.homeTeam
            tvAway.text = match.awayTeam

            // Durum sütunu
            tvStatus.text = MatchStatusFormatter.statusText(match)
            tvStatus.setTextColor(if (match.isLive) live else secondary)

            // Skor
            tvScore.text = if (match.hasScore) "${match.homeScore} - ${match.awayScore}" else "-"
            tvScore.setTextColor(if (match.isLive) live else primary)

            // Biten maçta kazanan takımı kalın yaz
            val homeWon = match.state == MatchState.FINISHED && winnerIsHome(match)
            val awayWon = match.state == MatchState.FINISHED && winnerIsAway(match)
            tvHome.setTypeface(null, if (homeWon) Typeface.BOLD else Typeface.NORMAL)
            tvAway.setTypeface(null, if (awayWon) Typeface.BOLD else Typeface.NORMAL)

            // Ertelenen maçı soluk göster
            itemView.alpha = if (match.state == MatchState.POSTPONED) 0.55f else 1f

            val extra = MatchStatusFormatter.extraInfo(match)
            tvExtra.text = extra
            tvExtra.visibility = if (extra == null) View.GONE else View.VISIBLE

            bindRedCards(tvHomeRed, match.homeRedCards)
            bindRedCards(tvAwayRed, match.awayRedCards)

            ivHomeLogo.load(match.homeLogo)
            ivAwayLogo.load(match.awayLogo)

            itemView.setOnClickListener { onClick(match) }
        }

        private fun bindRedCards(view: TextView, count: Int) {
            view.visibility = if (count > 0) View.VISIBLE else View.GONE
            view.text = if (count > 1) count.toString() else ""
        }

        // Penaltıyla bitenlerde normal skor eşit olabilir, o yüzden penaltıya da bakıyoruz
        private fun winnerIsHome(m: Match): Boolean = compareScores(m) > 0
        private fun winnerIsAway(m: Match): Boolean = compareScores(m) < 0

        private fun compareScores(m: Match): Int {
            val home = m.homeScore.toIntOrNull() ?: return 0
            val away = m.awayScore.toIntOrNull() ?: return 0
            if (home != away) return home.compareTo(away)
            val pen = m.penaltyScore?.split(" - ")?.mapNotNull { it.trim().toIntOrNull() }
            return if (pen != null && pen.size == 2) pen[0].compareTo(pen[1]) else 0
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_MATCH = 1

        private val DIFF = object : DiffUtil.ItemCallback<MatchListItem>() {
            override fun areItemsTheSame(oldItem: MatchListItem, newItem: MatchListItem): Boolean =
                when {
                    oldItem is MatchListItem.Header && newItem is MatchListItem.Header ->
                        oldItem.competitionId == newItem.competitionId
                    oldItem is MatchListItem.Row && newItem is MatchListItem.Row ->
                        oldItem.match.id == newItem.match.id
                    else -> false
                }

            override fun areContentsTheSame(oldItem: MatchListItem, newItem: MatchListItem): Boolean =
                oldItem == newItem
        }
    }
}
