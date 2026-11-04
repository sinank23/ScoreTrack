package com.example.scoretrack.model

data class Match(
    val id: Int,
    val apiMatchId: String,   // <-- YENİ: gerçek maç ID'si (detay sayfası için)
    val leagueName: String,
    val leagueFlag: String,
    val countryName: String,
    val homeTeam: String,
    val awayTeam: String,
    val scoreText: String,
    val homeLogo: String,
    val awayLogo: String,
    val isLive: Boolean
)

data class MatchResponse(val response: List<ApiMatch>?)
data class ApiMatch(val fixture: Fixture, val league: League, val teams: Teams, val goals: Goals)
data class Fixture(val id: Int, val date: String, val status: Status)
data class Status(val short: String, val elapsed: Int?)
data class League(val name: String, val country: String?, val flag: String?)
data class Teams(val home: Team, val away: Team)
data class Team(val id: Int?, val name: String?, val logo: String?)
data class Goals(val home: Int?, val away: Int?)

data class EventResponse(val response: List<MatchEvent>?)
data class MatchEvent(
    val time: EventTime,
    val team: Team,
    val player: PlayerInfo,
    val assist: PlayerInfo?,
    val type: String,
    val detail: String
)
data class EventTime(val elapsed: Int, val extra: Int?)
data class PlayerInfo(val id: Int?, val name: String?)

data class LineupResponse(val response: List<Lineup>?)
data class Lineup(val team: Team?, val formation: String?, val startXI: List<StartXI>?)
data class StartXI(val player: PlayerGridInfo?)
data class PlayerGridInfo(val id: Int?, val name: String?, val number: Int?, val grid: String?)