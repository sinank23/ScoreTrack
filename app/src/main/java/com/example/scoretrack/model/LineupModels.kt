package com.example.scoretrack.model

// Sahadaki bir oyuncu (diziliş çizimi için)
// x: boy ekseninde 0..1 (0 = ev sahibinin kalesi, 1 = deplasmanın kalesi)
// y: en ekseninde 0..1
data class PitchPlayer(
    val isHome: Boolean,
    val number: String,
    val name: String,
    val x: Float,
    val y: Float
)

// Kadro listesindeki bir oyuncu
data class SquadPlayer(
    val number: String,
    val name: String,
    val position: String,   // Kal, Def, Ort, For (yedeklerde boş)
    val notes: String       // "⚽x2  ↓77'" gibi
)

data class TeamLineup(
    val formation: String?,
    val coach: String?,
    val starters: List<SquadPlayer>,
    val substitutes: List<SquadPlayer>
)

data class MatchLineups(
    val home: TeamLineup,
    val away: TeamLineup,
    val pitch: List<PitchPlayer>
)