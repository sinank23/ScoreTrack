package com.example.scoretrack.model

// Veri artık doğrudan liste olarak geldiği için,
// MackolikResponse sınıfına ihtiyacımız yok, direkt List<MackolikMatch> alacağız.
typealias MackolikResponse = List<MackolikMatch>

data class MackolikMatch(
    val homeTeam: String,
    val awayTeam: String,
    val score: String,
    val status: String,
    val leagueName: String
)