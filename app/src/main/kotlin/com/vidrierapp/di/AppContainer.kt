package com.vidrierapp.di

import android.content.Context
import androidx.room.Room
import com.vidrierapp.data.local.database.MIGRATION_1_2
import com.vidrierapp.data.local.database.VidrierAppDatabase
import com.vidrierapp.data.remote.api.MockInterceptor
import com.vidrierapp.data.remote.api.VidrierAppApiService
import com.vidrierapp.data.repository.RetrofitShopRepository
import com.vidrierapp.data.repository.RoomVisitRepository
import com.vidrierapp.domain.repository.ShopRepository
import com.vidrierapp.domain.repository.VisitRepository
import com.vidrierapp.domain.usecase.GetAllVisitsUseCase
import com.vidrierapp.domain.usecase.GetShopDetailUseCase
import com.vidrierapp.domain.usecase.GetShopsUseCase
import com.vidrierapp.domain.usecase.GetVisitsUseCase
import com.vidrierapp.domain.usecase.LogVisitUseCase
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(private val context: Context) {

    // Flag de debug activable para el motor de mocking local
    private val isMockModeEnabled: Boolean = true

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .apply {
                if (isMockModeEnabled) {
                    addInterceptor(MockInterceptor(context, json))
                }
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.vidrierapp.example.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val apiService: VidrierAppApiService by lazy {
        retrofit.create(VidrierAppApiService::class.java)
    }

    val database: VidrierAppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            VidrierAppDatabase::class.java,
            "vidrierapp.db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    val shopRepository: ShopRepository by lazy {
        RetrofitShopRepository(apiService)
    }

    val visitRepository: VisitRepository by lazy {
        RoomVisitRepository(database.visitDao())
    }

    val getShopsUseCase: GetShopsUseCase by lazy {
        GetShopsUseCase(shopRepository)
    }

    val getShopDetailUseCase: GetShopDetailUseCase by lazy {
        GetShopDetailUseCase(shopRepository)
    }

    val logVisitUseCase: LogVisitUseCase by lazy {
        LogVisitUseCase(visitRepository)
    }

    val getAllVisitsUseCase: GetAllVisitsUseCase by lazy {
        GetAllVisitsUseCase(visitRepository)
    }

    val getVisitsUseCase: GetVisitsUseCase by lazy {
        GetVisitsUseCase(visitRepository)
    }
}
