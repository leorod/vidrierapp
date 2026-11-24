package com.vidrierapp.di

import android.content.Context
import androidx.room.Room
import com.vidrierapp.data.local.database.MIGRATION_1_2
import com.vidrierapp.data.local.database.MIGRATION_2_3
import com.vidrierapp.data.local.database.MIGRATION_3_4
import com.vidrierapp.data.local.database.VidrierAppDatabase
import com.vidrierapp.data.location.FusedLocationProvider
import com.vidrierapp.data.remote.api.MockInterceptor
import com.vidrierapp.data.remote.api.VidrierAppApiService
import com.vidrierapp.data.storage.FilePhotoStorage
import com.vidrierapp.data.repository.RetrofitShopRepository
import com.vidrierapp.data.repository.RoomVisitRepository
import com.vidrierapp.data.repository.SharedPreferencesPassRepository
import com.vidrierapp.data.repository.SharedPreferencesReportedShopsRepository
import com.vidrierapp.domain.location.LocationProvider
import com.vidrierapp.domain.repository.CachedShopRepository
import com.vidrierapp.domain.repository.PassRepository
import com.vidrierapp.domain.repository.ReportedShopsRepository
import com.vidrierapp.domain.repository.ShopRepository
import com.vidrierapp.domain.repository.VisitRepository
import com.vidrierapp.domain.storage.PhotoStorage
import com.vidrierapp.domain.usecase.GetAllVisitsUseCase
import com.vidrierapp.domain.usecase.GetCachedShopsUseCase
import com.vidrierapp.domain.usecase.GetShopDetailUseCase
import com.vidrierapp.domain.usecase.GetShopsUseCase
import com.vidrierapp.domain.usecase.GetVisitsUseCase
import com.vidrierapp.domain.usecase.LogVisitUseCase
import com.vidrierapp.domain.usecase.ObservePassAccessUseCase
import com.vidrierapp.domain.usecase.ObserveReportedShopsUseCase
import com.vidrierapp.domain.usecase.PurchasePassUseCase
import com.vidrierapp.domain.usecase.ReportRentedUseCase
import com.vidrierapp.domain.usecase.StartTrialUseCase
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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }

    private val retrofitShopRepository: RetrofitShopRepository by lazy {
        RetrofitShopRepository(apiService, database.cachedLocalDao(), json, System::currentTimeMillis)
    }

    val shopRepository: ShopRepository get() = retrofitShopRepository

    val cachedShopRepository: CachedShopRepository get() = retrofitShopRepository

    val visitRepository: VisitRepository by lazy {
        RoomVisitRepository(database.visitDao())
    }

    val photoStorage: PhotoStorage by lazy {
        FilePhotoStorage(context.applicationContext)
    }

    val locationProvider: LocationProvider by lazy {
        FusedLocationProvider(context.applicationContext)
    }

    val clock: () -> Long = System::currentTimeMillis

    val passRepository: PassRepository by lazy {
        SharedPreferencesPassRepository(
            context.applicationContext.getSharedPreferences("pass_state", Context.MODE_PRIVATE)
        )
    }

    val observePassAccessUseCase: ObservePassAccessUseCase by lazy {
        ObservePassAccessUseCase(passRepository, clock)
    }

    val startTrialUseCase: StartTrialUseCase by lazy {
        StartTrialUseCase(passRepository, clock)
    }

    val purchasePassUseCase: PurchasePassUseCase by lazy {
        PurchasePassUseCase(passRepository, clock)
    }

    val getShopsUseCase: GetShopsUseCase by lazy {
        GetShopsUseCase(shopRepository)
    }

    val getCachedShopsUseCase: GetCachedShopsUseCase by lazy {
        GetCachedShopsUseCase(cachedShopRepository)
    }

    val reportedShopsRepository: ReportedShopsRepository by lazy {
        SharedPreferencesReportedShopsRepository(
            context.applicationContext.getSharedPreferences("reported_shops", Context.MODE_PRIVATE)
        )
    }

    val reportRentedUseCase: ReportRentedUseCase by lazy {
        ReportRentedUseCase(shopRepository, reportedShopsRepository)
    }

    val observeReportedShopsUseCase: ObserveReportedShopsUseCase by lazy {
        ObserveReportedShopsUseCase(reportedShopsRepository)
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
