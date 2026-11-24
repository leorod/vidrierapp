package com.vidrierapp.di

import android.content.Context
import androidx.room.Room
import com.vidrierapp.data.local.database.MIGRATION_1_2
import com.vidrierapp.data.local.database.MIGRATION_2_3
import com.vidrierapp.data.local.database.VidrierAppDatabase
import com.vidrierapp.data.location.FusedLocationProvider
import com.vidrierapp.data.remote.api.MockInterceptor
import com.vidrierapp.data.remote.api.VidrierAppApiService
import com.vidrierapp.data.repository.RetrofitShopRepository
import com.vidrierapp.data.repository.RoomVisitRepository
import com.vidrierapp.domain.location.LocationProvider
import com.vidrierapp.domain.repository.CachedShopRepository
import com.vidrierapp.domain.repository.ShopRepository
import com.vidrierapp.domain.repository.VisitRepository
import com.vidrierapp.domain.usecase.GetAllVisitsUseCase
import com.vidrierapp.domain.usecase.GetCachedShopsUseCase
import com.vidrierapp.domain.usecase.GetShopDetailUseCase
import com.vidrierapp.domain.usecase.GetShopsUseCase
import com.vidrierapp.domain.usecase.GetVisitsUseCase
import com.vidrierapp.domain.usecase.LogVisitUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { cachedShopRepository.purgeExpiredCache() }
        }
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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }

    private val retrofitShopRepository: RetrofitShopRepository by lazy {
        RetrofitShopRepository(apiService, database.cachedLocalDao(), json, System::currentTimeMillis)
    }

    val shopRepository: ShopRepository get() = retrofitShopRepository

    val cachedShopRepository: CachedShopRepository get() = retrofitShopRepository

    val visitRepository: VisitRepository by lazy {
        RoomVisitRepository(database.visitDao())
    }

    val locationProvider: LocationProvider by lazy {
        FusedLocationProvider(context.applicationContext)
    }

    val getShopsUseCase: GetShopsUseCase by lazy {
        GetShopsUseCase(shopRepository)
    }

    val getCachedShopsUseCase: GetCachedShopsUseCase by lazy {
        GetCachedShopsUseCase(cachedShopRepository)
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
