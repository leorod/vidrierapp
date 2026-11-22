# Project & Build Configuration
- Module & Namespace: com.vidrierapp
- SDK Levels: minSdk = 24 (Android 7.0, per RNF-13), compileSdk = 37, targetSdk = 37
- Gradle & Plugins: AGP 9.0.1, Gradle 9.1.0, Kotlin 2.3.20, KSP 2.3.14
- Core Dependencies in libs.versions.toml:
  - Jetpack Compose + Material 3 (including Material Icons Extended)
  - Navigation 3 (navigation3-runtime, navigation3-ui)
  - Room 2.8.5 (room-runtime, room-ktx, room-compiler via KSP)
  - Retrofit 3.0.0 + OkHttp 5.5.1 + Kotlinx Serialization

# Clean Architecture & MVVM Directory Structure

```
app/src/main/kotlin/com/vidrierapp/
├── VidrierAppApplication.kt                  # Application class exposing AppContainer
├── MainActivity.kt                           # Root Activity with Edge-to-Edge & Nav entry
├── di/
│   └── AppContainer.kt                       # Service Locator / DI container (Room, Retrofit, UseCases)
├── domain/                                   # Pure Kotlin domain logic & abstractions
│   ├── model/
│   │   ├── Shop.kt                           # Commercial property domain entity
│   │   ├── Category.kt                       # Business category (Rubro)
│   │   ├── Vote.kt                           # Aggregated vote metrics
│   │   ├── Visit.kt                          # Field visit log entity
│   │   └── ConsumptionFrequency.kt          # Frequency enum (Diaria, Semanal, etc.)
│   ├── repository/
│   │   ├── ShopRepository.kt                 # Remote shop & demand data abstraction
│   │   └── VisitRepository.kt                # Local visit log abstraction
│   └── usecase/
│       ├── GetShopsUseCase.kt
│       ├── GetShopDetailUseCase.kt
│       ├── LogVisitUseCase.kt
│       └── GetVisitsUseCase.kt
├── data/                                     # Data layer implementations & mapping
│   ├── remote/
│   │   ├── api/
│   │   │   └── VidrierAppApiService.kt       # Retrofit service interface (/api/v1/shops)
│   │   └── dto/
│   │       ├── ShopDto.kt                    # Network payload DTO
│   │       └── VoteDto.kt
│   ├── local/
│   │   ├── database/
│   │   │   └── VidrierAppDatabase.kt         # Room Database
│   │   ├── dao/
│   │   │   └── VisitDao.kt                   # Room DAO for offline-first visit log (RNF-08)
│   │   └── entity/
│   │       └── VisitEntity.kt                # Room entity for local persistence
│   ├── mapper/
│   │   ├── ShopMapper.kt                     # DTO <-> Domain mappers
│   │   └── VisitMapper.kt                    # Entity <-> Domain mappers
│   └── repository/
│       ├── RetrofitShopRepository.kt         # ShopRepository implementation
│       └── RoomVisitRepository.kt             # VisitRepository implementation
└── presentation/                             # Presentation layer (Compose UI + ViewModels + UDF)
    ├── navigation/
    │   ├── NavDestinations.kt                # Navigation 3 destinations (Map, Detail, Visit)
    │   └── AppNavigation.kt                  # Navigation router skeleton
    ├── map/
    │   ├── MapScreen.kt                      # Interactive map screen scaffold (RF01, RF02)
    │   ├── MapViewModel.kt                   # StateFlow-driven ViewModel
    │   └── MapUiState.kt                     # Immutable UI State
    ├── detail/
    │   ├── ShopDetailScreen.kt               # Shop details & demand breakdown scaffold (RF03)
    │   ├── ShopDetailViewModel.kt
    │   └── ShopDetailUiState.kt
    ├── visit/
    │   ├── LogVisitScreen.kt                 # Field inspection visit log scaffold (RF04)
    │   ├── VisitViewModel.kt
    │   └── VisitUiState.kt
    └── theme/
        ├── Color.kt                          # Material 3 colors (contrast >= 4.5:1 per RNF-03)
        ├── Theme.kt                          # AppTheme setup
        └── Type.kt                           # Dynamic typography definitions (RNF-04)
```

# Resource Management & Manifest
- strings.xml: Centralized resource keys covering all screens, TalkBack accessibility labels, dialogs, and error messages (satisfying RNF-11 string interning).
- AndroidManifest.xml: Configured with foreground location (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), CAMERA, INTERNET, and linked to VidrierAppApplication.
The project compiles cleanly (./gradlew assembleDebug exits with code 0).