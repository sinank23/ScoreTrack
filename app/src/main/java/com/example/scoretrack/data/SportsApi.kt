package com.example.scoretrack.data

import com.example.scoretrack.model.EventResponse
import com.example.scoretrack.model.LineupResponse
import com.example.scoretrack.model.MatchResponse
import com.example.scoretrack.model.PlayerResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SportsApi {
    @GET("fixtures?live=all")
    suspend fun getLiveMatches(): MatchResponse

    @GET("fixtures")
    suspend fun getMatchesByDate(
        @Query("date") date: String,
        @Query("timezone") timezone: String
    ): MatchResponse

    @GET("fixtures/events")
    suspend fun getMatchEvents(@Query("fixture") fixtureId: Int): EventResponse

    @GET("fixtures/lineups")
    suspend fun getMatchLineups(@Query("fixture") fixtureId: Int): LineupResponse

    @GET("players")
    suspend fun getPlayerProfile(
        @Query("id") playerId: Int,
        @Query("season") season: Int = 2023
    ): PlayerResponse
}