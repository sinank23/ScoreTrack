package com.example.scoretrack.data

import com.example.scoretrack.model.Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MackolikRepository {

    suspend fun fetchMatches(dateStr: String): List<Match> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.mackolikApi.getLiveScores(matchDate = dateStr)
            val data = response.data

            data.matches.values.mapNotNull { matchDto ->
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

                Match(
                    id = matchDto.id.hashCode(),
                    leagueName = competition?.name ?: "Diğer",
                    homeTeam = matchDto.homeTeam.name,
                    awayTeam = matchDto.awayTeam.name,
                    scoreText = scoreText,
                    isLive = matchDto.state == "live",
                    homeLogo = "",
                    awayLogo = "",
                    countryName = competition?.country?.name ?: "",
                    leagueFlag = ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}