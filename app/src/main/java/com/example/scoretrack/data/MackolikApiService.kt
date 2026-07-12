package com.example.scoretrack.data

import com.example.scoretrack.model.LiveScoreResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MackolikApiService {

    @GET("perform/p0/ajax/components/competition/livescores/json")
    suspend fun getLiveScores(
        @Query("sports[]") sports: List<String> = listOf("Soccer"),
        @Query("matchDate") matchDate: String
    ): LiveScoreResponse
}