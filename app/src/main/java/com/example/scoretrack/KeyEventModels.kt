package com.example.scoretrack.model

import com.google.gson.annotations.SerializedName

// Mackolik "key-events" uç noktasının ham JSON modelleri.
// Örnek: ajax/football/key-events?ajaxViewName=events&matchId=5pj94g9tr074nroow0eawmzo4

data class KeyEventsResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("data") val data: KeyEventsData?
)

data class KeyEventsData(
    @SerializedName("keyEvents") val keyEvents: List<KeyEventDto>?,
    @SerializedName("matchState") val matchState: String?
)

data class KeyEventDto(
    @SerializedName("type") val type: String?,          // goal, card, substitute, penalty-missed
    @SerializedName("subType") val subType: String?,    // goal, own-goal, penalty-goal, yc, y2c, rc, pm
    @SerializedName("position") val position: String?,  // home / away
    @SerializedName("periodId") val periodId: Int?,     // 1, 2, 3, 4 (uzatmalar), 5 (penaltılar)
    @SerializedName("timeMin") val timeMin: String?,    // "12", "45 +3"
    @SerializedName("playerName") val playerName: String?,
    @SerializedName("assistPlayerName") val assistPlayerName: String?,
    @SerializedName("playerOutName") val playerOutName: String?,
    @SerializedName("score") val score: String?         // sadece gollerde: "2-0"
)

// Ekranda kullandığımız sade olay modeli
enum class EventKind(val icon: String) {
    GOAL("⚽"),
    PENALTY_GOAL("⚽"),
    OWN_GOAL("⚽"),
    PENALTY_MISSED("❌"),
    YELLOW("🟨"),
    SECOND_YELLOW("🟨🟥"),
    RED("🟥"),
    SUBSTITUTION("🔄"),
    OTHER("•")
}

data class MatchEvent(
    val kind: EventKind,
    val isHome: Boolean,
    val minute: String,      // "45+3'"
    val periodId: Int,
    val player: String,
    val detail: String?,     // "Asist: D. Sanchez", "Çıkan: R. Sallai", "Penaltı" ...
    val score: String?       // gollerde "2 - 0"
)