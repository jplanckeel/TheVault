package com.thevault.di

import com.thevault.config.ApiConfig
import com.thevault.data.remote.CatalogRemoteDataSource
import com.thevault.data.remote.GameRemoteDataSource
import com.thevault.data.remote.ItemSyncRemoteDataSource
import com.thevault.data.remote.PriceRemoteDataSource
import com.thevault.data.remote.TheVaultBoCatalogDataSource
import com.thevault.data.remote.TheVaultBoPriceDataSource
import com.thevault.data.remote.TheVaultBoRemoteDataSource
import com.thevault.data.remote.TheVaultBoSyncDataSource
import com.thevault.data.repository.AccessoryRepositoryImpl
import com.thevault.data.repository.AchievementRepositoryImpl
import com.thevault.data.repository.CatalogRepositoryImpl
import com.thevault.data.repository.CatalogSyncRepositoryImpl
import com.thevault.data.repository.ConsoleRepositoryImpl
import com.thevault.data.repository.GameRepositoryImpl
import com.thevault.data.repository.PriceRepositoryImpl
import com.thevault.data.repository.SettingsRepositoryImpl
import com.thevault.data.repository.ValueHistoryRepositoryImpl
import com.thevault.data.repository.VaultStatsRepositoryImpl
import com.thevault.data.sync.BackgroundSyncScheduler
import com.thevault.domain.repository.AccessoryRepository
import com.thevault.domain.repository.AchievementRepository
import com.thevault.domain.repository.CatalogRepository
import com.thevault.domain.repository.CatalogSyncRepository
import com.thevault.domain.repository.ConsoleRepository
import com.thevault.domain.repository.GameRepository
import com.thevault.domain.repository.PriceRepository
import com.thevault.domain.repository.SettingsRepository
import com.thevault.domain.repository.ValueHistoryRepository
import com.thevault.domain.repository.VaultStatsRepository
import com.thevault.domain.usecase.CsvService
import com.thevault.domain.usecase.DeleteGameUseCase
import com.thevault.domain.usecase.GetAccessoryByIdUseCase
import com.thevault.domain.usecase.GetConsoleByIdUseCase
import com.thevault.domain.usecase.GetGameByIdUseCase
import com.thevault.domain.usecase.GetPriceHistoryUseCase
import com.thevault.domain.usecase.MarkAchievementSeenUseCase
import com.thevault.domain.usecase.ObserveAccessoriesUseCase
import com.thevault.domain.usecase.ObserveAchievementUnlocksUseCase
import com.thevault.domain.usecase.ObserveConsolesUseCase
import com.thevault.domain.usecase.ObserveGamesUseCase
import com.thevault.domain.usecase.ObserveQuotesUseCase
import com.thevault.domain.usecase.ObserveSettingUseCase
import com.thevault.domain.usecase.ObserveSyncedIdsUseCase
import com.thevault.domain.usecase.ObserveValueHistoryUseCase
import com.thevault.domain.usecase.ObserveVaultStatsUseCase
import com.thevault.domain.usecase.RecordAchievementUnlocksUseCase
import com.thevault.domain.usecase.RecordValueSnapshotUseCase
import com.thevault.domain.usecase.RefreshCatalogUseCase
import com.thevault.domain.usecase.RefreshQuotesUseCase
import com.thevault.domain.usecase.SaveAccessoryUseCase
import com.thevault.domain.usecase.SaveConsoleUseCase
import com.thevault.domain.usecase.SaveGameUseCase
import com.thevault.domain.usecase.SearchGamesUseCase
import com.thevault.domain.usecase.SearchRemoteGamesUseCase
import com.thevault.domain.usecase.SetSettingUseCase
import com.thevault.domain.usecase.SyncPendingItemsUseCase
import com.thevault.domain.usecase.SyncScheduler
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val REQUEST_TIMEOUT_MS = 20_000L
private const val CONNECT_TIMEOUT_MS = 8_000L

/** Scope of the background work that outlives the screens (catalogue sync). */
private val AppScope = named("appScope")

