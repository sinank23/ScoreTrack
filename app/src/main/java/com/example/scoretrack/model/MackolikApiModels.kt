package com.example.scoretrack.model

import com.google.gson.annotations.SerializedName

// Mackolik "livescores" uç noktasının ham JSON modelleri.
// Örnek: perform/p0/ajax/components/competition/livescores/json?sports[]=Soccer&matchDate=2026-11-04

data class LiveScoreResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("data") val data: LiveScoreData?
)

data class LiveScoreData(
    @SerializedName("competitions") val competitions: Map<String, CompetitionDto>?,
    @SerializedName("matches") val matches: Map<String, MatchDto>?
)

data class CompetitionDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("country") val country: CountryDto?,
    @SerializedName("sport") val sport: String?,
    @SerializedName("code") val code: String?
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
    @SerializedName("sortOrder") val sortOrder: Int?,

    // Başlama saati (UTC, milisaniye)
    @SerializedName("mstUtc") val mstUtc: Long?,

    // "pre" (başlamadı), "live" (oynanıyor), "post" (bitti)
    @SerializedName("state") val state: String?,
    // "none", "halfTime", "fullTime", "penalties", "postponed", "suspended" ...
    @SerializedName("substate") val substate: String?,

    // Devre bilgisi: 1 = ilk yarı, 2 = ikinci yarı, 3/4 = uzatmalar, 10 = devre arası
    @SerializedName("periodId") val periodId: Int?,
    // İçinde bulunulan devrenin başladığı an (UTC, milisaniye). Dakikayı buradan hesaplıyoruz.
    @SerializedName("periodStart") val periodStart: Long?,

    // Mackolik'in hazır durum etiketi: "MS", "İY", "ERT", "PEN", "DUR" ... (bazen null)
    @SerializedName("statusBoxContent") val statusBoxContent: String?,

    @SerializedName("score") val score: ScoreDto?,
    @SerializedName("redCards") val redCards: SidesIntDto?,
    @SerializedName("winner") val winner: String?
)

data class TeamDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String?
)

data class ScoreDto(
    @SerializedName("home") val home: String?,
    @SerializedName("away") val away: String?,
    @SerializedName("ht") val ht: SidesIntDto?,
    @SerializedName("pen") val pen: SidesIntDto?,
    @SerializedName("agg") val agg: SidesIntDto?
)

data class SidesIntDto(
    @SerializedName("home") val home: Int?,
    @SerializedName("away") val away: Int?
)
