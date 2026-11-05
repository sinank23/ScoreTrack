package com.example.scoretrack.model

import com.google.gson.annotations.SerializedName

// Mackolik "matchHeader" uç noktası: maç sayfasının üst kısmını küçük bir HTML parçası olarak verir.
data class MatchHeaderResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("data") val data: MatchHeaderData?
)

data class MatchHeaderData(
    @SerializedName("html") val html: String?
)

// Canlı maçın güncel durumu
data class LiveHeader(
    val state: String,      // "preGame", "liveGame", "postGame"
    val homeScore: String,
    val awayScore: String,
    val status: String,     // "71'", "İY", "MS"
    val detail: String?     // "İY 1 - 1"
) {
    val isLive: Boolean get() = state == "liveGame"
    val hasStarted: Boolean get() = state == "liveGame" || state == "postGame"
}