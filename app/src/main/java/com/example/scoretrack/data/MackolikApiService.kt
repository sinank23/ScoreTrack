package com.example.scoretrack.data

import com.example.scoretrack.model.KeyEventsResponse
import com.example.scoretrack.model.LiveScoreResponse
import com.example.scoretrack.model.MatchHeaderResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MackolikApiService {

    @GET("perform/p0/ajax/components/competition/livescores/json")
    suspend fun getLiveScores(
        @Query("sports[]") sports: List<String> = listOf("Soccer"),
        @Query("matchDate") matchDate: String
    ): LiveScoreResponse

    // Maç olayları: goller, kartlar, oyuncu değişiklikleri
    @GET("ajax/football/key-events")
    suspend fun getKeyEvents(
        @Query("matchId") matchId: String,
        @Query("ajaxViewName") viewName: String = "events"
    ): KeyEventsResponse

    // Maç sayfasının HTML'i (kadrolar burada). Adresteki "x" yerine normalde takım adları
    // yazıyor ama Mackolik doğru sayfaya kendisi yönlendiriyor.
    @GET("mac/x/{matchId}")
    suspend fun getMatchPage(@Path("matchId") matchId: String): ResponseBody

    // Maçın güncel skoru ve dakikası (küçük bir HTML parçası, ~4 KB)
    @GET("perform/p0/ajax/components/match/matchHeader")
    suspend fun getMatchHeader(
        @Query("matchId") matchId: String,
        @Query("ajaxViewName") viewName: String = "match-details",
        @Query("ajaxPartialViewName") partialViewName: String = "match-details-status",
        @Query("displayMode") displayMode: String = "all",
        @Query("sdapiLanguageCode") language: String = "tr-mk"
    ): MatchHeaderResponse
}