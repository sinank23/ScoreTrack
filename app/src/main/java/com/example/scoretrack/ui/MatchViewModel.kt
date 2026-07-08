package com.example.scoretrack.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoretrack.data.RetrofitClient
import com.example.scoretrack.model.Lineup
import com.example.scoretrack.model.Match
import com.example.scoretrack.model.MatchEvent
import com.example.scoretrack.model.MatchResponse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MatchViewModel : ViewModel() {

    private val _matches = MutableLiveData<List<Match>>()
    val matches: LiveData<List<Match>> get() = _matches

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _matchEvents = MutableLiveData<List<MatchEvent>>()
    val matchEvents: LiveData<List<MatchEvent>> get() = _matchEvents

    private val _matchLineups = MutableLiveData<List<Lineup>>()
    val matchLineups: LiveData<List<Lineup>> get() = _matchLineups

    fun fetchMatchesByDate(date: String) {
        viewModelScope.launch {
            try {
                val apiData = RetrofitClient.apiService.getMatchesByDate(date, "Europe/Istanbul")
                processApiData(apiData)
            } catch (e: Exception) { _error.value = "Hata: ${e.message}" }
        }
    }

    fun fetchLiveMatches() {
        viewModelScope.launch {
            try {
                val apiData = RetrofitClient.apiService.getLiveMatches()
                processApiData(apiData)
            } catch (e: Exception) { _error.value = "Hata: ${e.message}" }
        }
    }

    fun fetchMatchEvents(fixtureId: Int) {
        viewModelScope.launch {
            try {
                val apiData = RetrofitClient.apiService.getMatchEvents(fixtureId)
                // YENİ: Liste boş gelirse çökmesin diye ?: emptyList() ekledik
                _matchEvents.value = apiData.response ?: emptyList()
            } catch (e: Exception) { _error.value = "Detaylar alınamadı!" }
        }
    }

    fun fetchMatchLineups(fixtureId: Int) {
        viewModelScope.launch {
            try {
                val apiData = RetrofitClient.apiService.getMatchLineups(fixtureId)
                // YENİ: Liste boş gelirse çökmesin diye ?: emptyList() ekledik
                _matchLineups.value = apiData.response ?: emptyList()
            } catch (e: Exception) { _error.value = "Kadrolar alınamadı!" }
        }
    }

    private fun processApiData(apiData: MatchResponse) {
        // YENİ: apiData.response nullable olduğu için "?." ile güvenli çağırdık
        val mappedMatches = apiData.response?.map { apiMatch ->
            val status = apiMatch.fixture.status.short
            val elapsed = apiMatch.fixture.status.elapsed
            val hScore = apiMatch.goals.home?.toString() ?: "0"
            val aScore = apiMatch.goals.away?.toString() ?: "0"

            val liveCodes = listOf("1H", "HT", "2H", "ET", "BT", "P", "INT", "LIVE")
            val isMatchLive = liveCodes.contains(status)

            val scoreOrTime = when {
                status == "NS" || status == "TBD" -> getMatchTime(apiMatch.fixture.date)
                isMatchLive -> {
                    val minText = if (status == "HT") "İY" else if (elapsed != null) "$elapsed'" else ""
                    if (minText.isNotEmpty()) "$hScore - $aScore\n$minText" else "$hScore - $aScore"
                }
                else -> "$hScore - $aScore"
            }

            Match(
                id = apiMatch.fixture.id,
                leagueName = apiMatch.league.name,
                leagueFlag = apiMatch.league.flag ?: "",
                countryName = apiMatch.league.country ?: "World",
                // YENİ: İsim veya Logo boş gelirse çökmesin diye varsayılan değerler verdik (?: "")
                homeTeam = apiMatch.teams.home.name ?: "Bilinmeyen",
                awayTeam = apiMatch.teams.away.name ?: "Bilinmeyen",
                scoreText = scoreOrTime,
                homeLogo = apiMatch.teams.home.logo ?: "",
                awayLogo = apiMatch.teams.away.logo ?: "",
                isLive = isMatchLive
            )
        } ?: emptyList() // Maç listesi hepten boş gelirse boş liste döndür dedik

        val sorted = mappedMatches.sortedWith(compareBy<Match> { match ->
            val name = match.leagueName.lowercase().trim()
            val country = match.countryName.lowercase().trim()
            when {
                (name == "süper lig" || name == "super lig") && country == "turkey" -> 1
                (name == "1. lig" || name == "1.lig") && country == "turkey" -> 2
                (name.contains("türkiye kupası") || name.contains("turkiye kupasi")) && country == "turkey" -> 3
                name == "uefa champions league" || name == "champions league" -> 4
                name == "uefa europa league" || name == "europa league" -> 5
                name == "uefa europa conference league" || name == "conference league" -> 6
                name == "premier league" && country == "england" -> 7
                (name == "primera division" || name == "la liga") && country == "spain" -> 8
                name == "serie a" && country == "italy" -> 9
                (name == "bundesliga 1" || name == "bundesliga") && country == "germany" -> 10
                name == "ligue 1" && country == "france" -> 11
                name == "eredivisie" && country == "netherlands" -> 12
                name == "primeira liga" && country == "portugal" -> 13
                name == "pro league" && country == "saudi arabia" -> 14
                else -> 99
            }
        }.thenBy { it.leagueName })

        _matches.value = sorted
    }

    private fun getMatchTime(utcDate: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH)
            val date = parser.parse(utcDate)
            val formatter = SimpleDateFormat("HH:mm", Locale("tr", "TR"))
            formatter.timeZone = TimeZone.getTimeZone("Europe/Istanbul")
            date?.let { formatter.format(it) } ?: "--:--"
        } catch (e: Exception) {
            try {
                val fallbackParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
                fallbackParser.timeZone = TimeZone.getTimeZone("UTC")
                val fallbackDate = fallbackParser.parse(utcDate.take(19))
                val formatter = SimpleDateFormat("HH:mm", Locale("tr", "TR"))
                formatter.timeZone = TimeZone.getTimeZone("Europe/Istanbul")
                fallbackDate?.let { formatter.format(it) } ?: "--:--"
            } catch (ex: Exception) { "--:--" }
        }
    }
}