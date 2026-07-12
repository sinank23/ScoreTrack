package com.example.scoretrack.model

import com.google.gson.annotations.SerializedName

data class LiveScoreResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: LiveScoreData
)

data class LiveScoreData(
    @SerializedName("competitions") val competitions: Map<String, CompetitionDto>,
    @SerializedName("matches") val matches: Map<String, MatchDto>
)

data class CompetitionDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("country") val country: CountryDto?,
    @SerializedName("sport") val sport: String?
)

data class CountryDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

data class MatchDto(
    @SerializedName("id") val id: String,
    @SerializedName("matchName") val matchName: String?,
    @SerializedName("homeTeam") val homeTeam: TeamDto,
    @SerializedName("awayTeam") val awayTeam: TeamDto,
    @SerializedName("competitionId") val competitionId: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("statusBoxContent") val statusBoxContent: String?,
    @SerializedName("score") val score: ScoreDto?
)

data class TeamDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String?
)

data class ScoreDto(
    @SerializedName("home") val home: String?,
    @SerializedName("away") val away: String?
)