package com.example.scoretrack.data

import com.example.scoretrack.model.LiveScoreResponse
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.scoretrack.model.KeyEventsResponse
import okhttp3.ResponseBody
import retrofit2.http.Path

interface MackolikApiService {

    @GET("perform/p0/ajax/components/competition/livescores/json")
    suspend fun getLiveScores(
        @Query("sports[]") sports: List<String> = listOf("Soccer"),
        @Query("matchDate") matchDate: String
    ): LiveScoreResponse

    //maç olayları
    @GET("ajax/football/key-events")
    suspend fun getKeyEvents(
        @Query("matchId") matchId: String,
        @Query("ajaxViewName") viewName: String = "events"
    ): KeyEventsResponse


    // Maç sayfasının HTML'i (kadrolar burada). Adresteki "x" yerine normalde takım adları
    // yazıyor ama Mackolik doğru sayfaya kendisi yönlendiriyor.
    @GET("mac/x/{matchId}")
    suspend fun getMatchPage(@Path("matchId") matchId: String): ResponseBody
}