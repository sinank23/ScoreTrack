package com.example.scoretrack.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.load
import com.example.scoretrack.R
import com.example.scoretrack.model.Match

class MatchAdapter(
    private var matchList: List<Match>,
    private val onMatchClick: (Int) -> Unit // <-- TIKLAMA SENSÖRÜ EKLENDİ
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    class MatchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val llLeagueHeader: LinearLayout = view.findViewById(R.id.llLeagueHeader)
        val ivLeagueFlag: ImageView = view.findViewById(R.id.ivLeagueFlag)
        val tvLeagueHeader: TextView = view.findViewById(R.id.tvLeagueHeader)
        val tvHome: TextView = view.findViewById(R.id.tvHomeTeam)
        val tvAway: TextView = view.findViewById(R.id.tvAwayTeam)
        val tvScore: TextView = view.findViewById(R.id.tvScore)
        val ivHomeLogo: ImageView = view.findViewById(R.id.ivHomeLogo)
        val ivAwayLogo: ImageView = view.findViewById(R.id.ivAwayLogo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val currentMatch = matchList[position]
        val context = holder.itemView.context

        val svgImageLoader = ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build()

        if (position == 0 || matchList[position - 1].leagueName != currentMatch.leagueName) {
            holder.llLeagueHeader.visibility = View.VISIBLE
            holder.tvLeagueHeader.text = currentMatch.leagueName
            holder.ivLeagueFlag.load(currentMatch.leagueFlag, svgImageLoader) { crossfade(true) }
        } else {
            holder.llLeagueHeader.visibility = View.GONE
        }

        holder.tvHome.text = currentMatch.homeTeam
        holder.tvAway.text = currentMatch.awayTeam
        holder.tvScore.text = currentMatch.scoreText

        if (currentMatch.isLive) holder.tvScore.setTextColor(Color.RED)
        else holder.tvScore.setTextColor(Color.BLACK)

        holder.ivHomeLogo.load(currentMatch.homeLogo) { crossfade(true) }
        holder.ivAwayLogo.load(currentMatch.awayLogo) { crossfade(true) }

        // MAÇA TIKLANDIĞINDA ID'Yİ GÖNDERİYORUZ
        holder.itemView.setOnClickListener {
            onMatchClick(currentMatch.id)
        }
    }

    override fun getItemCount(): Int = matchList.size

    fun updateData(newList: List<Match>) {
        matchList = newList
        notifyDataSetChanged()
    }
}