package com.vidrierapp.data.remote.api

import com.vidrierapp.data.remote.dto.ReportRentedResponseDto
import com.vidrierapp.data.remote.dto.ShopDto
import com.vidrierapp.data.remote.dto.CheckoutRequestDto
import com.vidrierapp.data.remote.dto.TrialRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    @POST("api/v1/shops/{id}/report-rented")
    suspend fun reportRented(
        @Path("id") id: String
    ): ReportRentedResponseDto

    @POST("api/v1/auth/trial")
    suspend fun startTrial(@Body request: TrialRequestDto)

    @POST("api/v1/payments/checkout")
    suspend fun checkout(@Body request: CheckoutRequestDto)
}
