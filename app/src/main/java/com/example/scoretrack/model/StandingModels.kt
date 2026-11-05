package com.example.scoretrack.model

// Puan tablosundaki bir satır
data class StandingRow(
    val rank: Int,
    val teamId: String,
    val teamName: String,
    val logo: String,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDiff: Int,
    val points: Int,
    val zone: Int?          // 1 = Şampiyonlar Ligi, ... (renk için), null = renksiz
)

// Tablonun altındaki açıklama: hangi renk ne demek
data class ZoneLegend(
    val zone: Int,
    val label: String       // "Şampiyonlar Ligi", "Küme Düşme" ...
)

data class LeagueTable(
    val title: String,              // "Türkiye Trendyol Süper Lig 2026/2027"
    val rows: List<StandingRow>,
    val legend: List<ZoneLegend>
)