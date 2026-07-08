package com.example.scoretrack.model

data class PlayerResponse(val response: List<PlayerWrapper>?)
data class PlayerWrapper(val player: PlayerDetails?, val statistics: List<PlayerStats>?)

data class PlayerDetails(
    val id: Int?,
    val name: String?,
    val age: Int?,
    val nationality: String?,
    val height: String?,
    val weight: String?,
    val photo: String?
)

data class PlayerStats(
    val team: Team?,
    val games: Games?,
    val goals: PlayerGoals?
)

data class Games(val appearences: Int?, val position: String?)
data class PlayerGoals(val total: Int?, val assists: Int?)