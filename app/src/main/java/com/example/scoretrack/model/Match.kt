package com.example.scoretrack.model

enum class MatchState {
    SCHEDULED,   // başlamadı
    LIVE,        // oynanıyor (devre arası, penaltılar ve durdurulmuş maçlar dahil)
    FINISHED,    // bitti
    POSTPONED    // ertelendi / iptal
}

// Ekranda kullandığımız, Mackolik'ten bağımsız sade maç modeli.
data class Match(
    val id: String,
    val competitionId: String,
    val leagueName: String,
    val countryName: String,
    val leagueFlag: String,
    val leagueRank: Int,          // küçük olan üstte gösterilir

    val homeTeam: String,
    val awayTeam: String,
    val homeLogo: String,
    val awayLogo: String,

    val homeScore: String,        // başlamamış maçta boş
    val awayScore: String,

    val kickoffMillis: Long,      // UTC milisaniye
    val state: MatchState,
    val statusLabel: String?,     // Mackolik'in hazır etiketi (MS, İY, ERT, PEN, DUR) varsa
    val periodId: Int?,
    val periodStart: Long?,

    val halfTimeScore: String?,   // "1 - 0"
    val penaltyScore: String?,    // "5 - 3"
    val homeRedCards: Int,
    val awayRedCards: Int,
    val sortOrder: Int
) {
    val isLive: Boolean get() = state == MatchState.LIVE
    val hasScore: Boolean get() = homeScore.isNotEmpty() && awayScore.isNotEmpty()
}