/** Koin module of the shared (domain + data) layer. */
val sharedModule =
    module {
        // ─── Network ──────────────────────────────────────────────────────────────
        single {
            HttpClient {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            coerceInputValues = true
                        },
                    )
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MS
                    connectTimeoutMillis = CONNECT_TIMEOUT_MS
                }
            }
        }
        single<GameRemoteDataSource> { TheVaultBoRemoteDataSource(get(), ApiConfig.BASE_URL, ApiConfig.API_KEY) }
        single<ItemSyncRemoteDataSource> { TheVaultBoSyncDataSource(get(), ApiConfig.BASE_URL, ApiConfig.API_KEY) }
        single<PriceRemoteDataSource> { TheVaultBoPriceDataSource(get(), ApiConfig.BASE_URL, ApiConfig.API_KEY) }
        single<CatalogRemoteDataSource> { TheVaultBoCatalogDataSource(get(), ApiConfig.BASE_URL, ApiConfig.API_KEY) }
        single<CatalogRepository> { CatalogRepositoryImpl(get(), ApiConfig.BASE_URL.substringBefore("/api/")) }
        single<com.thevault.data.remote.VaultRemoteDataSource> {
            com.thevault.data.remote.TheVaultBoVaultDataSource(
                get(),
                ApiConfig.BASE_URL,
            )
        }

        // ─── Background work ─────────────────────────────────────────────────────
        single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        single<SyncScheduler> { BackgroundSyncScheduler(get(AppScope), get()) }

        // ─── Use Cases ────────────────────────────────────────────────────────────
        factory { ObserveGamesUseCase(get()) }
        factory { GetGameByIdUseCase(get()) }
        factory { SaveGameUseCase(get(), get(), get()) }
        factory { DeleteGameUseCase(get()) }
        factory { ObserveVaultStatsUseCase(get()) }
        factory { SearchGamesUseCase(get()) }
        factory { SearchRemoteGamesUseCase(get()) }
        factory { ObserveConsolesUseCase(get()) }
        factory { GetConsoleByIdUseCase(get()) }
        factory { ObserveAccessoriesUseCase(get()) }
        factory { GetAccessoryByIdUseCase(get()) }
        factory { SaveConsoleUseCase(get(), get(), get()) }
        factory { SaveAccessoryUseCase(get(), get(), get()) }
        factory {
            com.thevault.domain.usecase
                .DeleteConsoleUseCase(get())
        }
        factory {
            com.thevault.domain.usecase
                .DeleteAccessoryUseCase(get())
        }
        factory { CsvService(get(), get(), get()) }
        factory { RefreshCatalogUseCase(get(), get(), get(), get(), get()) }
        factory {
            com.thevault.domain.usecase
                .RefreshCollectionUseCase(get(), get(), get(), get(), get(), get())
        }
        factory { SyncPendingItemsUseCase(get(), get(), get(), get()) }
        factory { ObserveSyncedIdsUseCase(get()) }
        factory { ObserveQuotesUseCase(get()) }
        factory { RefreshQuotesUseCase(get(), get()) }
        factory { GetPriceHistoryUseCase(get()) }
        factory {
            com.thevault.domain.usecase
                .ObservePriceSnapshotsUseCase(get())
        }
        factory {
            com.thevault.domain.usecase
                .RecordImportedPricesUseCase(get())
        }
        factory {
            com.thevault.domain.usecase
                .GetImportedPricesUseCase(get())
        }
        factory { ObserveValueHistoryUseCase(get()) }
        factory { RecordValueSnapshotUseCase(get()) }
        factory { ObserveAchievementUnlocksUseCase(get()) }
        factory { RecordAchievementUnlocksUseCase(get()) }
        factory { MarkAchievementSeenUseCase(get()) }
        factory { ObserveSettingUseCase(get()) }
        factory { SetSettingUseCase(get()) }
        factory {
            com.thevault.domain.usecase
                .ObserveSetsUseCase(get())
        }
        factory {
            com.thevault.domain.usecase
                .SaveSetUseCase(get())
        }
        factory {
            com.thevault.domain.usecase
                .DeleteSetUseCase(get())
        }
        factory {
            com.thevault.domain.usecase
                .EditSetEntryUseCase(get())
        }

        factory {
            com.thevault.domain.usecase
                .AccountUseCase(get())
        }
        factory {
            com.thevault.domain.usecase
                .SignInUseCase(get())
        }
        factory {
            com.thevault.domain.usecase
                .BackupVaultUseCase(get(), get(), get(), get(), get())
        }
        factory {
            com.thevault.domain.usecase
                .RestoreVaultUseCase(get(), get(), get(), get(), get())
        }
        factory {
            com.thevault.domain.usecase
                .BackupVersionsUseCase(get())
        }
        factory {
            com.thevault.domain.usecase
                .ShareLinksUseCase(get())
        }
        factory {
            com.thevault.domain.usecase
                .EditShareUseCase(get())
        }

        // ─── Repositories ──────────────────────────────────────────────────────────
        single<VaultStatsRepository> { VaultStatsRepositoryImpl(get()) }
        single<GameRepository> { GameRepositoryImpl(get()) }
        single<ConsoleRepository> { ConsoleRepositoryImpl(get()) }
        single<AccessoryRepository> { AccessoryRepositoryImpl(get()) }
        single<PriceRepository> { PriceRepositoryImpl(get(), get()) }
        single<ValueHistoryRepository> { ValueHistoryRepositoryImpl(get()) }
        single<AchievementRepository> { AchievementRepositoryImpl(get()) }
        single<SettingsRepository> { SettingsRepositoryImpl(get()) }
        single<CatalogSyncRepository> { CatalogSyncRepositoryImpl(get(), get()) }
        single<com.thevault.domain.repository.VaultRepository> {
            com.thevault.data.repository
                .VaultRepositoryImpl(get(), get(), ApiConfig.BASE_URL.substringBefore("/api/"))
        }
        single<com.thevault.domain.repository.SetRepository> {
            com.thevault.data.repository
                .SetRepositoryImpl(get())
        }
    }
