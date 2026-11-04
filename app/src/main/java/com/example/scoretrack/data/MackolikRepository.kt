package com.example.scoretrack.data

import com.example.scoretrack.model.Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MackolikRepository {

    // Öncelik listesi: listenin başındaki lig, ekranın en üstünde gösterilir.
    // İstersen buraya yeni lig isimleri ekleyip sırasını değiştirebilirsin.
    private val LEAGUE_PRIORITY = listOf(
        "dünya kupası",
        "şampiyonlar ligi",
        "avrupa ligi",
        "süper lig",
        "premier league",
        "la liga",
        "bundesliga",
        "serie a",
        "ligue 1"
    )

    private fun priorityOf(leagueName: String): Int {
        val lower = leagueName.lowercase()
        val index = LEAGUE_PRIORITY.indexOfFirst { lower.contains(it) }
        return if (index == -1) LEAGUE_PRIORITY.size else index
    }

    private fun teamLogoUrl(teamId: String?): String {
        return if (teamId.isNullOrEmpty()) "" else "https://file.mackolikfeeds.com/teams/$teamId"
    }

    suspend fun fetchMatches(dateStr: String): List<Match> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.mackolikApi.getLiveScores(matchDate = dateStr)
            val data = response.data

            val mapped = data.matches.values.mapNotNull { matchDto ->
                val competition = data.competitions[matchDto.competitionId]

                if (competition?.sport != null && competition.sport != "S") {
                    return@mapNotNull null
                }

                val homeScore = matchDto.score?.home.orEmpty()
                val awayScore = matchDto.score?.away.orEmpty()
                val scoreText = if (homeScore.isNotEmpty() && awayScore.isNotEmpty()) {
                    "$homeScore - $awayScore"
                } else {
                    "-"
                }

                val leagueName = competition?.name ?: "Diğer"
                val countryId = competition?.country?.id

                Match(
                    id = matchDto.id.hashCode(),
                    apiMatchId = matchDto.id,
                    leagueName = leagueName,
                    homeTeam = matchDto.homeTeam.name,
                    awayTeam = matchDto.awayTeam.name,
                    scoreText = scoreText,
                    isLive = matchDto.state == "live",
                    homeLogo = teamLogoUrl(matchDto.homeTeam.id),
                    awayLogo = teamLogoUrl(matchDto.awayTeam.id),
                    countryName = competition?.country?.name ?: "",
                    leagueFlag = countryId?.let { "https://file.mackolikfeeds.com/areas/$it" } ?: ""
                )
            }

            // Maçları önce lig önceliğine, sonra lig ismine göre sırala
            // (aynı ligin maçları böylece bir arada kalır, adapter'daki başlık mantığı bozulmaz)
            mapped.sortedWith(compareBy({ priorityOf(it.leagueName) }, { it.leagueName }))

        } catch (e: Exception) {
            emptyList()
        }
    }
}