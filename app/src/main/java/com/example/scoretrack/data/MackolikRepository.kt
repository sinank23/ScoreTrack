package com.example.scoretrack.data

import com.example.scoretrack.model.EventKind
import com.example.scoretrack.model.KeyEventDto
import com.example.scoretrack.model.LiveScoreData
import com.example.scoretrack.model.Match
import com.example.scoretrack.model.MatchDto
import com.example.scoretrack.model.MatchEvent
import com.example.scoretrack.model.MatchLineups
import com.example.scoretrack.model.MatchState
import com.example.scoretrack.model.SidesIntDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object MackolikRepository {

    // Mackolik ligleri zaten kendi önem sırasına göre gönderiyor (sortOrder).
    // Buradakiler o sıralamadan bağımsız olarak HER ZAMAN en üste çıkar.
    // Eşleşme: (ülke adı, lig adının içinde geçen kelime). İstediğin kadar ekleyebilirsin.
    private val PINNED_LEAGUES = listOf(
        "Türkiye" to "Süper Lig"
    )

    private fun teamLogoUrl(teamId: String?): String =
        if (teamId.isNullOrEmpty()) "" else "https://file.mackolikfeeds.com/teams/$teamId"

    private fun flagUrl(countryId: String?): String =
        if (countryId.isNullOrEmpty()) "" else "https://file.mackolikfeeds.com/areas/$countryId"

    private fun SidesIntDto?.asScoreText(): String? {
        val home = this?.home ?: return null
        val away = this.away ?: return null
        return "$home - $away"
    }

    // ---------------- MAÇ LİSTESİ ----------------

    // Hata olursa EXCEPTION FIRLATIR; yakalamak ViewModel'in işi.
    suspend fun fetchMatches(dateStr: String): List<Match> = withContext(Dispatchers.IO) {
        val response = RetrofitClient.mackolikApi.getLiveScores(matchDate = dateStr)
        if (response.status != "success" || response.data == null) {
            throw IOException("Mackolik beklenmeyen bir yanıt döndü (${response.status})")
        }
        mapResponse(response.data)
    }

    fun mapResponse(data: LiveScoreData): List<Match> {
        val competitions = data.competitions.orEmpty()
        val matches = data.matches.orEmpty().values

        // Her ligin Mackolik'teki en küçük sortOrder değeri = o ligin sırası
        val mackolikRank: Map<String?, Int> = matches
            .groupBy { it.competitionId }
            .mapValues { (_, list) -> list.minOf { it.sortOrder ?: Int.MAX_VALUE } }

        return matches.mapNotNull { dto ->
            val competition = competitions[dto.competitionId]
            if (competition?.sport != null && competition.sport != "S") return@mapNotNull null

            val leagueName = competition?.name ?: "Diğer"
            val countryName = competition?.country?.name.orEmpty()

            val pinnedIndex = PINNED_LEAGUES.indexOfFirst { (country, name) ->
                countryName.equals(country, ignoreCase = true) && leagueName.contains(name, ignoreCase = true)
            }
            val rank = if (pinnedIndex >= 0) pinnedIndex - PINNED_LEAGUES.size - 1
            else mackolikRank[dto.competitionId] ?: Int.MAX_VALUE

            Match(
                id = dto.id,
                competitionId = dto.competitionId ?: "unknown",
                leagueName = leagueName,
                countryName = countryName,
                leagueFlag = flagUrl(competition?.country?.id),
                leagueRank = rank,
                homeTeam = dto.homeTeam.name,
                awayTeam = dto.awayTeam.name,
                homeLogo = teamLogoUrl(dto.homeTeam.id),
                awayLogo = teamLogoUrl(dto.awayTeam.id),
                homeScore = dto.score?.home.orEmpty(),
                awayScore = dto.score?.away.orEmpty(),
                kickoffMillis = dto.mstUtc ?: 0L,
                state = stateOf(dto),
                statusLabel = dto.statusBoxContent?.takeIf { it.isNotBlank() },
                periodId = dto.periodId,
                periodStart = dto.periodStart,
                halfTimeScore = dto.score?.ht.asScoreText(),
                penaltyScore = dto.score?.pen.asScoreText(),
                homeRedCards = dto.redCards?.home ?: 0,
                awayRedCards = dto.redCards?.away ?: 0,
                sortOrder = dto.sortOrder ?: Int.MAX_VALUE
            )
        }.sortedWith(
            compareBy<Match>({ it.leagueRank }, { it.competitionId }, { it.kickoffMillis }, { it.sortOrder })
        )
    }

    private fun stateOf(dto: MatchDto): MatchState = when (dto.state) {
        "live" -> MatchState.LIVE
        "post" -> MatchState.FINISHED
        else -> {
            // Başlamamış ama Mackolik bir etiket koymuşsa (ERT, İPT ...) maç ertelenmiş/iptal demektir
            val postponed = dto.substate in setOf("postponed", "cancelled", "abandoned") ||
                    !dto.statusBoxContent.isNullOrBlank()
            if (postponed) MatchState.POSTPONED else MatchState.SCHEDULED
        }
    }

    // ---------------- MAÇ OLAYLARI ----------------

    suspend fun fetchMatchEvents(matchId: String): List<MatchEvent> = withContext(Dispatchers.IO) {
        val response = RetrofitClient.mackolikApi.getKeyEvents(matchId = matchId)
        if (response.status != "success") {
            throw IOException("Maç olayları alınamadı (${response.status})")
        }
        mapKeyEvents(response.data?.keyEvents.orEmpty())
    }

    fun mapKeyEvents(events: List<KeyEventDto>): List<MatchEvent> = events.map { dto ->
        val kind = when (dto.type) {
            "goal" -> when (dto.subType) {
                "own-goal" -> EventKind.OWN_GOAL
                "penalty-goal" -> EventKind.PENALTY_GOAL
                else -> EventKind.GOAL
            }
            "card" -> when (dto.subType) {
                "y2c" -> EventKind.SECOND_YELLOW
                "rc" -> EventKind.RED
                else -> EventKind.YELLOW
            }
            "substitute" -> EventKind.SUBSTITUTION
            "penalty-missed" -> EventKind.PENALTY_MISSED
            else -> EventKind.OTHER
        }

        val detail = when (kind) {
            EventKind.GOAL -> dto.assistPlayerName?.let { "Asist: $it" }
            EventKind.PENALTY_GOAL -> "Penaltı"
            EventKind.OWN_GOAL -> "Kendi kalesine"
            EventKind.PENALTY_MISSED -> "Penaltı kaçırdı"
            EventKind.SECOND_YELLOW -> "İkinci sarı kart"
            EventKind.SUBSTITUTION -> dto.playerOutName?.let { "Çıkan: $it" }
            else -> null
        }

        MatchEvent(
            kind = kind,
            isHome = dto.position == "home",
            minute = dto.timeMin?.replace(" ", "")?.let { "$it'" } ?: "",
            periodId = dto.periodId ?: 0,
            player = dto.playerName ?: "?",
            detail = detail,
            score = dto.score?.replace("-", " - ")
        )
    }

    // ---------------- KADROLAR ----------------

    // Kadrolar henüz açıklanmadıysa null döner
    suspend fun fetchLineups(matchId: String): MatchLineups? = withContext(Dispatchers.IO) {
        val html = RetrofitClient.mackolikApi.getMatchPage(matchId).string()
        LineupParser.parse(html)
    }
}