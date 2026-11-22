package com.vidrierapp.data.remote.api

import com.vidrierapp.data.remote.dto.ShopDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface VidrierAppApiService {
    @GET("api/v1/shops")
    suspend fun getShops(
        @Query("category") category: String? = null
    ): List<ShopDto>

    @GET("api/v1/shops/{id}")
    suspend fun getShopDetail(
        @Path("id") id: String
    ): ShopDto
}
