package com.example.scoretrack.data

import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface SportsApi {
    // SİHİRLİ DOKUNUŞ: ?sports=soccer diyerek Mackolik'e sadece futbol maçlarını istediğimizi zorunlu olarak belirttik!
    @GET("perform/p0/ajax/components/competition/livescores/json?sports=soccer")
    suspend fun getMackolikMatches(
        @Query("matchDate") date: String
    ): JsonElement
}